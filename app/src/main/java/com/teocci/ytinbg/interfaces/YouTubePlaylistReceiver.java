package com.teocci.ytinbg.interfaces;

import com.teocci.ytinbg.model.YouTubePlaylist;

import java.util.ArrayList;

/**
 * Interface which enables passing playlist to the fragments
 * Created by Teocci on 15.3.16..
 */
public interface YouTubePlaylistReceiver
{
    void onPlaylistReceived(ArrayList<YouTubePlaylist> youTubePlaylistList);
}
