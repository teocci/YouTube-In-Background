package com.teocci.ytinbg.youtube;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.teocci.ytinbg.interfaces.YouTubePlaylistReceiver;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;
import com.teocci.ytinbg.utils.Utils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.teocci.ytinbg.utils.Config.YOUTUBE_ACQUIRE_PLAYLIST_FIELDS;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_ACQUIRE_PLAYLIST_PART;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_API_KEY;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_PLAYLIST_VIDEO_FIELDS;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_PLAYLIST_VIDEO_PART;
import static com.teocci.ytinbg.youtube.YouTubeSingleton.getInstance;
import static com.teocci.ytinbg.youtube.YouTubeSingleton.getYouTube;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-09
 */

public class YouTubePlaylistVideoLoader extends AsyncTask<String, Void, List<YouTubeVideo>>
{

    private final static String TAG = LogHelper.makeLogTag(YouTubePlaylistVideoLoader.class);

    private YouTube youtube;

    private Context context;
    private String playlistId;

    private YouTubePlaylistReceiver youTubePlaylistReceiver;



    public YouTubePlaylistVideoLoader(Context context)
    {
        getInstance(context);
        this.context = context;
        this.youtube = getYouTube();
        this.playlistId = null;
        this.youTubePlaylistReceiver = null;
    }

    public YouTubePlaylistVideoLoader(Context context, String playlistId)
    {
        getInstance(context);
        this.context = context;
        this.youtube = getYouTube();
        this.playlistId = playlistId;
        this.youTubePlaylistReceiver = null;
    }

    @Override
    protected void onPostExecute(List<YouTubeVideo> ytVideos)
    {
        youTubePlaylistReceiver.onPlaylistVideoReceived(ytVideos);
    }

    @Override
    protected List<YouTubeVideo> doInBackground(String... params)
    {
        if (playlistId == null) return null;
        try {
            return acquirePlaylistVideos();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Acquires all videos for a specific playlist
     *
     * @param playlistId - YouTube Playlist Id
     */
    public void acquire(String playlistId)
    {
        this.playlistId = playlistId;
        this.execute();
    }

    public void setYouTubePlaylistReceiver(YouTubePlaylistReceiver youTubePlaylistReceiver)
    {
        this.youTubePlaylistReceiver = youTubePlaylistReceiver;
    }

    private List<YouTubeVideo> acquirePlaylistVideos()
    {

        List<PlaylistItem> playlistItemList = new ArrayList<>();
        List<YouTubeVideo> playlistItems = new ArrayList<>();
        String nextToken = "";

        // Retrieve the playlist of the channel's uploaded videos.
        YouTube.PlaylistItems.List playlistItemRequest;
        try {
            playlistItemRequest = youtube.playlistItems().list(YOUTUBE_ACQUIRE_PLAYLIST_PART);
            playlistItemRequest.setPlaylistId(playlistId);
            playlistItemRequest.setKey(YOUTUBE_API_KEY);
            playlistItemRequest.setMaxResults(Config.MAX_VIDEOS_RETURNED);
            playlistItemRequest.setFields(YOUTUBE_ACQUIRE_PLAYLIST_FIELDS);
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
                Log.d(TAG, "loadInBackground: 404 error");
                if (youTubePlaylistReceiver != null)
                    youTubePlaylistReceiver.onPlaylistNotFound(playlistId, e.getStatusCode());
            } else {
                e.printStackTrace();
            }

            return Collections.emptyList();
        } catch (UnknownHostException e) {
            Toast.makeText(context.getApplicationContext(), "Check internet connection",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return Collections.emptyList();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        // Videos to get duration
        YouTube.Videos.List videosList = null;
        try {
            videosList = youtube.videos().list(YOUTUBE_PLAYLIST_VIDEO_PART);
            videosList.setKey(YOUTUBE_API_KEY);
            videosList.setFields(YOUTUBE_PLAYLIST_VIDEO_FIELDS);

            // Save all ids from searchList list in order to find video list
            StringBuilder contentDetails = new StringBuilder();

            int index = 0;
            for (PlaylistItem result : playlistItemList) {
                contentDetails.append(result.getContentDetails().getVideoId());
                if (index < playlistItemList.size() - 1)
                    contentDetails.append(",");
                index++;
            }

            // Find video list
            videosList.setId(contentDetails.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (videosList == null) return Collections.emptyList();

        VideoListResponse resp = null;
        try {
            resp = videosList.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (resp == null) return Collections.emptyList();

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
        return playlistItems;
    }
}