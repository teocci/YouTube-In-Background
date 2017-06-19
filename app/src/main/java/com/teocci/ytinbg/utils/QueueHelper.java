package com.teocci.ytinbg.utils;

import android.content.Context;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;

import com.teocci.ytinbg.model.YouTubeVideo;

import java.util.List;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-10
 */

public class QueueHelper
{
    private static final String TAG = QueueHelper.class.getSimpleName();

    public static int getYouTubeVideoIndexOnQueue(Iterable<YouTubeVideo> queue,
                                                  String ytVideoId) {
        int index = 0;
        for (YouTubeVideo item : queue) {
            if (ytVideoId.equals(item.getId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static int getVideoIndexOnQueue(Iterable<YouTubeVideo> queue,
                                           String ytVideoId) {
        int index = 0;
        for (YouTubeVideo item : queue) {
            if (ytVideoId.equals(item.getId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

//    private static List<YouTubeVideo> convertToQueue(
//            Iterable<YouTubeVideo> youTubeVideos, String... categories) {
//        List<YouTubeVideo> queue = new ArrayList<>();
//        int count = 0;
//        for (YouTubeVideo youTubeVideo : youTubeVideos) {
//
//            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
//            // at the QueueItem media IDs.
//            String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
//                    youTubeVideo.getDescription().getMediaId(), categories);
//
//            YouTubeVideo youTubeVideoCopy = new YouTubeVideo.Builder(youTubeVideo)
//                    .putString(YouTubeVideo.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
//                    .build();
//
//            // We don't expect queues to change after created, so we use the item index as the
//            // queueId. Any other number unique in the queue would work.
//            YouTubeVideo item = new YouTubeVideo(
//                    youTubeVideoCopy.getDescription(), count++);
//            queue.add(item);
//        }
//        return queue;
//
//    }

//    /**
//     * Create a random queue with at most {@link #RANDOM_QUEUE_SIZE} elements.
//     *
//     * @param musicProvider the provider used for fetching music.
//     * @return list containing {@link YouTubeVideo}'s
//     */
//    public static List<YouTubeVideo> getRandomQueue(MusicProvider musicProvider) {
//        List<YouTubeVideo> result = new ArrayList<>(RANDOM_QUEUE_SIZE);
//        Iterable<YouTubeVideo> shuffled = musicProvider.getShuffledMusic();
//        for (YouTubeVideo metadata: shuffled) {
//            if (result.size() == RANDOM_QUEUE_SIZE) {
//                break;
//            }
//            result.add(metadata);
//        }
//        LogHelper.d(TAG, "getRandomQueue: result.size=", result.size());
//
//        return convertToQueue(result, MEDIA_ID_MUSICS_BY_SEARCH, "random");
//    }

    public static boolean isIndexPlayable(int index, List<YouTubeVideo> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }

    /**
     * Determine if two queues contain identical media id's in order.
     *
     * @param list1 containing {@link YouTubeVideo}'s
     * @param list2 containing {@link YouTubeVideo}'s
     * @return boolean indicating whether the queue's match
     */
    public static boolean equals(List<YouTubeVideo> list1,
                                 List<YouTubeVideo> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i=0; i<list1.size(); i++) {
            if (!TextUtils.equals(list1.get(i).getId(), list2.get(i).getId())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine if queue item matches the currently playing queue item
     *
     * @param context for retrieving the {@link MediaControllerCompat}
     * @param youTubeVideo to compare to currently playing {@link YouTubeVideo}
     * @return boolean indicating whether queue item matches currently playing queue item
     */
    public static boolean isQueueItemPlaying(Context context,
                                             YouTubeVideo youTubeVideo) {
        // Queue item is considered to be playing or paused based on both the controller's
        // current media id and the controller's active queue item id
//        MediaControllerCompat controller = ((FragmentActivity) context).getSupportMediaController();
//        if (controller != null && controller.getPlaybackState() != null) {
//            long currentPlayingQueueId = controller.getPlaybackState().getActiveQueueItemId();
//            String currentPlayingMediaId = controller.getMetadata().getDescription().getMediaId();
//            String itemMusicId = MediaIDHelper.extractMusicIDFromMediaID(youTubeVideo.getDescription().getMediaId());
//            if (youTubeVideo.getQueueId() == currentPlayingQueueId
//                    && currentPlayingMediaId != null
//                    && TextUtils.equals(currentPlayingMediaId, itemMusicId)) {
//                return true;
//            }
//        }
        return false;
    }
}
