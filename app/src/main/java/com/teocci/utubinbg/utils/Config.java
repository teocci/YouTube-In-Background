package com.teocci.utubinbg.utils;

/**
 * Basic configuration values used in app
 * Created by teocci on 2.2.16..
 */

public final class Config {

    public static final boolean DEBUG = false;

    public static final int YOUTUBE_MEDIA_NO_NEW_REQUEST = -1;
    public static final int YOUTUBE_MEDIA_TYPE_VIDEO = 0;
    public static final int YOUTUBE_MEDIA_TYPE_PLAYLIST = 1;

    public static final String YOUTUBE_TYPE = "YT_MEDIA_TYPE";
    public static final String YOUTUBE_TYPE_VIDEO = "YT_VIDEO";
    public static final String YOUTUBE_TYPE_PLAYLIST= "YT_PLAYLIST";
    public static final String YOUTUBE_TYPE_PLAYLIST_VIDEO_POS = "YT_PLAYLIST_VIDEO_POS";

     public static final String YOUTUBE_API_KEY = "AIzaSyBKUw2-bvYNfO3mvWBED-VHVFkgr_JD8aY";

    public static final long NUMBER_OF_VIDEOS_RETURNED = 50; //due to YouTube API rules - MAX 50

}