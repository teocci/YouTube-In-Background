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
package com.teocci.utubinbg.interfaces;

import com.teocci.utubinbg.YouTubeVideo;

import java.util.ArrayList;

/**
 * Interface which enables passing videos to the fragments
 * Created by Teocci on 10.3.16..
 */
public interface YouTubeVideosReceiver {
    void onVideosReceived(ArrayList<YouTubeVideo> youTubeVideos);
    void onPlaylistNotFound(String playlistId, int errorCode);
}