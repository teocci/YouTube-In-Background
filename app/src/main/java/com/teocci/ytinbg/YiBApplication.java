package com.teocci.ytinbg;

import android.app.Application;
import android.content.Context;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */

public class YiBApplication extends Application
{
    private static Context context;

    public void onCreate()
    {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getYiBContext()
    {
        return context;
    }
}
