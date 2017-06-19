package com.teocci.ytinbg.ui.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
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
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.utils.LogHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mPlayPause;
    private TextView mStart;
    private TextView mEnd;
    private SeekBar mSeekbar;
    private TextView mLine1;
    private TextView mLine2;
    private TextView mLine3;
    private View mControllers;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private ImageView mBackgroundImage;

    private String mArtUrl;

    private final Handler mHandler = new Handler();
    private MediaBrowserCompat mMediaBrowser;

    private final Runnable mUpdateProgressTask = new Runnable()
    {
        @Override
        public void run()
        {
            updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;
    private PlaybackStateCompat mLastPlaybackState;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback()
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
            LogHelper.d(TAG, "Received metadata state change to mediaId=",
                    metadata.getDescription().getMediaId(),
                    " song=", metadata.getDescription().getTitle());
            PlaybackControlsFragment.this.onMetadataChanged(metadata);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

//        mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
//        mPlayPause.setEnabled(true);
//        mPlayPause.setOnClickListener(mButtonListener);

        videoTitle = rootView.findViewById(R.id.video_title);
        videoViewsNumber = rootView.findViewById(R.id.views_number);
        videoExtraInfo = rootView.findViewById(R.id.extra_info);
        videoThumbnail = rootView.findViewById(R.id.video_thumbnail);
        mPauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_white_48dp);
        mPlayDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_white_48dp);
        mPlayPause = rootView.findViewById(R.id.play_pause);
        mSkipNext = rootView.findViewById(R.id.next);
        mSkipPrev = rootView.findViewById(R.id.prev);
        mStart = rootView.findViewById(R.id.startText);
        mEnd = rootView.findViewById(R.id.endText);
        mSeekbar = rootView.findViewById(R.id.seekBar1);
        mControllers = rootView.findViewById(R.id.controllers);

        mSkipNext.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MediaControllerCompat.TransportControls controls = MediaControllerCompat
                        .getMediaController(getActivity())
                        .getTransportControls();
                controls.skipToNext();
            }
        });

        mSkipPrev.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MediaControllerCompat.TransportControls controls = MediaControllerCompat
                        .getMediaController(getActivity())
                        .getTransportControls();
                controls.skipToPrevious();
            }
        });


        mPlayPause.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
                PlaybackStateCompat stateObj = controller.getPlaybackState();
                final int state = stateObj == null ? PlaybackStateCompat.STATE_NONE : stateObj.getState();


                switch (state) {
                    case PlaybackStateCompat.STATE_PLAYING: // fall through
                    case PlaybackStateCompat.STATE_BUFFERING:
                    case PlaybackStateCompat.STATE_CONNECTING:
                        pauseMedia();
                        stopSeekbarUpdate();
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                    case PlaybackStateCompat.STATE_STOPPED:
                    case PlaybackStateCompat.STATE_NONE:
                        playMedia();
                        scheduleSeekbarUpdate();
                        break;
                    default:
                        LogHelper.d(TAG, "onClick with state ", state);
                }

            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                mStart.setText(DateUtils.formatElapsedTime(progress / 1000));
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
                        .getMediaController(getActivity())
                        .getTransportControls()
                        .seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        mSeekbar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        mSeekbar.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

//        rootView.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                Intent intent = new Intent(getActivity(), FullScreenPlayerActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
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
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            onConnected();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        LogHelper.e(TAG, "fragment.onStop");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.unregisterCallback(mCallback);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
    }

    public void onConnected()
    {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        LogHelper.e(TAG, "onConnected, mediaController==null? ", controller == null);
        if (controller != null) {
            controller.registerCallback(mCallback);
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
            if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                    state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
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
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate()
    {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state)
    {
        if (getActivity() == null) {
            LogHelper.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) {
            return;
        }
        LogHelper.e(TAG, "onPlaybackStateChanged ", state);
//        boolean enablePlay = false;
//        switch (state.getState()) {
//            case PlaybackStateCompat.STATE_PAUSED:
//            case PlaybackStateCompat.STATE_STOPPED:
//                enablePlay = true;
//                break;
//            case PlaybackStateCompat.STATE_ERROR:
//                LogHelper.e(TAG, "error playbackstate: ", state.getErrorMessage());
//                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
//                break;
//        }
//
//        if (enablePlay) {
//            mPlayPause.setImageDrawable(
//                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_36dp));
//        } else {
//            mPlayPause.setImageDrawable(
//                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_36dp));
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
        LogHelper.e(TAG, "onMetadataChanged ", metadata);
        if (getActivity() == null) {
            LogHelper.w(TAG, "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (metadata == null) {
            return;
        }

        updateMediaDescription(metadata.getDescription());
        updateDuration(metadata);
    }

    private void playMedia()
    {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia()
    {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }

    private void fetchImage(@NonNull MediaDescriptionCompat description)
    {
        String artUrl = null;
        if (description.getIconUri() != null) {
            artUrl = description.getIconUri().toString();
        }
        LogHelper.e(TAG, "fetchImage called ");
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl;
            Picasso.with(getActivity())
                    .load(mArtUrl)
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
        LogHelper.e(TAG, "updateMediaDescription called ");
        videoTitle.setText(description.getTitle());
        videoViewsNumber.setText(description.getSubtitle());
        fetchImage(description);
    }

    private void updateDuration(MediaMetadataCompat metadata)
    {
        if (metadata == null) {
            return;
        }
        LogHelper.e(TAG, "updateDuration called ");
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mSeekbar.setMax(duration);
        mEnd.setText(DateUtils.formatElapsedTime(duration / 1000));
    }

    private void updatePlaybackState(PlaybackStateCompat state)
    {
        if (state == null) {
            return;
        }
        LogHelper.e(TAG, "updatePlaybackState called ");
        mLastPlaybackState = state;

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPauseDrawable);
                mControllers.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mControllers.setVisibility(VISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                mPlayPause.setVisibility(INVISIBLE);
                stopSeekbarUpdate();
                break;
            default:
                LogHelper.d(TAG, "Unhandled state ", state.getState());
        }

        mSkipNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
                ? INVISIBLE : VISIBLE);
        mSkipPrev.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
                ? INVISIBLE : VISIBLE);
    }

    private void updateProgress()
    {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        mSeekbar.setProgress((int) currentPosition);
    }
}
