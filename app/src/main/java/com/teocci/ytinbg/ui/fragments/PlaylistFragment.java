package com.teocci.ytinbg.ui.fragments;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.google.api.services.youtube.YouTube;
import com.teocci.ytinbg.BackgroundAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.YouTubeSearch;
import com.teocci.ytinbg.adapters.PlaylistAdapter;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.interfaces.YouTubePlaylistReceiver;
import com.teocci.ytinbg.interfaces.YouTubeVideoReceiver;
import com.teocci.ytinbg.model.YouTubePlaylist;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.ui.decoration.DividerDecoration;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;
import com.teocci.ytinbg.utils.NetworkConf;

import java.util.ArrayList;

/**
 * Class that handles list of the playlistList acquired from YouTube
 * Created by teocci on 7.3.16..
 */
public class PlaylistFragment extends Fragment implements AdapterView.OnItemClickListener, YouTubeVideoReceiver,
        YouTubePlaylistReceiver
{
    private static final String TAG = LogHelper.makeLogTag(PlaylistFragment.class);

    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;

    private RecyclerView playlistListView;
    private PlaylistAdapter playlistAdapter;
    private String chosenAccountName;

    private YouTubeSearch youTubeSearch;
    private TextView userNameTextView;
    private NetworkConf networkConf;
    private SwipeRefreshLayout swipeToRefresh;

    public PlaylistFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

//        handler = new Handler();
//        playlistList = new ArrayList<>();
//
        youTubeSearch = new YouTubeSearch(getActivity(), this);
        youTubeSearch.setYouTubePlaylistReceiver(this);
        youTubeSearch.setYouTubeVideoReceiver(this);

        networkConf = new NetworkConf(getActivity());
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
                // Check network connectivity
                if (!networkConf.isNetworkAvailable()) {
                    networkConf.createNetErrorDialog();
                    return;
                }

                if (chosenAccountName == null) {
                    chooseAccount();
                } else {
                    youTubeSearch.setAuthSelectedAccountName(chosenAccountName);
                    youTubeSearch.searchPlaylist();
                }
            }
        });

        if (savedInstanceState != null) {
            chosenAccountName = savedInstanceState.getString(Config.ACCOUNT_NAME);
            youTubeSearch.setAuthSelectedAccountName(chosenAccountName);
            userNameTextView.setText(extractUserName(chosenAccountName));
            Toast.makeText(
                    getContext(),
                    getString(R.string.toast_message_hello) + extractUserName(chosenAccountName),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            loadAccount();
        }

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

    /**
     * Loads account saved in preferences
     */
    private void loadAccount()
    {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        chosenAccountName = sp.getString(Config.ACCOUNT_NAME, null);

        if (chosenAccountName != null) {
            youTubeSearch.setAuthSelectedAccountName(chosenAccountName);
            userNameTextView.setText(extractUserName(chosenAccountName));
            Toast.makeText(
                    getContext(),
                    getResources().getString(R.string.toast_message_hello) + extractUserName(chosenAccountName),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Save account in preferences for future usages
     */
    private void saveAccount()
    {
        LogHelper.e(TAG, "Saving account name... " + chosenAccountName);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.edit().putString(Config.ACCOUNT_NAME, chosenAccountName).apply();
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

    /**
     * Handles Google OAuth 2.0 authorization or account chosen result
     *
     * @param requestCode to use when launching the resolution activity
     * @param resultCode  Standard activity result: operation succeeded.
     * @param data        The received Intent includes an extra for KEY_ACCOUNT_NAME, specifying
     *                    the account name (an email address) you must use to acquire the OAuth 2
     *                    .0 token.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_AUTHORIZATION:
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null
                        && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager
                            .KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        chosenAccountName = accountName;
                        youTubeSearch.setAuthSelectedAccountName(accountName);
                        userNameTextView.setText(extractUserName(chosenAccountName));
                        Toast.makeText(
                                getContext(),
                                getResources().getString(R.string.toast_message_hello) +
                                        extractUserName(chosenAccountName),
                                Toast.LENGTH_SHORT
                        ).show();
                        saveAccount();
                    }

                    youTubeSearch.searchPlaylist();
                }
                break;
        }
    }

    /**
     * Choose Google account if OAuth 2.0 choosing is necessary
     * acquiring YouTube private playlistList requires OAuth 2.0 authorization
     */
    private void chooseAccount()
    {
        try {
//            Log.e(TAG, "getCredential()" + youTubeSearch.getCredential());
            startActivityForResult(
                    youTubeSearch.getCredential().newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER
            );
        } catch (Exception e){
            e.printStackTrace();
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
        youTubeSearch.acquirePlaylistVideos(playlistAdapter.getYouTubePlaylist(position).getId());
    }

    /**
     * Called when playlistList video items are received
     *
     * @param youTubeVideos - list to be played in background service
     */
    @Override
    public void onVideosReceived(ArrayList<YouTubeVideo> youTubeVideos,
                                 YouTube.Search.List searchList,
                                 String nextPageToken)
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
            serviceIntent.putExtra(Config.YOUTUBE_TYPE, Config.YOUTUBE_MEDIA_TYPE_PLAYLIST);
            serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, youTubeVideos);
            getActivity().startService(serviceIntent);
        }
    }

    /**
     * Called when playlistList are received
     *
     * @param ytPlaylists - list of playlistList to be shown in list view
     */
    @Override
    public void onPlaylistReceived(final ArrayList<YouTubePlaylist> ytPlaylists)
    {
        //refresh playlistList in database
        YouTubeSqlDb.getInstance().playlistModel().deleteAll();
        for (YouTubePlaylist playlist : ytPlaylists) {
            YouTubeSqlDb.getInstance().playlistModel().create(playlist);
        }

        if (playlistAdapter != null) {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    playlistAdapter.setYouTubePlaylists(ytPlaylists);
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