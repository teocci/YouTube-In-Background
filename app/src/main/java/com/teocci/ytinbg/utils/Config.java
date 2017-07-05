package com.teocci.ytinbg.utils;

import com.teocci.ytinbg.ui.MainActivity;

/**
 * Basic configuration values used in app
 * Created by teocci on 2.2.16..
 */

public final class Config
{
    // YouTube Media Type
    public static final int YOUTUBE_MEDIA_NO_NEW_REQUEST = -1;
    public static final int YOUTUBE_MEDIA_TYPE_VIDEO = 0;
    public static final int YOUTUBE_MEDIA_TYPE_PLAYLIST = 1;


    public static final String KEY_YOUTUBE_TYPE = "YT_MEDIA_TYPE";
    public static final String KEY_YOUTUBE_TYPE_VIDEO = "YT_VIDEO";
    public static final String KEY_YOUTUBE_TYPE_PLAYLIST = "YT_PLAYLIST";
    public static final String KEY_YOUTUBE_TYPE_PLAYLIST_VIDEO_POS = "YT_PLAYLIST_VIDEO_POS";

    public static final String YOUTUBE_LINK = "YT_DOWNLOAD_LINK";

    public static final String YOUTUBE_API_KEY = "AIzaSyAN074XUjainWwyXkhv2hergNIlh2uTWUc";

    // Media Session Token"
    public static final String INTENT_SESSION_TOKEN = "com.teocci.ytinbg.SESSION_TOKEN";
    public static final String KEY_SESSION_TOKEN = "SESSION_TOKEN";

    // Download properties
    public static final int YT_ITAG_FOR_AUDIO = 140;

    public static final String YT_SHORT_LINK = "://youtu.be/";
    public static final String YT_WATCH_LINK = "youtube.com/watch?v=";
    public static final String YT_PREFIX_LINK = "https://youtu.be/";

    public static final String YT_REGEX = "(?:[?&]vi?=|\\/embed\\/|\\/\\d\\d?\\/|\\/vi?\\/|https?:\\/\\/(?:www\\" +
            ".)?youtu\\.be\\/)([A-Za-z0-9_\\-]{11})";


    public static final String ACCOUNT_NAME = "GOOGLE_ACCOUNT_NAME";

    public static final long NUMBER_OF_VIDEOS_RETURNED = 50; //due to YouTube API rules - MAX 50

    // Resolution reasonable for carrying around as an icon (generally in
    // MediaDescription.getIconBitmap). This should not be bigger than necessary, because
    // the MediaDescription object should be lightweight. If you set it too high and try to
    // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
    public static final int MAX_WIDTH_ICON = 128;  // pixels
    public static final int MAX_HEIGHT_ICON = 128;  // pixels

    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "com.teocci.ytinbg.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";

    public static final String KEY_LOCK = "YTinBG_lock";

    // Action to thumbs up a media item
    public static final String CUSTOM_ACTION_THUMBS_UP = "com.teocci.ytinbg.THUMBS_UP";
    public static final String EXTRA_START_FULLSCREEN = "com.teocci.ytinbg.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the {@link MainActivity}, speeding up the screen rendering
     * while the {@link android.support.v4.media.session.MediaControllerCompat} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION = "com.teocci.ytinbg.CURRENT_MEDIA_DESCRIPTION";
    
    public static final String ACTION_PAUSE = "com.teocci.ytinbg.pause";
    public static final String ACTION_PLAY = "com.teocci.ytinbg.play";
    public static final String ACTION_PREV = "com.teocci.ytinbg.prev";
    public static final String ACTION_NEXT = "com.teocci.ytinbg.next";
    public static final String ACTION_STOP = "com.teocci.ytinbg.stop";

    public static final String MODE_REPEAT_ONE = "mode_repeat_one";
    public static final String MODE_REPEAT_ALL = "mode_repeat_all";
    public static final String MODE_REPEAT_NONE = "mode_repeat_none";
    public static final String MODE_SHUFFLE = "mode_shuffle";


    public static final String YOUTUBE_CHANNEL_LIST = "snippet";

    // See: https://developers.google.com/youtube/v3/docs/playlistItems/list
    public static final String YOUTUBE_PLAYLIST_PART = "id,snippet,contentDetails,status";
    public static final String YOUTUBE_PLAYLIST_FIELDS = "items(id,snippet/title,snippet/thumbnails/default/url," +
            "contentDetails/itemCount,status)";
    public static final String YOUTUBE_ACQUIRE_PLAYLIST_PART = "id,contentDetails,snippet";
    public static final String YOUTUBE_ACQUIRE_PLAYLIST_FIELDS = "items(contentDetails/videoId,snippet/title," +
            "snippet/thumbnails/default/url),nextPageToken";

    // See: https://developers.google.com/youtube/v3/docs/videos/list
    public static final String YOUTUBE_SEARCH_LIST_TYPE = "video";
    public static final String YOUTUBE_SEARCH_LIST_PART = "id,snippet";
    public static final String YOUTUBE_SEARCH_LIST_FIELDS = "pageInfo,nextPageToken,items(id/videoId,snippet/title," +
            "snippet/thumbnails/default/url)";
    public static final String YOUTUBE_VIDEO_LIST_PART = "id,contentDetails,statistics";
    public static final String YOUTUBE_VIDEO_LIST_FIELDS = "items(contentDetails/duration,statistics/viewCount)";


    public static final String YOUTUBE_VIDEO_PART = "id,snippet,contentDetails,statistics";
    public static final String YOUTUBE_VIDEO_FIELDS = "items(id,snippet/title," +
            "snippet/thumbnails/default/url,contentDetails/duration,statistics/viewCount)";

    public static final String YOUTUBE_PLAYLIST_VIDEO_PART = "id,contentDetails";
    public static final String YOUTUBE_PLAYLIST_VIDEO_FIELDS = "items(contentDetails/duration)" +
            "statistics/viewCount)";

    public static final String YOUTUBE_LANGUAGE_KEY = "hl";
    // video resource properties that the response will include.
    // selector specifying which fields to include in a partial response.

    public static final int YOUTUBE_ITAG_251 = 251;    // webm - stereo, 48 KHz 160 Kbps (opus)
    public static final int YOUTUBE_ITAG_250 = 250;    // webm - stereo, 48 KHz 64 Kbps (opus)
    public static final int YOUTUBE_ITAG_249 = 249;    // webm - stereo, 48 KHz 48 Kbps (opus)
    public static final int YOUTUBE_ITAG_171 = 171;    // webm - stereo, 48 KHz 128 Kbps (vortis)
    public static final int YOUTUBE_ITAG_141 = 141;    // mp4a - stereo, 44.1 KHz 256 Kbps (aac)
    public static final int YOUTUBE_ITAG_140 = 140;    // mp4a - stereo, 44.1 KHz 128 Kbps (aac)
    public static final int YOUTUBE_ITAG_43 = 43;      // webm - stereo, 44.1 KHz 128 Kbps (vortis)
    public static final int YOUTUBE_ITAG_22 = 22;      // mp4 - stereo, 44.1 KHz 192 Kbps (aac)
    public static final int YOUTUBE_ITAG_18 = 18;      // mp4 - stereo, 44.1 KHz 96 Kbps (aac)
    public static final int YOUTUBE_ITAG_36 = 36;      // mp4 - stereo, 44.1 KHz 32 Kbps (aac)
    public static final int YOUTUBE_ITAG_17 = 17;      // mp4 - stereo, 44.1 KHz 24 Kbps (aac)

    public static final String LOG_PREFIX = "yib_";
}