package com.teocci.ytinbg.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.BackgroundAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.interfaces.CurrentVideoReceiver;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * A full screen player that shows the current playing music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class FullScreenPlayerActivity extends ActionBarCastActivity {
    private static final String TAG = LogHelper.makeLogTag(FullScreenPlayerActivity.class);
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private ImageView skipPrev;
    private ImageView skipNext;
    private ImageView playPause;
    private TextView startTextView;
    private TextView endTextView;
    private SeekBar seekBar;
    private TextView line1;
    private TextView line2;
    private TextView line3;
    private ProgressBar loading;
    private View controllersView;
    private Drawable pauseDrawable;
    private Drawable playDrawable;
    private ImageView backgroundImage;

    private String mCurrentArtUrl;
    private final Handler handler = new Handler();
    private MediaBrowserCompat mediaBrowser;

    private final Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduledFuture;
    private PlaybackStateCompat lastPlaybackState;

    private final CurrentVideoReceiver currentVideoReceiver = new CurrentVideoReceiver(){
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state)
        {
            LogHelper.d(TAG, "onPlaybackstate changed", state);
            updatePlaybackState(state);
        }

        @Override
        public void onCurrentVideoChanged(YouTubeVideo currentVideo)
        {
            if (currentVideo != null) {
                updateYouTubeVideoDescription(currentVideo);
                updateDuration(currentVideo);
            }
        }
    };

    private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            LogHelper.d(TAG, "onPlaybackstate changed", state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
//                updateMediaDescription(metadata.getDescription());
//                updateDuration(metadata);
            }
        }
    };

    private final MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            LogHelper.d(TAG, "onConnected");
            try {
                connectToSession(mediaBrowser.getSessionToken());
            } catch (RemoteException e) {
                LogHelper.e(TAG, e, "could not connect media controller");
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_player);
        initializeToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        backgroundImage = (ImageView) findViewById(R.id.background_image);
        pauseDrawable = ContextCompat.getDrawable(this, R.drawable.ic_pause_white_48dp);
        playDrawable = ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_white_48dp);
        playPause = (ImageView) findViewById(R.id.play_pause);
        skipNext = (ImageView) findViewById(R.id.next);
        skipPrev = (ImageView) findViewById(R.id.prev);
        startTextView = (TextView) findViewById(R.id.startText);
        endTextView = (TextView) findViewById(R.id.endText);
        seekBar = (SeekBar) findViewById(R.id.seekBar1);
        line1 = (TextView) findViewById(R.id.line1);
        line2 = (TextView) findViewById(R.id.line2);
        line3 = (TextView) findViewById(R.id.line3);
        loading = (ProgressBar) findViewById(R.id.progressBar1);
        controllersView = findViewById(R.id.controllers);

        skipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                    getSupportMediaController().getTransportControls();
                controls.skipToNext();
            }
        });

        skipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                    getSupportMediaController().getTransportControls();
                controls.skipToPrevious();
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStateCompat state = getSupportMediaController().getPlaybackState();
                if (state != null) {
                    MediaControllerCompat.TransportControls controls =
                            getSupportMediaController().getTransportControls();
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING: // fall through
                        case PlaybackStateCompat.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        default:
                            LogHelper.d(TAG, "onClick with state ", state.getState());
                    }
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                startTextView.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getSupportMediaController().getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null) {
            updateFromParams(getIntent());
        }

        mediaBrowser = new MediaBrowserCompat(this,
            new ComponentName(this, BackgroundAudioService.class), connectionCallback, null);
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException
    {
        MediaControllerCompat mediaController = new MediaControllerCompat(FullScreenPlayerActivity.this, token);
        if (mediaController.getMetadata() == null) {
            finish();
            return;
        }

        setSupportMediaController(mediaController);
        mediaController.registerCallback(controllerCallback);
        PlaybackStateCompat state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadataCompat metadata = mediaController.getMetadata();
//        if (metadata != null) {
//            updateYouTubeVideoDescription(metadata.getDescription());
//            updateDuration(metadata);
//        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void updateFromParams(Intent intent) {
        if (intent != null) {
            YouTubeVideo currentVideo = intent.getParcelableExtra(Config.CURRENT_YOUTUBE_VIDEO);
            if (currentVideo != null) {
                updateYouTubeVideoDescription(currentVideo);
            }
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!executorService.isShutdown()) {
            scheduledFuture = executorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            handler.post(updateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
        }
        if (getSupportMediaController() != null) {
            getSupportMediaController().unregisterCallback(controllerCallback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        executorService.shutdown();
    }

    private void updateYouTubeVideoDescription(YouTubeVideo currentVideo) {
        if (currentVideo == null) {
            return;
        }
        LogHelper.d(TAG, "updateYouTubeVideoDescription called ");
        line1.setText(currentVideo.getTitle());
        line2.setText(currentVideo.getViewCount());
        //load bitmap for largeScreen
        if (currentVideo.getThumbnailURL() != null && !currentVideo.getThumbnailURL().isEmpty()) {
            Picasso.with(this).load(currentVideo.getThumbnailURL()).into(backgroundImage);
        }
    }

    private void updateDuration(YouTubeVideo youTubeVideo) {
        if (youTubeVideo == null) {
            return;
        }
        LogHelper.d(TAG, "updateDuration called ");
        int duration = Integer.parseInt(youTubeVideo.getDuration());
        seekBar.setMax(duration);
        endTextView.setText(DateUtils.formatElapsedTime(duration/1000));
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        lastPlaybackState = state;
        if (getSupportMediaController() != null && getSupportMediaController().getExtras() != null) {
            String castName = getSupportMediaController()
                    .getExtras().getString(Config.EXTRA_CONNECTED_CAST);
            String line3Text = castName == null ? "" : getResources()
                        .getString(R.string.casting_to_device, castName);
            line3.setText(line3Text);
        }

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                loading.setVisibility(INVISIBLE);
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(pauseDrawable);
                controllersView.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                controllersView.setVisibility(VISIBLE);
                loading.setVisibility(INVISIBLE);
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                loading.setVisibility(INVISIBLE);
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                playPause.setVisibility(INVISIBLE);
                loading.setVisibility(VISIBLE);
                line3.setText(R.string.loading);
                stopSeekbarUpdate();
                break;
            default:
                LogHelper.d(TAG, "Unhandled state ", state.getState());
        }

        skipNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
            ? INVISIBLE : VISIBLE );
        skipPrev.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
            ? INVISIBLE : VISIBLE );
    }

    private void updateProgress() {

        long currentPosition = lastPlaybackState.getPosition();
        if (lastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    lastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * lastPlaybackState.getPlaybackSpeed();
        }
        seekBar.setProgress((int) currentPosition);
    }
}
