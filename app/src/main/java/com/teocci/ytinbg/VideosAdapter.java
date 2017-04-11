package com.teocci.ytinbg;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.nhaarman.listviewanimations.util.Swappable;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.ui.DownloadActivity;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Custom ArrayAdapter which enables setup of a videoList view row views
 * Created by teocci on 8.2.16..
 */
public class VideosAdapter extends ArrayAdapter<YouTubeVideo> implements Swappable, UndoAdapter
{
    private static final String TAG = LogHelper.makeLogTag(VideosAdapter.class);

    private Activity context;
    private final List<YouTubeVideo> videoList;
    private boolean[] videoChecked;
    private boolean isFavoriteList;

    public VideosAdapter(Activity context, List<YouTubeVideo> videoList, boolean isFavoriteList)
    {
        super(context, R.layout.video_item, videoList);
        this.videoList = videoList;
        this.context = context;
        this.videoChecked = new boolean[50];
        this.isFavoriteList = isFavoriteList;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent)
    {
        VideoViewHolder videoViewHolder;
        if (convertView == null) {
            convertView = context.getLayoutInflater().inflate(R.layout.video_item, parent, false);
            videoViewHolder = new VideoViewHolder(convertView);
            convertView.setTag(videoViewHolder);
        }else {
            videoViewHolder = (VideoViewHolder) convertView.getTag();
        }

        final YouTubeVideo searchResult = videoList.get(position);

        Picasso.with(context)
                .load(searchResult.getThumbnailURL())
                .centerCrop()
                .fit()
                .into(videoViewHolder.thumbnail);
        videoViewHolder.title.setText(searchResult.getTitle());
        videoViewHolder.duration.setText(searchResult.getDuration());
        videoViewHolder.viewCount.setText(searchResult.getViewCount());

        //set checked if exists in database
        videoChecked[position] = YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE
                .FAVORITE).checkIfExists(searchResult.getId());
        videoViewHolder.checkBoxFavorite.setChecked(videoChecked[position]);

        videoViewHolder.checkBoxFavorite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton btn, boolean isChecked)
            {
                videoChecked[position] = isChecked;
            }
        });

        videoViewHolder.checkBoxFavorite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (((CheckBox) v).isChecked()) {
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).create
                            (searchResult);
                } else {
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).delete
                            (searchResult.getId());
                    if (isFavoriteList) {
                        videoList.remove(position);
                        notifyDataSetChanged();
                    }
                }
            }
        });

        videoViewHolder.imageButtonShare.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                doShareLink(searchResult.getTitle(), searchResult.getId());
            }
        });

        videoViewHolder.imageButtonDownload.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                doDownloadVideo(searchResult.getId());
            }
        });

        return convertView;
    }

    @Override
    public long getItemId(int i)
    {
        return getItem(i).hashCode();
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public void swapItems(int i, int i1)
    {
        YouTubeVideo firstItem = getItem(i);

        videoList.set(i, getItem(i1));
        videoList.set(i1, firstItem);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getUndoView(int i, @Nullable View convertView, @NonNull ViewGroup viewGroup)
    {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.undo_row, viewGroup, false);
        }
        return view;
    }

    @NonNull
    @Override
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
            shareIntent.putExtra(Intent.EXTRA_TEXT, text + " https://youtu.be/" + link);
        } else {

            shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.app_name));
            shareIntent.putExtra(Intent.EXTRA_TEXT, "https://youtu.be/" + link);
        }

//        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }

    private void doDownloadVideo(String link)
    {
        Intent downloadIntent = new Intent(context, DownloadActivity.class);
        downloadIntent.putExtra(Config.YOUTUBE_LINK, "https://youtu.be/" + link);
        context.startActivity(downloadIntent);
    }



    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnail;
        public TextView title;
        public TextView duration;
        public TextView viewCount;
        public CheckBox checkBoxFavorite;
        public ImageButton imageButtonDownload;
        public ImageButton imageButtonShare;

        public VideoViewHolder(View convertView) {
            super(convertView);

            thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            title = (TextView) convertView.findViewById(R.id.video_title);
            duration = (TextView) convertView.findViewById(R.id.video_duration);
            viewCount = (TextView) convertView.findViewById(R.id.views_number);
            checkBoxFavorite = (CheckBox) convertView.findViewById(R.id.image_button_favorite);
            imageButtonDownload = (ImageButton) convertView.findViewById(R.id.image_button_download);
            imageButtonShare = (ImageButton) convertView.findViewById(R.id.image_button_share);
        }
    }
}
