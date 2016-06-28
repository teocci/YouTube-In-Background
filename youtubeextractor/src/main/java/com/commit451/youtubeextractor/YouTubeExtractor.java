package com.commit451.youtubeextractor;

import okhttp3.OkHttpClient;
import retrofit2.Call;

/**
 * Class that allows you to extract desired data from a YouTube video, such as streamable {@link android.net.Uri}s
 * given its video id, which is typically contained within the YouTube video url, ie. https://www.youtube.com/watch?v=dQw4w9WgXcQ
 * has a video id of dQw4w9WgXcQ
 */
public class YouTubeExtractor extends BaseExtractor<YouTube> implements YouTube {


    /**
     * Create a YouTubeExtractor
     * @return a new {@link YouTubeExtractor}
     */
    public static YouTubeExtractor create() {
        return new YouTubeExtractor();
    }

    /**
     * Create a new YouTubeExtractor with the OkHttp client
     * @return a new {@link YouTubeExtractor}
     */
    public static YouTubeExtractor create(OkHttpClient.Builder okHttpBuilder) {
        return new YouTubeExtractor(okHttpBuilder);
    }

    /**
     * Create a new YouTubeExtractor
     */
    private YouTubeExtractor() {
        this(new OkHttpClient.Builder());
    }

    private YouTubeExtractor(OkHttpClient.Builder okHttpBuilder) {
        super(YouTube.class, okHttpBuilder, null);
    }

    @Override
    public Call<YouTubeExtractionResult> extract(String videoId) {
        return getYouTube().extract(videoId);
    }
}
