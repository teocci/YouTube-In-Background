package com.teocci.ytinbg.interfaces;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.teocci.ytinbg.model.YouTubeVideo;

import java.util.ArrayList;

/**
 * Interface which enables passing videos to the fragments
 * Created by Teocci on 10.3.16..
 */
public interface YouTubeVideoReceiver
{
    void onVideosReceived(ArrayList<YouTubeVideo> youTubeVideos, YouTube.Search.List searchList, String nextPageToken);

    void onPlaylistNotFound(String playlistId, int errorCode);
}