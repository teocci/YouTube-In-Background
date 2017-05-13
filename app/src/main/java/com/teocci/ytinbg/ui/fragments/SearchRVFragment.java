package com.teocci.ytinbg.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.teocci.ytinbg.BackgroundAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.YouTubeSearch;
import com.teocci.ytinbg.adapters.VideosAdapter;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.interfaces.YouTubeVideoReceiver;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.ui.decoration.DividerDecoration;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;
import com.teocci.ytinbg.utils.NetworkConf;

import java.util.ArrayList;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Apr/11
 */

public class SearchRVFragment extends RecyclerFragment implements YouTubeVideoReceiver
{
    private static final String TAG = LogHelper.makeLogTag(SearchRVFragment.class);

    private Handler handler;
    private YouTubeSearch youTubeSearch;
    private ProgressBar loadingProgressBar;
    private NetworkConf networkConf;

    public static SearchRVFragment newInstance()
    {
        SearchRVFragment fragment = new SearchRVFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
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
        return new VideosAdapter(getActivity(), false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        // Check network connectivity
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        Toast.makeText(
                getContext(),
                getResources().getString(R.string.toast_message_loading),
                Toast.LENGTH_SHORT
        ).show();

        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED)
                .create(videosAdapter.getYouTubeVideos(position));

        Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE, Config.YOUTUBE_MEDIA_TYPE_VIDEO);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, videosAdapter.getYouTubeVideos(position));
        getActivity().startService(serviceIntent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler();
        networkConf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        loadingProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean visible)
    {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
            // Only manually call onResume if fragment is already visible
            // Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!getUserVisibleHint()) {
            // Do nothing for now
        }

        youTubeSearch = new YouTubeSearch(getActivity(), this);
        youTubeSearch.setYouTubeVideoReceiver(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
//        videosFoundListView = (DynamicListView) getListView();
    }

    /**
     * Search for query on youTube by using YouTube Data API V3
     *
     * @param query the keyword for the search
     */
    public void searchQuery(String query)
    {
        // Check network connectivity
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        youTubeSearch.searchVideos(query);
    }

    /**
     * Called when video items are received
     *
     * @param youTubeVideos - videos to be shown in list view
     */
    @Override
    public void onVideosReceived(final ArrayList<YouTubeVideo> youTubeVideos)
    {
        if (videosAdapter != null) {
            Log.e(TAG, youTubeVideos.toString());
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    videosAdapter.setYouTubeVideos(youTubeVideos);
                }

            });
        }
//        recyclerView.smoothScrollToPosition(0);
//        searchResultsList.clear();
//        scrollResultsList.clear();
//        scrollResultsList.addAll(youTubeVideos);

        handler.post(new Runnable()
        {
            public void run()
            {
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * Called when playlist cannot be found
     * NOT USED in this fragment
     *
     * @param playlistId the playlist ID
     * @param errorCode  the error code obtained
     */
    @Override
    public void onPlaylistNotFound(String playlistId, int errorCode) { }

}