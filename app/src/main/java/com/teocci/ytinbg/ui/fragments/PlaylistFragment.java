package com.teocci.ytinbg.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.teocci.ytinbg.BackgroundAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.adapters.PlaylistAdapter;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.interfaces.YouTubePlaylistReceiver;
import com.teocci.ytinbg.model.YouTubePlaylist;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.ui.decoration.DividerDecoration;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;
import com.teocci.ytinbg.utils.NetworkHelper;
import com.teocci.ytinbg.youtube.YouTubePlaylistLoader;
import com.teocci.ytinbg.youtube.YouTubePlaylistVideoLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles list of the playlistList acquired from YouTube
 * Created by teocci on 7.3.16..
 */
public class PlaylistFragment extends Fragment implements AdapterView.OnItemClickListener,
        YouTubePlaylistReceiver
{
    private static final String TAG = PlaylistFragment.class.getSimpleName();

    private RecyclerView playlistListView;
    private PlaylistAdapter playlistAdapter;
    private String chosenAccountName;

    private YouTubePlaylistLoader ytPlaylistLoader;
    private YouTubePlaylistVideoLoader ytPlaylistVideoLoader;
    private TextView userNameTextView;
    private NetworkHelper networkConf;
    private SwipeRefreshLayout swipeToRefresh;

    public PlaylistFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ytPlaylistLoader = new YouTubePlaylistLoader(getActivity());
        ytPlaylistLoader.setYouTubePlaylistReceiver(this);

        ytPlaylistVideoLoader = new YouTubePlaylistVideoLoader(getActivity());
        ytPlaylistVideoLoader.setYouTubePlaylistReceiver(this);

        networkConf = new NetworkHelper(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_playlists, container, false);

        /* Setup the ListView */
        playlistListView = (RecyclerView) rootView.findViewById(R.id.playlists);
        playlistListView.setLayoutManager(getLayoutManager());
        playlistListView.addItemDecoration(getItemDecoration());

        playlistListView.getItemAnimator().setAddDuration(500);
        playlistListView.getItemAnimator().setChangeDuration(500);
        playlistListView.getItemAnimator().setMoveDuration(500);
        playlistListView.getItemAnimator().setRemoveDuration(500);

        playlistAdapter = getAdapter();
        playlistAdapter.setOnItemClickListener(this);
        playlistListView.setAdapter(playlistAdapter);

        userNameTextView = (TextView) rootView.findViewById(R.id.user_name);
        swipeToRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeToRefresh);

        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                searchPlaylists();
            }
        });

        return rootView;
    }

    protected RecyclerView.LayoutManager getLayoutManager()
    {
        return new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    }

    protected RecyclerView.ItemDecoration getItemDecoration()
    {
        //We must draw dividers ourselves if we want them in a list
        return new DividerDecoration(getActivity());
    }

    protected PlaylistAdapter getAdapter()
    {
        return new PlaylistAdapter(getActivity());
    }

    public void searchPlaylists()
    {
        ytPlaylistLoader.acquire();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!getUserVisibleHint()) {
            // Do nothing for now
        }

//        playlistList.clear();
//        playlistList.addAll(YouTubeSqlDb.getInstance().playlistModel().readAll());
//        playlistAdapter.notifyDataSetChanged();
    }


    @Override
    public void setUserVisibleHint(boolean visible)
    {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
//            LogHelper.d(TAG, "PlaylistFragment visible and resumed");
            // Only manually call onResume if fragment is already visible
//            Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        // Check network connectivity
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        // Results are in onVideosReceived callback method
        ytPlaylistVideoLoader.acquire(playlistAdapter.getYouTubePlaylist(position).getId());
    }

    /**
     * Called when playlistList are received
     *
     * @param youTubePlaylistList - list of playlistList to be shown in list view
     */
    @Override
    public void onPlaylistReceived(final List<YouTubePlaylist> youTubePlaylistList)
    {
        if (youTubePlaylistList == null) {
            swipeToRefresh.setRefreshing(false);
            return;
        }
        //refresh playlistList in database
        YouTubeSqlDb.getInstance().playlistModel().deleteAll();
        for (YouTubePlaylist playlist : youTubePlaylistList) {
            YouTubeSqlDb.getInstance().playlistModel().create(playlist);
        }

        if (playlistAdapter != null) {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    playlistAdapter.setYouTubePlaylists(youTubePlaylistList);
                    swipeToRefresh.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void onPlaylistNotFound(final String playlistId, int errorCode)
    {
        LogHelper.e(TAG, "Error 404. Playlist not found!");
        getActivity().runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(
                        getContext(),
                        getResources().getString(R.string.toast_message_playlist_not_exist),
                        Toast.LENGTH_SHORT
                ).show();
                if (!playlistId.equals("empty")) {
                    removePlaylist(playlistId);
                }
            }
        });
    }

    /**
     * Called when playlistList video items are received
     *
     * @param youTubeVideos - videos to be shown in list view
     */
    @Override
    public void onPlaylistVideoReceived(List<YouTubeVideo> youTubeVideos)
    {
        // Whenever the playlistList is empty, do not start service
        if (youTubeVideos.isEmpty()) {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(
                            getContext(),
                            getResources().getString(R.string.toast_message_playlist_empty),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        } else {
            Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
            serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
            serviceIntent.putExtra(Config.KEY_YOUTUBE_TYPE, Config.YOUTUBE_MEDIA_TYPE_PLAYLIST);
            serviceIntent.putExtra(Config.KEY_YOUTUBE_TYPE_PLAYLIST, (ArrayList) youTubeVideos);
            getActivity().startService(serviceIntent);
        }
    }

    /**
     * Remove playlistList with specific ID from DB and list
     *
     * @param playlistId the playlist ID to be deleted
     */
    private void removePlaylist(final String playlistId)
    {
        playlistAdapter.removeYouTubePlaylistById(playlistId);
    }

    /**
     * Extracts user name from email address
     *
     * @param emailAddress
     * @return
     */
    private String extractUserName(String emailAddress)
    {
        if (emailAddress != null) {
            String[] parts = emailAddress.split("@");
            if (parts.length > 0) {
                if (parts[0] != null) {
                    return parts[0];
                }
            }
        }
        return "";
    }
}