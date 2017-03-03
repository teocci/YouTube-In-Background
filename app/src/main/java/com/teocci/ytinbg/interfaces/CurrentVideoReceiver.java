package com.teocci.ytinbg.interfaces;

import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;

import com.teocci.ytinbg.model.YouTubeVideo;

/**
 * Created by teocci on 3/2/17.
 */

public interface CurrentVideoReceiver
{
    public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state);

    public void onCurrentVideoChanged(YouTubeVideo currentVideo);

}
