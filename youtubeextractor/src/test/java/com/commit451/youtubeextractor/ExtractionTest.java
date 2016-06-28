package com.commit451.youtubeextractor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import retrofit2.Response;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ExtractionTest {

    private static final String GRID_YOUTUBE_ID = "9d8wWcJLnFI";

    @Test
    public void testExtraction() throws Exception {
        YouTubeExtractor extractor = YouTubeExtractor.create();
        Response<YouTubeExtractionResult> resultResponse = extractor.extract(GRID_YOUTUBE_ID).execute();
        TestUtil.assertRetrofitResponseSuccess(resultResponse);
        //Verified before that this ID should hold at least one video and image URI
//        Uri bestVideoUri = resultResponse.body().getBestAvaiableQualityVideoUri();
//        Assert.assertNotNull("did not have a video uri", bestVideoUri);
//        Uri bestImageUri = resultResponse.body().getBestAvaiableQualityThumbUri();
//        Assert.assertNotNull("Did not have an image uri", bestImageUri);
    }
}
