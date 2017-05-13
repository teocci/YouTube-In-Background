package com.teocci.ytinbg.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.teocci.ytinbg.R;
import com.teocci.ytinbg.adapters.VideosAdapter;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-May-13
 */

public abstract class RecyclerFragment extends Fragment implements AdapterView.OnItemClickListener
{
    protected RecyclerView recyclerView;
    protected VideosAdapter videosAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
//        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_recycler, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.section_list);
        recyclerView.setLayoutManager(getLayoutManager());
        recyclerView.addItemDecoration(getItemDecoration());

        recyclerView.getItemAnimator().setAddDuration(1000);
        recyclerView.getItemAnimator().setChangeDuration(1000);
        recyclerView.getItemAnimator().setMoveDuration(1000);
        recyclerView.getItemAnimator().setRemoveDuration(1000);

        videosAdapter = getAdapter();
        videosAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(videosAdapter);

        return rootView;
    }

    /**
     * Required Overrides for Sample Fragments
     */

    protected abstract RecyclerView.LayoutManager getLayoutManager();

    protected abstract RecyclerView.ItemDecoration getItemDecoration();

    protected abstract VideosAdapter getAdapter();
}
