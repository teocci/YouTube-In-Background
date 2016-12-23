package com.teocci.ytinbg.utils;

import android.util.Log;

import com.teocci.ytinbg.model.YouTubeVideo;

import java.util.Iterator;
import java.util.List;

/**
 * Helper methods
 * Created by teocci on 4.2.16..
 */
public class Utils
{
    private static final String TAG = "Utils";

    /**
     * Converting ISO8601 formatted duration to normal readable time
     */
    public static String convertISO8601DurationToNormalTime(String isoTime)
    {
        String formattedTime = new String();

        if (isoTime.contains("H") && isoTime.contains("M") && isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("H"));
            String minutes = isoTime.substring(isoTime.indexOf("H") + 1, isoTime.indexOf("M"));
            String seconds = isoTime.substring(isoTime.indexOf("M") + 1, isoTime.indexOf("S"));
            formattedTime = hours + ":" + formatTo2Digits(minutes) + ":" + formatTo2Digits(seconds);
        } else if (!isoTime.contains("H") && isoTime.contains("M") && isoTime.contains("S")) {
            String minutes = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("M"));
            String seconds = isoTime.substring(isoTime.indexOf("M") + 1, isoTime.indexOf("S"));
            formattedTime = minutes + ":" + formatTo2Digits(seconds);
        } else if (isoTime.contains("H") && !isoTime.contains("M") && isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("H"));
            String seconds = isoTime.substring(isoTime.indexOf("H") + 1, isoTime.indexOf("S"));
            formattedTime = hours + ":00:" + formatTo2Digits(seconds);
        } else if (isoTime.contains("H") && isoTime.contains("M") && !isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("H"));
            String minutes = isoTime.substring(isoTime.indexOf("H") + 1, isoTime.indexOf("M"));
            formattedTime = hours + ":" + formatTo2Digits(minutes) + ":00";
        } else if (!isoTime.contains("H") && !isoTime.contains("M") && isoTime.contains("S")) {
            String seconds = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("S"));
            formattedTime = "0:" + formatTo2Digits(seconds);
        } else if (!isoTime.contains("H") && isoTime.contains("M") && !isoTime.contains("S")) {
            String minutes = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("M"));
            formattedTime = minutes + ":00";
        } else if (isoTime.contains("H") && !isoTime.contains("M") && !isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("H"));
            formattedTime = hours + ":00:00";
        }

        return formattedTime;
    }

    /**
     * Makes values consist of 2 letters "01"
     */
    private static String formatTo2Digits(String str)
    {
        if (str.length() < 2) {
            str = "0" + str;
        }
        return str;
    }

    /**
     * Prints videos nicely formatted
     *
     * @param videos
     */
    public static void prettyPrintVideos(List<YouTubeVideo> videos)
    {
        Log.d(TAG, "=============================================================");
        Log.d(TAG, "\t\tTotal Videos: " + videos.size());
        Log.d(TAG, "=============================================================\n");

        Iterator<YouTubeVideo> playlistEntries = videos.iterator();

        while (playlistEntries.hasNext()) {
            YouTubeVideo playlistItem = playlistEntries.next();
            Log.d(TAG, " video name  = " + playlistItem.getTitle());
            Log.d(TAG, " video id    = " + playlistItem.getId());
            Log.d(TAG, " duration    = " + playlistItem.getDuration());
            Log.d(TAG, " thumbnail   = " + playlistItem.getThumbnailURL());
            Log.d(TAG, "\n-------------------------------------------------------------\n");
        }
    }

    /**
     * Prints video nicely formatted
     *
     * @param playlistEntry
     */
    public static void prettyPrintVideoItem(YouTubeVideo playlistEntry)
    {
        Log.d(TAG, "*************************************************************");
        Log.d(TAG, "\t\tItem:");
        Log.d(TAG, "*************************************************************");

        Log.d(TAG, " video name  = " + playlistEntry.getTitle());
        Log.d(TAG, " video id    = " + playlistEntry.getId());
        Log.d(TAG, " duration    = " + playlistEntry.getDuration());
        Log.d(TAG, " thumbnail   = " + playlistEntry.getThumbnailURL());
        Log.d(TAG, "\n*************************************************************\n");
    }
}
