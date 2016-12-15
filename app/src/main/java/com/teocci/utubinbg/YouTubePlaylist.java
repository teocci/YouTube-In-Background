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
package com.teocci.utubinbg;

import java.io.Serializable;

/**
 * YouTube playlist class
 * Created by Teocci on 8.3.16..
 */
public class YouTubePlaylist implements Serializable {

    private static final String TAG = "UTUBINBG PLAYLIST CLASS";
    private String title;
    private String thumbnailURL;
    private String id;
    private long numberOfVideos;
    private String status;


    public YouTubePlaylist() {
        this.title = "";
        this.thumbnailURL = "";
        this.id = "";
        this.numberOfVideos = 0;
        this.status = "";
    }

    public YouTubePlaylist(String title, String thumbnailURL, String id, long numberOfVideos, String status) {
        this.title = title;
        this.thumbnailURL = thumbnailURL;
        this.id = id;
        this.numberOfVideos = numberOfVideos;
        this.status = status;
    }

    public long getNumberOfVideos() {
        return numberOfVideos;
    }

    public void setNumberOfVideos(long numberOfVideos) {
        this.numberOfVideos = numberOfVideos;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnail) {
        this.thumbnailURL = thumbnail;
    }

    public String getStatus() {
        return status;
    }

    public void setPrivacy(String status) {
        this.status = status;
    }


    @Override
    public String toString() {
        return "YouTubePlaylist {" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", number of videos=" + numberOfVideos +
                ", " + status +
                '}';
    }
}
