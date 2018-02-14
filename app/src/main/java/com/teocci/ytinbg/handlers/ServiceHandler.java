package com.teocci.ytinbg.handlers;

import android.os.Handler;
import android.os.Message;

import com.teocci.ytinbg.BackgroundExoAudioService;

import java.lang.ref.WeakReference;

/**
 * Define how the handler will process messages
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Feb-13
 */

public class ServiceHandler extends Handler
{
    private final WeakReference<BackgroundExoAudioService> weakReference;

    public ServiceHandler(BackgroundExoAudioService service)
    {
        weakReference = new WeakReference<>(service);
    }

    // Define how to handle any incoming messages here
    @Override
    public void handleMessage(Message message)
    {
//            LogHelper.e(TAG, "ServiceHandler | handleMessage");
        BackgroundExoAudioService service = weakReference.get();
        if (service != null && service.getPlayback() != null) {
            if (service.getPlayback().isPlaying()) {
                service.sendSessionTokenToActivity();
            }
        }
    }
}