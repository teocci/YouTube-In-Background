package com.teocci.ytinbg.interfaces;

import com.teocci.ytinbg.model.YouTubeVideo;

import java.util.List;

/**
 * Interface which enables passing videos to the fragments
 * Created by Teocci on 10.3.16..
 */
public interface YouTubeVideoReceiver
{
    void onVideosReceived(List<YouTubeVideo> youTubeVideos, String currentPageToken, String nextPageToken);
}