package com.teocci.ytinbg.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.teocci.ytinbg.R;
import com.teocci.ytinbg.utils.LogHelper;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP;
import static com.teocci.ytinbg.utils.BuildUtil.minAPI26;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jun-28
 */
public class NotificationBuilder
{
    private static final String TAG = LogHelper.makeLogTag(NotificationBuilder.class);

    private static final String CHANNEL_ID = "com.teocci.ytinbg.notification.YIB_CHANNEL_ID";

    private static final int NOTIFICATION_ID = 412;

    private final Context context;

    private final NotificationManager notificationManager;

    private MediaControllerCompat mediaController;
    private MediaDescriptionCompat mediaDescription;
    private PlaybackStateCompat playbackState;

    private NotificationCompat.Builder builder;

    private Action skipToPreviousAction;
    private Action playAction;
    private Action pauseAction;
    private Action skipToNextAction;

    private PendingIntent stopPendingIntent;

    public NotificationBuilder(Context context)
    {
        this.context = context;

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        skipToPreviousAction = new Action(
                R.drawable.exo_controls_previous,
                context.getString(R.string.notification_skip_to_previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_PREVIOUS)
        );
        playAction = new Action(
                R.drawable.exo_controls_play,
                context.getString(R.string.notification_play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PLAY)
        );
        pauseAction = new Action(
                R.drawable.exo_controls_pause,
                context.getString(R.string.notification_pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PAUSE)
        );
        skipToNextAction = new Action(
                R.drawable.exo_controls_next,
                context.getString(R.string.notification_skip_to_next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_NEXT)
        );

        stopPendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_STOP);
    }

    public Notification buildNotification(MediaSessionCompat.Token sessionToken) throws RemoteException
    {
        if (shouldCreateNowRunningChannel()) {
            createNotificationChannel();
        }
        mediaController = new MediaControllerCompat(context, sessionToken);
        MediaMetadataCompat metadata = mediaController.getMetadata();
        mediaDescription = metadata.getDescription();
        playbackState = mediaController.getPlaybackState();

        builder = new NotificationCompat.Builder(context, CHANNEL_ID);

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
                .setCancelButtonIntent(stopPendingIntent)
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(playPauseIndex)
                .setShowCancelButton(true);

        return builder.setContentIntent(mediaController.getSessionActivity())
                .setContentText(mediaDescription.getSubtitle())
                .setContentTitle(mediaDescription.getTitle())
                .setDeleteIntent(stopPendingIntent)
                .setLargeIcon(mediaDescription.getIconBitmap())
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
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
                            context.getString(R.string.notification_channel),
                            NotificationManager.IMPORTANCE_LOW
                    );

            notificationChannel.setDescription(context.getString(R.string.notification_channel_description));

            notificationManager.createNotificationChannel(notificationChannel);
        }
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
