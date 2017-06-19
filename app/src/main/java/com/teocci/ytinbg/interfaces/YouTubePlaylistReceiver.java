package com.teocci.ytinbg.interfaces;

import com.teocci.ytinbg.model.YouTubePlaylist;
import com.teocci.ytinbg.model.YouTubeVideo;

import java.util.List;

/**
 * Interface which enables passing playlist to the fragments
 * Created by Teocci on 15.3.16..
 */
public interface YouTubePlaylistReceiver
{
    void onPlaylistReceived(List<YouTubePlaylist> youTubePlaylistList);

    void onPlaylistNotFound(String playlistId, int errorCode);

    void onPlaylistVideoReceived(List<YouTubeVideo> youTubeVideos);
}
