package com.teocci.ytinbg;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.teocci.ytinbg.receivers.MediaButtonIntentReceiver;
import com.teocci.ytinbg.utils.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Service class for background youtube playback
 * Created by Teocci on 9.3.16..
 */
public class BackgroundAudioService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {

    private static final String TAG = "BackgroundAudioService";

    private static final int YOUTUBE_ITAG_251 = 251;    // webm - stereo, 48 KHz 160 Kbps
    private static final int YOUTUBE_ITAG_250 = 250;    // webm - stereo, 48 KHz 64 Kbps
    private static final int YOUTUBE_ITAG_249 = 249;    // webm - stereo, 48 KHz 48 Kbps
    private static final int YOUTUBE_ITAG_171 = 171;    // webm - stereo, 48 KHz 128 Kbps
    private static final int YOUTUBE_ITAG_141 = 141;    // mp4a - stereo, 44.1 KHz 256 Kbps
    private static final int YOUTUBE_ITAG_140 = 140;    // mp4a - stereo, 44.1 KHz 128 Kbps
    private static final int YOUTUBE_ITAG_22 = 22;      // mp4 - stereo, 44.1 KHz 192 Kbps
    private static final int YOUTUBE_ITAG_18 = 18;      // mp4 - stereo, 44.1 KHz 96 Kbps
    private static final int YOUTUBE_ITAG_17 = 17;      // mp4 - stereo, 44.1 KHz 24 Kbps

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    private BackgroundAudioService backgroundAudioService = this;

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat mediaController;

    private int mediaType = Config.YOUTUBE_MEDIA_NO_NEW_REQUEST;

    private boolean isStarting = false;

    private YouTubeVideo currentVideo;
    private String currentVideoTitle;
    private int currentVideoPosition;

    private ArrayList<YouTubeVideo> youTubeVideos;
    private ListIterator<YouTubeVideo> iterator;

    private NotificationCompat.Builder builder = null;

    private boolean nextWasCalled = false;
    private boolean previousWasCalled = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        currentVideo = new YouTubeVideo();
        currentVideoPosition = -1;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        initMediaSessions();
        initPhoneCallListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void initPhoneCallListener() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call: Pause music
                    pauseVideo();
                } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    //Not in call: Play music
                    resumeVideo();
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
     * @param intent
     */
    private void handleIntent(Intent intent) {
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
     * @param intent
     */
    private void handleMedia(Intent intent) {
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
                Log.e(TAG, "currentVideoPosition: " + currentVideoPosition);
                iterator = youTubeVideos.listIterator(currentVideoPosition);
                playNext();
                break;
            default:
                Log.d(TAG, "Unknown command");
                break;
        }
    }

    /**
     * Initializes media sessions and receives media events
     */
    private void initMediaSessions() {
        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        //
        // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
        // permission in AndroidManifest.xml.
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        ComponentName eventReceiver = new ComponentName(getApplicationContext().getPackageName(),
                MediaButtonIntentReceiver.class.getName());
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
            mediaController = new MediaControllerCompat(getApplicationContext(), mediaSession.getSessionToken());

            mediaSession.setCallback(
                    new MediaSessionCompat.Callback() {
                        @Override
                        public void onPlay() {
                            super.onPlay();
                            buildNotification(generateAction(
                                    android.R.drawable.ic_media_pause,
                                    getString(R.string.action_pause),
                                    ACTION_PAUSE
                            ));
                        }

                        @Override
                        public void onPause() {

                            super.onPause();
                            pauseVideo();
                            buildNotification(generateAction(
                                    android.R.drawable.ic_media_play,
                                    getString(R.string.action_play),
                                    ACTION_PLAY
                            ));
                        }

                        @Override
                        public void onSkipToNext() {
                            super.onSkipToNext();
                            if (!isStarting) {
                                playNext();
                            }
                            buildNotification(generateAction(
                                    android.R.drawable.ic_media_pause,
                                    getString(R.string.action_pause),
                                    ACTION_PAUSE
                            ));
                        }

                        @Override
                        public void onSkipToPrevious() {
                            super.onSkipToPrevious();
                            if (!isStarting) {
                                playPrevious();
                            }
                            buildNotification(generateAction(
                                    android.R.drawable.ic_media_pause,
                                    getString(R.string.action_pause),
                                    ACTION_PAUSE
                            ));
                        }

                        @Override
                        public void onStop() {
                            super.onStop();
                            stopPlayer();
                            //remove notification and stop service
                            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.cancel(1);
                            Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
                            stopService(intent);
                        }

                        @Override
                        public void onSetRating(RatingCompat rating) {
                            super.onSetRating(rating);
                        }
                    }
            );
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    /**
     * Builds notification panel with buttons and info on it
     *
     * @param action Action to be applied
     */

    private void buildNotification(NotificationCompat.Action action) {

        final NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        Intent clickIntent = new Intent(this, MainActivity.class);
        clickIntent.setAction(Intent.ACTION_MAIN);
        clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, 0);

        builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.utubinbg_icon);
        builder.setContentTitle(currentVideo.getTitle());
        builder.setContentInfo(currentVideo.getDuration());
        builder.setShowWhen(false);
        builder.setContentIntent(clickPendingIntent);
        builder.setDeleteIntent(stopPendingIntent);
        builder.setOngoing(false);
        builder.setSubText(currentVideo.getViewCount());
        builder.setStyle(style);

        //load bitmap for largeScreen
        if (currentVideo.getThumbnailURL() != null && !currentVideo.getThumbnailURL().isEmpty()) {
            Picasso.with(this)
                    .load(currentVideo.getThumbnailURL())
                    .into(target);
        }

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, getApplicationContext().getString(R.string.action_previous), ACTION_PREVIOUS));
        builder.addAction(action);
        builder.addAction(generateAction(android.R.drawable.ic_media_next, getApplicationContext().getString(R.string.action_next), ACTION_NEXT));
        style.setShowActionsInCompactView(0, 1, 2);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());

    }

    /**
     * Field which handles image loading
     */
    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            updateNotificationLargeIcon(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            Log.d(TAG, "Load bitmap... failed");
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
        }
    };

    /**
     * Updates only large icon in notification panel when bitmap is decoded
     *
     * @param bitmap the large icon in the notification panel
     */
    private void updateNotificationLargeIcon(Bitmap bitmap) {
        builder.setLargeIcon(bitmap);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Generates specific action with parameters below
     *
     * @param icon         the icon number
     * @param title        the title of the notification
     * @param intentAction the action
     * @return
     */
    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    /**
     * Plays next video in playlist
     */
    private void playNext() {
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

        Log.e(TAG, "Start playNext");
        currentVideoPosition = iterator.nextIndex();
        currentVideo = iterator.next();

        nextWasCalled = true;
        playVideo();
    }

    /**
     * Plays previous video in playlist
     */
    private void playPrevious() {
        //if media type is video not playlist, just loop it
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

        Log.e(TAG, "Start playPrevious");
        currentVideoPosition = iterator.previousIndex();
        currentVideo = iterator.previous();

        previousWasCalled = true;
        playVideo();
    }

    /**
     * Plays video
     */
    private void playVideo() {
        isStarting = true;
        extractUrlAndPlay();
    }

    /**
     * Pauses video
     */
    private void pauseVideo() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    /**
     * Resumes video
     */
    private void resumeVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    /**
     * Restarts video
     */
    private void restartVideo() {
        mediaPlayer.start();
    }

    /**
     * Seeks to specific time
     *
     * @param seekTo specific time
     */
    private void seekVideo(int seekTo) {
        mediaPlayer.seekTo(seekTo);
    }

    /**
     * Stops video
     */
    private void stopPlayer() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    private void verifyIterator() {
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
     * Get the best available audio stream
     *
     * @param ytFiles Array of available streams
     * @return Audio stream with highest bitrate
     */
    private YtFile getBestStream(SparseArray<YtFile> ytFiles) {
        if (ytFiles.get(YOUTUBE_ITAG_141) != null) {
            return ytFiles.get(YOUTUBE_ITAG_141);
        } else if (ytFiles.get(YOUTUBE_ITAG_140) != null) {
            return ytFiles.get(YOUTUBE_ITAG_140);
        } else if (ytFiles.get(YOUTUBE_ITAG_251) != null) {
            return ytFiles.get(YOUTUBE_ITAG_251);
        } else if (ytFiles.get(YOUTUBE_ITAG_250) != null) {
            return ytFiles.get(YOUTUBE_ITAG_250);
        } else if (ytFiles.get(YOUTUBE_ITAG_249) != null) {
            return ytFiles.get(YOUTUBE_ITAG_249);
        } else if (ytFiles.get(YOUTUBE_ITAG_171) != null) {
            return ytFiles.get(YOUTUBE_ITAG_171);
        } else if (ytFiles.get(YOUTUBE_ITAG_18) != null) {
            return ytFiles.get(YOUTUBE_ITAG_18);
        } else if (ytFiles.get(YOUTUBE_ITAG_22) != null) {
            return ytFiles.get(YOUTUBE_ITAG_22);
        }

        return ytFiles.get(YOUTUBE_ITAG_17);
    }

    /**
     * Extracts link from youtube video ID, so mediaPlayer can play it
     */
    private void extractUrlAndPlay() {
        Log.e(TAG, "extractUrlAndPlay: extract url for video id=" + currentVideo.getId());
        final String youtubeLink = "http://youtube.com/watch?v=" + currentVideo.getId();

        Log.e(TAG, youtubeLink);

        YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
            @Override
            public void onUrisAvailable(String videoId, final String videoTitle, SparseArray<YtFile> ytFiles) {
                if (ytFiles != null) {
                    YtFile ytFile = getBestStream(ytFiles);
                    try {
                        Log.e(TAG, ytFile.getUrl());
                        Log.e(TAG, "Start playback");
                        if (mediaPlayer != null) {
                            mediaPlayer.reset();
                            mediaPlayer.setDataSource(ytFile.getUrl());
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setOnPreparedListener(backgroundAudioService);
                            currentVideoTitle = videoTitle;
                            mediaPlayer.prepareAsync();
//                            mediaPlayer.start();
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            }
        };
        // Ignore the webm container format
        // ytEx.setIncludeWebM(false);
        // ytEx.setParseDashManifest(true);
        // Lets execute the request
        ytEx.execute(youtubeLink);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
    }

    @Override
    public void onCompletion(MediaPlayer _mediaPlayer) {
        if (mediaType == Config.YOUTUBE_MEDIA_TYPE_PLAYLIST) {
            playNext();
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        } else {
            restartVideo();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isStarting = false;
        mp.start();
        Toast.makeText(
                getApplicationContext(),
                getResources().getString(R.string.toast_message_playing, currentVideoTitle),
                Toast.LENGTH_SHORT
        ).show();
    }
}