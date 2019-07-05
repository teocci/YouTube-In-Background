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
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static com.teocci.ytinbg.utils.BuildUtil.minAPI26;
import static com.teocci.ytinbg.utils.Config.CUSTOM_ACTION_NEXT;
import static com.teocci.ytinbg.utils.Config.CUSTOM_ACTION_PAUSE;
import static com.teocci.ytinbg.utils.Config.CUSTOM_ACTION_PLAY;
import static com.teocci.ytinbg.utils.Config.CUSTOM_ACTION_PREV;
import static com.teocci.ytinbg.utils.Config.CUSTOM_ACTION_STOP;

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

    private final Context context;

    private final BackgroundExoAudioService exoAudioService;

    private MediaSessionCompat.Token sessionToken;
    private MediaControllerCompat mediaController;
    private MediaControllerCompat.TransportControls transportControls;

    private PlaybackStateCompat playbackState;
//    private MediaMetadataCompat metadata;

    private YouTubeVideo currentYouTubeVideo;

    private final NotificationManager notificationManager;

    private Action skipToPreviousAction;
    private Action playAction;
    private Action pauseAction;
    private Action skipToNextAction;

    private final PendingIntent stopPendingIntent;
    private final PendingIntent clickPendingIntent;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent previousIntent;
    private final PendingIntent nextIntent;
    private final PendingIntent stopCastIntent;

//    private final int mNotificationColor;

    private boolean hasRegisterReceiver = false;
    private boolean isForegroundService = false;

    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback()
    {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state)
        {
            LogHelper.e(TAG, "Received new playback state", state);
            playbackState = state;
            updateNotification(state);
//            if (state.getState() == PlaybackStateCompat.STATE_STOPPED ||
//                    state.getState() == PlaybackStateCompat.STATE_NONE) {
//                stopNotification();
//            } else {
//                Notification notification = createNotification();
//                if (notification != null) {
//                    notificationManager.notify(NOTIFICATION_ID, notification);
//                }
//            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata)
        {
            LogHelper.e(TAG, "Received new metadata ", metadata.getDescription());
            MediaNotificationManager.this.currentYouTubeVideo = exoAudioService.getCurrentYouTubeVideo();

            updateNotification(mediaController.getPlaybackState());
//            Notification notification = createNotification();
//            if (notification != null) {
//                notificationManager.notify(NOTIFICATION_ID, notification);
//            }
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
        context = exoAudioService.getApplicationContext();

        updateSessionToken();

//        mNotificationColor = ResourceHelper.getThemeColor(exoAudioService, R.attr.colorPrimary, Color.DKGRAY);

        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = context.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(CUSTOM_ACTION_PAUSE).setPackage(pkg),
                FLAG_CANCEL_CURRENT
        );
        playIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(CUSTOM_ACTION_PLAY).setPackage(pkg),
                FLAG_CANCEL_CURRENT
        );
        previousIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(CUSTOM_ACTION_PREV).setPackage(pkg),
                FLAG_CANCEL_CURRENT
        );
        nextIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(CUSTOM_ACTION_NEXT).setPackage(pkg),
                FLAG_CANCEL_CURRENT
        );
        stopCastIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(CUSTOM_ACTION_STOP).setPackage(pkg),
                FLAG_CANCEL_CURRENT
        );


        skipToPreviousAction = new Action(
                R.drawable.exo_controls_previous,
                context.getString(R.string.notification_skip_to_previous),
                previousIntent
        );
        playAction = new Action(
                R.drawable.exo_controls_play,
                context.getString(R.string.notification_play),
                playIntent
        );
        pauseAction = new Action(
                R.drawable.exo_controls_pause,
                context.getString(R.string.notification_pause),
                pauseIntent
        );
        skipToNextAction = new Action(
                R.drawable.exo_controls_next,
                context.getString(R.string.notification_skip_to_next),
                nextIntent
        );

        stopPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(CUSTOM_ACTION_STOP).setPackage(pkg),
                FLAG_CANCEL_CURRENT
        );

        Intent clickIntent = new Intent(exoAudioService, MainActivity.class);
        clickIntent.setAction(Intent.ACTION_MAIN);
        clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        clickPendingIntent = PendingIntent.getActivity(exoAudioService, 0, clickIntent, 0);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        if (notificationManager != null) notificationManager.cancelAll();
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();
        LogHelper.d(TAG, "Received intent with action " + action);
        if (action == null || action.isEmpty()) return;

        switch (action) {
            case CUSTOM_ACTION_PAUSE:
                transportControls.pause();
                break;
            case CUSTOM_ACTION_PLAY:
                transportControls.play();
                break;
            case CUSTOM_ACTION_NEXT:
                transportControls.skipToNext();
                break;
            case CUSTOM_ACTION_PREV:
                transportControls.skipToPrevious();
                break;
            case CUSTOM_ACTION_STOP:
                Intent i = new Intent(context, BackgroundExoAudioService.class);
//                i.setAction(MusicService.ACTION_CMD);
//                i.putExtra(MusicService.CMD_NAME, MusicService.CMD_STOP_CASTING);
//                mService.startService(i);
                break;
            default:
                LogHelper.w(TAG, "Unknown intent ignored. Action=", action);
        }
    }


    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification()
    {
        if (!hasRegisterReceiver) {
            currentYouTubeVideo = exoAudioService.getCurrentYouTubeVideo();
            MediaMetadataCompat metadata = mediaController.getMetadata();
            LogHelper.e(TAG, "metadata: " + metadata);

            playbackState = mediaController.getPlaybackState();

            // The notification must be updated after setting started to true
//            Notification notification = createNotification();
            updateNotification(mediaController.getPlaybackState());
//            if (notification != null) {
//                mediaController.registerCallback(callback);
//                IntentFilter filter = new IntentFilter();
//                filter.addAction(CUSTOM_ACTION_NEXT);
//                filter.addAction(CUSTOM_ACTION_PAUSE);
//                filter.addAction(CUSTOM_ACTION_PLAY);
//                filter.addAction(CUSTOM_ACTION_PREV);
//                filter.addAction(CUSTOM_ACTION_STOP);
//                exoAudioService.registerReceiver(this, filter);
//
//                exoAudioService.startForeground(NOTIFICATION_ID, notification);
//                hasRegisterReceiver = true;
//            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification()
    {
        if (hasRegisterReceiver) {
            hasRegisterReceiver = false;
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


    /**
     * This may look strange, but the documentation for [Service.startForeground]
     * notes that "calling this method does *not* put the service in the started
     * state itself, even though the name sounds like it."
     *
     * @param state current playback state
     */
    private void updateNotification(PlaybackStateCompat state)
    {
        int updatedState = state.getState();

        // Skip building a notification when state is "none" and metadata is null.
        Notification notification = skipBuildNotification(updatedState) ? buildNotification(sessionToken) : null;

        if (notification != null && !hasRegisterReceiver) {
            mediaController.registerCallback(callback);
            IntentFilter filter = new IntentFilter();
            filter.addAction(CUSTOM_ACTION_NEXT);
            filter.addAction(CUSTOM_ACTION_PAUSE);
            filter.addAction(CUSTOM_ACTION_PLAY);
            filter.addAction(CUSTOM_ACTION_PREV);
            filter.addAction(CUSTOM_ACTION_STOP);
            exoAudioService.registerReceiver(this, filter);
            hasRegisterReceiver = true;
        }

        switch (updatedState) {
            case STATE_BUFFERING:
            case STATE_PLAYING:
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }

                if (!isForegroundService) {
                    Intent intent = new Intent(context, BackgroundExoAudioService.class);
                    ContextCompat.startForegroundService(context, intent);
                    exoAudioService.startForeground(NOTIFICATION_ID, notification);
                    isForegroundService = true;
                }

                break;
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_ERROR:
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_REWINDING:
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
            case PlaybackStateCompat.STATE_STOPPED:
                if (isForegroundService) {
                    exoAudioService.stopForeground(false);
                    isForegroundService = false;

                    // If playback has ended, also stop the service.
                    if (updatedState == PlaybackStateCompat.STATE_NONE) {
                        exoAudioService.stopSelf();
                    }

                    if (notification != null) {
                        notificationManager.notify(NOTIFICATION_ID, notification);
                    } else {
                        removeNowPlayingNotification();
                    }
                }

                break;
        }
    }

    public Notification buildNotification(MediaSessionCompat.Token sessionToken)
    {
        LogHelper.d(TAG, "updateNotificationMetadata. currentYouTubeVideo=" + currentYouTubeVideo);
        if (currentYouTubeVideo == null || playbackState == null) return null;

        if (shouldCreateNowRunningChannel()) {
            createNotificationChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        // Only add actions for skip back, play/pause, skip forward, based on what's enabled.
        int playPauseIndex = 0;
        if (isSkipToPreviousEnabled()) {
            builder.addAction(skipToPreviousAction);
            playPauseIndex++;
        }

        if (isPlaying()) {
            builder.addAction(pauseAction);
        } else if (isPlayEnabled()) {
            builder.addAction(playAction);
        }

        if (isSkipToNextEnabled()) {
            builder.addAction(skipToNextAction);
        }

        MediaStyle mediaStyle = new MediaStyle()
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(playPauseIndex)
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopPendingIntent);

        builder.setContentIntent(mediaController.getSessionActivity())
                .setContentTitle(currentYouTubeVideo.getTitle())
                .setContentInfo(currentYouTubeVideo.getDuration())
                .setSubText(Utils.formatViewCount(currentYouTubeVideo.getViewCount()))
                .setContentIntent(clickPendingIntent)
                .setDeleteIntent(stopPendingIntent)
                .setOnlyAlertOnce(true)
                .setUsesChronometer(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);


        if (isPlaying() && playbackState.getPosition() > 0) {
            builder
                    .setWhen(System.currentTimeMillis() - playbackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            builder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        // Load bitmap
        if (currentYouTubeVideo.getThumbnailURL() != null && !currentYouTubeVideo.getThumbnailURL().isEmpty()) {
            target.setNotificationBuilder(builder);
            Picasso.with(exoAudioService)
                    .load(currentYouTubeVideo.getThumbnailURL())
                    .resize(Config.MAX_WIDTH_ICON, Config.MAX_HEIGHT_ICON)
                    .centerCrop()
                    .into(target);
        }

        return builder.build();
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
                if (hasRegisterReceiver) {
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
                FLAG_CANCEL_CURRENT);
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
        if (shouldCreateNowRunningChannel()) {
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

        // show only play/pause in compact view
        MediaStyle mediaStyle = new MediaStyle()
                .setCancelButtonIntent(stopPendingIntent)
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(playPauseButtonPosition)
                .setShowCancelButton(true);

        notificationBuilder
                .setStyle(mediaStyle)
//                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(currentYouTubeVideo.getTitle())
                .setContentInfo(currentYouTubeVideo.getDuration())
                .setDeleteIntent(stopPendingIntent)
                .setUsesChronometer(true)
                .setOnlyAlertOnce(true)
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
        if (playbackState.getState() == STATE_PLAYING) {
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
        if (playbackState == null || !hasRegisterReceiver) {
            LogHelper.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            exoAudioService.stopForeground(true);
            return;
        }
        if (playbackState.getState() == STATE_PLAYING && playbackState.getPosition() >= 0) {
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
        builder.setOngoing(playbackState.getState() == STATE_PLAYING);
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

    /**
     * Removes the [NOW_PLAYING_NOTIFICATION] notification.
     * <p>
     * Since `stopForeground(false)` was already called (see
     * [MediaControllerCallback.onPlaybackStateChanged], it's possible to cancel the notification
     * with `notificationManager.cancel(NOW_PLAYING_NOTIFICATION)` if minSdkVersion is >=
     * [Build.VERSION_CODES.LOLLIPOP].
     * <p>
     * Prior to [Build.VERSION_CODES.LOLLIPOP], notifications associated with a foreground
     * service remained marked as "ongoing" even after calling [Service.stopForeground],
     * and cannot be cancelled normally.
     * <p>
     * Fortunately, it's possible to simply call [Service.stopForeground] a second time, this
     * time with `true`. This won't change anything about the service's state, but will simply
     * remove the notification.
     */
    private void removeNowPlayingNotification()
    {
        exoAudioService.stopForeground(true);
    }

    /**
     * @return true if if the minimum API is Oreo and if the notification channel does not exist.
     */
    private boolean shouldCreateNowRunningChannel()
    {
        return minAPI26() && !nowRunningChannelExist();
    }

    /**
     * @return true if the notification channel does exist.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private boolean nowRunningChannelExist()
    {
        return notificationManager.getNotificationChannel(CHANNEL_ID) != null;
    }

    private boolean skipBuildNotification(int updatedState)
    {
        return mediaController.getMetadata() != null && updatedState != PlaybackStateCompat.STATE_NONE;
    }

    private boolean isSkipToPreviousEnabled()
    {
        return (playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0;
    }

    private boolean isSkipToNextEnabled()
    {
        return (playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0;
    }

    private boolean isPlayEnabled()
    {
        return (playbackState.getActions() & PlaybackStateCompat.ACTION_PLAY) != 0;
    }

    private boolean isPlaying()
    {
        return (playbackState.getActions() & PlaybackStateCompat.STATE_PLAYING) != 0;
    }
}
