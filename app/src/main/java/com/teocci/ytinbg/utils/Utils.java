package com.teocci.ytinbg.utils;

import android.util.Log;

import com.teocci.ytinbg.model.YouTubeVideo;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Helper methods
 * Created by teocci on 4.2.16..
 */
public class Utils
{
    private static final String TAG = "Utils";

    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();

    static {
        suffixes.put(1_000L, "K");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "B");
        suffixes.put(1_000_000_000_000L, "Q");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "S");
    }

    public static String formatViewCount(String viewCounts)
    {
        String[] split = viewCounts.split(" ");
        String[] segments = split[0].split(",");

//        return formatSting(segments) + " " + split[1];
//        조회수 173,893회

        return formatLong(segmentToLong(segments)) + " " + split[1];
    }

    private static long segmentToLong(String[] segments)
    {
        long number = 0;
        int count = segments.length - 1;
        for (String segment : segments) {
            Log.e(TAG, "segment");
            number += Integer.parseInt(segment) * Math.pow(10, 3 * count--);
        }

        return number;
    }

    public static String formatSting(String[] segments)
    {
        int count = segments.length;
        String suffix = count > 2 ? " M" : count > 1 ? " K" : "";
        count = count > 2 ? count - 2 : count > 1 ? count - 1 : count;
        String number = "";
        for (String segment : segments) {
            number += segment;
            if (count-- == 1) break;
            number += ",";
        }

        return number + suffix;
    }

    public static String formatLong(long value)
    {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return formatLong(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + formatLong(-value);
        if (value < 1000) return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

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

    public static boolean empty( final String s ) {
        // Null-safe, short-circuit evaluation.
        return s == null || s.trim().isEmpty();
    }
}
