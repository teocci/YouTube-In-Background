package com.teocci.ytinbg.utils;

/**
 * Basic configuration values used in app
 * Created by teocci on 2.2.16..
 */

public final class Config
{
    public static final int YOUTUBE_MEDIA_NO_NEW_REQUEST = -1;
    public static final int YOUTUBE_MEDIA_TYPE_VIDEO = 0;
    public static final int YOUTUBE_MEDIA_TYPE_PLAYLIST = 1;

    public static final String YOUTUBE_LINK = "YT_DOWNLOAD_LINK";
    public static final String YOUTUBE_TYPE = "YT_MEDIA_TYPE";
    public static final String YOUTUBE_TYPE_VIDEO = "YT_VIDEO";
    public static final String YOUTUBE_TYPE_PLAYLIST = "YT_PLAYLIST";
    public static final String YOUTUBE_TYPE_PLAYLIST_VIDEO_POS = "YT_PLAYLIST_VIDEO_POS";

    public static final String YOUTUBE_API_KEY = "AIzaSyAN074XUjainWwyXkhv2hergNIlh2uTWUc";

    // Download properties
    public static final int YT_ITAG_FOR_AUDIO = 140;

    public static final String YT_SHORT_LINK = "://youtu.be/";
    public static final String YT_WATCH_LINK = "youtube.com/watch?v=";
    public static final String YT_PREFIX_LINK = "https://youtu.be/";


    public static final String ACCOUNT_NAME = "GOOGLE_ACCOUNT_NAME";

    public static final long NUMBER_OF_VIDEOS_RETURNED = 50; //due to YouTube API rules - MAX 50

    // Resolution reasonable for carrying around as an icon (generally in
    // MediaDescription.getIconBitmap). This should not be bigger than necessary, because
    // the MediaDescription object should be lightweight. If you set it too high and try to
    // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
    public static final int MAX_WIDTH_ICON = 128;  // pixels
    public static final int MAX_HEIGHT_ICON = 128;  // pixels
}