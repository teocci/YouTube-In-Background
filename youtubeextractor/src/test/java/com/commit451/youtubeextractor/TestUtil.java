package com.commit451.youtubeextractor;

import org.junit.Assert;

import retrofit2.Response;

/**
 * Simple test util
 */
public class TestUtil {

    public static void assertRetrofitResponseSuccess(Response response) throws Exception {
        if (!response.isSuccessful()) {
            Assert.assertTrue(response.errorBody().string(), response.isSuccessful());
        }
    }
}
