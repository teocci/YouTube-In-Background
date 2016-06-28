package com.commit451.youtubeextractor;

import android.support.annotation.Nullable;

import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Base YouTube extractor, which you can extend if you want to customize to provide your own call adapter
 * @param <T> the Retrofit interface
 */
public abstract class BaseExtractor<T> {

    private static final String BASE_URL = "https://www.youtube.com/";
    static final int YOUTUBE_VIDEO_QUALITY_SMALL_240 = 36;
    static final int YOUTUBE_VIDEO_QUALITY_MEDIUM_360 = 18;
    static final int YOUTUBE_VIDEO_QUALITY_HD_720 = 22;
    static final int YOUTUBE_VIDEO_QUALITY_HD_1080 = 37;

    private final T mYouTube;

    private final YoutubeExtractorInterceptor mYoutubeExtractorInterceptor = new YoutubeExtractorInterceptor();

    public BaseExtractor(Class<T> youTubeClass, OkHttpClient.Builder okBuilder,
                         @Nullable CallAdapter.Factory callAdapterFactory) {

        if (okBuilder == null) {
            okBuilder = new OkHttpClient.Builder();
        }
        okBuilder.addInterceptor(mYoutubeExtractorInterceptor);

        Retrofit.Builder retrofitBuilder = new Retrofit.Builder();

        retrofitBuilder
            .baseUrl(BASE_URL)
            .client(okBuilder.build())
            .addConverterFactory(YouTubeExtractionConverterFactory.create());

        if (callAdapterFactory != null) {
            retrofitBuilder.addCallAdapterFactory(callAdapterFactory);
        }

        mYouTube = retrofitBuilder.build().create(youTubeClass);
    }

    public T getYouTube() {
        return mYouTube;
    }

    /**
     * Set the language. Defaults to {@link java.util.Locale#getDefault()}
     * @param language the language
     */
    public void setLanguage(String language) {
        mYoutubeExtractorInterceptor.setLanguage(language);
    }

}
