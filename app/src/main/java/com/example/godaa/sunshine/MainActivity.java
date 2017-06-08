package com.example.godaa.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.godaa.sunshine.data.WeatherContract;
import com.example.godaa.sunshine.sync.SunshineSyncAdapter;


public class MainActivity extends AppCompatActivity implements Forcastfragment.Callback {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    String Detail_tag = "DEFTAG";
    private boolean mTwoPane;
    String mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        //getSupportActionBar().setHomeAsUpIndicator(R.drawable.art_clear);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_logo);
        getSupportActionBar().setTitle("");
       /* Toolbar toolbar = (Toolbar) findViewById(R.id.apptoolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_light_rain);*/
        PreferenceManager.setDefaultValues(this, R.xml.settings_main, false);
        mLocation = Utility.getPreferredLocation(this);
        if (findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode
            mTwoPane = true;

            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new Detailfragment(),Detail_tag)
                        .commit();
            }
        } else {
            mTwoPane = false;
            //for set no different between today and action bar
            getSupportActionBar().setElevation(0f);
        }

        Forcastfragment forecastFragment = ((Forcastfragment) getSupportFragmentManager()
        .findFragmentById(R.id.fragment_forecast));

        forecastFragment.setmUseTodayLayout(!mTwoPane);

        // Make sure we've gotten an account created
       // SunshineSyncAdapter.initializeSyncAdapter(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_setting) {
            Intent settingActivityIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingActivityIntent);
            return true;
        }


        if (id == R.id.action_delet) {
            delettableweatherandlocation();
        }
        return super.onOptionsItemSelected(item);
    }

    private void delettableweatherandlocation() {
        int deletedRows_weather = getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI, null, null);
        int deletedRows_location = getContentResolver().delete(WeatherContract.LocationEntry.CONTENT_URI, null, null);
        Log.i("mainActivity", "in method delettableweatherandlocation from location" + deletedRows_location + " from weather " + deletedRows_weather);
    }


    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(this);
        if (location != null && !location.equals(mLocation)) {
            Forcastfragment forcastfragment = (Forcastfragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if (forcastfragment != null) {
                forcastfragment.onLocationchanged();
            }

            Detailfragment detailfragment = (Detailfragment) getSupportFragmentManager().findFragmentByTag(Detail_tag);
            if (detailfragment != null) {
                detailfragment.onLocationchanged(location);
            }

            mLocation=location

            ;
        }
    }

    @Override
    public void onItemSelected(String date) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction
///////do two thing intialize detailfragment and send date
//            Bundle args=new Bundle();
//            args.putParcelable("Detailfragment.detail_key","uriwithdate");
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, Detailfragment.newInstance(date),Detail_tag)
                    .commit();
            Log.i("in Mainactivity", "in OnItmselcted" + date);

        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailActivity.DATE_KEY, date);
            Log.i("in Mainactivity", "in OnItmselcted" + String.valueOf(date));
            startActivity(intent);
        }
    }
}
