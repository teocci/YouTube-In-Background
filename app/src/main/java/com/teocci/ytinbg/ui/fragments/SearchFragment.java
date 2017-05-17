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

import com.google.api.services.youtube.YouTube;
import com.teocci.ytinbg.BackgroundAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.YouTubeSearch;
import com.teocci.ytinbg.adapters.VideosAdapter;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.interfaces.OnLoadMoreListener;
import com.teocci.ytinbg.interfaces.YouTubeVideoReceiver;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.ui.decoration.DividerDecoration;
import com.teocci.ytinbg.utils.Config;

import java.util.ArrayList;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Apr/11
 */

public class SearchFragment extends RecyclerFragment implements YouTubeVideoReceiver, OnLoadMoreListener
{
    private static final String TAG = SearchFragment.class.getSimpleName();

    private Handler handler;
    private YouTubeSearch youTubeSearch;
    private ProgressBar loadingProgressBar;

    private String currentQuery;
    private String nextPageToken;
    private int visibleThreshold = 1;
    private int lastVisibleItem, totalItemCount;
    private boolean isLoading;

    public static SearchFragment newInstance()
    {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        loadingProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
//                Log.e(TAG, "totalItemCount: " + totalItemCount + " lastVisibleItem: " + lastVisibleItem);
                if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    videoListAdapter.onItemHolderOnLoadMore();
                    isLoading = true;
                }
            }
        });

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
                .create(videoListAdapter.getYouTubeVideos(position));

        Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE, Config.YOUTUBE_MEDIA_TYPE_VIDEO);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, videoListAdapter.getYouTubeVideos(position));
        getActivity().startService(serviceIntent);
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
        currentQuery = query;
        // Check network connectivity
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }
        videoListAdapter.clearYouTubeVideos();
        loadingProgressBar.setVisibility(View.VISIBLE);
        youTubeSearch.searchVideos(currentQuery);
    }

    /**
     * Called when video items are received
     *
     * @param youTubeVideos - videos to be shown in list view
     */
    @Override
    public void onVideosReceived(final ArrayList<YouTubeVideo> youTubeVideos,
                                 final YouTube.Search.List searchList,
                                 String nextPageToken)
    {
        if (videoListAdapter != null) {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    if (searchList.getPageToken() == null) {
                        Log.e(TAG, "Adding First Page Videos");
                        videoListAdapter.setYouTubeVideos(youTubeVideos);
                        recyclerView.smoothScrollToPosition(0);
                    } else {
                        Log.e(TAG, "Adding Next Page Videos");
                        videoListAdapter.addMoreYouTubeVideos(youTubeVideos);
                    }
                }

            });

            this.nextPageToken = nextPageToken;
            if (nextPageToken != null) {
                Log.e(TAG, "Adding setOnLoadMoreListener");
                videoListAdapter.setOnLoadMoreListener(this);
            } else {
                Log.e(TAG, "Removing setOnLoadMoreListener");
                videoListAdapter.removeOnLoadMoreListener();
            }
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

    @Override
    public void onLoadMore()
    {
        //Here adding null object to last position,check the condition in getItemViewType() method,if object is null then display progress
//        dataModels.add(null);
//        simpleAdapter.notifyItemInserted(dataModels.size() - 1);

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                youTubeSearch.searchNextVideos(currentQuery, nextPageToken);
                isLoading = false;
            }
        }, 1000);
    }
}