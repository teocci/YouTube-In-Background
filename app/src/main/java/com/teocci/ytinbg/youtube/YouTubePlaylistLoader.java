package com.teocci.ytinbg.youtube;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.teocci.ytinbg.interfaces.YouTubePlaylistReceiver;
import com.teocci.ytinbg.model.YouTubePlaylist;
import com.teocci.ytinbg.utils.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.teocci.ytinbg.utils.Config.YOUTUBE_CHANNEL_LIST;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_PLAYLIST_FIELDS;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_PLAYLIST_PART;
import static com.teocci.ytinbg.youtube.YouTubeSingleton.getCredential;
import static com.teocci.ytinbg.youtube.YouTubeSingleton.getInstance;
import static com.teocci.ytinbg.youtube.YouTubeSingleton.getYouTubeWithCredentials;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-09
 */

public class YouTubePlaylistLoader extends AsyncTask<String, Void, List<YouTubePlaylist>>
{
    private static final String TAG = YouTubePlaylistLoader.class.getSimpleName();

    private Context context;
    private YouTube youtube;

    private YouTubePlaylistReceiver youTubePlaylistReceiver;

    public YouTubePlaylistLoader(Context context)
    {
        getInstance(context);
        this.context = context;
        this.youtube = getYouTubeWithCredentials();
        this.youTubePlaylistReceiver = null;
    }

    @Override
    protected void onPostExecute(List<YouTubePlaylist> ytPlaylistList)
    {
        youTubePlaylistReceiver.onPlaylistReceived(ytPlaylistList);
    }

    @Override
    protected List<YouTubePlaylist> doInBackground(String... params)
    {
        try {
            return searchPlaylist();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void setYouTubePlaylistReceiver(YouTubePlaylistReceiver youTubePlaylistReceiver)
    {
        this.youTubePlaylistReceiver = youTubePlaylistReceiver;
    }

    /**
     * Acquires all playlist for a specific account
     *
     */
    public void acquire()
    {
        this.execute();
    }

    /**
     * Search playlist for a current user
     */
    public List<YouTubePlaylist> searchPlaylist()
    {
        if (getCredential() == null){
            Log.e(TAG, "getCredential: is null!");
            return Collections.emptyList();
        }
        if (getCredential().getSelectedAccountName() == null) {
            Log.d(TAG, "loadInBackground: account not picked!");
            return Collections.emptyList();
        }

        try {
            ChannelListResponse channelListResponse = youtube.channels().list(YOUTUBE_CHANNEL_LIST)
                    .setMine(true)
                    .execute();

            List<Channel> channelList = channelListResponse.getItems();
            if (channelList.isEmpty()) {
                Log.d(TAG, "Can't find user channel");
            }
            Channel channel = channelList.get(0);

            YouTube.Playlists.List searchList = youtube.playlists()
                    .list(YOUTUBE_PLAYLIST_PART)
                    .setKey(Config.YOUTUBE_API_KEY);

            searchList.setChannelId(channel.getId());
            searchList.setFields(YOUTUBE_PLAYLIST_FIELDS);
            searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);

            PlaylistListResponse playListResponse = searchList.execute();
            List<Playlist> playlists = playListResponse.getItems();

            if (playlists != null) {
                Iterator<Playlist> iteratorPlaylistResults = playlists.iterator();
                if (!iteratorPlaylistResults.hasNext()) {
                    Log.d(TAG, " There aren't any results for your query.");
                }

                List<YouTubePlaylist> ytPlaylistList = new ArrayList<>();

                while (iteratorPlaylistResults.hasNext()) {
                    Playlist playlist = iteratorPlaylistResults.next();

                    YouTubePlaylist playlistItem = new YouTubePlaylist(playlist.getSnippet().getTitle(),
                            playlist.getSnippet().getThumbnails().getDefault().getUrl(),
                            playlist.getId(),
                            playlist.getContentDetails().getItemCount(),
                            playlist.getStatus().getPrivacyStatus());
                    ytPlaylistList.add(playlistItem);
                }

                return ytPlaylistList;
            }
        } catch (UserRecoverableAuthIOException e) {
            Log.d(TAG, "loadInBackground: exception REQUEST_AUTHORIZATION");
            e.printStackTrace();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                youTubePlaylistReceiver.onPlaylistNotFound("empty", e.getStatusCode());
            } else {
                Log.e(TAG, "GoogleJsonResponseException code: " + e.getDetails().getCode()
                        + " : " + e.getDetails().getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.d(TAG, "loadInBackground: " + e.getMessage());
            e.printStackTrace();
        }

        return Collections.emptyList();
    }
}