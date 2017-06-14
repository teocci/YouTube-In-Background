package com.teocci.ytinbg.interfaces;

import com.teocci.ytinbg.model.YouTubeVideo;

import java.util.List;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-13
 */

public interface YouTubeVideoUpdateListener
{
    void onYouTubeVideoChanged(YouTubeVideo youTubeVideo);

    void onYouTubeVideoRetrieveError();

    void onCurrentQueueIndexUpdated(int queueIndex);

    void onQueueUpdated(String title, List<YouTubeVideo> newQueue);
}
