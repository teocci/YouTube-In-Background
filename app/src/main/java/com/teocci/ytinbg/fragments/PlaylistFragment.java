package com.teocci.ytinbg.fragments;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.util.Swappable;
import com.teocci.ytinbg.BackgroundAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.YouTubePlaylist;
import com.teocci.ytinbg.YouTubeSearch;
import com.teocci.ytinbg.YouTubeVideo;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.interfaces.YouTubePlaylistReceiver;
import com.teocci.ytinbg.interfaces.YouTubeVideosReceiver;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.NetworkConf;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Class that handles list of the playlistList acquired from YouTube
 * Created by teocci on 7.3.16..
 */
public class PlaylistFragment extends Fragment implements YouTubeVideosReceiver, YouTubePlaylistReceiver {

    private static final String TAG = "PlaylistFragment";

    private ArrayList<YouTubePlaylist> playlistList;
    private DynamicListView playlistListView;
    private Handler handler;
    private PlaylistAdapter playlistAdapter;

    public static final String ACCOUNT_KEY = "accountName";
    private String chosenAccountName;

    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;

    private YouTubeSearch youTubeSearch;
    private TextView userNameTextView;
    private NetworkConf networkConf;
    private SwipeRefreshLayout swipeToRefresh;

    public PlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
        playlistList = new ArrayList<>();

        youTubeSearch = new YouTubeSearch(getActivity(), this);
        youTubeSearch.setYouTubePlaylistReceiver(this);
        youTubeSearch.setYouTubeVideosReceiver(this);

        networkConf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playlists, container, false);

        /* Setup the ListView */
        playlistListView = (DynamicListView) v.findViewById(R.id.playlists);
        userNameTextView = (TextView) v.findViewById(R.id.user_name);
        swipeToRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipeToRefresh);

        setupListViewAndAdapter();

        if (savedInstanceState != null) {
            chosenAccountName = savedInstanceState.getString(ACCOUNT_KEY);
            youTubeSearch.setAuthSelectedAccountName(chosenAccountName);
            userNameTextView.setText(extractUserName(chosenAccountName));
            Toast.makeText(getContext(), "Hi " + extractUserName(chosenAccountName), Toast.LENGTH_SHORT).show();
        } else {
            loadAccount();
        }

        return v;
    }

    /**
     * Loads account saved in preferences
     */
    private void loadAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        chosenAccountName = sp.getString(ACCOUNT_KEY, null);

        if (chosenAccountName != null) {
            youTubeSearch.setAuthSelectedAccountName(chosenAccountName);
            userNameTextView.setText(extractUserName(chosenAccountName));
            Toast.makeText(getContext(), "Hi " + extractUserName(chosenAccountName), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Save account in preferences for future usages
     */
    private void saveAccount() {
        Log.e(TAG, "Saving account name... " + chosenAccountName);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.edit().putString(ACCOUNT_KEY, chosenAccountName).apply();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getUserVisibleHint()) {
            //do nothing for now
        }

        playlistList.clear();
        playlistList.addAll(YouTubeSqlDb.getInstance().playlists().readAll());
        playlistAdapter.notifyDataSetChanged();
    }


    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
            //Log.d(TAG, "PlaylistFragment visible and resumed");
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    /**
     * Handles Google OAuth 2.0 authorization or account chosen result
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        chosenAccountName = accountName;
                        youTubeSearch.setAuthSelectedAccountName(accountName);
                        userNameTextView.setText(extractUserName(chosenAccountName));
                        Toast.makeText(getContext(), "Hi " + extractUserName(chosenAccountName), Toast.LENGTH_SHORT).show();
                        saveAccount();
                    }

                    youTubeSearch.searchPlaylists();
                }
                break;
        }
    }

    /**
     * Choose Google account if OAuth 2.0 choosing is necessary
     * acquiring YouTube private playlistList requires OAuth 2.0 authorization
     */
    private void chooseAccount() {
        startActivityForResult(youTubeSearch.getCredential().newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }

    /**
     * Setups list view and adapter for storing YouTube playlistList
     */
    public void setupListViewAndAdapter() {
        playlistAdapter = new PlaylistAdapter(getActivity());
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(playlistAdapter);
        animationAdapter.setAbsListView(playlistListView);
        playlistListView.setAdapter(animationAdapter);

        /* Enable drag and drop functionality */
        playlistListView.enableDragAndDrop();
        playlistListView.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                                   final int position, final long id) {
                        playlistListView.startDragging(position);
                        return true;
                    }
                }
        );

        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (networkConf.isNetworkAvailable()) {
                    if (chosenAccountName == null) {
                        chooseAccount();
                    } else {
                        youTubeSearch.setAuthSelectedAccountName(chosenAccountName);
                        youTubeSearch.searchPlaylists();
                    }
                } else {
                    networkConf.createNetErrorDialog();
                }
            }
        });

        playlistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos,
                                    long id) {
                //check network connectivity
                if (!networkConf.isNetworkAvailable()) {
                    networkConf.createNetErrorDialog();
                    return;
                }
                youTubeSearch.acquirePlaylistVideos(playlistList.get(pos).getId()); //results are in onVideosReceived callback method
            }
        });

    }

    /**
     * Called when playlistList video items are received
     *
     * @param youTubeVideos - list to be played in background service
     */
    @Override
    public void onVideosReceived(ArrayList<YouTubeVideo> youTubeVideos) {
        //if playlistList is empty, do not start service
        if (youTubeVideos.isEmpty()) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getContext(), "Playlist is empty!", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onPlaylistNotFound(final String playlistId, int errorCode) {
        Log.e(TAG, "Error 404. Playlist not found!");
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getContext(), "Playlist does not exist!", Toast.LENGTH_SHORT).show();
                removePlaylist(playlistId);
            }
        });
    }

    /**
     * Remove playlistList with specific ID from DB and list
     *
     * @param playlistId
     */
    private void removePlaylist(final String playlistId) {
        YouTubeSqlDb.getInstance().playlists().delete(playlistId);

        for (YouTubePlaylist playlist : this.playlistList) {
            if (playlist.getId().equals(playlistId)) {
                this.playlistList.remove(playlist);
                break;
            }
        }

        playlistAdapter.notifyDataSetChanged();
    }

    /**
     * Called when playlistList are received
     *
     * @param youTubePlaylistList - list of playlistList to be shown in list view
     */
    @Override
    public void onPlaylistReceived(ArrayList<YouTubePlaylist> youTubePlaylistList) {

        //refresh playlistList in database
        YouTubeSqlDb.getInstance().playlists().deleteAll();
        for (YouTubePlaylist playlist : youTubePlaylistList) {
            YouTubeSqlDb.getInstance().playlists().create(playlist);
        }

        playlistList.clear();
        playlistList.addAll(youTubePlaylistList);
        handler.post(new Runnable() {
            public void run() {
                if (playlistAdapter != null) {
                    playlistAdapter.notifyDataSetChanged();
                    swipeToRefresh.setRefreshing(false);
                }
            }
        });
    }

    /**
     * Extracts user name from email address
     *
     * @param emailAddress
     * @return
     */
    private String extractUserName(String emailAddress) {
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

    /**
     * Custom array adapter class which enables drag and drop list items swapping
     */
    public class PlaylistAdapter extends ArrayAdapter<YouTubePlaylist> implements Swappable {

        public PlaylistAdapter(Activity context) {
            super(context, R.layout.video_item, playlistList);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.playlist_item, parent, false);
            }
            ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            TextView title = (TextView) convertView.findViewById(R.id.playlist_title);
            TextView videosNumber = (TextView) convertView.findViewById(R.id.videos_number);
            TextView privacy = (TextView) convertView.findViewById(R.id.privacy);

            YouTubePlaylist searchResult = playlistList.get(position);

            Picasso.with(getContext()).load(searchResult.getThumbnailURL()).into(thumbnail);
            title.setText(searchResult.getTitle());
            videosNumber.setText("Number of videos: " + String.valueOf(searchResult.getNumberOfVideos()));
            privacy.setText("Status: " + searchResult.getStatus());

            return convertView;
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).hashCode();
        }


        @Override
        public boolean hasStableIds() {
            return true;
        }


        @Override
        public void swapItems(int i, int i1) {
            YouTubePlaylist firstItem = getItem(i);

            playlistList.set(i, getItem(i1));
            playlistList.set(i1, firstItem);

            notifyDataSetChanged();
        }
    }
}