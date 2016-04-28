/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teocci.utubinbg.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.teocci.utubinbg.YouTubePlaylist;
import com.teocci.utubinbg.YouTubeVideo;

import java.util.ArrayList;

/**
 * SQLite database for storing recentlyWatchedVideos and playlist
 * Created by Teocci on 17.3.16..
 */
public class YouTubeSqlDb {

    private static final String TAG = "TEOCCI TABLE SQL";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "YouTubeDb.db";

    public static final String RECENTLY_WATCHED_TABLE_NAME = "recently_watched_videos";
    public static final String FAVORITES_TABLE_NAME = "favorites_videos";


    public enum VIDEOS_TYPE {FAVORITE, RECENTLY_WATCHED}

    private YouTubeDbHelper dbHelper;

    private Playlists playlists;
    private Videos recentlyWatchedVideos;
    private Videos favoriteVideos;

    private static YouTubeSqlDb ourInstance = new YouTubeSqlDb();

    public static YouTubeSqlDb getInstance() {
        return ourInstance;
    }

    private YouTubeSqlDb() {
    }

    public void init(Context context) {
        dbHelper = new YouTubeDbHelper(context);
        dbHelper.getWritableDatabase();

        playlists = new Playlists();
        recentlyWatchedVideos = new Videos(RECENTLY_WATCHED_TABLE_NAME);
        favoriteVideos = new Videos(FAVORITES_TABLE_NAME);
    }

    public Videos videos(VIDEOS_TYPE type) {
        if (type == VIDEOS_TYPE.FAVORITE) {
            return favoriteVideos;
        } else if (type == VIDEOS_TYPE.RECENTLY_WATCHED) {
            return recentlyWatchedVideos;
        }
        Log.e(TAG, "Error. Unknown video type!");
        return null;
    }

    public Playlists playlists() {
        return playlists;
    }

    private final class YouTubeDbHelper extends SQLiteOpenHelper {
        public YouTubeDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(YouTubeVideoEntry.DATABASE_FAVORITES_TABLE_CREATE);
            db.execSQL(YouTubeVideoEntry.DATABASE_RECENTLY_WATCHED_TABLE_CREATE);
            db.execSQL(YouTubePlaylistEntry.DATABASE_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(YouTubeVideoEntry.DROP_QUERY_RECENTLY_WATCHED);
            db.execSQL(YouTubeVideoEntry.DROP_QUERY_FAVORITES);
            db.execSQL(YouTubePlaylistEntry.DROP_QUERY);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    /**
     * Class that enables basic CRUD operations on Playlists database table
     */
    public class Videos {

        private String tableName;

        private Videos(String tableName) {
            this.tableName = tableName;
        }

        /**
         * Creates video entry in playlists table
         *
         * @param video
         * @return
         */
        public boolean create(YouTubeVideo video) {
            if(checkIfExists(video.getId())){
                return false;
            }
            // Gets the data repository in write mode
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(YouTubeVideoEntry.COLUMN_VIDEO_ID, video.getId());
            values.put(YouTubeVideoEntry.COLUMN_TITLE, video.getTitle());
            values.put(YouTubeVideoEntry.COLUMN_DURATION, video.getDuration());
            values.put(YouTubeVideoEntry.COLUMN_THUMBNAIL_URL, video.getThumbnailURL());
            values.put(YouTubeVideoEntry.COLUMN_VIEWS_NUMBER, video.getViewCount());

            return db.insert(tableName, YouTubeVideoEntry.COLUMN_NAME_NULLABLE, values) > 0;
        }

        /**
         * Checks if entry is already present in database
         * @param videoId
         * @return
         */
        public boolean checkIfExists(String videoId) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String Query = "SELECT * FROM " + tableName + " WHERE " + YouTubeVideoEntry.COLUMN_VIDEO_ID + "='" + videoId + "'";
            Cursor cursor = db.rawQuery(Query, null);
            if (cursor.getCount() <= 0) {
                cursor.close();
                return false;
            }
            cursor.close();
            return true;
        }

        /**
         * Reads all recentlyWatchedVideos from playlists database
         *
         * @return
         */
        public ArrayList<YouTubeVideo> readAll() {

            final String SELECT_QUERY_ORDER_DESC = "SELECT * FROM " + tableName + " ORDER BY "
                    + YouTubeVideoEntry.COLUMN_ENTRY_ID + " DESC";

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            ArrayList<YouTubeVideo> list = new ArrayList<>();

            Cursor c = db.rawQuery(SELECT_QUERY_ORDER_DESC, null);
            while (c.moveToNext()) {
                String videoId = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_VIDEO_ID));
                String title = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_TITLE));
                String duration = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_DURATION));
                String thumbnailUrl = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_THUMBNAIL_URL));
                String viewsNumber = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_VIEWS_NUMBER));
                list.add(new YouTubeVideo(videoId, title, thumbnailUrl, duration, viewsNumber));
            }
            c.close();
            return list;
        }

        /**
         * Deletes video entry with provided ID
         *
         * @param videoId
         * @return
         */
        public boolean delete(String videoId) {
            return dbHelper.getWritableDatabase().delete(tableName,
                    YouTubeVideoEntry.COLUMN_VIDEO_ID + "='" + videoId + "'", null) > 0;
        }

        /**
         * Deletes all entries from database
         *
         * @return
         */
        public boolean deleteAll() {
            return dbHelper.getWritableDatabase().delete(tableName, "1", null) > 0;
        }
    }

    /**
     * Class that enables basic CRUD operations on Videos database table
     */
    public class Playlists {

        private Playlists() {
        }

        /**
         * Creates playlist entry in playlists table
         *
         * @param youTubePlaylist
         * @return
         */
        public boolean create(YouTubePlaylist youTubePlaylist) {
            // Gets the data repository in write mode
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(YouTubePlaylistEntry.COLUMN_PLAYLIST_ID, youTubePlaylist.getId());
            values.put(YouTubePlaylistEntry.COLUMN_TITLE, youTubePlaylist.getTitle());
            values.put(YouTubePlaylistEntry.COLUMN_VIDEOS_NUMBER, youTubePlaylist.getNumberOfVideos());
            values.put(YouTubePlaylistEntry.COLUMN_STATUS, youTubePlaylist.getStatus());
            values.put(YouTubePlaylistEntry.COLUMN_THUMBNAIL_URL, youTubePlaylist.getThumbnailURL());

            // Insert the new row, returning the primary key value of the new row. If -1, operation has failed
            return db.insert(YouTubePlaylistEntry.TABLE_NAME, YouTubePlaylistEntry.COLUMN_NAME_NULLABLE, values) > 0;
        }

        /**
         * Reads all playlists from playlists database
         *
         * @return
         */
        public ArrayList<YouTubePlaylist> readAll() {

            ArrayList<YouTubePlaylist> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor c = db.rawQuery(YouTubePlaylistEntry.SELECT_QUERY_ORDER_DESC, null);
            while (c.moveToNext()) {
                String playlistId = c.getString(c.getColumnIndexOrThrow(YouTubePlaylistEntry.COLUMN_PLAYLIST_ID));
                String title = c.getString(c.getColumnIndexOrThrow(YouTubePlaylistEntry.COLUMN_TITLE));
                long number = c.getLong(c.getColumnIndexOrThrow(YouTubePlaylistEntry.COLUMN_VIDEOS_NUMBER));
                String status = c.getString(c.getColumnIndexOrThrow(YouTubePlaylistEntry.COLUMN_STATUS));
                String thumbnailUrl = c.getString(c.getColumnIndexOrThrow(YouTubePlaylistEntry.COLUMN_THUMBNAIL_URL));
                list.add(new YouTubePlaylist(title, thumbnailUrl, playlistId, number, status));
            }
            c.close();
            return list;
        }

        /**
         * Deletes playlist entry with provided ID
         *
         * @param playlistId
         * @return
         */
        public boolean delete(String playlistId) {
            return dbHelper.getWritableDatabase().delete(YouTubePlaylistEntry.TABLE_NAME,
                    YouTubePlaylistEntry.COLUMN_PLAYLIST_ID + "='" + playlistId + "'", null) > 0;
        }

        /**
         * Deletes all entries from database
         *
         * @return
         */
        public boolean deleteAll() {
            return dbHelper.getWritableDatabase().delete(YouTubePlaylistEntry.TABLE_NAME, "1", null) > 0;
        }
    }

    /**
     * Inner class that defines Videos table entry
     */
    public static abstract class YouTubeVideoEntry implements BaseColumns {
        public static final String COLUMN_ENTRY_ID = "_id";
        public static final String COLUMN_VIDEO_ID = "video_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DURATION = "duration";
        public static final String COLUMN_THUMBNAIL_URL = "thumbnail_url";
        public static final String COLUMN_VIEWS_NUMBER = "views_number";

        public static final String COLUMN_NAME_NULLABLE = "null";

        private static final String DATABASE_RECENTLY_WATCHED_TABLE_CREATE =
                "CREATE TABLE " + RECENTLY_WATCHED_TABLE_NAME + "(" +
                        COLUMN_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_VIDEO_ID + " TEXT NOT NULL UNIQUE," +
                        COLUMN_TITLE + " TEXT NOT NULL," +
                        COLUMN_DURATION + " TEXT," +
                        COLUMN_THUMBNAIL_URL + " TEXT," +
                        COLUMN_VIEWS_NUMBER + " TEXT)";

        private static final String DATABASE_FAVORITES_TABLE_CREATE =
                "CREATE TABLE " + FAVORITES_TABLE_NAME + "(" +
                        COLUMN_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_VIDEO_ID + " TEXT NOT NULL UNIQUE," +
                        COLUMN_TITLE + " TEXT NOT NULL," +
                        COLUMN_DURATION + " TEXT," +
                        COLUMN_THUMBNAIL_URL + " TEXT," +
                        COLUMN_VIEWS_NUMBER + " TEXT)";

        public static final String DROP_QUERY_RECENTLY_WATCHED = "DROP TABLE " + RECENTLY_WATCHED_TABLE_NAME;
        public static final String DROP_QUERY_FAVORITES = "DROP TABLE " + FAVORITES_TABLE_NAME;
    }

    /**
     * Inner class that defines Playlist table entry
     */
    public static abstract class YouTubePlaylistEntry implements BaseColumns {
        public static final String TABLE_NAME = "playlists";
        public static final String COLUMN_ENTRY_ID = "_id";
        public static final String COLUMN_PLAYLIST_ID = "playlist_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_VIDEOS_NUMBER = "videos_number";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_THUMBNAIL_URL = "thumbnail_url";

        public static final String COLUMN_NAME_NULLABLE = "null";

        private static final String DATABASE_TABLE_CREATE =
                "CREATE TABLE " + YouTubePlaylistEntry.TABLE_NAME + "(" +
                        COLUMN_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_PLAYLIST_ID + " TEXT NOT NULL UNIQUE," +
                        COLUMN_TITLE + " TEXT NOT NULL," +
                        COLUMN_VIDEOS_NUMBER + " INTEGER," +
                        COLUMN_THUMBNAIL_URL + " TEXT," +
                        COLUMN_STATUS + " TEXT);";

        public static final String DROP_QUERY = "DROP TABLE " + TABLE_NAME;
        public static final String SELECT_QUERY_ORDER_DESC = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ENTRY_ID + " DESC";
    }
}
