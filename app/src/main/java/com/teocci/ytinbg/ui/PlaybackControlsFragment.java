package com.teocci.ytinbg.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.interfaces.CurrentVideoReceiver;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;


/**
 * Created by teocci on 3/2/17.
 * <p>
 * A class that shows the Media Queue to the user.
 */
public class PlaybackControlsFragment extends Fragment
{
    private static final String TAG = LogHelper.makeLogTag(PlaybackControlsFragment.class);

    private ImageButton playPauseButton;
    private TextView videoTitle;
    private TextView videoDuration;
    private TextView videoExtraInfo;
    private ImageView videoThumbnail;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private CurrentVideoReceiver currentVideoReceiver = new CurrentVideoReceiver()
    {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state)
        {
            LogHelper.e(TAG, "Received playback state change to state ", state.getState());
            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onCurrentVideoChanged(YouTubeVideo currentVideo)
        {
            if (currentVideo == null) {
                return;
            }
            LogHelper.e(TAG, "Received metadata state change to mediaId=",
                    currentVideo.getId(),
                    " song=", currentVideo.getTitle());
            PlaybackControlsFragment.this.onCurrentVideoChanged(currentVideo);
        }
    };



    private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            LogHelper.d(TAG, "onPlaybackstate changed", state);

            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
//            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
//                updateMediaDescription(metadata.getDescription());
//                updateDuration(metadata);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        playPauseButton = (ImageButton) rootView.findViewById(R.id.play_pause);
        playPauseButton.setEnabled(true);
        playPauseButton.setOnClickListener(mButtonListener);

        this.videoThumbnail = (ImageView) rootView.findViewById(R.id.video_thumbnail);
        videoTitle = (TextView) rootView.findViewById(R.id.video_title);
        videoDuration = (TextView) rootView.findViewById(R.id.video_duration);
        videoExtraInfo = (TextView) rootView.findViewById(R.id.views_number);
        rootView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getActivity(), FullScreenPlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                MediaControllerCompat controller = ((FragmentActivity) getActivity())
                        .getSupportMediaController();
                MediaMetadataCompat metadata = controller.getMetadata();
                if (metadata != null) {
                    intent.putExtra(Config.CURRENT_YOUTUBE_VIDEO,
                            metadata.getDescription());
                }
                startActivity(intent);
            }
        });
        return rootView;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        LogHelper.d(TAG, "fragment.onStart");
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            onConnected();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        LogHelper.d(TAG, "fragment.onStop");
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.unregisterCallback(controllerCallback);
        }
    }

    public void onConnected()
    {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        LogHelper.d(TAG, "onConnected, mediaController==null? ", controller == null);
        if (controller != null) {
//            onYouTubeVideoChanged(controller.getMetadata());
            onPlaybackStateChanged(controller.getPlaybackState());
            controller.registerCallback(controllerCallback);
        }
    }

    public void setCurrentVideoReceiver(CurrentVideoReceiver youTubeVideosReceiver)
    {
        this.currentVideoReceiver = youTubeVideosReceiver;
    }

    private void onYouTubeVideoChanged(YouTubeVideo currentVideo)
    {
        LogHelper.d(TAG, "onYouTubeVideoChanged ", currentVideo);
        if (getActivity() == null) {
            LogHelper.w(TAG, "onYouTubeVideoChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (currentVideo == null) {
            return;
        }

        videoTitle.setText(currentVideo.getTitle());
        videoDuration.setText(currentVideo.getDuration());
        //load bitmap for largeScreen
        if (currentVideo.getThumbnailURL() != null && !currentVideo.getThumbnailURL().isEmpty()) {
            Picasso.with(getActivity()).load(currentVideo.getThumbnailURL()).into(videoThumbnail);
        }
    }

    public void setExtraInfo(String extraInfo)
    {
        if (extraInfo == null) {
            videoExtraInfo.setVisibility(View.GONE);
        } else {
            videoExtraInfo.setText(extraInfo);
            videoExtraInfo.setVisibility(View.VISIBLE);
        }
    }

    private final View.OnClickListener mButtonListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            MediaControllerCompat controller = ((FragmentActivity) getActivity())
                    .getSupportMediaController();
            PlaybackStateCompat stateObj = controller.getPlaybackState();
            final int state = stateObj == null ?
                    PlaybackStateCompat.STATE_NONE : stateObj.getState();
            LogHelper.d(TAG, "Button pressed, in state " + state);
            switch (v.getId()) {
                case R.id.play_pause:
                    LogHelper.d(TAG, "Play button pressed, in state " + state);
                    if (state == PlaybackStateCompat.STATE_PAUSED ||
                            state == PlaybackStateCompat.STATE_STOPPED ||
                            state == PlaybackStateCompat.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackStateCompat.STATE_PLAYING ||
                            state == PlaybackStateCompat.STATE_BUFFERING ||
                            state == PlaybackStateCompat.STATE_CONNECTING) {
                        pauseMedia();
                    }
                    break;
            }
        }
    };

    private void playMedia()
    {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia()
    {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }


    public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state)
    {
        if (getActivity() == null) {
            LogHelper.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }

        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                LogHelper.e(TAG, "error playbackstate: ", state.getState());
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay) {
            playPauseButton.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_36dp));
        } else {
            playPauseButton.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_36dp));
        }
    }

    public void onCurrentVideoChanged(YouTubeVideo currentVideo)
    {
        if (currentVideo == null) {
            return;
        }
        LogHelper.d(TAG, "Received metadata state change to mediaId=",
                currentVideo.getId(),
                " song=", currentVideo.getTitle());
        PlaybackControlsFragment.this.onYouTubeVideoChanged(currentVideo);
    }
}
