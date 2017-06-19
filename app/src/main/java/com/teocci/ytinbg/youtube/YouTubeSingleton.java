package com.teocci.ytinbg.youtube;

import android.content.Context;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.teocci.ytinbg.BuildConfig;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.YiBApplication;
import com.teocci.ytinbg.ui.MainActivity;
import com.teocci.ytinbg.utils.LogHelper;

import java.io.IOException;
import java.util.Arrays;

import static com.teocci.ytinbg.utils.Auth.SCOPES;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-09
 */

public class YouTubeSingleton
{
    private static String TAG = LogHelper.makeLogTag(YouTubeSingleton.class);

    private static YouTube youTube;
    private static YouTube youTubeWithCredentials;
    private static GoogleAccountCredential credential;


    // Create the instance
    private static YouTubeSingleton instance;

    public static YouTubeSingleton getInstance()
    {
        if (instance == null) {
            synchronized(YouTubeSingleton.getInstance()) {
                if (instance == null)
                    instance = new YouTubeSingleton();
            }
        }
        // Return the instance
        return instance;
    }

    private YouTubeSingleton()
    {
        Context context = YiBApplication.getYiBContext();
        String appName = context.getString(R.string.app_name);
        credential = GoogleAccountCredential
                .usingOAuth2(context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        youTube = new YouTube.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                new HttpRequestInitializer()
                {
                    @Override
                    public void initialize(HttpRequest httpRequest) throws IOException {}
                }
        ).setApplicationName(appName).build();

        youTubeWithCredentials = new YouTube.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                credential
        ).setApplicationName(appName).build();
    }

    public static YouTube getYouTube()
    {
        return youTube;
    }

    public static YouTube getYouTubeWithCredentials()
    {
        return youTubeWithCredentials;
    }

    public static GoogleAccountCredential getCredential()
    {
        return credential;
    }
}
