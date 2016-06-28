package com.commit451.youtubeextractor;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Locale;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class YoutubeExtractorInterceptor implements Interceptor {

    static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    static final String LANGUAGE_QUERY_PARAM = "language";

    @NonNull String mLanguage;

    YoutubeExtractorInterceptor() {
        this.mLanguage = Locale.getDefault().getLanguage();
    }

    @Override public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        HttpUrl url = request.url()
            .newBuilder()
            .addQueryParameter(LANGUAGE_QUERY_PARAM, mLanguage)
            .build();

        Request requestWithHeaders = request.newBuilder()
            .addHeader(ACCEPT_LANGUAGE_HEADER, mLanguage)
            .url(url)
            .build();
        return chain.proceed(requestWithHeaders);
    }

    public void setLanguage(@NonNull String language) {
        mLanguage = language;
    }
}
