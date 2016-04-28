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
package com.teocci.utubinbg.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.teocci.utubinbg.BackgroundAudioService;
import com.teocci.utubinbg.R;
import com.teocci.utubinbg.VideosAdapter;
import com.teocci.utubinbg.YouTubeVideo;
import com.teocci.utubinbg.database.YouTubeSqlDb;
import com.teocci.utubinbg.utils.Config;
import com.teocci.utubinbg.utils.NetworkConf;

import java.util.ArrayList;

/**
 * Created by Teocci on 21.3.16..
 */
public class FavoritesFragment extends Fragment {
    private static final String TAG = "TEOCCI Favorites Fragmet";

    private ArrayList<YouTubeVideo> favoriteVideos;

    private DynamicListView favoritesListView;
    private VideosAdapter videoListAdapter;
    private NetworkConf conf;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        favoriteVideos = new ArrayList<>();
        conf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_recently_watched, container, false);
        TextView fragmentListTitle = (TextView) v.findViewById(R.id.fragment_title_text_view);
        fragmentListTitle.setText("Favorites");
        favoritesListView = (DynamicListView) v.findViewById(R.id.recently_played);
        setupListViewAndAdapter();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getUserVisibleHint()) {
            //do nothing for now
        }
        favoriteVideos.clear();
        favoriteVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).readAll());
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
            //Log.d(TAG, "RecentlyWatchedFragment visible and resumed");
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    /**
     * Setups list view and adapter for storing recently watched YouTube videos
     */
    private void setupListViewAndAdapter() {

        /* Setup the adapter */
        videoListAdapter = new VideosAdapter(getActivity(), favoriteVideos, true);
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(videoListAdapter);
        animationAdapter.setAbsListView(favoritesListView);
        favoritesListView.setAdapter(videoListAdapter);

        addListeners();
    }

    /**
     * Adds listener for list item choosing
     */
    void addListeners() {
        favoritesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, final int pos,
                                    long id) {
                if (conf.isNetworkAvailable()) {

                    Toast.makeText(getContext(), "Playing: " + favoriteVideos.get(pos).getTitle(), Toast.LENGTH_SHORT).show();
                    //add item to recently watched list
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(favoriteVideos.get(pos));

                    Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
                    serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
                    serviceIntent.putExtra(Config.YOUTUBE_TYPE, Config.YOUTUBE_MEDIA_TYPE_PLAYLIST);
                    serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, favoriteVideos);
                    serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, pos);
                    getActivity().startService(serviceIntent);
                } else {
                    conf.createNetErrorDialog();
                }
            }
        });
    }

    /**
     * Clears recently played list items
     */
    public void clearFavoritesList() {
        favoriteVideos.clear();
        videoListAdapter.notifyDataSetChanged();
    }
}
