package com.teocci.ytinbg.playback;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.teocci.ytinbg.BackgroundExoAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.interfaces.Playback;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.utils.LogHelper;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;
import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_LOSS;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.net.wifi.WifiManager.WIFI_MODE_FULL;
import static com.google.android.exoplayer2.Player.STATE_BUFFERING;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_IDLE;
import static com.google.android.exoplayer2.Player.STATE_READY;
import static com.teocci.ytinbg.utils.Config.ACTION_CMD;
import static com.teocci.ytinbg.utils.Config.APP_NAME;
import static com.teocci.ytinbg.utils.Config.CMD_NAME;
import static com.teocci.ytinbg.utils.Config.CMD_PAUSE;
import static com.teocci.ytinbg.utils.Config.KEY_LOCK;
import static com.teocci.ytinbg.utils.Utils.getBestStream;
import static com.teocci.ytinbg.utils.Utils.validateUrl;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-08
 */

public class LocalPlayback implements Playback
{
    private static final String TAG = LogHelper.makeLogTag(LocalPlayback.class);

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private final Context context;
    private final WifiManager.WifiLock wifiLock;

    private Callback callback;

    private String currentYouTubeVideoId;

    private int currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;

    private final AudioManager audioManager;
    private final ExoPlayerEventListener eventListener = new ExoPlayerEventListener();

    private SimpleExoPlayer exoPlayer;

    // Whether to return STATE_NONE or STATE_STOPPED when exoPlayer is null;
    private boolean exoPlayerNullIsStopped = false;

    private boolean playOnFocusGain;
    private boolean audioNoisyReceiverRegistered;

    private boolean extractingYTURL = false;
    private boolean extracted = false;

    private final IntentFilter audioNoisyIntentFilter = new IntentFilter(ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                LogHelper.e(TAG, "audioNoisyReceiver: Headphones disconnected.");
                if (isPlaying()) {
//                    pauseVideo();
                    LogHelper.e(TAG, "audioNoisyReceiver: Video Pause");
                    Intent i = new Intent(context, BackgroundExoAudioService.class);
                    i.setAction(ACTION_CMD);
                    i.putExtra(CMD_NAME, CMD_PAUSE);
                    LocalPlayback.this.context.startService(i);
                }
            }
        }
    };

//    private final BroadcastReceiver mediaButtonReceiver = new BroadcastReceiver()
//    {
//        @Override
//        public void onReceive(Context context, Intent intent)
//        {
//            LogHelper.e(TAG, "mediaButtonReceiver");
//            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
//                if (isPlaying()) {
//                    pauseVideo();
//                    LogHelper.e(TAG, "mediaButtonReceiver: Video Pause");
//                } else {
//                    playVideo();
//                }
//            }
//        }
//    };

    public LocalPlayback(Context context)
    {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext;

        this.audioManager = (AudioManager) applicationContext.getSystemService(AUDIO_SERVICE);
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        this.wifiLock = ((WifiManager) applicationContext
                .getSystemService(WIFI_SERVICE))
                .createWifiLock(WIFI_MODE_FULL, KEY_LOCK);
    }

    @Override
    public void start()
    {
        // Nothing to do
    }

    @Override
    public void stop(boolean notifyListeners)
    {
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        releaseResources(true);
    }

    @Override
    public void setState(int state)
    {
        // Nothing to do (exoPlayer holds its own state).
    }

    @Override
    public int getState()
    {
        if (exoPlayer == null) {
            return exoPlayerNullIsStopped
                    ? PlaybackStateCompat.STATE_STOPPED
                    : PlaybackStateCompat.STATE_NONE;
        }
        switch (exoPlayer.getPlaybackState()) {
            case STATE_IDLE:
                return PlaybackStateCompat.STATE_PAUSED;
            case STATE_BUFFERING:
                return PlaybackStateCompat.STATE_BUFFERING;
            case STATE_READY:
                return exoPlayer.getPlayWhenReady()
                        ? PlaybackStateCompat.STATE_PLAYING
                        : PlaybackStateCompat.STATE_PAUSED;
            case STATE_ENDED:
                return PlaybackStateCompat.STATE_PAUSED;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    @Override
    public boolean isConnected()
    {
        return true;
    }

    @Override
    public boolean isPlaying()
    {
        return playOnFocusGain || (exoPlayer != null && exoPlayer.getPlayWhenReady());
    }

    @Override
    public long getCurrentStreamPosition()
    {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    @Override
    public void updateLastKnownStreamPosition()
    {
        // Nothing to do. Position maintained by ExoPlayer.
    }

    @Override
    public long getDuration()
    {
        if (exoPlayer != null && exoPlayer.getPlayWhenReady()) {
            LogHelper.e(TAG, "getDuration");
            return exoPlayer.getDuration();
        }
        return -1;
    }

    @Override
    public void play(YouTubeVideo youTubeVideo)
    {
        LogHelper.e(TAG, "play");
        playOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String youTubeVideoId = youTubeVideo.getId();
        boolean videoHasChanged = !TextUtils.equals(youTubeVideoId, currentYouTubeVideoId);
        if (videoHasChanged) {
//            LogHelper.e(TAG, "play | videoHasChanged");
            extracted = false;
            currentYouTubeVideoId = youTubeVideoId;
        }
        if (videoHasChanged || exoPlayer == null || !extracted) {
//            LogHelper.e(TAG, "play | calling: extractUrlAndPlay " + (!extractingYTURL ? "true" : "false"));
            if (!extractingYTURL) extractUrlAndPlay();
        } else if (!extractingYTURL) {
//            LogHelper.e(TAG, "play | calling: seekTo and configurePlayerState");
            seekTo(0);
            configurePlayerState();
        }
    }

    @Override
    public void pause()
    {
        // Pause player and cancel the 'foreground service' state.
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
        }
        // While paused, retain the player instance, but give up audio focus.
        releaseResources(false);
        unregisterAudioNoisyReceiver();
    }

    @Override
    public void seekTo(long position)
    {
        LogHelper.d(TAG, "seekTo called with ", position);
        if (exoPlayer != null) {
            registerAudioNoisyReceiver();
            exoPlayer.seekTo(position);
        }
    }

    @Override
    public void setCallback(Callback callback)
    {
        this.callback = callback;
    }

    public void setCurrentYouTubeVideoId(String youTubeVideoId)
    {
        this.currentYouTubeVideoId = youTubeVideoId;
    }

    public String getCurrentYouTubeVideoId()
    {
        return currentYouTubeVideoId;
    }

    private void tryToGetAudioFocus()
    {
        LogHelper.d(TAG, "tryToGetAudioFocus");
        int result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                STREAM_MUSIC,
                AUDIOFOCUS_GAIN
        );
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocusState = AUDIO_FOCUSED;
        } else {
            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Extracts link from youtube video ID, so mediaPlayer can play it
     */
    @SuppressLint("StaticFieldLeak")
    private void extractUrlAndPlay()
    {
        final String youtubeLink = "https://youtube.com/watch?v=" + currentYouTubeVideoId;
        extractingYTURL = true;

        LogHelper.e(TAG, "extractUrlAndPlay | called");
        new YouTubeExtractor(context)
        {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta)
            {
                final Context context = weakContext.get();
                if (context == null) {
                    return;
                }
//                LogHelper.e(TAG, "extractUrlAndPlay | started");
                if (ytFiles == null) {
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.toast_message_error_extracting,
                                    videoMeta.getTitle()
                            ),
                            Toast.LENGTH_SHORT
                    ).show();
//                    LogHelper.e(TAG, "extractUrlAndPlay | ended-error");
                    extractingYTURL = false;
                    extracted = false;
                    return;
                }
                YtFile ytFile = getBestStream(ytFiles);
                LogHelper.e(TAG, ytFile.getUrl());
                if (validateUrl(ytFile.getUrl())) {
//                    LogHelper.e(TAG, "extractUrlAndPlay | validateUrl extracted");

                    releaseResources(false); // Release everything except the player
                    if (exoPlayer == null) {
                        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());
                        exoPlayer.addListener(eventListener);
                    }

                    @C.AudioUsage int usage = Util.getAudioUsageForStreamType(STREAM_MUSIC);
                    @C.AudioContentType int contentType = Util.getAudioContentTypeForStreamType(STREAM_MUSIC);
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setUsage(usage)
                            .setContentType(contentType)
                            .build();

                    exoPlayer.setAudioAttributes(audioAttributes);

                    // Produces DataSource instances through which media data is loaded.
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                            context,
                            Util.getUserAgent(context, APP_NAME),
                            null
                    );
                    // Produces Extractor instances for parsing the media data.
//                    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                    // The MediaSource represents the media to be played.
//                    MediaSource mediaSource = new ExtractorMediaSource(
//                            Uri.parse(ytFile.getUrl()),
//                            dataSourceFactory,
//                            extractorsFactory,
//                            null,
//                            null
//                    );

                    MediaSource mediaSource = new ExtractorMediaSource
                            .Factory(dataSourceFactory)
                            .createMediaSource(Uri.parse(ytFile.getUrl()));

                    // Prepares media to play (happens on background thread) and triggers
                    // {@code onPlayerStateChanged} callback when the stream is ready to play.
//                    LogHelper.e(TAG, "extractUrlAndPlay | exoPlayer calls: prepare");
                    exoPlayer.prepare(mediaSource);
                    // If we are streaming from the internet, we want to hold a
                    // Wifi lock, which prevents the Wifi radio from going to
                    // sleep while the song is playing.
                    wifiLock.acquire();
                    extracted = true;

//                    LogHelper.e(TAG, "extractUrlAndPlay calls: configurePlayerState");
                    configurePlayerState();
                } else {
//                        Log.e(TAG, "No Link found");
                    Toast.makeText(
                            context,
                            context.getResources().getString(
                                    R.string.toast_message_error_playing_url,
                                    videoMeta.getTitle()
                            ),
                            Toast.LENGTH_SHORT
                    ).show();
//                    LogHelper.e(TAG, "extractUrlAndPlay | ended-error: No Link found");
                }

                extractingYTURL = false;
            }
        }.extract(youtubeLink, true, true);
    }

    private void giveUpAudioFocus()
    {
        LogHelper.d(TAG, "giveUpAudioFocus");
        if (audioManager.abandonAudioFocus(audioFocusChangeListener) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Reconfigures the player according to audio focus settings and starts/restarts it. This method
     * starts/restarts the ExoPlayer instance respecting the current audio focus state. So if we
     * have focus, it will play normally; if we don't have focus, it will either leave the player
     * paused or set it to a low volume, depending on what is permitted by the current focus
     * settings.
     */
    private void configurePlayerState()
    {
        LogHelper.d(TAG, "configurePlayerState. currentAudioFocusState=", currentAudioFocusState);
        if (currentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pause();
        } else {
            registerAudioNoisyReceiver();

            if (currentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we 'duck', ie: play softly
                exoPlayer.setVolume(VOLUME_DUCK);
            } else {
                exoPlayer.setVolume(VOLUME_NORMAL);
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                exoPlayer.setPlayWhenReady(true);
                playOnFocusGain = false;
            }
        }
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener()
            {
                @Override
                public void onAudioFocusChange(int focusChange)
                {
                    LogHelper.d(TAG, "onAudioFocusChange. focusChange=", focusChange);
                    switch (focusChange) {
                        case AUDIOFOCUS_GAIN:
                            currentAudioFocusState = AUDIO_FOCUSED;
                            break;
                        case AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                            currentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
                            break;
                        case AUDIOFOCUS_LOSS_TRANSIENT:
                            // Lost audio focus, but will gain it back (shortly), so note whether
                            // playback should resume
                            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                            playOnFocusGain = exoPlayer != null && exoPlayer.getPlayWhenReady();
                            break;
                        case AUDIOFOCUS_LOSS:
                            // Lost audio focus, probably "permanently"
                            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                            break;
                    }

                    if (exoPlayer != null) {
                        // Update the player state based on the change
                        configurePlayerState();
                    }
                }
            };

    /**
     * Releases resources used by the service for playback, which is mostly just the WiFi lock for
     * local playback. If requested, the ExoPlayer instance is also released.
     *
     * @param releasePlayer Indicates whether the player should also be released
     */
    private void releaseResources(boolean releasePlayer)
    {
        LogHelper.d(TAG, "releaseResources. releasePlayer=", releasePlayer);

        // Stops and releases player (if requested and available).
        if (releasePlayer && exoPlayer != null) {
            exoPlayer.release();
            exoPlayer.removeListener(eventListener);
            exoPlayer = null;
            exoPlayerNullIsStopped = true;
            playOnFocusGain = false;
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void registerAudioNoisyReceiver()
    {
        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
            audioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver()
    {
        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(audioNoisyReceiver);
            audioNoisyReceiverRegistered = false;
        }
    }

    private final class ExoPlayerEventListener implements ExoPlayer.EventListener
    {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason)
        {
            // Nothing to do.
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections)
        {
            // Nothing to do.
        }

        @Override
        public void onLoadingChanged(boolean isLoading)
        {
            // Nothing to do.
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
        {
            switch (playbackState) {
                case STATE_IDLE:
                case STATE_BUFFERING:
                case STATE_READY:
                    if (callback != null) {
                        callback.onPlaybackStatusChanged(getState());
                    }
                    break;
                case STATE_ENDED:
                    // The media player finished playing the current song.
                    if (callback != null) {
                        callback.onCompletion();
                    }
                    break;
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode)
        {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled)
        {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error)
        {
            final String what;
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    what = error.getSourceException().getMessage();
                    break;
                case ExoPlaybackException.TYPE_RENDERER:
                    what = error.getRendererException().getMessage();
                    break;
                case ExoPlaybackException.TYPE_UNEXPECTED:
                    what = error.getUnexpectedException().getMessage();
                    break;
                default:
                    what = "Unknown: " + error;
            }

            LogHelper.e(TAG, "ExoPlayer error: what=" + what);
            if (callback != null) {
                callback.onError("ExoPlayer error " + what);
            }
        }

        @Override
        public void onPositionDiscontinuity(int reason)
        {
            // Nothing to do.
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters)
        {
            // Nothing to do.
        }

        @Override
        public void onSeekProcessed()
        {

        }
    }
}