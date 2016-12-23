package com.teocci.ytinbg.interfaces;

import com.teocci.ytinbg.model.YouTubeVideo;

import java.util.ArrayList;

/**
 * Interface which enables passing videos to the fragments
 * Created by Teocci on 10.3.16..
 */
public interface YouTubeVideosReceiver
{
    void onVideosReceived(ArrayList<YouTubeVideo> youTubeVideos);

    void onPlaylistNotFound(String playlistId, int errorCode);
}