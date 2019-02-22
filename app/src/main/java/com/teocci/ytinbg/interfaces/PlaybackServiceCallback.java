package com.teocci.ytinbg.interfaces;

import android.support.v4.media.session.PlaybackStateCompat;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Feb-22
 */
public interface PlaybackServiceCallback
{
    void onPlaybackStart();

    void onNotificationRequired();

    void onPlaybackStop();

    void onPlaybackStateUpdated(PlaybackStateCompat newState);
}
