/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teocci.utubinbg;

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

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.teocci.utubinbg.receivers.MediaButtonIntentReceiver;
import com.teocci.utubinbg.utils.Config;

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

    private static final String TAG = "UTUBINBG SERVICE CLASS";

    private static final int YOUTUBE_ITAG_140 = 140; //mp4a - stereo, 44.1 KHz 128 Kbps
    private static final int YOUTUBE_ITAG_22 = 22; //mp4 - stereo, 44.1 KHz 96-100 Kbps
    private static final int YOUTUBE_ITAG_18 = 18; //mp4 - stereo, 44.1 KHz 96-100 Kbps
    private static final int YOUTUBE_ITAG_17 = 17;

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;

    private int mediaType = Config.YOUTUBE_MEDIA_NO_NEW_REQUEST;

    private YouTubeVideo videoItem;

    private boolean isStarting = false;

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
        videoItem = new YouTubeVideo();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        initMediaSessions();
        initPhoneCallListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void initPhoneCallListener(){
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call: Pause music
                    pauseVideo();
                } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                    //Not in call: Play music
                    resumeVideo();
                } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };

        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
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
            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mController.getTransportControls().pause();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        }
    }

    /**
     * Handles media - playlists and videos sent from fragments
     *
     * @param intent
     */
    private void handleMedia(Intent intent) {
        int intentMediaType = intent.getIntExtra(Config.YOUTUBE_TYPE, Config.YOUTUBE_MEDIA_NO_NEW_REQUEST);
        switch (intentMediaType) {
            case Config.YOUTUBE_MEDIA_NO_NEW_REQUEST: //video is paused,so no new playback requests should be processed
                mMediaPlayer.start();
                break;
            case Config.YOUTUBE_MEDIA_TYPE_VIDEO:
                mediaType = Config.YOUTUBE_MEDIA_TYPE_VIDEO;
                videoItem = (YouTubeVideo) intent.getSerializableExtra(Config.YOUTUBE_TYPE_VIDEO);
                if (videoItem.getId() != null) {
                    playVideo();
                }
                break;
            case Config.YOUTUBE_MEDIA_TYPE_PLAYLIST: //new playlist playback request
                mediaType = Config.YOUTUBE_MEDIA_TYPE_PLAYLIST;
                youTubeVideos = (ArrayList<YouTubeVideo>) intent.getSerializableExtra(Config.YOUTUBE_TYPE_PLAYLIST);
                int startPosition = intent.getIntExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, 0);
                iterator = youTubeVideos.listIterator(startPosition);
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
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        ComponentName eventReceiver = new ComponentName(getApplicationContext().getPackageName(),
                MediaButtonIntentReceiver.class.getName());
        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                0,
                new Intent(Intent.ACTION_MEDIA_BUTTON),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        mSession = new MediaSessionCompat(getApplicationContext(), "simple player session",
                eventReceiver, buttonReceiverIntent);

        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());

            mSession.setCallback(
                    new MediaSessionCompat.Callback() {
                        @Override
                        public void onPlay() {
                            super.onPlay();
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onPause() {

                            super.onPause();
                            pauseVideo();
                            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
                        }

                        @Override
                        public void onSkipToNext() {
                            super.onSkipToNext();
                            if (!isStarting) {
                                playNext();
                            }
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onSkipToPrevious() {
                            super.onSkipToPrevious();
                            if (!isStarting) {
                                playPrevious();
                            }
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
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
        builder.setContentTitle(videoItem.getTitle());
        builder.setContentInfo(videoItem.getDuration());
        builder.setShowWhen(false);
        builder.setContentIntent(clickPendingIntent);
        builder.setDeleteIntent(stopPendingIntent);
        builder.setOngoing(false);
        builder.setSubText(videoItem.getViewCount());
        builder.setStyle(style);

        //load bitmap for largeScreen
        if (videoItem.getThumbnailURL() != null && !videoItem.getThumbnailURL().isEmpty()) {
            Picasso.with(this)
                    .load(videoItem.getThumbnailURL())
                    .into(target);
        }

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
        builder.addAction(action);
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
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
     * @param bitmap
     */
    private void updateNotificationLargeIcon(Bitmap bitmap) {
        builder.setLargeIcon(bitmap);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Generates specific action with parameters below
     *
     * @param icon
     * @param title
     * @param intentAction
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
        //if media type is video not playlist, just loop it
        if (mediaType == Config.YOUTUBE_MEDIA_TYPE_VIDEO) {
            seekVideo(0);
            restartVideo();
            return;
        }

        if (previousWasCalled) {
            previousWasCalled = false;
            iterator.next();
        }

        if (!iterator.hasNext()) {
            iterator = youTubeVideos.listIterator();
        }

        videoItem = iterator.next();
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

        if (!iterator.hasPrevious()) {
            iterator = youTubeVideos.listIterator(youTubeVideos.size());
        }

        videoItem = iterator.previous();
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
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    /**
     * Resumes video
     */
    private void resumeVideo() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    /**
     * Restarts video
     */
    private void restartVideo() {
        mMediaPlayer.start();
    }

    /**
     * Seeks to specific time
     *
     * @param seekTo
     */
    private void seekVideo(int seekTo) {
        mMediaPlayer.seekTo(seekTo);
    }

    /**
     * Stops video
     */
    private void stopPlayer() {
        mMediaPlayer.stop();
        mMediaPlayer.release();
    }

    /**
     * Get the best available audio stream
     *
     * @param ytFiles Array of available streams
     * @return Audio stream with highest bitrate
     */
    private YtFile getBestStream(SparseArray<YtFile> ytFiles) {
        if (ytFiles.get(141) != null) {
            return ytFiles.get(141); //mp4a - stereo, 44.1 KHz 256 Kbps
        } else if (ytFiles.get(251) != null) {
            return ytFiles.get(251); //webm - stereo, 48 KHz 160 Kbps
        } else if (ytFiles.get(140) != null) {
            return ytFiles.get(140);  //mp4a - stereo, 44.1 KHz 128 Kbps
        }
        return ytFiles.get(17); //mp4 - stereo, 44.1 KHz 96-100 Kbps
    }

    /**
     * Extracts link from youtube video ID, so mediaPlayer can play it
     */
    private void extractUrlAndPlay() {
        Log.d(TAG, "extractUrlAndPlay: extract url for video id=" + videoItem.getId());
        final String youtubeLink = "http://youtube.com/watch?v=" + videoItem.getId();

        Log.e(TAG, youtubeLink);

        YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
            @Override
            public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
                if (ytFiles != null) {
                    YtFile ytFile = getBestStream(ytFiles);
                    try {
                        Log.e(TAG, ytFile.getUrl());
                        Log.d(TAG, "Start playback");
                        if (mMediaPlayer != null) {
                            mMediaPlayer.reset();
                            mMediaPlayer.setDataSource(ytFile.getUrl());
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.prepare();
                            mMediaPlayer.start();
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
    }

}