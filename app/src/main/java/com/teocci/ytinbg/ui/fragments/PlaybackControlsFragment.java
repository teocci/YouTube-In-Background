package com.teocci.ytinbg.ui.fragments;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.utils.LogHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * Created by teocci.
 * <p>
 * This fragment shows the current playing youtube video with. Also has controls to seek/pause/play the audio.
 *
 * @author teocci@yandex.com on 2017-Jul-03
 */

public class PlaybackControlsFragment extends Fragment
{
    private static final String TAG = LogHelper.makeLogTag(PlaybackControlsFragment.class);

    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private TextView videoTitle;
    private TextView videoViewsNumber;
    private TextView videoExtraInfo;

    private ImageView videoThumbnail;
    private ImageView skipPrev;
    private ImageView skipNext;
    private ImageView playPause;

    private TextView tvStart;
    private TextView tvEnd;
    private SeekBar seekbar;
    private TextView mLine1;
    private TextView mLine2;
    private TextView mLine3;

    private View controls;
    private Drawable pauseDrawable;
    private Drawable playDrawable;
    private ImageView backgroundImage;

    private String artUrl;

    private final Handler handler = new Handler();
    private MediaBrowserCompat mediaBrowser;

    private final Runnable updateProgressTask = this::updateProgress;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduleFuture;
    private PlaybackStateCompat lastPlaybackState;

    private FragmentActivity activity;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback()
    {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state)
        {
            LogHelper.d(TAG, "Received playback state change to state ", state.getState());
            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata)
        {
            if (metadata == null) {
                return;
            }
            LogHelper.d(TAG, "Received metadata state change to mediaId=", metadata.getDescription().getMediaId(),
                    " song=", metadata.getDescription().getTitle()
            );
            PlaybackControlsFragment.this.onMetadataChanged(metadata);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);
        activity = getActivity();

//        playPause = (ImageButton) rootView.findViewById(R.id.play_pause);
//        playPause.setEnabled(true);
//        playPause.setOnClickListener(mButtonListener);

        videoTitle = rootView.findViewById(R.id.video_title);
        videoViewsNumber = rootView.findViewById(R.id.views_number);
        videoExtraInfo = rootView.findViewById(R.id.extra_info);
        videoThumbnail = rootView.findViewById(R.id.video_thumbnail);
        pauseDrawable = ContextCompat.getDrawable(activity, R.drawable.ic_pause_white_48dp);
        playDrawable = ContextCompat.getDrawable(activity, R.drawable.ic_play_arrow_white_48dp);
        playPause = rootView.findViewById(R.id.play_pause);
        skipNext = rootView.findViewById(R.id.next);
        skipPrev = rootView.findViewById(R.id.prev);
        tvStart = rootView.findViewById(R.id.startText);
        tvEnd = rootView.findViewById(R.id.endText);
        seekbar = rootView.findViewById(R.id.seekBar1);
        controls = rootView.findViewById(R.id.controllers);

        skipNext.setOnClickListener(v -> {
            MediaControllerCompat.TransportControls controls = MediaControllerCompat
                    .getMediaController(activity)
                    .getTransportControls();
            controls.skipToNext();
        });

        skipPrev.setOnClickListener(v -> {
            MediaControllerCompat.TransportControls controls = MediaControllerCompat
                    .getMediaController(activity)
                    .getTransportControls();
            controls.skipToPrevious();
        });


        playPause.setOnClickListener(v -> {
            MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
            PlaybackStateCompat stateObj = controller.getPlaybackState();
            final int state = stateObj == null ? STATE_NONE : stateObj.getState();


            switch (state) {
                case STATE_PLAYING: // fall through
                case STATE_BUFFERING:
                case PlaybackStateCompat.STATE_CONNECTING:
                    pauseMedia();
                    stopSeekbarUpdate();
                    break;
                case STATE_PAUSED:
                case STATE_STOPPED:
                case STATE_NONE:
                    playMedia();
                    scheduleSeekbarUpdate();
                    break;
                default:
                    LogHelper.d(TAG, "onClick with state ", state);
            }
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                tvStart.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                MediaControllerCompat
                        .getMediaController(activity)
                        .getTransportControls()
                        .seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        seekbar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        seekbar.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

//        rootView.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                Intent intent = new Intent(activity, FullScreenPlayerActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
//                MediaMetadataCompat metadata = controller.getMetadata();
//                if (metadata != null) {
//                    intent.putExtra(MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION,
//                            metadata.getDescription());
//                }
//                startActivity(intent);
//            }
//        });
        return rootView;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        LogHelper.e(TAG, "fragment.onStart");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
        if (controller != null) {
            onConnected();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        LogHelper.e(TAG, "fragment.onStop");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
        if (controller != null) {
            controller.unregisterCallback(mediaControllerCallback);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopSeekbarUpdate();
        executorService.shutdown();
    }

    public void onConnected()
    {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
        LogHelper.e(TAG, "onConnected, mediaController==null? ", controller == null);
        if (controller != null) {
            controller.registerCallback(mediaControllerCallback);
            onPlaybackStateChanged(controller.getPlaybackState());
            onMetadataChanged(controller.getMetadata());

            PlaybackStateCompat state = controller.getPlaybackState();
            updatePlaybackState(state);
            MediaMetadataCompat metadata = controller.getMetadata();
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
            updateProgress();
            if (state != null && (state.getState() == STATE_PLAYING || state.getState() == STATE_BUFFERING)) {
                scheduleSeekbarUpdate();
            }
        }
    }

    public void setVideoExtraInfo(String videoExtraInfo)
    {
        if (videoExtraInfo == null) {
            this.videoExtraInfo.setVisibility(View.GONE);
        } else {
            this.videoExtraInfo.setText(videoExtraInfo);
            this.videoExtraInfo.setVisibility(View.VISIBLE);
        }
    }

    private void scheduleSeekbarUpdate()
    {
        stopSeekbarUpdate();
        if (!executorService.isShutdown()) {
            scheduleFuture = executorService.scheduleAtFixedRate(
                    () -> handler.post(updateProgressTask),
                    PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void stopSeekbarUpdate()
    {
        if (scheduleFuture != null) {
            scheduleFuture.cancel(false);
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state)
    {
        if (activity == null) {
            LogHelper.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) return;
        LogHelper.e(TAG, "onPlaybackStateChanged ", state);
//        boolean enablePlay = false;
//        switch (state.getState()) {
//            case PlaybackStateCompat.STATE_PAUSED:
//            case PlaybackStateCompat.STATE_STOPPED:
//                enablePlay = true;
//                break;
//            case PlaybackStateCompat.STATE_ERROR:
//                LogHelper.e(TAG, "error playbackstate: ", state.getErrorMessage());
//                Toast.makeText(activity, state.getErrorMessage(), Toast.LENGTH_LONG).show();
//                break;
//        }
//
//        if (enablePlay) {
//            playPause.setImageDrawable(
//                    ContextCompat.getDrawable(activity, R.drawable.ic_play_arrow_black_36dp));
//        } else {
//            playPause.setImageDrawable(
//                    ContextCompat.getDrawable(activity, R.drawable.ic_pause_black_36dp));
//        }

        updatePlaybackState(state);


//        String extraInfo = null;
//        if (controller != null && controller.getExtras() != null) {
//            String castName = controller.getExtras().getString(BackgroundExoAudioService.EXTRA_CONNECTED_CAST);
//            if (castName != null) {
//                videoExtraInfo = getResources().getString(R.string.casting_to_device, castName);
//            }
//        }
//        setVideoExtraInfo(videoExtraInfo);
    }

    private void onMetadataChanged(MediaMetadataCompat metadata)
    {
//        LogHelper.e(TAG, "onMetadataChanged ", metadata);
//        LogHelper.e(TAG, "METADATA_KEY_MEDIA_ID ", metadata.getString(METADATA_KEY_MEDIA_ID));
        if (activity == null) {
            LogHelper.w(TAG, "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (metadata == null) return;

        if (!activity.isFinishing()) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }
    }

    private void playMedia()
    {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia()
    {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }

    private void stopMedia()
    {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
        if (controller != null) {
            controller.getTransportControls().stop();
        }
    }

    private void fetchImage(@NonNull MediaDescriptionCompat description)
    {
        String artUrl = null;
        if (description.getIconUri() != null) {
            artUrl = description.getIconUri().toString();
        }
        LogHelper.e(TAG, "fetchImage called ");
        if (!TextUtils.equals(artUrl, this.artUrl)) {
            this.artUrl = artUrl;
            Picasso.with(activity)
                    .load(this.artUrl)
                    .centerCrop()
                    .fit()
                    .into(videoThumbnail);
        }
    }

    private void updateMediaDescription(MediaDescriptionCompat description)
    {
        if (description == null) {
            return;
        }
//        LogHelper.e(TAG, "updateMediaDescription called ");
        videoTitle.setText(description.getTitle());
        videoViewsNumber.setText(description.getSubtitle());
        fetchImage(description);
    }

    private void updateDuration(MediaMetadataCompat metadata)
    {
        if (metadata == null) return;
        if (lastPlaybackState.getState() != STATE_PAUSED) {
//            LogHelper.e(TAG, "updateDuration called ");
            int duration = (int) metadata.getLong(METADATA_KEY_DURATION);
            seekbar.setMax(duration);
            tvEnd.setText(DateUtils.formatElapsedTime(duration / 1000));
        }
    }

    private void updatePlaybackState(PlaybackStateCompat state)
    {
        if (state == null) return;

//        LogHelper.e(TAG, "updatePlaybackState called ");
        lastPlaybackState = state;

        switch (state.getState()) {
            case STATE_PLAYING:
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(pauseDrawable);
                controls.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case STATE_PAUSED:
                controls.setVisibility(VISIBLE);
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case STATE_NONE:
            case STATE_STOPPED:
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case STATE_BUFFERING:
                playPause.setVisibility(INVISIBLE);
                stopSeekbarUpdate();
                break;
            default:
                LogHelper.d(TAG, "Unhandled state ", state.getState());
        }

        skipNext.setVisibility((state.getActions() & ACTION_SKIP_TO_NEXT) == 0 ? INVISIBLE : VISIBLE);
        skipPrev.setVisibility((state.getActions() & ACTION_SKIP_TO_PREVIOUS) == 0 ? INVISIBLE : VISIBLE);
    }

    private void updateProgress()
    {
        if (lastPlaybackState == null) return;

        long currentPosition = lastPlaybackState.getPosition();
        if (lastPlaybackState.getState() != STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() - lastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * lastPlaybackState.getPlaybackSpeed();
        }
        seekbar.setProgress((int) currentPosition);
//        LogHelper.e(TAG, "currentPosition: " + currentPosition);
    }
}
