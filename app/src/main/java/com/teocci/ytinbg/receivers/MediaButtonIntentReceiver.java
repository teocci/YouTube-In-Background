package com.teocci.ytinbg.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by teocci on 23.3.16..
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TEOCCI" , "onRecive");
    }
}
