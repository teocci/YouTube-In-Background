package com.teocci.ytinbg.ui.fragments;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.teocci.ytinbg.R;
import com.teocci.ytinbg.utils.LogHelper;

/**
 * Created by teocci on 3/7/17.
 */

public class PlaybackControlsFragment extends Fragment
{
    private static final String TAG = LogHelper.makeLogTag(PlaybackControlsFragment.class);

    private ImageButton mPlayPause;
    private TextView videoTitle;
    private TextView videoViewsNumber;
    private TextView videoExtraInfo;
    private ImageView videoThumbnail;
    private String mArtUrl;
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

        videoTitle = (TextView) rootView.findViewById(R.id.video_title);
        videoViewsNumber = (TextView) rootView.findViewById(R.id.views_number);
        videoExtraInfo = (TextView) rootView.findViewById(R.id.extra_info);
        videoThumbnail = (ImageView) rootView.findViewById(R.id.video_thumbnail);
//        rootView.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                Intent intent = new Intent(getActivity(), FullScreenPlayerActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                MediaControllerCompat controller = ((FragmentActivity) getActivity())
//                        .getSupportMediaController();
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
            controller.unregisterCallback(mCallback);
        }
    }

    public void onConnected()
    {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        LogHelper.d(TAG, "onConnected, mediaController==null? ", controller == null);
        if (controller != null) {
            onMetadataChanged(controller.getMetadata());
            onPlaybackStateChanged(controller.getPlaybackState());
            controller.registerCallback(mCallback);
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata)
    {
        LogHelper.d(TAG, "onMetadataChanged ", metadata);
        if (getActivity() == null) {
            LogHelper.w(TAG, "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (metadata == null) {
            return;
        }

        videoTitle.setText(metadata.getDescription().getTitle());
        videoViewsNumber.setText(metadata.getDescription().getSubtitle());
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl;
            Bitmap art = metadata.getDescription().getIconBitmap();
//            AlbumArtCache cache = AlbumArtCache.getInstance();
//            if (art == null) {
//                art = cache.getIconImage(mArtUrl);
//            }
//            if (art != null) {
//                videoThumbnail.setImageBitmap(art);
//            } else {
//                cache.fetch(artUrl, new AlbumArtCache.FetchListener()
//                        {
//                            @Override
//                            public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon)
//                            {
//                                if (icon != null) {
//                                    LogHelper.d(TAG, "album art icon of w=", icon.getWidth(),
//                                            " h=", icon.getHeight());
//                                    if (isAdded()) {
//                                        videoThumbnail.setImageBitmap(icon);
//                                    }
//                                }
//                            }
//                        }
//                );
//            }
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

    private void onPlaybackStateChanged(PlaybackStateCompat state)
    {
        LogHelper.d(TAG, "onPlaybackStateChanged ", state);
        if (getActivity() == null) {
            LogHelper.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) {
            return;
        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                LogHelper.e(TAG, "error playbackstate: ", state.getErrorMessage());
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay) {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_36dp));
        } else {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_36dp));
        }

        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        String extraInfo = null;
//        if (controller != null && controller.getExtras() != null) {
//            String castName = controller.getExtras().getString(BackgroundExoAudioService.EXTRA_CONNECTED_CAST);
//            if (castName != null) {
//                videoExtraInfo = getResources().getString(R.string.casting_to_device, castName);
//            }
//        }
//        setVideoExtraInfo(videoExtraInfo);
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
}
