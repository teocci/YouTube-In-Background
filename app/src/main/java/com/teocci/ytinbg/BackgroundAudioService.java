package com.teocci.ytinbg;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.SparseArray;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.receivers.MediaButtonIntentReceiver;
import com.teocci.ytinbg.ui.MainActivity;
import com.teocci.ytinbg.utils.Config;
import com.teocci.ytinbg.utils.LogHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Service class for background youtube playback
 * Created by Teocci on 9.3.16..
 */
public class BackgroundAudioService extends Service implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener
{
    private static final String TAG = LogHelper.makeLogTag(BackgroundAudioService.class);

    private static final int NOTIFICATION_ID = 412;

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

    private static final int YOUTUBE_ITAG_251 = 251;    // webm - stereo, 48 KHz 160 Kbps (opus)
    private static final int YOUTUBE_ITAG_250 = 250;    // webm - stereo, 48 KHz 64 Kbps (opus)
    private static final int YOUTUBE_ITAG_249 = 249;    // webm - stereo, 48 KHz 48 Kbps (opus)
    private static final int YOUTUBE_ITAG_171 = 171;    // webm - stereo, 48 KHz 128 Kbps (vortis)
    private static final int YOUTUBE_ITAG_141 = 141;    // mp4a - stereo, 44.1 KHz 256 Kbps (aac)
    private static final int YOUTUBE_ITAG_140 = 140;    // mp4a - stereo, 44.1 KHz 128 Kbps (aac)
    private static final int YOUTUBE_ITAG_43 = 43;      // webm - stereo, 44.1 KHz 128 Kbps (vortis)
    private static final int YOUTUBE_ITAG_22 = 22;      // mp4 - stereo, 44.1 KHz 192 Kbps (aac)
    private static final int YOUTUBE_ITAG_18 = 18;      // mp4 - stereo, 44.1 KHz 96 Kbps (aac)
    private static final int YOUTUBE_ITAG_17 = 17;      // mp4 - stereo, 44.1 KHz 24 Kbps (aac)

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    public static final String MODE_REPEAT_ONE = "mode_repeat_one";
    public static final String MODE_REPEAT_ALL = "mode_repeat_all";
    public static final String MODE_REPEAT_NONE = "mode_repeat_none";
    public static final String MODE_SHUFFLE = "mode_shuffle";

    private BackgroundAudioService backgroundAudioService = this;

    // Type of audio focus we have:
    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat mediaController;

    private int mediaType = Config.YOUTUBE_MEDIA_NO_NEW_REQUEST;

    private boolean isStarting = false;

    private int playState;
    private boolean playOnFocusGain;
    private volatile boolean audioNoisyReceiverRegistered;

    private Context context;
    private WifiManager.WifiLock wifiLock;

    private YouTubeVideo currentVideo;
    private String currentVideoTitle;
    private int currentVideoPosition;

    private ArrayList<YouTubeVideo> youTubeVideos;
    private ListIterator<YouTubeVideo> iterator;

    private NotificationCompat.Builder notificationBuilder = null;

    private boolean nextWasCalled = false;
    private boolean previousWasCalled = false;


    private volatile int currentPosition;

    private final IntentFilter audioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                LogHelper.e(TAG, "audioNoisyReceiver: Headphones disconnected.");
                if (isPlaying()) {
                    pauseVideo();
                    LogHelper.e(TAG, "audioNoisyReceiver: Video Pause");
                }
            }
        }
    };

    /**
     * Field which handles image loading
     */
    private Target target = new Target()
    {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom)
        {
            updateNotificationLargeIcon(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable drawable)
        {
            LogHelper.d(TAG, "Load bitmap... failed");
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {}
    };

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @SuppressLint("WifiManagerPotentialLeak")
    @Override
    public void onCreate()
    {
        super.onCreate();
        context = getApplicationContext();
        currentVideo = new YouTubeVideo();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        currentVideoPosition = -1;

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        this.wifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "YTinBG_lock");
        initMediaSessions();
        initPhoneCallListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    public boolean isPlaying() {
        return playOnFocusGain || (mediaPlayer != null && mediaPlayer.isPlaying());
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded()
    {
        LogHelper.e(TAG, "createMediaPlayerIfNeeded. needed? ", (mediaPlayer == null));
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mediaPlayer.reset();
        }
    }

    /**
     * Initializes media sessions and receives media events
     */
    private void initMediaSessions()
    {
        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        //
        // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
        // permission in AndroidManifest.xml.
//        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        ComponentName eventReceiver = new ComponentName(
                getApplicationContext().getPackageName(),
                MediaButtonIntentReceiver.class.getName()
        );
        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                0,
                new Intent(Intent.ACTION_MEDIA_BUTTON),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        mediaSession = new MediaSessionCompat(
                getApplicationContext(),
                TAG,
                eventReceiver,
                buttonReceiverIntent
        );

        try {
            mediaController = new MediaControllerCompat(
                    getApplicationContext(),
                    mediaSession.getSessionToken()
            );

            mediaSession.setCallback(
                    new MediaSessionCompat.Callback()
                    {
                        @Override
                        public void onPlay()
                        {
                            super.onPlay();
                            updateAction(ACTION_PAUSE);
//                            buildNotification(generateAction(
//                                    R.drawable.ic_pause_white_24dp,
//                                    getString(R.string.action_pause),
//                                    ACTION_PAUSE
//                            ));
                        }

                        @Override
                        public void onPause()
                        {
                            super.onPause();
                            pauseVideo();
                            updateAction(ACTION_PLAY);
//                            buildNotification(generateAction(
//                                    R.drawable.ic_play_arrow_white_24dp,
//                                    getString(R.string.action_play),
//                                    ACTION_PLAY
//                            ));
                        }

                        @Override
                        public void onSkipToNext()
                        {
                            super.onSkipToNext();
                            if (!isStarting) {
                                playNext();
                            }
                            updateAction(ACTION_PAUSE);
//                            buildNotification(generateAction(
//                                    R.drawable.ic_pause_white_24dp,
//                                    getString(R.string.action_pause),
//                                    ACTION_PAUSE
//                            ));
                        }

                        @Override
                        public void onSkipToPrevious()
                        {
                            super.onSkipToPrevious();
                            if (!isStarting) {
                                playPrevious();
                            }
                            updateAction(ACTION_PAUSE);
//                            buildNotification(generateAction(
//                                    R.drawable.ic_pause_white_24dp,
//                                    getString(R.string.action_pause),
//                                    ACTION_PAUSE
//                            ));
                        }

                        @Override
                        public void onStop()
                        {
                            super.onStop();
                            stopPlayer();
                            //remove notification and stop service
                            NotificationManager notificationManager = (NotificationManager)
                                    getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.cancel(NOTIFICATION_ID);
                            Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
                            stopService(intent);
                        }

                        @Override
                        public void onSetRating(RatingCompat rating)
                        {
                            super.onSetRating(rating);
                        }
                    }
            );
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void updateAction(String action)
    {
        LogHelper.d(TAG, "updateAction");
        String label;
        int icon;
        if (action.equals(ACTION_PAUSE)) {
            label = getString(R.string.action_pause);
            icon = R.drawable.ic_pause_white_24dp;
        } else if (action.equals(ACTION_PLAY)) {
            label = getString(R.string.action_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
        } else if (action.equals(ACTION_NEXT)) {
            label = getString(R.string.action_next);
            icon = R.drawable.ic_skip_next_white_24dp;
        } else if (action.equals(ACTION_PREVIOUS)) {
            label = getString(R.string.action_previous);
            icon = R.drawable.ic_skip_previous_white_24dp;
        } else {
            return;
        }
        buildNotification(generateAction(icon, label, action));
    }

    private void initPhoneCallListener()
    {
        PhoneStateListener phoneStateListener = new PhoneStateListener()
        {
            @Override
            public void onCallStateChanged(int state, String incomingNumber)
            {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call: Pause music
                    pauseVideo();
                    updateAction(ACTION_PLAY);
                } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    //Not in call: Play music
                    resumeVideo();
                    updateAction(ACTION_PAUSE);
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };

        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    /**
     * Handles intent (player options play/pause/stop...)
     *
     * @param intent provides the player actions
     */
    private void handleIntent(Intent intent)
    {
        if (intent == null || intent.getAction() == null)
            return;
        String action = intent.getAction();
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            handleMedia(intent);
            mediaController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mediaController.getTransportControls().pause();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mediaController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mediaController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mediaController.getTransportControls().stop();
        }
    }

    /**
     * Handles media - playlist and videos sent from fragments
     *
     * @param intent provides the Media Type
     */
    private void handleMedia(Intent intent)
    {
        int intentMediaType = intent.getIntExtra(Config.YOUTUBE_TYPE, Config.YOUTUBE_MEDIA_NO_NEW_REQUEST);

        switch (intentMediaType) {
            // Video has been paused. It is not necessary a new playback requests
            case Config.YOUTUBE_MEDIA_NO_NEW_REQUEST:
                mediaPlayer.start();
                break;
            case Config.YOUTUBE_MEDIA_TYPE_VIDEO:
                mediaType = Config.YOUTUBE_MEDIA_TYPE_VIDEO;
                currentVideo = (YouTubeVideo) intent.getSerializableExtra(Config.YOUTUBE_TYPE_VIDEO);
                if (currentVideo.getId() != null) {
                    playVideo();
                }
                break;
            // New playlist playback request
            case Config.YOUTUBE_MEDIA_TYPE_PLAYLIST:
                mediaType = Config.YOUTUBE_MEDIA_TYPE_PLAYLIST;
                youTubeVideos = (ArrayList<YouTubeVideo>) intent.getSerializableExtra(Config.YOUTUBE_TYPE_PLAYLIST);
                currentVideoPosition = intent.getIntExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, 0);
                LogHelper.e(TAG, "currentVideoPosition: " + currentVideoPosition);
                iterator = youTubeVideos.listIterator(currentVideoPosition);
                playNext();
                break;
            default:
                LogHelper.e(TAG, "Unknown command");
                break;
        }
    }

    /**
     * Builds notification panel with buttons and info on it
     *
     * @param action Action to be applied
     */

    private void buildNotification(NotificationCompat.Action action)
    {
        final NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        Intent clickIntent = new Intent(this, MainActivity.class);
        clickIntent.setAction(Intent.ACTION_MAIN);
        clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);
        notificationBuilder.setContentTitle(currentVideo.getTitle());
        notificationBuilder.setContentInfo(currentVideo.getDuration());
        notificationBuilder.setUsesChronometer(true);
        notificationBuilder.setContentIntent(clickPendingIntent);
        notificationBuilder.setDeleteIntent(stopPendingIntent);
//        notificationBuilder.setOngoing(true);
        notificationBuilder.setSubText(currentVideo.getViewCount());
        notificationBuilder.setStyle(style);
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        //load bitmap for largeScreen
        if (currentVideo.getThumbnailURL() != null && !currentVideo.getThumbnailURL().isEmpty()) {
            Picasso.with(this)
                    .load(currentVideo.getThumbnailURL())
                    .resize(Config.MAX_WIDTH_ICON, Config.MAX_HEIGHT_ICON)
                    .centerCrop()
                    .into(target);
        }

        notificationBuilder.addAction(generateIntentAction(ACTION_PREVIOUS));
        notificationBuilder.addAction(generateIntentAction(ACTION_NEXT));
//        notificationBuilder.updateAction(generateAction(
//                R.drawable.ic_skip_previous_white_24dp,
//                getApplicationContext().getString(R.string.action_previous),
//                ACTION_PREVIOUS
//        ));
//        notificationBuilder.updateAction(generateAction(
//                R.drawable.ic_skip_next_white_24dp,
//                getApplicationContext().getString(R.string.action_next),
//                ACTION_NEXT));
        notificationBuilder.addAction(action);
        LogHelper.e(TAG, "setNotificationPlaybackState. mediaPlayer=" + mediaPlayer);
        setNotificationPlaybackState(notificationBuilder);

        style.setShowActionsInCompactView(0, 1, 2);

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder)
    {
        LogHelper.e(TAG, "updateNotificationPlaybackState. mediaPlayer=" + mediaPlayer);
        if (mediaPlayer == null) {
            LogHelper.e(TAG, "updateNotificationPlaybackState. cancelling notification!");
            stopForeground(true);
            return;
        }
        if (mediaPlayer.isPlaying() && mediaPlayer.getCurrentPosition()  >= 0) {
            LogHelper.e(TAG, "updateNotificationPlaybackState. updating playback position to ",
                    (System.currentTimeMillis() - mediaPlayer.getCurrentPosition()) / 1000, " seconds");
            builder.setWhen(System.currentTimeMillis() - mediaPlayer.getCurrentPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            LogHelper.e(TAG, "updateNotificationPlaybackState. hiding playback position");
            builder.setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mediaPlayer.isPlaying());
    }

    /**
     * Updates only large icon in notification panel when bitmap is decoded
     *
     * @param bitmap the large icon in the notification panel
     */
    private void updateNotificationLargeIcon(Bitmap bitmap)
    {
        notificationBuilder.setLargeIcon(bitmap);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Updates only the notification panel's flag if the notification can be or not closed
     *
     * @param isOngoing is the videos is playing then the notification panel should not be dismissed
     */
    private void updateNotificationOngoing(boolean isOngoing)
    {
//        notificationBuilder.setOngoing(isOngoing);
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context
//                .NOTIFICATION_SERVICE);
//        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        if (isOngoing) {
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
        } else {
            stopForeground(true);
        }
    }

    /**
     * Generates specific action with parameters below
     *
     * @param icon         the icon number
     * @param title        the title of the notification
     * @param intentAction the action
     * @return NotificationCompat.Action
     */
    private NotificationCompat.Action generateAction(int icon, String title, String intentAction)
    {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, BackgroundAudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(context, 1, intent, 0);

        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    /**
     * Generates specific action with parameters below
     *
     * @param intentAction the action
     * @return NotificationCompat.Action
     */
    private NotificationCompat.Action generateIntentAction(String intentAction)
    {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, BackgroundAudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(context, 1, intent, 0);

        String label;
        int icon;
        if (intentAction.equals(ACTION_PAUSE)) {
            label = getString(R.string.action_pause);
            icon = R.drawable.ic_pause_white_24dp;
        } else if (intentAction.equals(ACTION_PLAY)) {
            label = getString(R.string.action_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
        } else if (intentAction.equals(ACTION_NEXT)) {
            label = getString(R.string.action_next);
            icon = R.drawable.ic_skip_next_white_24dp;
        } else if (intentAction.equals(ACTION_PREVIOUS)) {
            label = getString(R.string.action_previous);
            icon = R.drawable.ic_skip_previous_white_24dp;
        } else {
            return null;
        }

        return new NotificationCompat.Action.Builder(icon, label, pendingIntent).build();
    }

    /**
     * Plays next video in playlist
     */
    private void playNext()
    {
        // If media type is video not playlist, just loop it
        if (mediaType == Config.YOUTUBE_MEDIA_TYPE_VIDEO) {
            seekVideo(0);
            restartVideo();
            return;
        }

        if (previousWasCalled) {
            previousWasCalled = false;
            iterator.next();
        }

        if (iterator == null || !iterator.hasNext()) {
            iterator = youTubeVideos.listIterator();
        }

        LogHelper.e(TAG, "Start playNext");
        currentVideoPosition = iterator.nextIndex();
        currentVideo = iterator.next();

        nextWasCalled = true;
        playVideo();
    }

    /**
     * Plays previous video in playlist
     */
    private void playPrevious()
    {
        // Whenever the media type is not a playlist, just loop it
        if (mediaType == Config.YOUTUBE_MEDIA_TYPE_VIDEO) {
            restartVideo();
            return;
        }

        if (nextWasCalled) {
            iterator.previous();
            nextWasCalled = false;
        }

        if (iterator == null || !iterator.hasPrevious()) {
            iterator = youTubeVideos.listIterator(youTubeVideos.size());
        }

        LogHelper.e(TAG, "Start playPrevious");
        currentVideoPosition = iterator.previousIndex();
        currentVideo = iterator.previous();

        previousWasCalled = true;
        playVideo();
    }

    /**
     * Plays video
     */
    private void playVideo()
    {
        isStarting = true;
        relaxResources(false); // release everything except MediaPlayer
//        extractUrlAndPlay();
        newExtractUrlAndPlay();
    }

    /**
     * Pauses video
     */
    private void pauseVideo()
    {
        if (playState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                currentPosition = mediaPlayer.getCurrentPosition();
            }
            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
        }
        playState = PlaybackStateCompat.STATE_PAUSED;
        unregisterAudioNoisyReceiver();
//        stopForeground(true);
//        if (mCallback != null) {
//            mCallback.onPlaybackStatusChanged(playState);
//        }
    }

    /**
     * Resumes video
     */
    private void resumeVideo()
    {
        playOnFocusGain = true;
        tryToGetAudioFocus();
        if (playState == PlaybackStateCompat.STATE_PAUSED && mediaPlayer != null) {
            configMediaPlayerState();
        }
    }

    /**
     * Restarts video
     */
    private void restartVideo()
    {
        playOnFocusGain = true;
        tryToGetAudioFocus();
//        mediaPlayer.start();
        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), BackgroundAudioService.class));
        configMediaPlayerState();
        Toast.makeText(
                getApplicationContext(),
                getResources().getString(R.string.toast_message_playing, currentVideoTitle),
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Seeks to specific time
     *
     * @param seekTo specific time
     */
    private void seekVideo(int seekTo)
    {
        mediaPlayer.seekTo(seekTo);
    }

    /**
     * Stops video
     */
    private void stopPlayer()
    {
        currentPosition = getCurrentStreamPosition();
        // Give up Audio focus
        giveUpAudioFocus();
        // Relax all resources
        relaxResources(true);
        unregisterAudioNoisyReceiver();
        updateNotificationOngoing(false);
    }

    public int getCurrentStreamPosition()
    {
        return mediaPlayer != null ?
                mediaPlayer.getCurrentPosition() : currentPosition;
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                           be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer)
    {
        LogHelper.d(TAG, "relaxResources. releaseMediaPlayer=", releaseMediaPlayer);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void verifyIterator()
    {
        if (iterator == null) {
            int size = youTubeVideos.size();
            if (currentVideoPosition > -1 && currentVideoPosition < size) {
                iterator = youTubeVideos.listIterator(currentVideoPosition);
            } else {
                iterator = youTubeVideos.listIterator();
            }
        }
    }

    /**
     * Extracts link from youtube video ID, so mediaPlayer can play it
     */
    private void newExtractUrlAndPlay()
    {
        LogHelper.e(TAG, "extractUrlAndPlay: extracting url for video id=" + currentVideo.getId());
        final String youtubeLink = "http://youtube.com/watch?v=" + currentVideo.getId();
//        LogHelper.e(TAG, youtubeLink);

        new YouTubeExtractor(this) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {

                if (ytFiles == null) {
                    Toast.makeText(
                            getApplicationContext(),
                            getResources().getString(R.string.toast_message_error_extracting, currentVideoTitle),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                if (ytFiles != null) {
                    YtFile ytFile = getBestStream(ytFiles);

                    playOnFocusGain = true;
                    currentPosition = 0;
                    tryToGetAudioFocus();
                    registerAudioNoisyReceiver();
                    playState = PlaybackStateCompat.STATE_STOPPED;
                    relaxResources(false); // release everything except MediaPlayer

                    try {
                        LogHelper.e(TAG, ytFile.getUrl());
                        LogHelper.e(TAG, "extractUrlAndPlay: Start playback");

                        createMediaPlayerIfNeeded();

                        playState = PlaybackStateCompat.STATE_BUFFERING;

                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setDataSource(ytFile.getUrl());
                        mediaPlayer.setOnPreparedListener(backgroundAudioService);
                        currentVideoTitle = videoMeta.getTitle();

                        // Starts preparing the media player in the background. When
                        // it's done, it will call our OnPreparedListener (that is,
                        // the onPrepared() method on this class, since we set the
                        // listener to 'this'). Until the media player is prepared,
                        // we *cannot* call start() on it!
                        mediaPlayer.prepareAsync();

                        // If we are streaming from the internet, we want to hold a
                        // Wifi lock, which prevents the Wifi radio from going to
                        // sleep while the song is playing.
                        wifiLock.acquire();
                    } catch (IOException io) {
                        LogHelper.e(TAG, io, "extractUrlAndPlay: Exception playing song");
                        io.printStackTrace();
                    }
                }
            }
        }.extract(youtubeLink, true, true);
    }

    /**
     * Extracts link from youtube video ID, so mediaPlayer can play it
     */
    private void extractUrlAndPlay()
    {
        LogHelper.e(TAG, "extractUrlAndPlay: extracting url for video id=" + currentVideo.getId());
        final String youtubeLink = "http://youtube.com/watch?v=" + currentVideo.getId();
//        LogHelper.e(TAG, youtubeLink);

        YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this)
        {
            @Override
            public void onUrisAvailable(String videoId, final String videoTitle,
                                        SparseArray<YtFile> ytFiles)
            {
                if (ytFiles != null) {
                    YtFile ytFile = getBestStream(ytFiles);

                    playOnFocusGain = true;
                    currentPosition = 0;
                    tryToGetAudioFocus();
                    registerAudioNoisyReceiver();
                    playState = PlaybackStateCompat.STATE_STOPPED;
                    relaxResources(false); // Release everything except MediaPlayer

                    try {
                        LogHelper.e(TAG, ytFile.getUrl());
                        LogHelper.e(TAG, "extractUrlAndPlay: Start playback");

                        createMediaPlayerIfNeeded();

                        playState = PlaybackStateCompat.STATE_BUFFERING;

                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setDataSource(ytFile.getUrl());
                        mediaPlayer.setOnPreparedListener(backgroundAudioService);
                        currentVideoTitle = videoTitle;

                        // Starts preparing the media player in the background. When
                        // it's done, it will call our OnPreparedListener (that is,
                        // the onPrepared() method on this class, since we set the
                        // listener to 'this'). Until the media player is prepared,
                        // we *cannot* call start() on it!
                        mediaPlayer.prepareAsync();

                        // If we are streaming from the internet, we want to hold a
                        // Wifi lock, which prevents the Wifi radio from going to
                        // sleep while the song is playing.
                        wifiLock.acquire();
//                            mediaPlayer.start();

//                        if (callback != null) {
//                            callback.onPlaybackStatusChanged(playState);
//                        }
                    } catch (IOException io) {
                        LogHelper.e(TAG, io, "extractUrlAndPlay: Exception playing song");
                        io.printStackTrace();
                    }
                }
            }
        };
        // Ignore the webm container formatViewCount
//         ytEx.setIncludeWebM(false);
//         ytEx.setParseDashManifest(true);
        // Lets execute the request
        ytEx.execute(youtubeLink);
    }

    /**
     * Get the best available audio stream
     *
     * @param ytFiles Array of available streams
     * @return Audio stream with highest bitrate
     */
    private YtFile getBestStream(SparseArray<YtFile> ytFiles)
    {
        if (ytFiles.get(YOUTUBE_ITAG_141) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_141");
            return ytFiles.get(YOUTUBE_ITAG_141);
        } else if (ytFiles.get(YOUTUBE_ITAG_140) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_140");
            return ytFiles.get(YOUTUBE_ITAG_140);
        } else if (ytFiles.get(YOUTUBE_ITAG_251) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_251");
            return ytFiles.get(YOUTUBE_ITAG_251);
        } else if (ytFiles.get(YOUTUBE_ITAG_250) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_250");
            return ytFiles.get(YOUTUBE_ITAG_250);
        } else if (ytFiles.get(YOUTUBE_ITAG_249) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_249");
            return ytFiles.get(YOUTUBE_ITAG_249);
        } else if (ytFiles.get(YOUTUBE_ITAG_171) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_171");
            return ytFiles.get(YOUTUBE_ITAG_171);
        } else if (ytFiles.get(YOUTUBE_ITAG_18) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_18");
            return ytFiles.get(YOUTUBE_ITAG_18);
        } else if (ytFiles.get(YOUTUBE_ITAG_22) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_22");
            return ytFiles.get(YOUTUBE_ITAG_22);
        } else if (ytFiles.get(YOUTUBE_ITAG_43) != null) {
            LogHelper.e(TAG, " gets YOUTUBE_ITAG_43");
            return ytFiles.get(YOUTUBE_ITAG_43);
        }

        LogHelper.e(TAG, " gets YOUTUBE_ITAG_17");
        return ytFiles.get(YOUTUBE_ITAG_17);
    }

    private void startPlayback(SparseArray<YtFile> ytFiles)
    {

    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onCompletion(MediaPlayer _mediaPlayer)
    {
        if (mediaType == Config.YOUTUBE_MEDIA_TYPE_PLAYLIST) {
            playNext();
            updateAction(ACTION_PAUSE);
//            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        } else {
            restartVideo();
            updateAction(ACTION_PAUSE);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp)
    {
        isStarting = false;
//        mp.start();
//        playState = PlaybackStateCompat.STATE_PLAYING;


        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), BackgroundAudioService.class));
        configMediaPlayerState();
        Toast.makeText(
                getApplicationContext(),
                getResources().getString(R.string.toast_message_playing, currentVideoTitle),
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        return false;
    }

    public void seekTo(int position)
    {
        LogHelper.d(TAG, "seekTo called with ", position);

        if (mediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            currentPosition = position;
        } else {
            if (mediaPlayer.isPlaying()) {
                playState = PlaybackStateCompat.STATE_BUFFERING;
            }
            registerAudioNoisyReceiver();
            mediaPlayer.seekTo(position);
//            if (callback != null) {
//                callback.onPlaybackStatusChanged(playState);
//            }
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp)
    {
        LogHelper.d(TAG, "onSeekComplete from MediaPlayer:", mp.getCurrentPosition());
        currentPosition = mp.getCurrentPosition();
        if (playState == PlaybackStateCompat.STATE_BUFFERING) {
            registerAudioNoisyReceiver();
            mediaPlayer.start();
            playState = PlaybackStateCompat.STATE_PLAYING;
            updateAction(ACTION_PAUSE);
        }
//        if (callback != null) {
//            callback.onPlaybackStatusChanged(playState);
//        }
    }


    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus()
    {
        LogHelper.e(TAG, "tryToGetAudioFocus");
        int result = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocus = AUDIO_FOCUSED;
        } else {
            audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus()
    {
        LogHelper.e(TAG, "giveUpAudioFocus");
        if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configMediaPlayerState()
    {
        LogHelper.e(TAG, "configMediaPlayerState. audioFocus=", audioFocus);
        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (playState == PlaybackStateCompat.STATE_PLAYING) {
                pauseVideo();
                updateAction(ACTION_PLAY);
            }
        } else {  // We have audio focus:
            registerAudioNoisyReceiver();
            if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // Else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    LogHelper.e(TAG, "configMediaPlayerState startMediaPlayer. seeking to ", currentPosition);
                    if (currentPosition == mediaPlayer.getCurrentPosition()) {
                        mediaPlayer.start();
                        updateAction(ACTION_PAUSE);
                        playState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        mediaPlayer.seekTo(currentPosition);
                        playState = PlaybackStateCompat.STATE_BUFFERING;
                    }
                }
                playOnFocusGain = false;
            }
        }
//        if (callback != null) {
//            callback.onPlaybackStatusChanged(playState);
//        }
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange)
    {
        LogHelper.d(TAG, "onAudioFocusChange. focusChange=", focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            audioFocus = AUDIO_FOCUSED;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            audioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with audioFocus properly set.
            if (playState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                playOnFocusGain = true;
            }
        } else {
            LogHelper.e(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: ", focusChange);
        }
        configMediaPlayerState();
    }

    private void registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
            audioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            unregisterReceiver(audioNoisyReceiver);
            audioNoisyReceiverRegistered = false;
        }
    }
}