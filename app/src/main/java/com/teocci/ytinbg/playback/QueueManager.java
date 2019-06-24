package com.teocci.ytinbg.playback;

import android.content.res.Resources;

import com.teocci.ytinbg.R;
import com.teocci.ytinbg.interfaces.YouTubeVideoUpdateListener;
import com.teocci.ytinbg.model.YouTubeVideo;
import com.teocci.ytinbg.utils.LogHelper;
import com.teocci.ytinbg.utils.QueueHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-10
 */

public class QueueManager
{
    private static final String TAG = LogHelper.makeLogTag(QueueManager.class);

    private YouTubeVideoUpdateListener youTubeVideoUpdateListener;
    private Resources resources;

    // "Now playing" queue:
    private List<YouTubeVideo> playingQueue;
    private int currentIndex;

    public QueueManager(@NonNull Resources resources,
                        @NonNull YouTubeVideoUpdateListener listener)
    {
        this.youTubeVideoUpdateListener = listener;
        this.resources = resources;

        playingQueue = Collections.synchronizedList(new ArrayList<YouTubeVideo>());
        currentIndex = 0;
    }

    private void setCurrentQueueIndex(int index)
    {
        if (index >= 0 && index < playingQueue.size()) {
            currentIndex = index;
            youTubeVideoUpdateListener.onCurrentQueueIndexUpdated(currentIndex);
        }
    }

    public boolean setCurrentQueueItem(String youTubeVideoId)
    {
        // Set the current index on queue from the queue Id:
        int index = QueueHelper.getVideoIndexOnQueue(playingQueue, youTubeVideoId);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    public boolean skipQueuePosition(int amount)
    {
        int index = currentIndex + amount;
        if (index < 0) {
            // skip backwards before the first song will keep you on the first song
            index = 0;
        } else {
            // skip forwards when in last song will cycle back to start of the queue
            index %= playingQueue.size();
        }
        if (!QueueHelper.isIndexPlayable(index, playingQueue)) {
            LogHelper.e(TAG, "Cannot increment queue index by ", amount,
                    ". Current=", currentIndex, " queue length=", playingQueue.size());
            return false;
        }
        currentIndex = index;
        return true;
    }

    public YouTubeVideo getCurrentVideo()
    {
        if (!QueueHelper.isIndexPlayable(currentIndex, playingQueue)) {
            return null;
        }
        return playingQueue.get(currentIndex);
    }

    public int getCurrentVideoIndex(String youTubeVideoId)
    {
        // Set the current index on queue from the queue Id:
        return QueueHelper.getVideoIndexOnQueue(playingQueue, youTubeVideoId);
    }

    public int getCurrentQueueSize()
    {
        if (playingQueue == null) {
            return 0;
        }
        return playingQueue.size();
    }

    public void setCurrentQueue(String title, List<YouTubeVideo> newQueue)
    {
        setCurrentQueue(title, newQueue, null);
    }

    public void setCurrentQueue(YouTubeVideo initialVideo, List<YouTubeVideo> newQueue)
    {
        playingQueue = newQueue;
        if (newQueue == null) {
            playingQueue = Collections.synchronizedList(new ArrayList<YouTubeVideo>());
            playingQueue.add(initialVideo);
        }

        int index = 0;
        if (initialVideo != null) {
            index = QueueHelper.getYouTubeVideoIndexOnQueue(playingQueue, initialVideo.getId());
        }
        currentIndex = Math.max(index, 0);
        youTubeVideoUpdateListener.onQueueUpdated(
                resources.getString(R.string.fragment_tab_favorites),
                newQueue
        );
    }

    public void setCurrentQueue(String title, List<YouTubeVideo> newQueue,
                                String initialVideoId)
    {
        playingQueue = newQueue;
        int index = 0;
        if (initialVideoId != null) {
            index = QueueHelper.getYouTubeVideoIndexOnQueue(playingQueue, initialVideoId);
        }
        currentIndex = Math.max(index, 0);
        youTubeVideoUpdateListener.onQueueUpdated(title, newQueue);
    }

    public void updateYouTubeVideo()
    {
        YouTubeVideo currentYouTubeVideo = getCurrentVideo();
        if (currentYouTubeVideo == null) {
            youTubeVideoUpdateListener.onYouTubeVideoRetrieveError();
            return;
        }

        youTubeVideoUpdateListener.onYouTubeVideoChanged(currentYouTubeVideo);
    }
}
