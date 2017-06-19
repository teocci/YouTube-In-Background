package com.teocci.ytinbg.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.teocci.ytinbg.utils.LogHelper;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2016-Mar-23
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver
{
    private static final String TAG = LogHelper.makeLogTag(MediaButtonIntentReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent)
    {
        LogHelper.e(TAG, "onReceive MediaButtonIntentReceiver");
    }
}
