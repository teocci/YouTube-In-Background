package com.teocci.ytinbg.ui;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.teocci.ytinbg.BuildConfig;
import com.teocci.ytinbg.JsonAsyncTask;
import com.teocci.ytinbg.R;
import com.teocci.ytinbg.database.YouTubeSqlDb;
import com.teocci.ytinbg.interfaces.JsonAsyncResponse;
import com.teocci.ytinbg.utils.LogHelper;
import com.teocci.ytinbg.utils.NetworkConf;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = LogHelper.makeLogTag(MainActivity.class);
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private int initialColors[] = new int[2];

    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;

    private int[] tabIcons = {
            R.drawable.ic_favorite_tab_icon,
            android.R.drawable.ic_menu_recent_history,
            android.R.drawable.ic_menu_search,
            android.R.drawable.ic_menu_upload_you_tube
    };

    private NetworkConf networkConf;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        YouTubeSqlDb.getInstance().init(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        checkAndRequestPermissions();
        networkConf = new NetworkConf(this);

        setupTabIcons();
        loadColor();
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
            tabLayout.getTabAt(0).setIcon(tabIcons[0]);
            tabLayout.getTabAt(1).setIcon(tabIcons[1]);
            tabLayout.getTabAt(2).setIcon(tabIcons[2]);
            tabLayout.getTabAt(3).setIcon(tabIcons[3]);
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
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        searchFragment = new SearchFragment();
        recentlyPlayedFragment = new RecentlyWatchedFragment();
        adapter.addFragment(new FavoritesFragment(), getString(R.string.fragment_tab_favorites));
        adapter.addFragment(recentlyPlayedFragment, getString(R.string
                .fragment_tab_recently_watched));
        adapter.addFragment(searchFragment, getString(R.string.fragment_tab_search));
        adapter.addFragment(new PlaylistFragment(), getString(R.string.fragment_tab_playlist));
        viewPager.setAdapter(adapter);
    }

    /**
     * Class which provides adapter for fragment pager
     */
    class ViewPagerAdapter extends FragmentPagerAdapter
    {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager)
        {
            super(manager);
        }

        @Override
        public Fragment getItem(int position)
        {
            return fragmentList.get(position);
        }

        @Override
        public int getCount()
        {
            return fragmentList.size();
        }

        void addFragment(Fragment fragment, String title)
        {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return fragmentTitleList.get(position);
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

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
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

                    if (networkConf.isNetworkAvailable()) {

                        new JsonAsyncTask(new JsonAsyncResponse()
                        {
                            @Override
                            public void processFinish(ArrayList<String> result)
                            {
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

                            }
                        }).execute(suggestion);
                        return true;
                    }
                }
                return false;
            }
        });

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            DateFormat monthFormat = new SimpleDateFormat("MMMM");
            DateFormat yearFormat = new SimpleDateFormat("yyyy");
            Date date = new Date();

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Teocci");
            alertDialog.setIcon(R.mipmap.ic_launcher);
            alertDialog.setMessage("YTinBG v" + BuildConfig.VERSION_NAME + "\n\nteocci@naver" +
                    ".com\n\n" +
                    monthFormat.format(date) + " " + yearFormat.format(date) + ".\n");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();

            return true;
        } else if (id == R.id.action_clear_list) {
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED)
                    .deleteAll();
            recentlyPlayedFragment.clearRecentlyPlayedList();
            return true;
        } else if (id == R.id.action_search) {
            MenuItemCompat.expandActionView(item);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads app theme color saved in preferences
     */
    private void loadColor()
    {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        int backgroundColor = sp.getInt("BACKGROUND_COLOR", -1);
        int textColor = sp.getInt("TEXT_COLOR", -1);

        if (backgroundColor != -1 && textColor != -1) {
            setColors(backgroundColor, textColor);
        } else {
            initialColors = new int[]{
                    ContextCompat.getColor(this, R.color.color_primary),
                    ContextCompat.getColor(this, R.color.text_color_primary)};
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

    private  boolean checkAndRequestPermissions() {
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
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(
                    this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }
}
