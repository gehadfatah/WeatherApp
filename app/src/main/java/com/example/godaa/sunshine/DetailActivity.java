package com.example.godaa.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.example.godaa.sunshine.databinding.ActivityDetailNewBinding;

/**
 * Created by godaa on 02/03/2017.
 */
//call only in mpanetwo =false
public class  DetailActivity extends ActionBarActivity {
    public static final String DATE_KEY = "date";
    //activity_detail_new
ActivityDetailNewBinding activityDetailBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_new);
       //getSupportActionBar().setIcon(R.drawable.art_clear);
        //two to appear logo
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_logo_detail);
       // getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_logo_detail);
       // getSupportActionBar().setDisplayHomeAsUpEnabled(false);
       // getSupportActionBar().setHomeButtonEnabled(true);
        if (savedInstanceState == null) {
            String date = getIntent().getStringExtra(DATE_KEY);

            getSupportFragmentManager().beginTransaction()
                    //add not replace because its not two pane mode
                    .add(R.id.weather_detail_container, Detailfragment.newInstance(date))
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
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
        return super.onOptionsItemSelected(item);
    }

}
