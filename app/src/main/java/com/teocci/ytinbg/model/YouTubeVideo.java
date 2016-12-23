package com.teocci.ytinbg.model;

import com.teocci.ytinbg.utils.LogHelper;

import java.io.Serializable;

/**
 * YouTube video class
 * Created by teocci on 3.2.16..
 */
public class YouTubeVideo implements Serializable
{
    private static final String TAG = LogHelper.makeLogTag(YouTubeVideo.class);
    private static final long serialVersionUID = 1L;
    private String id;
    private String title;
    private String thumbnailURL;
    private String duration;
    private String viewCount;

    public YouTubeVideo()
    {
        this.id = "";
        this.title = "";
        this.thumbnailURL = "";
        this.duration = "";
        this.viewCount = "";
    }

    public YouTubeVideo(String id, String title, String thumbnailURL, String duration, String
            viewCount)
    {
        this.id = id;
        this.title = title;
        this.thumbnailURL = thumbnailURL;
        this.duration = duration;
        this.viewCount = viewCount;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getDuration()
    {
        return duration;
    }

    public void setDuration(String duration)
    {
        this.duration = duration;
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

    public String getViewCount()
    {
        return viewCount;
    }

    public void setViewCount(String viewCount)
    {
        this.viewCount = viewCount;
    }

    @Override
    public String toString()
    {
        return "YouTubeVideo {" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
