/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teocci.utubinbg;

import android.app.Activity;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.teocci.utubinbg.interfaces.YouTubePlaylistsReceiver;
import com.teocci.utubinbg.interfaces.YouTubeVideosReceiver;
import com.teocci.utubinbg.utils.Auth;
import com.teocci.utubinbg.utils.Config;
import com.teocci.utubinbg.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class for sending YouTube DATA API V3 request and receiving data from it
 * Created by teocci on 18.2.16..
 */
public class YouTubeSearch {

    private static final String TAG = "UTUBINBG SEARCH CLASS";

    private String appName;

    private Handler handler;
    private Activity activity;

    private YouTube youtube;

    private Fragment playlistFragment;

    private String chosenAccountName;

    private YouTubeVideosReceiver youTubeVideosReceiver;
    private YouTubePlaylistsReceiver youTubePlaylistsReceiver;

    private static final int REQUEST_AUTHORIZATION = 3;
    private GoogleAccountCredential credential;

    public YouTubeSearch(Activity activity, Fragment playlistFragment) {
        this.activity = activity;
        this.playlistFragment = playlistFragment;
        handler = new Handler();
        credential = GoogleAccountCredential.usingOAuth2(activity.getApplicationContext(), Arrays.asList(Auth.SCOPES));
        // set exponential backoff policy
        credential.setBackOff(new ExponentialBackOff());
        appName = activity.getResources().getString(R.string.app_name);
    }

    public void setYouTubeVideosReceiver(YouTubeVideosReceiver youTubeVideosReceiver) {
        this.youTubeVideosReceiver = youTubeVideosReceiver;
    }

    public void setYouTubePlaylistsReceiver(YouTubePlaylistsReceiver youTubePlaylistsReceiver) {
        this.youTubePlaylistsReceiver = youTubePlaylistsReceiver;
    }

    /**
     * Sets account name from app (chosen or acquired from shared preferences)
     *
     * @param authSelectedAccountName
     */
    public void setAuthSelectedAccountName(String authSelectedAccountName) {
        this.chosenAccountName = authSelectedAccountName;
        credential.setSelectedAccountName(chosenAccountName);
    }

    /**
     * Gets credential - OAuth 2.0
     *
     * @return
     */
    public GoogleAccountCredential getCredential() {
        return credential;
    }

    /**
     * Search videos for a specific query
     *
     * @param keywords - query
     */
    public void searchVideos(final String keywords) {
        new Thread() {
            public void run() {
                try {
                    youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest request) throws IOException {

                        }
                    }).setApplicationName(appName).build();

                    YouTube.Search.List searchList;
                    YouTube.Videos.List videosList;

                    searchList = youtube.search().list("id,snippet");
                    searchList.setKey(Config.YOUTUBE_API_KEY);
                    searchList.setType("video"); //TODO ADD PLAYLISTS SEARCH
                    searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
                    searchList.setFields("items(id/videoId,snippet/title,snippet/thumbnails/default/url)");

                    videosList = youtube.videos().list("id,contentDetails,statistics");
                    videosList.setKey(Config.YOUTUBE_API_KEY);
                    videosList.setFields("items(contentDetails/duration,statistics/viewCount)");

                    //search
                    searchList.setQ(keywords);
                    SearchListResponse searchListResponse = searchList.execute();
                    List<SearchResult> searchResults = searchListResponse.getItems();

                    //save all ids from searchList list in order to find video list
                    StringBuilder contentDetails = new StringBuilder();

                    int ii = 0;
                    for (SearchResult result : searchResults) {
                        contentDetails.append(result.getId().getVideoId());
                        if (ii < 49)
                            contentDetails.append(",");
                        ii++;
                    }

                    //find video list
                    videosList.setId(contentDetails.toString());
                    VideoListResponse resp = videosList.execute();
                    List<Video> videoResults = resp.getItems();
                    //make items for displaying in listView
                    ArrayList<YouTubeVideo> items = new ArrayList<>();
                    for (int i = 0; i < searchResults.size(); i++) {
                        YouTubeVideo item = new YouTubeVideo();
                        //searchList list info
                        item.setTitle(searchResults.get(i).getSnippet().getTitle());
                        item.setThumbnailURL(searchResults.get(i).getSnippet().getThumbnails().getDefault().getUrl());
                        item.setId(searchResults.get(i).getId().getVideoId());
                        //video info
                        if (videoResults.get(i) != null) {
                            BigInteger viewsNumber = videoResults.get(i).getStatistics().getViewCount();
                            String viewsFormatted = NumberFormat.getIntegerInstance().format(viewsNumber) + " views";
                            item.setViewCount(viewsFormatted);
                            String isoTime = videoResults.get(i).getContentDetails().getDuration();
                            String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                            item.setDuration(time);
                        } else {
                            item.setDuration("NA");
                        }

                        //add to the list
                        items.add(item);
                    }

                    youTubeVideosReceiver.onVideosReceived(items);

                } catch (IOException e) {
                    Log.e(TAG, "Could not initialize: " + e);
                    e.printStackTrace();
                    return;
                }
            }
        }.start();
    }

    /**
     * /**
     * Search playlists for a current user
     */
    public void searchPlaylists() {
        new Thread() {
            public void run() {
                credential.setSelectedAccountName(chosenAccountName);
                youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                        .setApplicationName(appName).build();
                try {
                    ChannelListResponse channelListResponse = youtube.channels().list("snippet").setMine(true).execute();

                    List<Channel> channelList = channelListResponse.getItems();
                    if (channelList.isEmpty()) {
                        Log.d(TAG, "Can't find user channel");
                    }
                    Channel channel = channelList.get(0);

                    YouTube.Playlists.List searchList = youtube.playlists().list("id,snippet,contentDetails,status").setKey(Config.YOUTUBE_API_KEY);

                    searchList.setChannelId(channel.getId());
                    searchList.setFields("items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status,contentDetails/videoId)");
                    searchList.setMaxResults((long) 50);

                    PlaylistListResponse playListResponse = searchList.execute();
                    List<Playlist> playlists = playListResponse.getItems();

                    if (playlists != null) {

                        Iterator<Playlist> iteratorPlaylistResults = playlists.iterator();

                        if (!iteratorPlaylistResults.hasNext()) {
                            Log.d(TAG, " There aren't any results for your query.");
                        }

                        ArrayList<YouTubePlaylist> youTubePlaylistList = new ArrayList<>();

                        while (iteratorPlaylistResults.hasNext()) {
                            Playlist playlist = iteratorPlaylistResults.next();

                            YouTubePlaylist playlistItem = new YouTubePlaylist(
                                    playlist.getSnippet().getTitle(),
                                    playlist.getSnippet().getThumbnails().getDefault().getUrl(),
                                    playlist.getId(),
                                    playlist.getContentDetails().getItemCount(),
                                    playlist.getStatus().getPrivacyStatus());
                            youTubePlaylistList.add(playlistItem);
                        }

                        youTubePlaylistsReceiver.onPlaylistsReceived(youTubePlaylistList);
                    }
                } catch (UserRecoverableAuthIOException e) {
                    playlistFragment.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    e.printStackTrace();
                } catch (GoogleJsonResponseException e) {
                    Log.e(TAG, "GoogleJsonResponseException code: " + e.getDetails().getCode()
                            + " : " + e.getDetails().getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "IOException: " + e.getMessage());
                    e.printStackTrace();
                } catch (Throwable t) {
                    Log.e(TAG, "Throwable: " + t.getMessage());
                    t.printStackTrace();
                }
            }
        }.start();

    }

    /**
     * Acquires all videos for a specific playlist
     *
     * @param playlistId
     */
    public void acquirePlaylistVideos(final String playlistId) {

        // Define a list to store items in the list of uploaded videos.
        new Thread(new Runnable() {
            @Override
            public void run() {

                Log.e(TAG, "Chosen name: " + chosenAccountName);
                credential.setSelectedAccountName(chosenAccountName);

                youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                        .setApplicationName(appName).build();

                ArrayList<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();
                ArrayList<YouTubeVideo> playlistItems = new ArrayList<>();

                String nextToken = "";
                // Retrieve the playlist of the channel's uploaded videos.
                YouTube.PlaylistItems.List playlistItemRequest = null;
                try {
                    playlistItemRequest = youtube.playlistItems().list("id,contentDetails,snippet");
                    playlistItemRequest.setPlaylistId(playlistId);
                    playlistItemRequest.setMaxResults(50l);
                    playlistItemRequest.setFields("items(contentDetails/videoId,snippet/title," +
                            "snippet/thumbnails/default/url),nextPageToken");
                    // Call API one or more times to retrieve all items in the list. As long as API
                    // response returns a nextPageToken, there are still more items to retrieve.
                    //do {
                    //playlistItemRequest.setPageToken(nextToken);
                    PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();
                    playlistItemList.addAll(playlistItemResult.getItems());
                    //nextToken = playlistItemResult.getNextPageToken();
                    //} while (nextToken != null);

                    Log.d(TAG, "all items size: " + playlistItemList.size());
                } catch (GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 404) {
                        youTubeVideosReceiver.onPlaylistNotFound(playlistId, e.getStatusCode());
                        return;
                    } else {
                        e.printStackTrace();
                    }
                } catch (UnknownHostException e) {
                    Toast.makeText(activity.getApplicationContext(), "Check internet connection", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                //videos to get duration
                YouTube.Videos.List videosList = null;
                try {
                    videosList = youtube.videos().list("id,contentDetails");
                    videosList.setKey(Config.YOUTUBE_API_KEY);
                    videosList.setFields("items(contentDetails/duration)");

                    //save all ids from searchList list in order to find video list
                    StringBuilder contentDetails = new StringBuilder();

                    int ii = 0;
                    for (PlaylistItem result : playlistItemList) {
                        contentDetails.append(result.getContentDetails().getVideoId());
                        if (ii < playlistItemList.size() - 1)
                            contentDetails.append(",");
                        ii++;
                    }
                    //find video list
                    videosList.setId(contentDetails.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                VideoListResponse resp = null;
                try {
                    resp = videosList.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                List<Video> videoResults = resp.getItems();
                Iterator<PlaylistItem> pit = playlistItemList.iterator();
                Iterator<Video> vit = videoResults.iterator();
                while (pit.hasNext()) {
                    PlaylistItem playlistItem = pit.next();
                    Video videoItem = vit.next();

                    YouTubeVideo youTubeVideo = new YouTubeVideo();
                    youTubeVideo.setId(playlistItem.getContentDetails().getVideoId());
                    youTubeVideo.setTitle(playlistItem.getSnippet().getTitle());
                    youTubeVideo.setThumbnailURL(playlistItem.getSnippet().getThumbnails().getDefault().getUrl());
                    //video info
                    if (videoItem != null) {
                        String isoTime = videoItem.getContentDetails().getDuration();
                        String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                        youTubeVideo.setDuration(time);
                    } else {
                        youTubeVideo.setDuration("NA");
                    }
                    playlistItems.add(youTubeVideo);
                }

                youTubeVideosReceiver.onVideosReceived(playlistItems);
            }
        }).start();
    }
}