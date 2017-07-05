package com.teocci.ytinbg.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.teocci.ytinbg.BackgroundExoAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.adapters.VideosAdapter;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.ui.decoration.DividerDecoration;
import com.teocci.ytinbg.utils.Config;

import java.util.ArrayList;
import java.util.List;

import static com.teocci.ytinbg.utils.Config.ACTION_PLAY;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Mar-21
 */
public class FavoritesFragment extends RecyclerFragment
{
    private static final String TAG = FavoritesFragment.class.getSimpleName();

    private List<YouTubeVideo> favoriteVideos;

    public static FavoritesFragment newInstance()
    {
        FavoritesFragment fragment = new FavoritesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FavoritesFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        favoriteVideos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_recently_watched, container, false);
        TextView fragmentListTitle = (TextView) rootView.findViewById(R.id.text_view_title);
        fragmentListTitle.setText(getResources().getString(R.string.fragment_title_favorite));

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recently_played);
        recyclerView.setLayoutManager(getLayoutManager());
        recyclerView.addItemDecoration(getItemDecoration());

        recyclerView.getItemAnimator().setAddDuration(500);
        recyclerView.getItemAnimator().setChangeDuration(500);
        recyclerView.getItemAnimator().setMoveDuration(500);
        recyclerView.getItemAnimator().setRemoveDuration(500);

        videoListAdapter = getAdapter();
        videoListAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(videoListAdapter);

        return rootView;
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager()
    {
        return new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    }

    @Override
    protected RecyclerView.ItemDecoration getItemDecoration()
    {
        //We must draw dividers ourselves if we want them in a list
        return new DividerDecoration(getActivity());
    }

    @Override
    protected VideosAdapter getAdapter()
    {
        return new VideosAdapter(getActivity(), true);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!getUserVisibleHint()) {
            //do nothing for now
        }
        favoriteVideos.clear();
        favoriteVideos.addAll(
                YouTubeSqlDb
                        .getInstance()
                        .videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE)
                        .readAll()
        );

        if (videoListAdapter != null) {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    videoListAdapter.setYouTubeVideos(favoriteVideos);
                }

            });
        }
    }

    @Override
    public void setUserVisibleHint(boolean visible)
    {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
//            LogHelper.d(TAG, "RecentlyWatchedFragment visible and resumed");
            // Only manually call onResume if fragment is already visible
            // Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        // Check network connectivity
        if (!networkConf.isNetworkAvailable(getActivity())) {
            networkConf.createNetErrorDialog();
            return;
        }

        Toast.makeText(
                getContext(),
                getResources().getString(R.string.toast_message_loading),
                Toast.LENGTH_SHORT
        ).show();

        favoriteVideos.clear();
        favoriteVideos.addAll(
                YouTubeSqlDb
                        .getInstance()
                        .videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE)
                        .readAll()
        );

        // Adds items in the recently watched list
        YouTubeSqlDb
                .getInstance()
                .videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED)
                .create(videoListAdapter.getYouTubeVideo(position));

        Intent serviceIntent = new Intent(getContext(), BackgroundExoAudioService.class);
        serviceIntent.setAction(ACTION_PLAY);
        serviceIntent.putExtra(Config.KEY_YOUTUBE_TYPE, Config.YOUTUBE_MEDIA_TYPE_PLAYLIST);
        serviceIntent.putExtra(Config.KEY_YOUTUBE_TYPE_PLAYLIST, (ArrayList)favoriteVideos);
        serviceIntent.putExtra(Config.KEY_YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, position);
        getActivity().startService(serviceIntent);
    }

    /**
     * Clears FavoriteList played list items
     */
    public void clearFavoritesList()
    {
        favoriteVideos.clear();
        videoListAdapter.notifyDataSetChanged();
    }
}
