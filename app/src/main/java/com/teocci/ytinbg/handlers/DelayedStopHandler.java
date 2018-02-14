package com.teocci.ytinbg.handlers;

import android.os.Handler;
import android.os.Message;

import com.teocci.ytinbg.BackgroundExoAudioService;
import com.teocci.ytinbg.utils.LogHelper;

import java.lang.ref.WeakReference;


/**
 * A simple handler that stops the service if playback is not active (playing)
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Feb-13
 */

public class DelayedStopHandler extends Handler
{
    private static final String TAG = LogHelper.makeLogTag(DelayedStopHandler.class);

    private final WeakReference<BackgroundExoAudioService> weakReference;

    public DelayedStopHandler(BackgroundExoAudioService service)
    {
        weakReference = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg)
    {
        BackgroundExoAudioService service = weakReference.get();
        if (service != null && service.getPlayback() != null) {
            if (service.getPlayback().isPlaying()) {
                LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                return;
            }
            LogHelper.d(TAG, "Stopping service with delay handler.");
            service.stopSelf();
        }
    }
}
