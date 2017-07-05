package com.teocci.ytinbg.ui;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.teocci.ytinbg.R;
import com.teocci.ytinbg.model.YouTubeFragmentedVideo;
import com.teocci.ytinbg.utils.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-May-15
 */

public class DownloadActivity extends Activity
{
    private static final String TAG = DownloadActivity.class.getSimpleName();

    private static String youtubeLink;

    private LinearLayout mainLayout;
    private ProgressBar mainProgressBar;
    private List<YouTubeFragmentedVideo> formatsToShowList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download);
        mainLayout = findViewById(R.id.main_layout);
        mainProgressBar = findViewById(R.id.progress_bar);
        Intent intent = getIntent();
        String ytLink = intent.getStringExtra(Config.YOUTUBE_LINK); //if it's a string you stored.

        // Check how it was started and if we can get the youtube link
        if (savedInstanceState == null && ytLink != null && !ytLink.trim().isEmpty()) {
            if (ytLink != null && (ytLink.contains(Config.YT_SHORT_LINK) || ytLink.contains(Config.YT_WATCH_LINK))) {
                youtubeLink = ytLink;
                // We have a valid link
                getYoutubeDownloadUrl(youtubeLink);
            } else {
                Toast.makeText(this, R.string.error_no_yt_link, Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (savedInstanceState != null && youtubeLink != null) {
            getYoutubeDownloadUrl(youtubeLink);
        } else {
            finish();
        }
    }

    private Context getActivityContext() { return DownloadActivity.this; }

    private void getYoutubeDownloadUrl(String youtubeLink)
    {
        new YouTubeExtractor(this)
        {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta)
            {
                mainProgressBar.setVisibility(View.GONE);
                if (ytFiles == null) {
                    TextView tv = new TextView(getActivityContext());
                    tv.setText(R.string.app_update);
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                    mainLayout.addView(tv);
                    return;
                }
                formatsToShowList = new ArrayList<>();
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    YtFile ytFile = ytFiles.get(itag);

                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                        addFormatToList(ytFile, ytFiles);
                    }
                }
                Collections.sort(formatsToShowList, new Comparator<YouTubeFragmentedVideo>()
                {
                    @Override
                    public int compare(YouTubeFragmentedVideo lhs, YouTubeFragmentedVideo rhs)
                    {
                        return lhs.height - rhs.height;
                    }
                });
                for (YouTubeFragmentedVideo files : formatsToShowList) {
                    addButtonToMainLayout(vMeta.getTitle(), files);
                }
            }
        }.extract(youtubeLink, true, false);
    }

    private void addFormatToList(YtFile ytFile, SparseArray<YtFile> ytFiles)
    {
        int height = ytFile.getFormat().getHeight();
        if (height != -1) {
            for (YouTubeFragmentedVideo frVideo : formatsToShowList) {
                if (frVideo.height == height && (frVideo.videoFile == null ||
                        frVideo.videoFile.getFormat().getFps() == ytFile.getFormat().getFps())) {
                    return;
                }
            }
        }
        YouTubeFragmentedVideo frVideo = new YouTubeFragmentedVideo();
        frVideo.height = height;
        if (ytFile.getFormat().isDashContainer()) {
            if (height > 0) {
                frVideo.videoFile = ytFile;
                frVideo.audioFile = ytFiles.get(Config.YT_ITAG_FOR_AUDIO);
            } else {
                frVideo.audioFile = ytFile;
            }
        } else {
            frVideo.videoFile = ytFile;
        }
        formatsToShowList.add(frVideo);
    }

    private void addButtonToMainLayout(final String videoTitle, final YouTubeFragmentedVideo ytFragmentedVideo)
    {
        // Display some buttons and let the user choose the formatViewCount
        String btnText;
        if (ytFragmentedVideo.height == -1) {
            btnText = "Audio " + ytFragmentedVideo.audioFile.getFormat().getAudioBitrate() + " kbit/s";
        } else {
            btnText = (ytFragmentedVideo.videoFile.getFormat().getFps() == 60) ?
                    ytFragmentedVideo.height + "p60" :
                    ytFragmentedVideo.height + "p";
        }

        Button btn = new Button(this);
        btn.setText(btnText);
        btn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String filename;
                if (videoTitle.length() > 55) {
                    filename = videoTitle.substring(0, 55);
                } else {
                    filename = videoTitle;
                }
                filename = filename.replaceAll("\\\\|>|<|\"|\\||\\*|\\?|%|:|#|/", "");
                filename += (ytFragmentedVideo.height == -1) ? "" : "-" + ytFragmentedVideo.height + "p";
                String downloadIds = "";
                boolean hideAudioDownloadNotification = false;
                if (ytFragmentedVideo.videoFile != null) {
                    downloadIds += downloadFromUrl(ytFragmentedVideo.videoFile.getUrl(), videoTitle,
                            filename + "." + ytFragmentedVideo.videoFile.getFormat().getExt(), false);
                    downloadIds += "-";
                    hideAudioDownloadNotification = true;
                }
                if (ytFragmentedVideo.audioFile != null) {
                    downloadIds += downloadFromUrl(ytFragmentedVideo.audioFile.getUrl(), videoTitle,
                            filename + "." + ytFragmentedVideo.audioFile.getFormat().getExt(), hideAudioDownloadNotification);
                }
                if (ytFragmentedVideo.audioFile != null)
                    cacheDownloadIds(downloadIds);
                finish();
            }
        });
        mainLayout.addView(btn);

        Log.e(TAG, "YouTubeFragmentedVideo added");
    }

    private long downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName, boolean hide)
    {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);
        if (hide) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setVisibleInDownloadsUi(false);
        } else
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        return manager.enqueue(request);
    }

    private void cacheDownloadIds(String downloadIds)
    {
        File dlCacheFile = new File(this.getCacheDir().getAbsolutePath() + "/" + downloadIds);
        try {
            dlCacheFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
