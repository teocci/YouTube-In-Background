package com.teocci.ytinbg.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.squareup.picasso.Picasso;
import com.teocci.ytinbg.BackgroundExoAudioService;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.ui.MainActivity;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;
import com.teocci.ytinbg.utils.Utils;

import static com.teocci.ytinbg.utils.Config.ACTION_NEXT;
import static com.teocci.ytinbg.utils.Config.ACTION_PAUSE;
import static com.teocci.ytinbg.utils.Config.ACTION_PLAY;
import static com.teocci.ytinbg.utils.Config.ACTION_PREV;
import static com.teocci.ytinbg.utils.Config.ACTION_STOP;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-09
 * <p>
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver
{
    private static final String TAG = LogHelper.makeLogTag(MediaNotificationManager.class);

    private static final String CHANNEL_ID = "com.teocci.ytinbg.notification.YIB_CHANNEL_ID";

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    private final BackgroundExoAudioService exoAudioService;
    private MediaSessionCompat.Token sessionToken;
    private MediaControllerCompat mediaController;
    private MediaControllerCompat.TransportControls transportControls;

    private PlaybackStateCompat playbackState;
//    private MediaMetadataCompat metadata;

    private YouTubeVideo currentYouTubeVideo;

    private final NotificationManager notificationManager;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent previousIntent;
    private final PendingIntent nextIntent;
    private final PendingIntent stopCastIntent;

//    private final int mNotificationColor;

    private boolean hasStarted = false;


    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback()
    {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state)
        {
            playbackState = state;
            LogHelper.e(TAG, "Received new playback state", state);
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED ||
                    state.getState() == PlaybackStateCompat.STATE_NONE) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata)
        {
            MediaNotificationManager.this.currentYouTubeVideo = exoAudioService.getCurrentYouTubeVideo();
            LogHelper.e(TAG, "Received new metadata ", metadata.getDescription());
            Notification notification = createNotification();
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed()
        {
            super.onSessionDestroyed();
            LogHelper.d(TAG, "Session was destroyed, resetting to the new session token");
            try {
                updateSessionToken();
            } catch (RemoteException e) {
                LogHelper.e(TAG, e, "could not connect media controller");
            }
        }
    };

    /**
     * Field which handles image loading
     */
    private NotificationTarget target = new NotificationTarget()
    {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from)
        {
            updateNotificationLargeIcon(bitmap, builder);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable)
        {
            LogHelper.d(TAG, "Load bitmap... failed");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {}
    };

    public MediaNotificationManager(BackgroundExoAudioService service) throws RemoteException
    {
        exoAudioService = service;
        updateSessionToken();

//        mNotificationColor = ResourceHelper.getThemeColor(exoAudioService, R.attr.colorPrimary,
//                Color.DKGRAY);

        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = exoAudioService.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(exoAudioService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(exoAudioService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(exoAudioService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(exoAudioService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopCastIntent = PendingIntent.getBroadcast(exoAudioService, REQUEST_CODE,
                new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        notificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification()
    {
        if (!hasStarted) {
            currentYouTubeVideo = exoAudioService.getCurrentYouTubeVideo();
            MediaMetadataCompat metadata = mediaController.getMetadata();
            LogHelper.e(TAG, "metadata: " + metadata);

            playbackState = mediaController.getPlaybackState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
                mediaController.registerCallback(callback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                filter.addAction(ACTION_STOP);
                exoAudioService.registerReceiver(this, filter);

                exoAudioService.startForeground(NOTIFICATION_ID, notification);
                hasStarted = true;
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification()
    {
        if (hasStarted) {
            hasStarted = false;
            mediaController.unregisterCallback(callback);
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                exoAudioService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            exoAudioService.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();
        LogHelper.d(TAG, "Received intent with action " + action);
        switch (action) {
            case ACTION_PAUSE:
                transportControls.pause();
                break;
            case ACTION_PLAY:
                transportControls.play();
                break;
            case ACTION_NEXT:
                transportControls.skipToNext();
                break;
            case ACTION_PREV:
                transportControls.skipToPrevious();
                break;
            default:
                LogHelper.w(TAG, "Unknown intent ignored. Action=", action);
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() throws RemoteException
    {
        MediaSessionCompat.Token freshToken = exoAudioService.getSessionToken();
        if (sessionToken == null && freshToken != null ||
                sessionToken != null && !sessionToken.equals(freshToken)) {
            if (mediaController != null) {
                mediaController.unregisterCallback(callback);
            }
            sessionToken = freshToken;
            if (sessionToken != null) {
                mediaController = new MediaControllerCompat(exoAudioService, sessionToken);
                transportControls = mediaController.getTransportControls();
                if (hasStarted) {
                    mediaController.registerCallback(callback);
                }
            }
        }
    }

    private PendingIntent createContentIntent(MediaDescriptionCompat description)
    {
        Intent openUI = new Intent(exoAudioService, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openUI.putExtra(Config.EXTRA_START_FULLSCREEN, true);
        if (description != null) {
            openUI.putExtra(Config.EXTRA_CURRENT_MEDIA_DESCRIPTION, description);
        }
        return PendingIntent.getActivity(exoAudioService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Notification createNotification()
    {
        LogHelper.d(TAG, "updateNotificationMetadata. currentYouTubeVideo=" + currentYouTubeVideo);
        if (currentYouTubeVideo == null || playbackState == null) {
            return null;
        }

        Intent clickIntent = new Intent(exoAudioService, MainActivity.class);
        clickIntent.setAction(Intent.ACTION_MAIN);
        clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(exoAudioService, 0, clickIntent, 0);

        // Notification channels are only supported on Android O+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(exoAudioService, CHANNEL_ID);
        int playPauseButtonPosition = 0;

        // If skip to previous action is enabled
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notificationBuilder.addAction(
                    R.drawable.ic_skip_previous_white_24dp,
                    exoAudioService.getString(R.string.action_previous), previousIntent
            );

            // If there is a "skip to previous" button, the play/pause button will
            // be the second one. We need to keep track of it, because the MediaStyle notification
            // requires to specify the index of the buttons (actions) that should be visible
            // when in compact view.
            playPauseButtonPosition = 1;
        }

        addPlayPauseAction(notificationBuilder);

        // If skip to next action is enabled
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            notificationBuilder.addAction(
                    R.drawable.ic_skip_next_white_24dp,
                    exoAudioService.getString(R.string.action_next),
                    nextIntent
            );
        }

        notificationBuilder
                .setStyle(new MediaStyle()
                        // show only play/pause in compact view
                        .setShowActionsInCompactView(playPauseButtonPosition))
//                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(currentYouTubeVideo.getTitle())
                .setContentInfo(currentYouTubeVideo.getDuration())
                .setUsesChronometer(true)
                .setContentIntent(clickPendingIntent)
                .setSubText(Utils.formatViewCount(currentYouTubeVideo.getViewCount()));

        setNotificationPlaybackState(notificationBuilder);

        //load bitmap
        if (currentYouTubeVideo.getThumbnailURL() != null && !currentYouTubeVideo.getThumbnailURL().isEmpty()) {
            target.setNotificationBuilder(notificationBuilder);
            Picasso.with(exoAudioService)
                    .load(currentYouTubeVideo.getThumbnailURL())
                    .resize(Config.MAX_WIDTH_ICON, Config.MAX_HEIGHT_ICON)
                    .centerCrop()
                    .into(target);
        }
        return notificationBuilder.build();
    }

    private void addPlayPauseAction(NotificationCompat.Builder builder)
    {
        LogHelper.d(TAG, "updatePlayPauseAction");
        String label;
        int icon;
        PendingIntent intent;
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = exoAudioService.getString(R.string.action_pause);
            icon = R.drawable.ic_pause_white_24dp;
            intent = pauseIntent;
        } else {
            label = exoAudioService.getString(R.string.action_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
            intent = playIntent;
        }
        builder.addAction(new NotificationCompat.Action(icon, label, intent));
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder)
    {
        LogHelper.d(TAG, "updateNotificationPlaybackState. playbackState=" + playbackState);
        if (playbackState == null || !hasStarted) {
            LogHelper.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            exoAudioService.stopForeground(true);
            return;
        }
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING
                && playbackState.getPosition() >= 0) {
            LogHelper.d(TAG, "updateNotificationPlaybackState. updating playback position to ",
                    (System.currentTimeMillis() - playbackState.getPosition()) / 1000, " seconds");
            builder
                    .setWhen(System.currentTimeMillis() - playbackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            LogHelper.d(TAG, "updateNotificationPlaybackState. hiding playback position");
            builder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

    /**
     * Updates only large icon in notification panel when bitmap is decoded
     *
     * @param bitmap the large icon in the notification panel
     */
    private void updateNotificationLargeIcon(Bitmap bitmap, NotificationCompat.Builder builder)
    {
        // If the media is still the same, update the notification:
        LogHelper.e(TAG, "updateNotificationLargeIcon: set bitmap");
        builder.setLargeIcon(bitmap);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Creates Notification Channel. This is required in Android O+ to display notifications.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel()
    {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            exoAudioService.getString(R.string.notification_channel),
                            NotificationManager.IMPORTANCE_LOW
                    );

            notificationChannel.setDescription(exoAudioService.getString(R.string.notification_channel_description));

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
