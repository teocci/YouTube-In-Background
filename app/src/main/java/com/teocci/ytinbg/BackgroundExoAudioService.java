package com.teocci.ytinbg;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.teocci.ytinbg.handlers.DelayedStopHandler;
import com.teocci.ytinbg.handlers.ServiceHandler;
import com.teocci.ytinbg.interfaces.Playback;
import com.teocci.ytinbg.interfaces.YouTubeVideoUpdateListener;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.notification.MediaNotificationManager;
import com.teocci.ytinbg.playback.LocalPlayback;
import com.teocci.ytinbg.playback.PlaybackManager;
import com.teocci.ytinbg.playback.QueueManager;
import com.teocci.ytinbg.receivers.MediaButtonIntentReceiver;
import com.teocci.ytinbg.utils.LogHelper;

import java.util.ArrayList;
import java.util.List;

import static com.teocci.ytinbg.BackgroundAudioService.ACTION_PREVIOUS;
import static com.teocci.ytinbg.utils.Config.ACTION_NEXT;
import static com.teocci.ytinbg.utils.Config.ACTION_PAUSE;
import static com.teocci.ytinbg.utils.Config.ACTION_PLAY;
import static com.teocci.ytinbg.utils.Config.ACTION_STOP;
import static com.teocci.ytinbg.utils.Config.INTENT_SESSION_TOKEN;
import static com.teocci.ytinbg.utils.Config.KEY_SESSION_TOKEN;
import static com.teocci.ytinbg.utils.Config.KEY_YOUTUBE_TYPE;
import static com.teocci.ytinbg.utils.Config.KEY_YOUTUBE_TYPE_PLAYLIST;
import static com.teocci.ytinbg.utils.Config.KEY_YOUTUBE_TYPE_PLAYLIST_VIDEO_POS;
import static com.teocci.ytinbg.utils.Config.KEY_YOUTUBE_TYPE_VIDEO;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_MEDIA_NO_NEW_REQUEST;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_MEDIA_TYPE_PLAYLIST;
import static com.teocci.ytinbg.utils.Config.YOUTUBE_MEDIA_TYPE_VIDEO;

/**
 * Service class for background youtube playback
 * Created by Teocci on 9.3.16..
 */
public class BackgroundExoAudioService extends Service implements
        PlaybackManager.PlaybackServiceCallback
{
    private static final String TAG = LogHelper.makeLogTag(BackgroundExoAudioService.class);

    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;


    private PlaybackManager playbackManager;

    private MediaSessionCompat mediaSession;
    private MediaControllerCompat mediaController;
    private MediaNotificationManager mediaNotificationManager;


    private LocalBroadcastManager mLocalBroadcastManager;

    private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);
    private ServiceHandler serviceHandler = new ServiceHandler(this);

    private int mediaType = YOUTUBE_MEDIA_NO_NEW_REQUEST;

    private Context context;

    private YouTubeVideo currentYouTubeVideo;
    private String currentVideoTitle;
    private int currentVideoPosition;

    private ArrayList<YouTubeVideo> youTubeVideos;

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
        currentYouTubeVideo = new YouTubeVideo();
        currentVideoPosition = -1;
//        mediaButtonFilter.setPriority(1000); //this line sets receiver priority

        initMediaSessions();
        initPhoneCallListener();
    }

    @Override
    public void onDestroy()
    {
//        if (mediaButtonIntentReceiver != null)
//            unregisterReceiver(mediaButtonIntentReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
//        LogHelper.e(TAG, "onStartCommand | intent: " + intent.getAction());
//        handleIntent(intent);
//        return super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            LogHelper.e(TAG, "onStartCommand | intent: " + intent);
            handleIntent(intent);
        }
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        serviceHandler.removeCallbacksAndMessages(null);
        serviceHandler.sendEmptyMessage(0);
        return START_STICKY;
    }

    /**
     * Callback method called from PlaybackManager whenever the music is about to play.
     */
    @Override
    public void onPlaybackStart()
    {
        mediaSession.setActive(true);
        delayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), BackgroundExoAudioService.class));
    }

    @Override
    public void onNotificationRequired()
    {
        mediaNotificationManager.startNotification();
        YouTubeVideo youTubeVideo = getCurrentYouTubeVideo();
        if (youTubeVideo != null) {
            MediaMetadataCompat.Builder ytVideo = new MediaMetadataCompat.Builder();
            ytVideo.putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                    youTubeVideo.getTitle()
            );
            ytVideo.putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                    youTubeVideo.getViewCount()
            );
            ytVideo.putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                    youTubeVideo.getThumbnailURL()
            );
            ytVideo.putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    playbackManager.getDuration()
            );


            mediaSession.setMetadata(ytVideo.build());
        }
    }

    @Override
    public void onPlaybackStop()
    {
        mediaSession.setActive(false);
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(true);

    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState)
    {
        mediaSession.setPlaybackState(newState);
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

        Context context = getApplicationContext();
//        Intent intent = new Intent(context, NowPlayingActivity.class);
//        PendingIntent pi = PendingIntent.getActivity(
//                context,
//                99 /*request code*/,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT
//        );

        ComponentName eventReceiver = new ComponentName(
                context.getPackageName(),
                MediaButtonIntentReceiver.class.getName()
        );

        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(Intent.ACTION_MEDIA_BUTTON),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        QueueManager queueManager = new QueueManager(
                getResources(),
                new YouTubeVideoUpdateListener()
                {
                    @Override
                    public void onYouTubeVideoChanged(YouTubeVideo youTubeVideo)
                    {
//                        mediaSession.setMetadata(youTubeVideo);
                        LogHelper.e(TAG, "onYouTubeVideoChanged: " + youTubeVideo);
                        if (youTubeVideo != null) {
                            currentYouTubeVideo = youTubeVideo;
                            MediaMetadataCompat.Builder ytVideo = new MediaMetadataCompat.Builder();
                            ytVideo.putString(
                                    MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                                    youTubeVideo.getTitle()
                            );
                            ytVideo.putString(
                                    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                                    youTubeVideo.getViewCount()
                            );
                            ytVideo.putString(
                                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                                    youTubeVideo.getThumbnailURL()
                            );
                            ytVideo.putLong(
                                    MediaMetadataCompat.METADATA_KEY_DURATION,
                                    playbackManager.getDuration()
                            );

                            mediaSession.setMetadata(ytVideo.build());
                        }
                    }

                    @Override
                    public void onYouTubeVideoRetrieveError()
                    {
                        playbackManager.updatePlaybackState(getString(R.string.error_no_yt_video_retrieve));
                    }

                    @Override
                    public void onCurrentQueueIndexUpdated(int queueIndex)
                    {
                        playbackManager.handlePlayRequest();
                    }

                    @Override
                    public void onQueueUpdated(String title, List<YouTubeVideo> newQueue)
                    {
//                        mediaSession.setQueue(newQueue);
                        mediaSession.setQueueTitle(title);
                    }
                }
        );

        LocalPlayback playback = new LocalPlayback(this);
        playbackManager = new PlaybackManager(this, getResources(), queueManager, playback);

        mediaSession = new MediaSessionCompat(
                context,
                TAG,
                eventReceiver,
                buttonReceiverIntent
        );


        playbackManager.updatePlaybackState(null);
        try {
            mediaController = new MediaControllerCompat(
                    context,
                    mediaSession.getSessionToken()
            );

            mediaSession.setCallback(playbackManager.getMediaSessionCallback());
            mediaNotificationManager = new MediaNotificationManager(this);
//            sendSessionTokenToActivity();
//            mediaSession.setSessionActivity(pi);
        } catch (RemoteException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not create a MediaNotificationManager", re);
        }
    }

    private void initPhoneCallListener()
    {
        PhoneStateListener phoneStateListener = new PhoneStateListener()
        {
            @Override
            public void onCallStateChanged(int state, String incomingNumber)
            {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    // Incoming call: Pause music
                    playbackManager.handlePauseRequest();
                } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    // Not in call: Play music
                    playbackManager.handlePlayRequest();
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // A call is dialing, active or on hold
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };

        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    public MediaSessionCompat.Token getSessionToken()
    {
        return mediaController.getSessionToken();
    }

    public void sendSessionTokenToActivity()
    {
//        LogHelper.e(TAG, "ServiceHandler | handleMessage sending: INTENT_SESSION_TOKEN");
        Intent intent = new Intent(INTENT_SESSION_TOKEN);
        Bundle b = new Bundle();
        b.putParcelable(KEY_SESSION_TOKEN, getSessionToken());
        intent.putExtra(KEY_SESSION_TOKEN, b);
        sendBroadcast(intent);
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
        int intentMediaType = intent.getIntExtra(KEY_YOUTUBE_TYPE, YOUTUBE_MEDIA_NO_NEW_REQUEST);

        switch (intentMediaType) {
            // Video has been paused. It is not necessary a new playback requests
            case YOUTUBE_MEDIA_NO_NEW_REQUEST:
                LogHelper.e(TAG, "handleMedia: YOUTUBE_MEDIA_NO_NEW_REQUEST");
                playbackManager.handlePlayRequest();
                break;
            case YOUTUBE_MEDIA_TYPE_VIDEO:
                mediaType = YOUTUBE_MEDIA_TYPE_VIDEO;
                currentYouTubeVideo = (YouTubeVideo) intent.getSerializableExtra(KEY_YOUTUBE_TYPE_VIDEO);
                if (currentYouTubeVideo.getId() != null) {
                    playbackManager.initPlaylist(currentYouTubeVideo, null);
                    LogHelper.e(TAG, "handleMedia: YOUTUBE_MEDIA_TYPE_VIDEO");
                    playbackManager.handlePlayRequest();
                    playbackManager.updateYouTubeVideo();
                }
                break;
            // New playlist playback request
            case YOUTUBE_MEDIA_TYPE_PLAYLIST:
                mediaType = YOUTUBE_MEDIA_TYPE_PLAYLIST;
                youTubeVideos = (ArrayList<YouTubeVideo>) intent.getSerializableExtra(KEY_YOUTUBE_TYPE_PLAYLIST);
                currentVideoPosition = intent.getIntExtra(KEY_YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, 0);
                LogHelper.e(TAG, "currentVideoPosition: " + currentVideoPosition);
                if (youTubeVideos != null && currentVideoPosition != -1) {
                    currentYouTubeVideo = youTubeVideos.get(currentVideoPosition);
                    playbackManager.initPlaylist(currentYouTubeVideo, youTubeVideos);
                    LogHelper.e(TAG, "handleMedia: YOUTUBE_MEDIA_TYPE_PLAYLIST");
                    playbackManager.handlePlayRequest();
                    playbackManager.updateYouTubeVideo();
                }
                break;
            default:
                LogHelper.e(TAG, "Unknown command");
                break;
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
        Intent intent = new Intent(context, BackgroundExoAudioService.class);
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
        Intent intent = new Intent(context, BackgroundExoAudioService.class);
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

//    @Override
//    public void onTaskRemoved(Intent rootIntent)
//    {
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context
//                .NOTIFICATION_SERVICE);
//        notificationManager.cancel(NOTIFICATION_ID);
//    }

    public YouTubeVideo getCurrentYouTubeVideo()
    {
        return currentYouTubeVideo;
    }

    public Playback getPlayback()
    {
        if (playbackManager == null) return null;
        else return playbackManager.getPlayback();
    }
}