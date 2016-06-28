package com.commit451.rxyoutubeextractor;


import com.commit451.youtubeextractor.YouTubeExtractorConstants;
import com.commit451.youtubeextractor.YouTubeExtractionResult;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Interface for RxYouTube extraction
 */
public interface RxYouTube {
    @GET(YouTubeExtractorConstants.INFO)
    Observable<YouTubeExtractionResult> extract(@Query(YouTubeExtractorConstants.VIDEO_ID) String videoId);
}
