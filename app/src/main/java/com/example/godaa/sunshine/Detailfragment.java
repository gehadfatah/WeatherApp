package com.example.godaa.sunshine;

import android.content.SharedPreferences;
import android.net.Network;
import android.net.Uri;
import java.net.*;

import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.CursorLoader;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.example.godaa.sunshine.data.WeatherContract;

import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Created by godaa on 05/03/2017.
 */

public class Detailfragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
            Uri mUri;
    public static final String DATE_KEY = "date";
    public static final String LOCATION_KEY = "location";
    private String mLocation;
    public static final int Detail_Loader=0;
    ShareActionProvider shareActionProvider;
    public final static String LOG_TAG = Detailfragment.class.getSimpleName();
    String mforecaststr;
    private final  static String forecast_share_hashtag = "#SunshineApp";
    String[] forecastProjectionfordetial = {WeatherContract.WeatherEntry.TABLE_NAME+"."+ WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };
    public static final    int weather_id_index=0;
    public static final   int Date_index=1;
    public static final  int Short_desc_index=2;
    public static final   int max_index=3;
    public static final   int min_index=4;
    public static final  int  wind_speed_index=5;
    public static final   int humidity_index =6;
    public static final   int pressure_index =7;
    public static final   int weather_id_icon=8;
    public static final   int degrees_index=9;
    public static final   int location_setting_index=10;


    public static Fragment newInstance(String date) {
        Detailfragment fragment = new Detailfragment();
        Bundle args = new Bundle();

        args.putString(DATE_KEY, date);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLocation != null) {
            outState.putString(LOCATION_KEY, mLocation);
        }
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }
        Bundle args = getArguments();
        if (args != null && args.containsKey(DATE_KEY)) {
            //we can specifiy bundle here in bundle parameter
            getLoaderManager().initLoader(Detail_Loader, null, this);
        }
    }

    public Detailfragment() {

    }
    @Override
    public void onResume() {
        super.onResume();

        Bundle args = getArguments();

        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))
                && args != null && args.containsKey(DATE_KEY)) {
            getLoaderManager().restartLoader(Detail_Loader, null, this);
        }
    }
    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail_fragment,menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        shareActionProvider  =(ShareActionProvider) MenuItemCompat.
                getActionProvider(menuItem);
        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(createshareforcastintent());

        }else {
            Log.d(LOG_TAG, "share action is null");
        }

    }
    private Intent createshareforcastintent() {
        // Intent sharedforcast = new Intent(Intent.ACTION_SEND);
        Intent sharedforcast = ShareCompat.IntentBuilder.from(getActivity()).
                getIntent()
                ;
        //use when we start another app and return to this app this activity
        // remember what we send or what we do in previous
        sharedforcast.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        sharedforcast.setType("text/plain");
        //what we attach in intent
        sharedforcast.putExtra(Intent.EXTRA_TEXT, mforecaststr + forecast_share_hashtag);
        return sharedforcast;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mLocation = Utility.getPreferredLocation(getActivity());
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(mLocation,
                    arguments.getString(DATE_KEY));        }
        return rootView;

    }



    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
      //  Intent i=getActivity().getIntent(); i.getData()
         String forecastDate = getArguments().getString(DATE_KEY);
        Log.i("in detailfragment", "forecastDate is " + forecastDate);
        // Sort order: Ascending by date
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
/*    Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                mLocation, forecastDate);*/
        if (null != mUri) {
            return new CursorLoader(getActivity(),mUri
                    , forecastProjectionfordetial,
                    null,
                    null,
                    sortOrder
            );
        }
       return null;
    }


    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        // String str=data.getString(Forcastfragment.)
           /* if (getActivity().getIntent().hasExtra(Intent.EXTRA_TEXT)) {
                mforecaststr = getActivity().getIntent().getStringExtra(Intent.EXTRA_TEXT);
                TextView tv = (TextView) rootview.findViewById(R.id.detail);
                tv.setText(mforecaststr);
            }*/
        if (!data.moveToFirst() ){
            return;
        }
        boolean isMetric = Utility.isMetric(getContext());
        //just low and high with getdouble
        double low = data.getDouble(min_index);
        double high = data.getDouble(max_index);
        String highlow=formatHighLows(high,low);
        String date_time = data.getString(Date_index);
        String short_desc = data.getString(Forcastfragment.Short_desc_index);
        float pressure = data.getFloat(pressure_index);
        float wind_speed = data.getFloat(wind_speed_index);
        float humidity = data.getFloat(humidity_index);
        int weather_icon = data.getInt(weather_id_icon);
        float degrees = data.getFloat(degrees_index);
        TextView tv_day = (TextView) getView().findViewById(R.id.day);
        //tv_day.setText(getReadableDateString(date_time));
       // Log.i(LOG_TAG, "get context is 1" + getContext().getClass().getSimpleName());
       // Log.i(LOG_TAG, "get context is " +this.getClass().getSimpleName());
        tv_day.setText(Utility.getFriendlyDayString(getActivity(),date_time,this.getClass().getSimpleName()));
        //Log.i("goda veh", "date is " + Utility.getDayName(context, String.valueOf(dateInMillis)));
        TextView tv_monthDay = (TextView) getView().findViewById(R.id.month_day);
       // tv_monthDay.setText(getReadableDateMonthDay(date_time));
        tv_monthDay.setText(Utility.getFormattedMonthDay(getContext(),date_time));
        TextView tv_max = (TextView) getView().findViewById(R.id.high_temp);
        tv_max.setText(Utility.formatTemperature(getContext(),high,isMetric));
        TextView tv_min = (TextView) getView().findViewById(R.id.low_temp);
        tv_min.setText(Utility.formatTemperature(getContext(),low,isMetric));
        TextView tv_description = (TextView) getView().findViewById(R.id.description);
       tv_description.setText(short_desc);
        TextView tv_humidty = (TextView) getView().findViewById(R.id.humidity);
        tv_humidty.setText(getContext().getString(R.string.format_humidity,String.valueOf(Math.round(humidity))));
        TextView tv_pressure = (TextView) getView().findViewById(R.id.pressure);
        tv_pressure.setText(getContext().getString(R.string.format_pressure,String.valueOf(Math.round(pressure))));
        TextView tv_wind_speed = (TextView) getView().findViewById(R.id.wind_speed);
        tv_wind_speed.setText(Utility.getFormattedWind (getContext(),wind_speed,degrees));
        ImageView imageView = (ImageView) getView().findViewById(R.id.image_view);
        imageView.setImageResource(Utility.getArtResourceForWeatherCondition(weather_icon));
        imageView.setContentDescription(short_desc);
        mforecaststr = String.format("%s -%s -%s -%s ",
                Utility.formatDate(date_time) ,
                short_desc,String.valueOf(low) , String.valueOf(high));

        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(createshareforcastintent());
        }
       /* mforecaststr=getReadableDateString(date_time)+
                " - " +short_desc+
                " - "+highlow;
        TextView tv = (TextView) getView().findViewById(R.id.detail);
        tv.setText(mforecaststr);
        */


    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        getLoaderManager().restartLoader(Detail_Loader, null, this);

    }

    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("EEE");
        return format.format(date).toString();
    }
    private String getReadableDateMonthDay(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("MMM d");
        return format.format(date).toString();
    }
    private String formatHighLows(double high, double low) {
        // Data is fetched in Celsius by default.
        // If user prefers to see in Fahrenheit, convert the values here.
        // We do this rather than fetching in Fahrenheit so that the user can
        // change this option without us having to re-fetch the data once
        // we start storing the values in a database.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String units = preferences.getString
                (this.getString(R.string.pref_units_key), this.getString(R.string.pref_units_default));
        if (units.equals(this.getString(R.string.pref_units_key))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        }
        long roundedlow = Math.round(low);
        long roundedhigh = Math.round(high);
        String highlowStr = roundedhigh + "/" + roundedlow;
        return  highlowStr;
    }


    public void onLocationchanged(String location) {

        Uri uri=mUri;
        if (null != uri) {
            String date = WeatherContract.WeatherEntry.getDateFromUri(uri);
           Uri ubdated_uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(location, date);
            mUri=ubdated_uri;
            getLoaderManager().restartLoader(Detail_Loader, null, this);
        }

    }
}
