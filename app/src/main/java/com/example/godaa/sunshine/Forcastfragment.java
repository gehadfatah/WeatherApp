
package com.example.godaa.sunshine;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.*;
import android.support.v4.content.CursorLoader;

import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.content.Intent;
import android.support.v4.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.TextView;
import android.widget.Toast;

import com.example.godaa.sunshine.data.ForecastAdapter;
import com.example.godaa.sunshine.data.WeatherContract;
import com.example.godaa.sunshine.data.WeatherContract.WeatherEntry;
import com.example.godaa.sunshine.data.WeatherContract.LocationEntry;
import com.example.godaa.sunshine.data.sunshine_cursorAdapter;
import com.example.godaa.sunshine.service.SunshineService;
import com.example.godaa.sunshine.sync.SunshineSyncAdapter;

import java.util.ArrayList;
import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 */
public class Forcastfragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = Forcastfragment.class.getSimpleName();
    RecyclerView mRecycleview;
  private  Context mcontext;
   private ArrayAdapter<String> mAdapter;
    private int Position = RecyclerView.NO_POSITION;
    ForecastAdapter mForecastAdapter;
    private sunshine_cursorAdapter sunshineAdapter;
    private static final int FORECAST_LOADER = 0;
    String[] forecastProjection = {WeatherEntry.TABLE_NAME+"."+WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract. LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.  LocationEntry.COLUMN_COORD_LAT,
            WeatherContract. LocationEntry.COLUMN_COORD_LONG,
            WeatherEntry.COLUMN_WEATHER_ID
    };
    public static final    int weather_id_index=0;
    public static final   int Date_index=1;
    public static final  int Short_desc_index=2;
    public  static final  int loction_setting_index=3;
    public static final   int max_index=4;
    public static final   int min_index=5;
    public  static final  int latitude_index=6;
    public  static final   int longitude_index=7;
    public  static final   int weather_id_condition_icon=8;
    private boolean mUseTodayLayout;
    private Callback mListener;
    private static final String LOCATION_KEY = "location";
    private String mLocation;

    private static final String POSITION_KEY = "position";
    private int mPosition;
    private ListView mListView;

    public Forcastfragment() {
    }

    public void onLocationchanged() {
        update_weather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);

    }
    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    //for frahment to talk to mainactivityu
    public interface Callback {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String date);
    }
   @Override
   public void onAttach(Activity activity) {
       mListener = (Callback) activity;
       super.onAttach(activity);
   }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }
   @Override
   public void onResume() {
       super.onResume();
       if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
           getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
       }
   }

    @Override
    public void onStart() {
        super.onStart();
       update_weather();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(LOCATION_KEY, mLocation);

       /* if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(POSITION_KEY, mPosition);
        }*/
        if (mPosition != RecyclerView.NO_POSITION) {
            outState.putInt(POSITION_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }
    @Override
    public void onActivityCreated( Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }

        getLoaderManager().initLoader(FORECAST_LOADER, null,this);
        super.onActivityCreated(savedInstanceState);
    }
    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forcast_fragment,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*case R.id.settings:
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
                break;*/
            case R.id.actionrefresdh:
                update_weather();
                break;
            case R.id.map:
                openPreferredLocationInMap();
                break;
            default:

        }
        return super.onOptionsItemSelected(item);
    }
    private void openPreferredLocationInMap() {
      /*  if (sunshineAdapter != null) {
            Cursor  cursor = sunshineAdapter.getCursor();*/
          if (mForecastAdapter != null) {
            Cursor  cursor = mForecastAdapter.getCursor();
            if (null != cursor) {
                cursor.moveToPosition(0);
                String lat = cursor.getString(latitude_index);
                String longtitude = cursor.getString(longitude_index);
                //Using the URI scheme for showing a location
                Uri geoLocation = Uri.parse("geo:" + lat + "," + longtitude);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(TAG, "Couldn't call " + mLocation + ", no app to view");
                }
            }

        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState != null && savedInstanceState.containsKey(LOCATION_KEY)) {
            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }
        mLocation = Utility.getPreferredLocation(getContext());

          if (savedInstanceState != null && savedInstanceState.containsKey(POSITION_KEY)) {
            mPosition = savedInstanceState.getInt(POSITION_KEY);
        }
        // pulate it in list view,what appear in each item in list
        View rootView = inflater.inflate(R.layout.fragment_blal, container, false);
        View emptyView = rootView.findViewById(R.id.recyclerview_forecast_empty);
        mRecycleview = (RecyclerView)rootView. findViewById(R.id.rv_numbers);
     //   mForecastAdapter=new ForecastAdapter(getContext());
mForecastAdapter=new ForecastAdapter(getContext(), new ForecastAdapter.ForecastAdapterOnClickHandler() {
    @Override
    public void onClick(long data, ForecastAdapter.ForecastAdapterViewHolder viewHolder) {
        mListener .onItemSelected(String.valueOf(data));
        mPosition = viewHolder.getAdapterPosition();
    }
}, emptyView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mRecycleview.setLayoutManager(linearLayoutManager);
        mRecycleview.setHasFixedSize(true);
        mRecycleview.setAdapter(mForecastAdapter);
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        return rootView;
        //       mListView = (ListView)rootView.findViewById(R.id.list_view_forecast);
        //      sunshineAdapter = new sunshine_cursorAdapter(getActivity(), null,0);
        //      sunshineAdapter.setUseTodayLayout(mUseTodayLayout);
        //       mListView.setAdapter(sunshineAdapter);
/*        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // String s= mAdapter.getItem(position);
                //  String s= (String) parent.getItemAtPosition(position);
                //Toast.makeText(getContext(),s,Toast.LENGTH_LONG).show();
                // I.putExtra(Intent.EXTRA_TEXT, s);
               *//* Intent I = new Intent(getActivity(),DetailActivity.class);
                Uri uri_withid = ContentUris.withAppendedId(WeatherEntry.CONTENT_URI, id);
                I.setData(uri_withid);
                startActivity(I);*//*
                sunshine_cursorAdapter da = (sunshine_cursorAdapter) parent.getAdapter();
                Cursor cursor = da.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    //call onItemSelected in mainActivity
                    mListener.onItemSelected(cursor.getString(Date_index));
                }
                mPosition = position;
            }
        });*/
    }


    public void update_weather() {
//for using service
        String location = Utility.getPreferredLocation(getContext());
        Intent alarmIntent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
        alarmIntent.putExtra(SunshineService.LOCATION_QUERY_EXTRA, location);
        //pending intent is task
        //see here we use getBroadcast to call start broadcastRecevier
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, alarmIntent,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),pendingIntent);
       /* Intent serviceIntent = new Intent(getActivity(), SunshineService.class);
        serviceIntent.putExtra(SunshineService.LOCATION_QUERY_EXTRA,location);
        getActivity().startService(serviceIntent);*/
//using asynk task
        //for just set database
       // new FetchWeatherTask(getActivity()).execute(location);
//using async adapter
     // SunshineSyncAdapter.syncImmediately(getActivity());
    }
    public void setmUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        /*if (sunshineAdapter != null) {
             sunshineAdapter.setUseTodayLayout(useTodayLayout); }*/
         if (mForecastAdapter != null) {
             mForecastAdapter.setUseTodayLayout(useTodayLayout);
         }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i("in oncreatrloader", "jjjj");
        mLocation = Utility.getPreferredLocation(getContext());
        String startDate = WeatherContract.getDbDateString(new Date());
        //should take space before ASC
        String sortorder = WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherUri = WeatherEntry.buildWeatherLocationWithStartDate(mLocation, String.valueOf(startDate));
        return new CursorLoader(getContext(), weatherUri,
                forecastProjection,
                null,
                null,
                sortorder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.i("in Forcastfrgment ", "on LoadFinished" +String.valueOf(data.moveToFirst()) );
//        sunshineAdapter.swapCursor(data);
        mForecastAdapter.swapCursor(data);
        if (mPosition != RecyclerView.NO_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mRecycleview.smoothScrollToPosition(mPosition);
        }
        /*if (!mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        } else if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to
            // restore to, do so now
             //  mListView.setSelection(mPosition);

        }*/
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

//           sunshineAdapter.swapCursor(null);
mForecastAdapter.swapCursor(null);
    }


}