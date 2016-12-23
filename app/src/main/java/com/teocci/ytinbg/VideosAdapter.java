package com.teocci.ytinbg;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.nhaarman.listviewanimations.util.Swappable;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.utils.LogHelper;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Custom ArrayAdapter which enables setup of a list view row views
 * Created by teocci on 8.2.16..
 */
public class VideosAdapter extends ArrayAdapter<YouTubeVideo> implements Swappable, UndoAdapter
{
    private static final String TAG = LogHelper.makeLogTag(VideosAdapter.class);

    private Activity context;
    private final List<YouTubeVideo> list;
    private boolean[] itemChecked;
    private boolean isFavoriteList;

    public VideosAdapter(Activity context, List<YouTubeVideo> list, boolean isFavoriteList)
    {
        super(context, R.layout.video_item, list);
        this.list = list;
        this.context = context;
        this.itemChecked = new boolean[50];
        this.isFavoriteList = isFavoriteList;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent)
    {
        if (convertView == null) {
            convertView = context.getLayoutInflater().inflate(R.layout.video_item, parent, false);
        }
        ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
        TextView title = (TextView) convertView.findViewById(R.id.video_title);
        TextView duration = (TextView) convertView.findViewById(R.id.video_duration);
        TextView viewCount = (TextView) convertView.findViewById(R.id.views_number);
        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.image_button);

        final YouTubeVideo searchResult = list.get(position);

        Picasso.with(context).load(searchResult.getThumbnailURL()).into(thumbnail);
        title.setText(searchResult.getTitle());
        duration.setText(searchResult.getDuration());
        viewCount.setText(searchResult.getViewCount());

        //set checked if exists in database
        itemChecked[position] = YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE
                .FAVORITE).checkIfExists(searchResult.getId());
        checkBox.setChecked(itemChecked[position]);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton btn, boolean isChecked)
            {
                itemChecked[position] = isChecked;
            }
        });

        checkBox.setOnClickListener(new View.OnClickListener()
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
                        list.remove(position);
                        notifyDataSetChanged();
                    }
                }
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

        list.set(i, getItem(i1));
        list.set(i1, firstItem);

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
}
