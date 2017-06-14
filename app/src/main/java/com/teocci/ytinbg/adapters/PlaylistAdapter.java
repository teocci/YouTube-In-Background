package com.teocci.ytinbg.adapters;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.interfaces.ItemTouchListener;
import com.teocci.ytinbg.interfaces.OnStartDragListener;
import com.teocci.ytinbg.model.YouTubePlaylist;
import com.teocci.ytinbg.ui.DownloadActivity;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-May-16
 */

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>
        implements ItemTouchListener
{
    private static final String TAG = PlaylistAdapter.class.getSimpleName();

    private Activity context;
    private final List<YouTubePlaylist> ytPlaylists;

    private AdapterView.OnItemClickListener onItemClickListener;
    private OnStartDragListener onStartDragListener;

    public PlaylistAdapter(Activity context)
    {
        this.ytPlaylists = new ArrayList<>();
        this.context = context;
        this.onItemClickListener = null;
        this.onStartDragListener = null;
    }

    public PlaylistAdapter(Activity context, List<YouTubePlaylist> ytPlaylists)
    {
        this.ytPlaylists = ytPlaylists;
        this.context = context;
        this.onItemClickListener = null;
        this.onStartDragListener = null;
    }

    @Override
    public PlaylistAdapter.PlaylistViewHolder onCreateViewHolder(ViewGroup container, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View root = inflater.inflate(R.layout.video_item, container, false);

        return new PlaylistAdapter.PlaylistViewHolder(root, this);
    }

    @Override
    public void onBindViewHolder(final PlaylistAdapter.PlaylistViewHolder playlistViewHolder, int position)
    {
        final YouTubePlaylist ytPlaylist = ytPlaylists.get(position);

        Picasso.with(context)
                .load(ytPlaylist.getThumbnailURL())
                .centerCrop()
                .fit()
                .into(playlistViewHolder.thumbnail);
        playlistViewHolder.title.setText(ytPlaylist.getTitle());
        playlistViewHolder.videoCount.setText(Long.toString(ytPlaylist.getNumberOfVideos()));
        playlistViewHolder.privacy.setText(ytPlaylist.getPrivacy());
    }

    @Override
    public long getItemId(int position)
    {
        if (position >= ytPlaylists.size()) return -1;
        return ytPlaylists.get(position).hashCode();
    }

    @Override
    public int getItemCount()
    {
        return ytPlaylists.size();
    }

    public YouTubePlaylist getYouTubePlaylist(int position)
    {
        if (position >= ytPlaylists.size()) return null;
        return ytPlaylists.get(position);
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

    @Override
    public boolean onItemMove(int fromPosition, int toPosition)
    {
        Collections.swap(ytPlaylists, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position)
    {
        if (position >= ytPlaylists.size()) return;
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED)
                .delete(ytPlaylists.get(position).getId());
        ytPlaylists.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * A common adapter UPDATE mechanism. As with PlaylistAdapter,
     * calling notifyDataSetChanged() will trigger the RecyclerView to update
     * the view. However, this method will not trigger any of the RecyclerView
     * animation features.
     */
    public void setYouTubePlaylists(List<YouTubePlaylist> ytPlaylists)
    {
        this.ytPlaylists.clear();
        this.ytPlaylists.addAll(ytPlaylists);
        notifyDataSetChanged();
    }

    public void removeYouTubePlaylistById(String playlistId) {
        for (YouTubePlaylist playlist : this.ytPlaylists) {
            if (playlist.getId().equals(playlistId)) {
                this.ytPlaylists.remove(playlist);
                break;
            }
        }

        notifyDataSetChanged();
    }

    /**
     * A common adapter reset mechanism. As with PlaylistAdapter,
     * calling notifyDataSetChanged() will trigger the RecyclerView to update
     * the view. However, this method will not trigger any of the RecyclerView
     * animation features.
     */
    public void clearYouTubePlaylists()
    {
        ytPlaylists.clear();
        notifyDataSetChanged();
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

    private void onItemHolderClick(PlaylistAdapter.PlaylistViewHolder itemHolder)
    {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(null, itemHolder.itemView,
                    itemHolder.getAdapterPosition(), itemHolder.getItemId());
        }
    }

    public void setOnStartDragListener(OnStartDragListener onStartDragListener)
    {
        this.onStartDragListener = onStartDragListener;
    }

    private void onItemHolderStartDrag(PlaylistAdapter.PlaylistViewHolder itemHolder)
    {
        if (onStartDragListener != null) {
            onStartDragListener.onStartDrag(itemHolder);
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class PlaylistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnTouchListener
    {
        public ImageView thumbnail;
        public TextView title;
        public TextView videoCount;
        public TextView privacy;

        private PlaylistAdapter playlistAdapter;

        public PlaylistViewHolder(View convertView, PlaylistAdapter playlistAdapter)
        {
            super(convertView);
            convertView.setOnClickListener(this);

            this.playlistAdapter = playlistAdapter;

            thumbnail = (ImageView) convertView.findViewById(R.id.playlist_thumbnail);
            title = (TextView) convertView.findViewById(R.id.playlist_title);
            videoCount = (TextView) convertView.findViewById(R.id.playlist_video_count);
            privacy = (TextView) convertView.findViewById(R.id.playlist_privacy);
        }

        @Override
        public void onClick(View v)
        {
            playlistAdapter.onItemHolderClick(this);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            // Start a drag whenever the handle view it touched
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                playlistAdapter.onItemHolderStartDrag(this);
            }
            return false;
        }
    }
}
