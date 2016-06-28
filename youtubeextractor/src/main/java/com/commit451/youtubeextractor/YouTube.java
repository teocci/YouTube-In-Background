package com.commit451.youtubeextractor;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YouTube {

    @GET(YouTubeExtractorConstants.INFO)
    Call<YouTubeExtractionResult> extract(@Query(YouTubeExtractorConstants.VIDEO_ID) String videoId);

}
