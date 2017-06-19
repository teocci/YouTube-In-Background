package com.teocci.ytinbg.notification;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.app.NotificationCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-12
 */

public class NotificationTarget implements Target
{
    public NotificationCompat.Builder builder;

    public void setNotificationBuilder(NotificationCompat.Builder builder)
    {
        this.builder = builder;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from)
    {

    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable)
    {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable)
    {

    }
}