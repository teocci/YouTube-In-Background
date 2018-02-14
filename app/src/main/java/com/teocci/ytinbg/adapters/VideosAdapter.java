package com.teocci.ytinbg.adapters;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.interfaces.ItemTouchListener;
import com.teocci.ytinbg.interfaces.OnLoadMoreListener;
import com.teocci.ytinbg.interfaces.OnStartDragListener;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.ui.DownloadActivity;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Custom ArrayAdapter which enables setup of a videoList view row views
 * Created by teocci on 8.2.16..
 */
public class VideosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ItemTouchListener
{
    private static final String TAG = VideosAdapter.class.getSimpleName();

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADER = 1;

    private Activity context;
    private final List<YouTubeVideo> videoList;
    //    private List<String> favorites;
    private boolean isFavoriteList;

    private AdapterView.OnItemClickListener onItemClickListener;
    private OnStartDragListener onStartDragListener;
    private OnLoadMoreListener onLoadMoreListener;

    public VideosAdapter(Activity context, boolean isFavoriteList)
    {
        this.videoList = new ArrayList<>();
//        this.favorites = new ArrayList<>();
        this.context = context;
        this.isFavoriteList = isFavoriteList;
        this.onItemClickListener = null;
        this.onStartDragListener = null;
    }

    public VideosAdapter(Activity context, List<YouTubeVideo> videoList, boolean isFavoriteList)
    {
        this.videoList = videoList;
//        this.favorites = new ArrayList<>();
        this.context = context;
        this.isFavoriteList = isFavoriteList;
        this.onItemClickListener = null;
        this.onStartDragListener = null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup container, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        if (viewType == VIEW_TYPE_ITEM) {
            View root = inflater.inflate(R.layout.video_item, container, false);

            return new VideoViewHolder(root, this);
        } else if (viewType == VIEW_TYPE_LOADER) {
            View root = inflater.inflate(R.layout.loading_item, container, false);

            return new LoaderViewHolder(root);
        }

        return null;

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder itemHolder, int position)
    {
        if (itemHolder instanceof VideoViewHolder) {
            final YouTubeVideo youTubeVideo = videoList.get(position);

            final VideoViewHolder videoViewHolder = (VideoViewHolder) itemHolder;
            Picasso.with(context)
                    .load(youTubeVideo.getThumbnailURL())
                    .centerCrop()
                    .fit()
                    .into(videoViewHolder.thumbnail);
            videoViewHolder.title.setText(youTubeVideo.getTitle());
            videoViewHolder.duration.setText(youTubeVideo.getDuration());
            videoViewHolder.viewCount.setText(Utils.formatViewCount(youTubeVideo.getViewCount()));

            //set checked if exists in database
            boolean isFavorite = YouTubeSqlDb
                    .getInstance()
                    .videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE)
                    .checkIfExists(youTubeVideo.getId());
//            if (isFavorite) favorites.add(youTubeVideo.getId());

            videoViewHolder.checkBoxFavorite.setChecked(isFavorite);

            videoViewHolder.checkBoxFavorite.setOnCheckedChangeListener((btn, isChecked) -> {
//                    if (!isChecked) favorites.remove(youTubeVideo.getId());
            });

            videoViewHolder.checkBoxFavorite.setOnClickListener(v -> {
                final boolean isChecked = ((CheckBox) v).isChecked();
                if (isChecked) {
                    YouTubeSqlDb
                            .getInstance()
                            .videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE)
                            .create(youTubeVideo);
                } else {
                    YouTubeSqlDb
                            .getInstance()
                            .videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE)
                            .delete(youTubeVideo.getId());
                    if (isFavoriteList) {
                        removeVideo(videoViewHolder.getAdapterPosition());
                    }
                }

            });

            videoViewHolder.imageButtonShare.setOnClickListener(v -> doShareLink(youTubeVideo.getTitle(), youTubeVideo.getId()));

            videoViewHolder.imageButtonDownload.setOnClickListener(v -> doDownloadVideo(youTubeVideo.getId()));
        } else if (itemHolder instanceof LoaderViewHolder) {
            LoaderViewHolder loadingViewHolder = (LoaderViewHolder) itemHolder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    @Override
    public long getItemId(int position)
    {
        if (position >= videoList.size()) return -1;
        return videoList.get(position).hashCode();
    }

    @Override
    public int getItemViewType(int position)
    {
        if (position >= videoList.size()) return -1;
        return videoList.get(position) == null ? VIEW_TYPE_LOADER : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount()
    {
        return videoList == null ? 0 : videoList.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition)
    {
        Collections.swap(videoList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position)
    {
        if (position >= videoList.size()) return;
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED)
                .delete(videoList.get(position).getId());
        videoList.remove(position);
        notifyItemRemoved(position);
    }

    public List<YouTubeVideo> getYouTubeVideos()
    {
        return videoList;
    }

    public YouTubeVideo getYouTubeVideo(int position)
    {
        if (position >= videoList.size()) return null;
        return videoList.get(position);
    }

    /**
     * A common adapter UPDATE mechanism. As with VideosAdapter,
     * calling notifyDataSetChanged() will trigger the RecyclerView to update
     * the view. However, this method will not trigger any of the RecyclerView
     * animation features.
     */
    public void setYouTubeVideos(List<YouTubeVideo> youTubeVideos)
    {
        clearYouTubeVideos();
        addYouTubeVideos(youTubeVideos);
    }

    public void addYouTubeVideo(YouTubeVideo youTubeVideo, int position)
    {
        if (position > videoList.size()) return;

        videoList.add(youTubeVideo);
        notifyItemInserted(position);
    }

    /**
     * Inserting a new item at the head of the list. This uses a specialized
     * RecyclerView method, notifyItemInserted(), to trigger any enabled item
     * animations in addition to updating the view.
     */
    public void addMoreYouTubeVideos(List<YouTubeVideo> youTubeVideos)
    {
        int position = videoList.size() + 1;
        videoList.addAll(youTubeVideos);
        notifyItemChanged(position, videoList);
//        Log.e(TAG, "Adding " + youTubeVideos.size() + " more elements. Total: " + videoList.size());
    }

    /**
     * A common adapter ADD mechanism. As with VideosAdapter,
     * calling notifyDataSetChanged() will trigger the RecyclerView to update
     * the view. However, this method will not trigger any of the RecyclerView
     * animation features.
     */
    public void addYouTubeVideos(List<YouTubeVideo> youTubeVideos)
    {
        videoList.addAll(youTubeVideos);
        notifyDataSetChanged();
    }

    /**
     * A common adapter reset mechanism. As with VideosAdapter,
     * calling notifyDataSetChanged() will trigger the RecyclerView to update
     * the view. However, this method will not trigger any of the RecyclerView
     * animation features.
     */
    public void clearYouTubeVideos()
    {
        videoList.clear();
        notifyDataSetChanged();
    }

    /**
     * Adds a null object to the last position, so the getItemViewType()
     * method will check if the new added object is null then the progressbar
     * will be displayed
     */
    public void addLoader()
    {
        videoList.add(null);
        notifyItemInserted(videoList.size() - 1);
    }

    /**
     * Removes the progressbar added by addLoader()
     */
    public void removeLoader()
    {
        videoList.remove(videoList.size() - 1);
        notifyItemRemoved(videoList.size());
    }

    /**
     * Inserting a new item at the head of the list. This uses a specialized
     * RecyclerView method, notifyItemRemoved(), to trigger any enabled item
     * animations in addition to updating the view.
     */
    public void removeVideo(int position)
    {
        if (position >= videoList.size()) return;

        videoList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, videoList.size());
    }

    public void swapItems(int positionA, int positionB)
    {
        if (positionA > videoList.size()) return;
        if (positionB > videoList.size()) return;

        YouTubeVideo firstItem = videoList.get(positionA);

        videoList.set(positionA, videoList.get(positionB));
        videoList.set(positionB, firstItem);

        notifyDataSetChanged();
    }

    public View getUndoView(int i, @Nullable View convertView, @NonNull ViewGroup viewGroup)
    {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.undo_row, viewGroup, false);
        }
        return view;
    }

    public View getUndoClickView(@NonNull View view)
    {
        return view.findViewById(R.id.button_undo_row);
    }

    private void doShareLink(String text, String link)
    {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        Intent chooserIntent = Intent.createChooser(shareIntent, context.getResources().getString(R.string.share_image_button));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.app_name));
            shareIntent.putExtra(Intent.EXTRA_TEXT, text + " " + Config.YT_PREFIX_LINK + link);
        } else {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.app_name));
            shareIntent.putExtra(Intent.EXTRA_TEXT, Config.YT_PREFIX_LINK + link);
        }

//        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }

    private void doDownloadVideo(String link)
    {
        Intent downloadIntent = new Intent(context, DownloadActivity.class);
        downloadIntent.putExtra(Config.YOUTUBE_LINK, Config.YT_PREFIX_LINK + link);
        context.startActivity(downloadIntent);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener)
    {
        this.onItemClickListener = onItemClickListener;
    }

    private void onItemHolderClick(VideoViewHolder itemHolder)
    {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(
                    null,
                    itemHolder.itemView,
                    itemHolder.getAdapterPosition(),
                    itemHolder.getItemId()
            );
        }
    }

    public void setOnStartDragListener(OnStartDragListener onStartDragListener)
    {
        this.onStartDragListener = onStartDragListener;
    }

    private void onItemHolderStartDrag(VideoViewHolder itemHolder)
    {
        if (onStartDragListener != null) {
            onStartDragListener.onStartDrag(itemHolder);
        }
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener)
    {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public void removeOnLoadMoreListener()
    {
        if (onLoadMoreListener != null) {
            onLoadMoreListener = null;
        }
    }

    public void onItemHolderOnLoadMore()
    {
        if (onLoadMoreListener != null) {
            onLoadMoreListener.onLoadMore();
        }
    }

    private void printVideoList()
    {
        Log.e(TAG, "videoList: ");
        for (YouTubeVideo video : videoList) {
            Log.e(TAG, video.getId());
        }
    }

    protected static class LoaderViewHolder extends RecyclerView.ViewHolder
    {
        private ProgressBar progressBar;

        private LoaderViewHolder(View convertView)
        {
            super(convertView);
            progressBar = (ProgressBar) convertView.findViewById(R.id.progress_bar);
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    protected static class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnTouchListener
    {
        public ImageView thumbnail;
        public TextView title;
        public TextView duration;
        public TextView viewCount;
        public CheckBox checkBoxFavorite;
        public ImageButton imageButtonDownload;
        public ImageButton imageButtonShare;

        private VideosAdapter videosAdapter;

        public VideoViewHolder(View convertView, VideosAdapter videosAdapter)
        {
            super(convertView);
            convertView.setOnClickListener(this);

            this.videosAdapter = videosAdapter;

            thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            title = (TextView) convertView.findViewById(R.id.video_title);
            duration = (TextView) convertView.findViewById(R.id.video_duration);
            viewCount = (TextView) convertView.findViewById(R.id.views_number);
            checkBoxFavorite = (CheckBox) convertView.findViewById(R.id.image_button_favorite);
            imageButtonDownload = (ImageButton) convertView.findViewById(R.id.image_button_download);
            imageButtonShare = (ImageButton) convertView.findViewById(R.id.image_button_share);
        }

        @Override
        public void onClick(View v)
        {
            videosAdapter.onItemHolderClick(this);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            // Start a drag whenever the handle view it touched
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                videosAdapter.onItemHolderStartDrag(this);
            }
            return false;
        }
    }
}
