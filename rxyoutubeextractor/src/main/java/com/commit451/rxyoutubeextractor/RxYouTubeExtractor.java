package com.commit451.rxyoutubeextractor;


import android.support.annotation.Nullable;

import com.commit451.youtubeextractor.BaseExtractor;
import com.commit451.youtubeextractor.YouTubeExtractionResult;

import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.Scheduler;

/**
 * Class that allows you to extract desired data from a YouTube video, such as streamable {@link android.net.Uri}s
 * given its video id, which is typically contained within the YouTube video url, ie. https://www.youtube.com/watch?v=dQw4w9WgXcQ
 * has a video id of dQw4w9WgXcQ
 */
public class RxYouTubeExtractor extends BaseExtractor<RxYouTube> implements RxYouTube {

    /**
     * Create a YouTubeExtractor
     * @return a new {@link RxYouTubeExtractor}
     */
    public static RxYouTubeExtractor create() {
        return create(null, null);
    }

    /**
     * Create a YouTubeExtractor with the provided params
     * @return a new {@link RxYouTubeExtractor}
     */
    public static RxYouTubeExtractor create(@Nullable OkHttpClient.Builder okhttpBuilder,
                                            @Nullable Scheduler scheduler) {
        RxJavaCallAdapterFactory factory;
        if (scheduler == null) {
            factory = RxJavaCallAdapterFactory.create();
        } else {
            factory = RxJavaCallAdapterFactory.createWithScheduler(scheduler);
        }
        return new RxYouTubeExtractor(okhttpBuilder, factory);
    }

    private RxYouTubeExtractor(OkHttpClient.Builder okHttpClientBuilder, RxJavaCallAdapterFactory factory) {
        super(RxYouTube.class, okHttpClientBuilder, factory);
    }

    @Override
    public Observable<YouTubeExtractionResult> extract(String videoId) {
        return getYouTube().extract(videoId);
    }
}
