package com.teocci.ytinbg.model;

import com.teocci.ytinbg.utils.LogHelper;

import java.io.Serializable;

/**
 * YouTube playlist class
 * Created by Teocci
 *
 * @author teocci@yandex.com on 2016-Aug-18
 */
public class YouTubePlaylist implements Serializable
{
    private static final String TAG = LogHelper.makeLogTag(YouTubePlaylist.class);
    private String title;
    private String thumbnailURL;
    private String id;
    private long numberOfVideos;
    private String privacy;


    public YouTubePlaylist()
    {
        this.title = "";
        this.thumbnailURL = "";
        this.id = "";
        this.numberOfVideos = 0;
        this.privacy = "";
    }

    public YouTubePlaylist(String title, String thumbnailURL, String id, long numberOfVideos,
                           String privacy)
    {
        this.title = title;
        this.thumbnailURL = thumbnailURL;
        this.id = id;
        this.numberOfVideos = numberOfVideos;
        this.privacy = privacy;
    }

    public long getNumberOfVideos()
    {
        return numberOfVideos;
    }

    public void setNumberOfVideos(long numberOfVideos)
    {
        this.numberOfVideos = numberOfVideos;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getThumbnailURL()
    {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnail)
    {
        this.thumbnailURL = thumbnail;
    }

    public String getPrivacy()
    {
        return privacy;
    }

    public void setPrivacy(String status)
    {
        this.privacy = status;
    }

    @Override
    public String toString()
    {
        return "YouTubePlaylist {" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", number of videos=" + numberOfVideos +
                ", " + privacy +
                '}';
    }
}
