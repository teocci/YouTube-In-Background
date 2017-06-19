package com.teocci.ytinbg.playback;

import android.content.res.Resources;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.teocci.ytinbg.interfaces.Playback;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.utils.LogHelper;

import java.util.List;


/**
 * Created by teocci.
 * Manage the interactions among the container service, the queue manager and the actual playback.
 *
 * @author teocci@yandex.com on 2017-Jun-08
 */

public class PlaybackManager implements Playback.Callback
{

    private static final String TAG = PlaybackManager.class.getSimpleName();

    private QueueManager queueManager;
    private Resources resources;
    private Playback playback;
    private PlaybackServiceCallback serviceCallback;
    private MediaSessionCallback mediaSessionCallback;

    public PlaybackManager(PlaybackServiceCallback serviceCallback, Resources resources,
                           QueueManager queueManager,
                           Playback playback)
    {
        this.serviceCallback = serviceCallback;
        this.resources = resources;
        this.queueManager = queueManager;
        this.mediaSessionCallback = new MediaSessionCallback();
        this.playback = playback;
        this.playback.setCallback(this);
    }

    public Playback getPlayback()
    {
        return playback;
    }

    public MediaSessionCompat.Callback getMediaSessionCallback()
    {
        return mediaSessionCallback;
    }

    /**
     * Handle a request to play music
     */
    public void handlePlayRequest()
    {
        LogHelper.e(TAG, "handlePlayRequest: mState=" + playback.getState());
        YouTubeVideo currentYouTubeVideo = queueManager.getCurrentVideo();
        if (currentYouTubeVideo != null) {
            serviceCallback.onPlaybackStart();
            playback.play(currentYouTubeVideo);
        }
    }

    /**
     * Handle a request to pause music
     */
    public void handlePauseRequest()
    {
        LogHelper.d(TAG, "handlePauseRequest: mState=" + playback.getState());
        if (playback.isPlaying()) {
            playback.pause();
            serviceCallback.onPlaybackStop();
        }
    }

    /**
     * Handle a request to stop music
     *
     * @param withError Error message in case the stop has an unexpected cause. The error
     *                  message will be set in the PlaybackState and will be visible to
     *                  MediaController clients.
     */
    public void handleStopRequest(String withError)
    {
        LogHelper.d(TAG, "handleStopRequest: mState=" + playback.getState() + " error=", withError);
        playback.stop(true);
        serviceCallback.onPlaybackStop();
        updatePlaybackState(withError);
    }


    /**
     * Update the current media player state, one of the following, optionally showing an error message.
     * <ul>
     * <li> {@link PlaybackState#STATE_NONE}</li>
     * <li> {@link PlaybackState#STATE_STOPPED}</li>
     * <li> {@link PlaybackState#STATE_PLAYING}</li>
     * <li> {@link PlaybackState#STATE_PAUSED}</li>
     * <li> {@link PlaybackState#STATE_FAST_FORWARDING}</li>
     * <li> {@link PlaybackState#STATE_REWINDING}</li>
     * <li> {@link PlaybackState#STATE_BUFFERING}</li>
     * <li> {@link PlaybackState#STATE_ERROR}</li>
     * <li> {@link PlaybackState#STATE_CONNECTING}</li>
     * <li> {@link PlaybackState#STATE_SKIPPING_TO_PREVIOUS}</li>
     * <li> {@link PlaybackState#STATE_SKIPPING_TO_NEXT}</li>
     * <li> {@link PlaybackState#STATE_SKIPPING_TO_QUEUE_ITEM}</li>
     * </ul>
     *
     * @param error if not null, error message to present to the user.
     */
    public void updatePlaybackState(String error)
    {
        LogHelper.d(TAG, "updatePlaybackState, playback state=" + playback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (playback != null && playback.isConnected()) {
            position = playback.getCurrentStreamPosition();
        }

        // Noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat
                .Builder()
                .setActions(getAvailableActions());

        int state = playback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        // Noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        YouTubeVideo currentYouTubeVideo = queueManager.getCurrentVideo();
        if (currentYouTubeVideo != null) {
            stateBuilder.setActiveQueueItemId(queueManager.getCurrentVideoIndex(currentYouTubeVideo.getId()));
        }

        serviceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_PAUSED) {
            serviceCallback.onNotificationRequired();
        }
    }

    private long getAvailableActions()
    {
        long actions =
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (playback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion()
    {
//        The media player finished playing the current song, so we go ahead
//        and start the next.
        if (queueManager.skipQueuePosition(1)) {
            handlePlayRequest();
            queueManager.updateYouTubeVideo();
        } else {
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state)
    {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error)
    {
        updatePlaybackState(error);
    }

    @Override
    public void setCurrentMediaId(String mediaId)
    {
        LogHelper.e(TAG, "setCurrentYouTubeVideoId", mediaId);
//        queueManager.setQueueFromMusic(mediaId);
    }


    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     *
     * @param playback switch to this playback
     */
    public void switchToPlayback(Playback playback, boolean resumePlaying)
    {
        if (playback == null) {
            throw new IllegalArgumentException("Playback cannot be null");
        }
        // Suspends current state.
        int oldState = this.playback.getState();
        long pos = this.playback.getCurrentStreamPosition();
        String currentMediaId = this.playback.getCurrentYouTubeVideoId();
        this.playback.stop(false);
        playback.setCallback(this);
        playback.setCurrentYouTubeVideoId(currentMediaId);
        playback.seekTo(pos < 0 ? 0 : pos);
        playback.start();
        // Swaps instance.
        this.playback = playback;
        switch (oldState) {
            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_PAUSED:
                this.playback.pause();
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                YouTubeVideo currentYouTubeVideo = queueManager.getCurrentVideo();
                if (resumePlaying && currentYouTubeVideo != null) {
                    LogHelper.e(TAG, "switchToPlayback: call | playback.play");
                    this.playback.play(currentYouTubeVideo);
                } else if (!resumePlaying) {
                    this.playback.pause();
                } else {
                    this.playback.stop(true);
                }
                break;
            case PlaybackStateCompat.STATE_NONE:
                break;
            default:
                LogHelper.d(TAG, "Default called. Old state is ", oldState);
        }
    }

    public void initPlaylist(YouTubeVideo currentYouTubeVideo, List<YouTubeVideo> ytVideoList)
    {
        if (currentYouTubeVideo != null) {
            queueManager.setCurrentQueue(currentYouTubeVideo, ytVideoList);
            queueManager.updateYouTubeVideo();
        }
    }

    public long getDuration()
    {
        long duration = this.playback.getDuration() > -1 ? this.playback.getDuration() : -1;
        LogHelper.e(TAG, "getDuration: " + duration);
        return duration;
    }

    public void updateYouTubeVideo()
    {
        queueManager.updateYouTubeVideo();
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback
    {
        @Override
        public void onPlay()
        {
            LogHelper.d(TAG, "play");
            if (queueManager.getCurrentVideo() != null) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSeekTo(long position)
        {
            LogHelper.d(TAG, "onSeekTo:", position);
            playback.seekTo((int) position);
        }

        @Override
        public void onPause()
        {
            LogHelper.d(TAG, "pause. current state=" + playback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop()
        {
            LogHelper.d(TAG, "stop. current state=" + playback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext()
        {
            LogHelper.d(TAG, "skipToNext");
            if (queueManager.skipQueuePosition(1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
            queueManager.updateYouTubeVideo();
        }

        @Override
        public void onSkipToPrevious()
        {
            if (queueManager.skipQueuePosition(-1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
            queueManager.updateYouTubeVideo();
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras)
        {

            LogHelper.e(TAG, "Unsupported action: ", action);

        }

        /**
         * Handle free and contextual searches.
         * <p/>
         * All voice searches on Android Auto are sent to this method through a connected
         * {@link android.support.v4.media.session.MediaControllerCompat}.
         * <p/>
         * Threads and async handling:
         * Search, as a potentially slow operation, should run in another thread.
         * <p/>
         * Since this method runs on the main thread, most apps with non-trivial metadata
         * should defer the actual search to another thread (for example, by using
         * an {@link AsyncTask} as we do here).
         **/
        @Override
        public void onPlayFromSearch(final String query, final Bundle extras)
        {
            LogHelper.d(TAG, "playFromSearch  query=", query, " extras=", extras);

//            playback.setState(PlaybackStateCompat.STATE_CONNECTING);
//            boolean successSearch = queueManager.setQueueFromSearch(query, extras);
//            if (successSearch) {
//                handlePlayRequest();
//                queueManager.updateYouTubeVideo();
//            } else {
//                updatePlaybackState("Could not find music");
//            }
        }
    }


    public interface PlaybackServiceCallback
    {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
