package com.teocci.ytinbg;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * AsyncTask for acquiring search suggestion in action bar
 * Created by Teocci on 19.2.16..
 */
public class JsonAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {

    private static final String TAG = "UTUBINBG JSON CLASS";

    private final int JSON_ERROR = 0;
    private final int JSON_ARRAY = 1;
    private final int JSON_OBJECT = 2;

    // you may separate this or combined to caller class.
    public interface AsyncResponse {
        void processFinish(ArrayList<String> result);
    }

    public AsyncResponse delegate = null;

    public JsonAsyncTask(AsyncResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ArrayList<String> doInBackground(String... params) {

        //encode param to avoid spaces in URL
        String encodedParam = "";
        try {
            encodedParam = URLEncoder.encode(params[0], "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ArrayList<String> items = new ArrayList<>();
        try {
            URL url = new URL("http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=" + encodedParam);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            // gets the server json data
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));

            String next;
            while ((next = bufferedReader.readLine()) != null) {

                if (checkJson(next) == JSON_ERROR) {
                    //if not valid, remove invalid parts (this is simple hack for URL above)
                    next = next.substring(19, next.length() - 1);
                }

                JSONArray ja = new JSONArray(next);

                for (int i = 0; i < ja.length(); i++) {

                    if (ja.get(i) instanceof JSONArray) {
                        JSONArray ja2 = ja.getJSONArray(i);

                        for (int j = 0; j < ja2.length(); j++) {

                            if (ja2.get(j) instanceof JSONArray) {
                                String suggestion = ((JSONArray) ja2.get(j)).getString(0);
                                //Log.d(TAG, "Suggestion: " + suggestion);
                                items.add(suggestion);
                            }
                        }
                    } else if (ja.get(i) instanceof JSONObject) {
                        //Log.d(TAG, "json object");
                    } else {
                        //Log.d(TAG, "unknown object");
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    protected void onPostExecute(ArrayList<String> result) {
        delegate.processFinish(result);
    }

    /**
     * Checks if JSON data is correctly formatted
     * @param string
     * @return
     */
    private int checkJson(String string) {
        try {
            Object json = new JSONTokener(string).nextValue();
            if (json instanceof JSONObject) {
                return JSON_OBJECT;
            } else if (json instanceof JSONArray) {
                return JSON_ARRAY;
            } else {
                return JSON_ERROR;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return JSON_ERROR;
        }
    }
}

