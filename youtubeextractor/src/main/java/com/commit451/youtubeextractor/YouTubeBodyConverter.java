package com.commit451.youtubeextractor;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import static java.util.Arrays.asList;

/**
 * Converts the bodies for the YouTubes
 */
class YouTubeBodyConverter implements Converter<ResponseBody, YouTubeExtractionResult> {

    private HttpUrl mBaseUrl;

    public YouTubeBodyConverter(HttpUrl baseUrl) {
        mBaseUrl = baseUrl;
    }

    @Override
    public YouTubeExtractionResult convert(ResponseBody value) throws IOException {
        String html = value.string();

        HashMap<String, String> video = getQueryMap(html, "UTF-8");

        if (video.containsKey("url_encoded_fmt_stream_map")) {
            List<String> streamQueries = new ArrayList<>(asList(video.get("url_encoded_fmt_stream_map").split(",")));

            String adaptiveFmts = video.get("adaptive_fmts");
            String[] split = adaptiveFmts.split(",");

            streamQueries.addAll(asList(split));

            SparseArray<String> streamLinks = new SparseArray<>();
            for (String streamQuery : streamQueries) {
                HashMap<String, String> stream = getQueryMap(streamQuery, "UTF-8");
                String type = stream.get("type").split(";")[0];
                String urlString = stream.get("url");

                if (urlString != null && MimeTypeMap.getSingleton().hasMimeType(type)) {
                    String signature = stream.get("sig");

                    if (signature != null) {
                        urlString = urlString + "&signature=" + signature;
                    }

                    if (getQueryMap(urlString, "UTF-8").containsKey("signature")) {
                        streamLinks.put(Integer.parseInt(stream.get("itag")), urlString);
                    }
                }
            }

            final Uri sd240VideoUri = extractVideoUri(YouTubeExtractor.YOUTUBE_VIDEO_QUALITY_SMALL_240, streamLinks);
            final Uri sd360VideoUri = extractVideoUri(YouTubeExtractor.YOUTUBE_VIDEO_QUALITY_MEDIUM_360, streamLinks);
            final Uri hd720VideoUri = extractVideoUri(YouTubeExtractor.YOUTUBE_VIDEO_QUALITY_HD_720, streamLinks);
            final Uri hd1080VideoUri = extractVideoUri(YouTubeExtractor.YOUTUBE_VIDEO_QUALITY_HD_1080, streamLinks);

            final Uri mediumThumbUri = video.containsKey("iurlmq") ? Uri.parse(video.get("iurlmq")) : null;
            final Uri highThumbUri = video.containsKey("iurlhq") ? Uri.parse(video.get("iurlhq")) : null;
            final Uri defaultThumbUri = video.containsKey("iurl") ? Uri.parse(video.get("iurl")) : null;
            final Uri standardThumbUri = video.containsKey("iurlsd") ? Uri.parse(video.get("iurlsd")) : null;
            //final String description = doc.select("p[id=\"eow-description\"]").first().html();

            return new YouTubeExtractionResult()
                    .setSd240VideoUri(sd240VideoUri)
                    .setSd360VideoUri(sd360VideoUri)
                    .setHd720VideoUri(hd720VideoUri)
                    .setHd1080VideoUri(hd1080VideoUri)
                    .setMediumThumbUri(mediumThumbUri)
                    .setHighThumbUri(highThumbUri)
                    .setDefaultThumbUri(defaultThumbUri)
                    .setStandardThumbUri(standardThumbUri);
        } else {
            throw new YouTubeExtractionException("Status: " + video.get("status") + "\nReason: " + video.get("reason") + "\nError code: " + video.get("errorcode"));
        }
    }

    private static HashMap<String, String> getQueryMap(String queryString, String charsetName) throws UnsupportedEncodingException {
        HashMap<String, String> map = new HashMap<>();

        String[] fields = queryString.split("&");

        for (String field : fields) {
            String[] pair = field.split("=");
            if (pair.length == 2) {
                String key = pair[0];
                String value = URLDecoder.decode(pair[1], charsetName).replace('+', ' ');
                map.put(key, value);
            }
        }

        return map;
    }

    @Nullable
    private Uri extractVideoUri(int quality, SparseArray<String> streamLinks) {
        Uri videoUri = null;
        if (streamLinks.get(quality, null) != null) {
            String streamLink = streamLinks.get(quality);
            videoUri = Uri.parse(streamLink);
        }
        return videoUri;
    }
}
