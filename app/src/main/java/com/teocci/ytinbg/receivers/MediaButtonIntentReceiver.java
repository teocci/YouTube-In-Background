package com.teocci.ytinbg.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.utils.LogHelper;

/**
 * Created by teocci on 23.3.16..
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver
{
    private static final String TAG = LogHelper.makeLogTag(MediaButtonIntentReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent)
    {
        LogHelper.d(TAG, "onReceive");
    }
}
