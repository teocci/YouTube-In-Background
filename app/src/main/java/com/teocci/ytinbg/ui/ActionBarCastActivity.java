package com.teocci.ytinbg.ui;

import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.teocci.ytinbg.R;
import com.teocci.ytinbg.utils.LogHelper;

/**
 * Abstract activity with toolbar, navigation drawer and cast support. Needs to be extended by
 * any activity that wants to be shown as a top level activity.
 * <p>
 * The requirements for a subclass is to call {@link #initializeToolbar()} on onCreate, after
 * setContentView() is called and have three mandatory layout elements:
 * a {@link android.support.v7.widget.Toolbar} with id 'toolbar',
 * a {@link android.support.v4.widget.DrawerLayout} with id 'drawerLayout' and
 * a {@link android.widget.ListView} with id 'drawerList'.
 */
public abstract class ActionBarCastActivity extends AppCompatActivity
{
    private static final String TAG = LogHelper.makeLogTag(ActionBarCastActivity.class);

    private static final int DELAY_MILLIS = 1000;

    private MenuItem mMediaRouteMenuItem;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    private boolean mToolbarInitialized;

//    private int mItemToOpenWhenDrawerCloses = -1;

//    private final DrawerLayout.DrawerListener mDrawerListener = new DrawerLayout.DrawerListener() {
//        @Override
//        public void onDrawerClosed(View drawerView) {
//            if (mDrawerToggle != null) mDrawerToggle.onDrawerClosed(drawerView);
//            if (mItemToOpenWhenDrawerCloses >= 0) {
//                Bundle extras = ActivityOptions.makeCustomAnimation(
//                    ActionBarCastActivity.this, R.anim.fade_in, R.anim.fade_out).toBundle();
//
//                Class activityClass = null;
//                switch (mItemToOpenWhenDrawerCloses) {
//                    case R.id.navigation_allmusic:
//                        activityClass = MainActivity.class;
//                        break;
//                    case R.id.navigation_playlists:
//                        activityClass = PlaceholderActivity.class;
//                        break;
//                }
//                if (activityClass != null) {
//                    startActivity(new Intent(ActionBarCastActivity.this, activityClass), extras);
//                    finish();
//                }
//            }
//        }
//
//        @Override
//        public void onDrawerStateChanged(int newState) {
//            if (mDrawerToggle != null) mDrawerToggle.onDrawerStateChanged(newState);
//        }
//
//        @Override
//        public void onDrawerSlide(View drawerView, float slideOffset) {
//            if (mDrawerToggle != null) mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
//        }
//
//        @Override
//        public void onDrawerOpened(View drawerView) {
//            if (mDrawerToggle != null) mDrawerToggle.onDrawerOpened(drawerView);
//            if (getSupportActionBar() != null) getSupportActionBar()
//                    .setTitle(R.string.app_name);
//        }
//    };

    private final FragmentManager.OnBackStackChangedListener mBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener()
            {
                @Override
                public void onBackStackChanged()
                {
                    updateDrawerToggle();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (!mToolbarInitialized) {
            throw new IllegalStateException("You must run super.initializeToolbar at " +
                    "the end of your onCreate method");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Whenever the fragment back stack changes, we may need to update the
        // action bar toggle: only top level screens show the hamburger-like icon, inner
        // screens - either Activities or fragments - show the "Up" icon instead.
        getFragmentManager().addOnBackStackChangedListener(mBackStackChangedListener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getFragmentManager().removeOnBackStackChangedListener(mBackStackChangedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        // If the drawer is open, back will close it
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        // Otherwise, it may return to the previous fragment stack
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Lastly, it will rely on the system behavior for back
            super.onBackPressed();
        }
    }

    @Override
    public void setTitle(CharSequence title)
    {
        super.setTitle(title);
        mToolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId)
    {
        super.setTitle(titleId);
        mToolbar.setTitle(titleId);
    }

    protected void initializeToolbar()
    {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " +
                    "'toolbar'");
        }
        mToolbar.inflateMenu(R.menu.menu_main);

//        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//        if (mDrawerLayout != null) {
//            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//            if (navigationView == null) {
//                throw new IllegalStateException("Layout requires a NavigationView " +
//                        "with id 'nav_view'");
//            }
//
//            // Create an ActionBarDrawerToggle that will handle opening/closing of the drawer:
//            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
//                    mToolbar, R.string.open_content_drawer, R.string.close_content_drawer);
//            mDrawerLayout.setDrawerListener(mDrawerListener);
//            populateDrawerItems(navigationView);
//            setSupportActionBar(mToolbar);
//            updateDrawerToggle();
//        } else {
            setSupportActionBar(mToolbar);
//        }

        mToolbarInitialized = true;
    }

//    private void populateDrawerItems(NavigationView navigationView)
//    {
//        navigationView.setNavigationItemSelectedListener(
//                new NavigationView.OnNavigationItemSelectedListener()
//                {
//                    @Override
//                    public boolean onNavigationItemSelected(MenuItem menuItem)
//                    {
//                        menuItem.setChecked(true);
//                        mItemToOpenWhenDrawerCloses = menuItem.getItemId();
//                        mDrawerLayout.closeDrawers();
//                        return true;
//                    }
//                });
//        if (MainActivity.class.isAssignableFrom(getClass())) {
//            navigationView.setCheckedItem(R.id.navigation_allmusic);
//        } else if (PlaceholderActivity.class.isAssignableFrom(getClass())) {
//            navigationView.setCheckedItem(R.id.navigation_playlists);
//        }
//    }

    protected void updateDrawerToggle()
    {
        if (mDrawerToggle == null) {
            return;
        }
        boolean isRoot = getFragmentManager().getBackStackEntryCount() == 0;
        mDrawerToggle.setDrawerIndicatorEnabled(isRoot);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(!isRoot);
            getSupportActionBar().setDisplayHomeAsUpEnabled(!isRoot);
            getSupportActionBar().setHomeButtonEnabled(!isRoot);
        }
        if (isRoot) {
            mDrawerToggle.syncState();
        }
    }
}
