package com.commit451.youtubeextractor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Extracts the stuff
 */
class YouTubeExtractionConverterFactory extends Converter.Factory {

    public static YouTubeExtractionConverterFactory create() {
        return new YouTubeExtractionConverterFactory();
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        // This is good, we only register if the call includes this type, so that we could potentially
        // still be okay with having additional converter factories if we needed to
        if (type == YouTubeExtractionResult.class) {
            return new YouTubeBodyConverter(retrofit.baseUrl());
        }
        // Allow others to give it a go
        return null;
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        throw new IllegalStateException("Not supported");
    }
}
