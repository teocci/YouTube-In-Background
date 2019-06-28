package com.teocci.ytinbg.utils;

import android.os.Build;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jan-16
 */

public class BuildUtil
{
    public static boolean minAPI18()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean minAPI19()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean minAPI21()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean minAPI23()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean minAPI26()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
}
