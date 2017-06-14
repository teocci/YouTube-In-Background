package com.teocci.ytinbg.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

/**
 * Checks whether internet connection is available or not
 * Created by Teocci on 17.3.16..
 */
public class NetworkConf
{
    private Activity activity;

    public NetworkConf(Activity activity)
    {
        this.activity = activity;
    }

    /**
     * Checks whether internet connection is available or not
     *
     * @return boolean
     */
    public boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void createNetErrorDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("You need a network connection to use this application. Please turn on" +
                " mobile network or Wi-Fi in Settings.")
                .setTitle("Unable to connect")
                .setCancelable(false)
                .setPositiveButton("Settings",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                Intent i = new Intent(Settings.ACTION_SETTINGS);
                                activity.startActivity(i);
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                //activity.finish();
                            }
                        }
                );
        AlertDialog alert = builder.create();
        alert.show();
    }
}
