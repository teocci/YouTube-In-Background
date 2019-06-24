package com.teocci.ytinbg.ui;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;
import com.teocci.ytinbg.BuildConfig;
import com.teocci.ytinbg.JsonAsyncTask;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.ui.fragments.FavoritesFragment;
import com.teocci.ytinbg.ui.fragments.PlaybackControlsFragment;
import com.teocci.ytinbg.ui.fragments.PlaylistFragment;
import com.teocci.ytinbg.ui.fragments.RecentlyWatchedFragment;
import com.teocci.ytinbg.ui.fragments.SearchFragment;
import com.teocci.ytinbg.utils.LogHelper;
import com.teocci.ytinbg.utils.NetworkHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static com.teocci.ytinbg.utils.Config.ACCOUNT_NAME;
import static com.teocci.ytinbg.utils.Config.INTENT_SESSION_TOKEN;
import static com.teocci.ytinbg.utils.Config.KEY_SESSION_TOKEN;
import static com.teocci.ytinbg.youtube.YouTubeSingleton.getCredential;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String PREF_ACCOUNT_NAME = "accountName";

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private Context context;
    private FragmentManager fragmentManager;

    private int initialColors[] = new int[2];

    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;
    private PlaybackControlsFragment playbackControlsFragment;

    private FragmentTransaction lastTransaction;

    private MediaSessionCompat.Token sessionToken;
//    private MediaBrowserCompat mMediaBrowser;

    private int[] tabIcons = {
            R.drawable.ic_favorite_tab_icon,
            R.drawable.ic_recent_history_tab_icon,
            R.drawable.ic_search_tab_icon,
            R.drawable.ic_playlist_tab_icon
    };

    private NetworkHelper networkConf;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    // Callback that ensures that we are showing the controls
    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback()
    {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state)
        {
            LogHelper.e(TAG, "onPlaybackStateChanged");
            if (shouldShowControls()) {
                showPlaybackControls();
            } else {
                LogHelper.e(TAG, "onPlaybackStateChanged: hiding controls cuz state = ", state.getState());
                hidePlaybackControls();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata)
        {
            LogHelper.e(TAG, "onMetadataChanged");
            if (shouldShowControls()) {
                showPlaybackControls();
            } else {
                LogHelper.e(TAG, "onMetadataChanged: hiding controls because metadata is null");
                hidePlaybackControls();
            }
        }
    };

    private BroadcastReceiver messageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Get extra data included in the Intent
            LogHelper.e(TAG, "on BroadcastReceiver receive");
            Bundle b = intent.getBundleExtra(KEY_SESSION_TOKEN);
            sessionToken = b.getParcelable(KEY_SESSION_TOKEN);
            if (sessionToken != null) {
                LogHelper.e(TAG, "on sessionToken receive");
                try {
                    connectToSession(sessionToken);
                } catch (RemoteException re) {
                    LogHelper.e(TAG, re, "could not connect media controller");
                    hidePlaybackControls();
                }
            }
        }
    };

//    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
//            new MediaBrowserCompat.ConnectionCallback()
//            {
//                @Override
//                public void onConnected()
//                {
//                    LogHelper.e(TAG, "onConnected");
//                    try {
//                        connectToSession(mMediaBrowser.getSessionToken());
//                    } catch (RemoteException re) {
//                        LogHelper.e(TAG, re, "could not connect media controller");
//                        hidePlaybackControls();
//                    }
//                }
//            };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        fragmentManager = getSupportFragmentManager();

        YouTubeSqlDb.getInstance().init(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        playbackControlsFragment = (PlaybackControlsFragment) fragmentManager
                .findFragmentById(R.id.fragment_playback_controls);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();
        loadColor();

        checkAndRequestPermissions();
        networkConf = new NetworkHelper(this);


        // Connect a media browser just to get the media session token. There are other ways
        // this can be done, for example by sharing the session token directly.
//        mMediaBrowser = new MediaBrowserCompat(
//                this,
//                new ComponentName(this, BackgroundExoAudioService.class),
//                mConnectionCallback, null
//        );
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        LogHelper.e(TAG, "Main Activity onStart");
        if (playbackControlsFragment == null) {
            throw new IllegalStateException("Missing fragment with id 'controls'. Cannot continue.");
        }

        hidePlaybackControls();

//        mMediaBrowser.connect();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        IntentFilter filter = new IntentFilter(INTENT_SESSION_TOKEN);
        registerReceiver(messageReceiver, filter);

        if (sessionToken != null) {
            LogHelper.e(TAG, "on sessionToken receive");
            try {
                connectToSession(sessionToken);
                if (lastTransaction != null) {
                    lastTransaction.commit();
                    lastTransaction = null;
                }
            } catch (RemoteException re) {
                LogHelper.e(TAG, re, "could not connect media controller");
                hidePlaybackControls();
            }
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        LogHelper.d(TAG, "Main Activity onStop");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null) {
            controller.unregisterCallback(mediaControllerCallback);
        }

//        mMediaBrowser.disconnect();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(messageReceiver);
    }

    /**
     * Override super.onNewIntent() so that calls to getIntent() will return the
     * latest intent that was used to start this Activity rather than the first
     * intent.
     */
    @Override
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        setIntent(intent);

        handleIntent(intent);
    }

    /**
     * Handles Google OAuth 2.0 authorization or account chosen result
     *
     * @param requestCode to use when launching the resolution activity
     * @param resultCode  Standard activity result: operation succeeded.
     * @param data        The received Intent includes an extra for KEY_ACCOUNT_NAME, specifying
     *                    the account name (an email address) you must use to acquire the OAuth 2
     *                    .0 token.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(PREF_ACCOUNT_NAME, accountName);
                    editor.apply();
                    getCredential().setSelectedAccountName(accountName);
                }
            }
        }
    }

    /**
     * Options menu in action bar
     *
     * @param menu Menu options in the action bar
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null && searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        // Suggestions
        final CursorAdapter suggestionAdapter = new SimpleCursorAdapter(this,
                R.layout.dropdown_menu,
                null,
                new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                new int[]{android.R.id.text1},
                0);
        final List<String> suggestions = new ArrayList<>();

        if (searchView != null) {
            searchView.setSuggestionsAdapter(suggestionAdapter);
            searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener()
            {
                @Override
                public boolean onSuggestionSelect(int position)
                {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position)
                {
                    searchView.setQuery(suggestions.get(position), false);
                    searchView.clearFocus();

                    Intent suggestionIntent = new Intent(Intent.ACTION_SEARCH);
                    suggestionIntent.putExtra(SearchManager.QUERY, suggestions.get(position));
                    handleIntent(suggestionIntent);

                    return true;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
            {
                @Override
                public boolean onQueryTextSubmit(String s)
                {
                    return false; // Whenever is true, no new intent will be started
                }

                @Override
                public boolean onQueryTextChange(String suggestion)
                {
                    // Check network connection. If not available, do not query.
                    // This also disables onSuggestionClick triggering
                    if (suggestion.length() > 2) { //make suggestions after 3rd letter
                        if (networkConf.isNetworkAvailable(getApplicationContext())) {
                            new JsonAsyncTask(result -> {
                                suggestions.clear();
                                suggestions.addAll(result);
                                String[] columns = {
                                        BaseColumns._ID,
                                        SearchManager.SUGGEST_COLUMN_TEXT_1
                                };
                                MatrixCursor cursor = new MatrixCursor(columns);

                                for (int i = 0; i < result.size(); i++) {
                                    String[] tmp = {Integer.toString(i), result.get(i)};
                                    cursor.addRow(tmp);
                                }
                                suggestionAdapter.swapCursor(cursor);

                            }).execute(suggestion);
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        MenuItem removeAccountItem = menu.findItem(R.id.action_remove_account);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String chosenAccountName = sp.getString(ACCOUNT_NAME, null);

        if (chosenAccountName != null) {
            removeAccountItem.setVisible(true);
        }
        return true;
    }

    /**
     * Handles selected item from action bar
     *
     * @param item the selected item from the action bar
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Locale locale = getResources().getConfiguration().locale;

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_about:
                DateFormat monthFormat = new SimpleDateFormat("MMMM", locale);
                DateFormat yearFormat = new SimpleDateFormat("yyyy", locale);
                Date date = new Date();

                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Teocci");
                alertDialog.setIcon(R.mipmap.ic_launcher);
                alertDialog.setMessage(
                        "YiB v" + BuildConfig.VERSION_NAME + "\n\nteocci@yandex.com\n\n" +
                                monthFormat.format(date) + " " + yearFormat.format(date) + ".\n"
                );
                alertDialog.setButton(
                        AlertDialog.BUTTON_NEUTRAL,
                        "OK",
                        (dialog, which) -> dialog.dismiss()
                );
                alertDialog.show();

                return true;
            case R.id.action_clear_list:
                YouTubeSqlDb.getInstance()
                        .videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED)
                        .deleteAll();
                recentlyPlayedFragment.clearRecentlyPlayedList();
                return true;
            case R.id.action_remove_account:
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                String chosenAccountName = sp.getString(ACCOUNT_NAME, null);

                if (chosenAccountName != null) {
                    sp.edit().remove(ACCOUNT_NAME).apply();
                }
                return true;
            case R.id.action_search:
                item.expandActionView();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle search intent and queries YouTube for videos
     *
     * @param intent search intent and queries
     */
    private void handleIntent(Intent intent)
    {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            // Switch to search fragment
            viewPager.setCurrentItem(2, true);

            if (searchFragment != null) {
                searchFragment.searchQuery(query);
            }
        }
    }

    /**
     * Setups icons for four tabs
     */
    private void setupTabIcons()
    {
        try {
            for (int i = 0, count = tabLayout.getTabCount(); i < count; i++) {
                tabLayout.getTabAt(i).setIcon(tabIcons[i]);
            }
        } catch (NullPointerException e) {
            LogHelper.e(TAG, "setupTabIcons are not found - Null");
        }
    }

    /**
     * Setups viewPager for switching between pages according to the selected tab
     *
     * @param viewPager for switching between pages
     */
    private void setupViewPager(ViewPager viewPager)
    {
        PagerAdapter pagerAdapter = new PagerAdapter(fragmentManager);

        searchFragment = SearchFragment.newInstance();
        recentlyPlayedFragment = RecentlyWatchedFragment.newInstance();
        pagerAdapter.addFragment(FavoritesFragment.newInstance(), null);
        pagerAdapter.addFragment(recentlyPlayedFragment, null);
        pagerAdapter.addFragment(searchFragment, null);
        pagerAdapter.addFragment(new PlaylistFragment(), null);
        viewPager.setAdapter(pagerAdapter);
    }

    /**
     * Class which provides adapter for fragment pager
     */
    class PagerAdapter extends FragmentPagerAdapter
    {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> titleList = new ArrayList<>();

        PagerAdapter(FragmentManager manager)
        {
            super(manager);
        }

        @Override
        public int getCount()
        {
            return fragmentList.size();
        }

        @Override
        public Fragment getItem(int position)
        {
            return fragmentList.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return titleList.get(position);
        }

        void addFragment(Fragment fragment, String title)
        {
            fragmentList.add(fragment);
            titleList.add(title);
        }
    }

    protected void onMediaControllerConnected()
    {
        // empty implementation, can be overridden by clients.
    }

    protected void showPlaybackControls()
    {
        LogHelper.e(TAG, "showPlaybackControls");
        if (networkConf.isNetworkAvailable(this)) {
            try {
                fragmentManager.beginTransaction()
                        .show(playbackControlsFragment)
//                        .commitAllowingStateLoss();
                        .commit();
            } catch (IllegalStateException ise) {
                // [According to](https://stackoverflow.com/questions/7575921)
//                ise.printStackTrace();
                lastTransaction = fragmentManager.beginTransaction()
                        .show(playbackControlsFragment);
            }
        }
    }

    protected void hidePlaybackControls()
    {
        LogHelper.e(TAG, "hidePlaybackControls");
        try {
            fragmentManager.beginTransaction()
                    .hide(playbackControlsFragment)
                    .commit();
        } catch (IllegalStateException ise) {
            // [According to](https://stackoverflow.com/questions/7575921)
//            ise.printStackTrace();
            lastTransaction = fragmentManager.beginTransaction()
                    .hide(playbackControlsFragment);
        }
    }

    /**
     * Check if the MediaSession is active and in a "playback-able" state
     * (not NONE and not STOPPED).
     *
     * @return true if the MediaSession's state requires playback controls to be visible.
     */
    protected boolean shouldShowControls()
    {
        LogHelper.e(TAG, "shouldShowControls");
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController == null ||
                mediaController.getMetadata() == null ||
                mediaController.getPlaybackState() == null) {
            return false;
        }
        switch (mediaController.getPlaybackState().getState()) {
            case STATE_ERROR:
            case STATE_NONE:
            case STATE_STOPPED:
                return false;
            default:
                return true;
        }
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException
    {
        LogHelper.e(TAG, "connectToSession");
        MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
        MediaControllerCompat.setMediaController(this, mediaController);

        mediaController.registerCallback(mediaControllerCallback);

        if (shouldShowControls()) {
            showPlaybackControls();
        } else {
            LogHelper.e(TAG, "connectionCallback.onConnected: hiding controls because metadata is null");
            hidePlaybackControls();
        }

        if (playbackControlsFragment != null) {
            playbackControlsFragment.onConnected();
        }

        onMediaControllerConnected();
    }

    /**
     * Loads app theme color saved in preferences
     */
    private void loadColor()
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int backgroundColor = sp.getInt("BACKGROUND_COLOR", -1);
        int textColor = sp.getInt("TEXT_COLOR", -1);

        if (backgroundColor != -1 && textColor != -1) {
            setColors(backgroundColor, textColor);
        } else {
            initialColors = new int[]{
                    ContextCompat.getColor(this, R.color.color_primary),
                    ContextCompat.getColor(this, R.color.text_color_primary)
            };
        }
    }

    /**
     * Save app theme color in preferences
     */
    private void setColors(int backgroundColor, int textColor)
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(backgroundColor);
        toolbar.setTitleTextColor(textColor);
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setBackgroundColor(backgroundColor);
        tabs.setTabTextColors(textColor, textColor);
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putInt("BACKGROUND_COLOR", backgroundColor).apply();
        sp.edit().putInt("TEXT_COLOR", textColor).apply();

        initialColors[0] = backgroundColor;
        initialColors[1] = textColor;
    }

    private boolean checkAndRequestPermissions()
    {
        int writeStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int getAccounts = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);
        int readAccounts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        int readPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        int accessWIFIState = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (writeStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (readStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (getAccounts != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.GET_ACCOUNTS);
        }
        if (readAccounts != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_CONTACTS);
        }
        if (readPhoneState != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_PHONE_STATE);
        }
        if (accessWIFIState != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS
            );
            return false;
        }
        return true;
    }
}
