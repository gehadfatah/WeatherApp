package com.example.godaa.sunshine.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.godaa.sunshine.MainActivity;
import com.example.godaa.sunshine.R;
import com.example.godaa.sunshine.Utility;
import com.example.godaa.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Created by godaa on 23/03/2017.
 */
public class SunshineService extends IntentService implements Response.Listener,Response.ErrorListener  {
      RequestQueue mQueue;
    public static final String REQUEST_TAG = "SunshineService";

    public static final String TAG = SunshineService.class.getSimpleName();
    public static final String LOCATION_QUERY_EXTRA = "lqe";

    private static boolean DEBUG = true;
    // For notifications
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // These indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;
    String postalCode;
    public SunshineService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
         postalCode = intent.getStringExtra(LOCATION_QUERY_EXTRA);
        if (postalCode == null || postalCode.isEmpty()) {
            return;
        }
        getWeatherForecastData(postalCode);

       /* try {
            getWeatherForecastData(postalCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }
  private void notifyWeather() {
        Context context = getApplicationContext();
      //if you check notfication check box or not
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
      Boolean displaynotification = preferences.getBoolean(context.getString(R.string.pref_enable_notification_key),
              Boolean.parseBoolean(context.getString(R.string.pref_enable_notification_default)));
      if (displaynotification) {


        // Checking the last update and notify if it's the first
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastSync = preferences.getLong(lastNotificationKey, 0);
//if you un update yourself from day or more
        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
            // Last sync was more than 1 day ago, let's send a notification
            String locationQuery = Utility.getPreferredLocation(context);

            Uri weatherUri = WeatherContract.WeatherEntry
                    .buildWeatherLocationWithDate(
                            locationQuery, WeatherContract.getDbDateString(new Date()));

            // We'll query our contentProvider, as always //for one row
            Cursor cursor =  context.getContentResolver().query(
                    weatherUri,
                    NOTIFY_WEATHER_PROJECTION,
                    null,
                    null,
                    null
            );

            if (cursor.moveToFirst()) {
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double maxTemp = cursor.getDouble(INDEX_MAX_TEMP);
                double minTemp = cursor.getDouble(INDEX_MIN_TEMP);
                String shortDesc = cursor.getString(INDEX_SHORT_DESC);
                int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                String title = context.getString(R.string.app_name);

                // Define the text fo the forecast
                String contentText = String.format(
                        context.getString(R.string.format_notification),
                        shortDesc,
                        Utility.formatTemperature(maxTemp, Utility.isMetric(context)),
                        Utility.formatTemperature(minTemp, Utility.isMetric(context))
                );

                // Build our notification using NotificationCompat.builder
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(iconId)
                                .setContentTitle(title)
                                .setContentText(contentText);


                // Make something interesting happen when the user clicks on the notificaiton.
                // In this case, opening the app is sufficient
                Intent resultIntent = new Intent(context, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                // mID allows you to update the notification later on
                mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());

                // Refreshing last sync
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.commit();
            }

        }
      }
    }


    private void getWeatherForecastData(String postalCode)  {


        //Wil contain the raw JSON response as a string
        // String  forecastJsonStr=null ;

        String format = "json";
        String units = "metric";
        int numDays = 14;
        final String  location_qyery=postalCode;

            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            String APPID = "appid";
            String appid="aa382dc2c439e05c9cd8e5af6726b756";
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, location_qyery)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID,appid)
                    .build();
            String url =builtUri.toString();
            Log.d(TAG, "url is  " + url.toString());
        mQueue = CustomVolleyRequestQueue.getInstance(this.getApplicationContext())
                .getRequestQueue();
        final CustomJSONObjectRequest jsonRequest = new CustomJSONObjectRequest(Request.Method
                .GET, url,this,this);

        jsonRequest.setTag(REQUEST_TAG);

        mQueue.add(jsonRequest);

    }
    /*@Override
    public void onDestroy() {
        super.onDestroy();
      //  Thread theard=new Thread(Thread.){};
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mQueue != null) {
            mQueue.cancelAll(REQUEST_TAG);
        }

        //Log.i(TAG, "in destroy");
    }*/


    private long insertLocationInDatabase(String locationSetting, String cityName, double lat, double lon) {
        long rowId = 0;

        // First check if the location with this city name exists in the db
        Cursor cursor = this.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[] {WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[] {locationSetting},
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {

            int locationIdIndex =  cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            rowId = cursor.getLong(locationIdIndex);

        } else {

            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri uri = this.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, locationValues);
            rowId = ContentUris.parseId(uri);

        }

        if (cursor != null) {
            cursor.close();
        }

        return rowId;
    }

    private void insertWeatherIntoDatabase(Vector<ContentValues> CVVector) {
        Log.i(TAG, "in insertWeatherIntoDatabas");
        if (CVVector.size() > 0) {
            ContentValues[] contentValuesArray = new ContentValues[CVVector.size()];
            CVVector.toArray(contentValuesArray);
            int row_deleted=  getApplicationContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
                    WeatherContract.WeatherEntry.COLUMN_DATE + " < ?",
                    new String[] {WeatherContract.getDbDateString(new Date())});
            Log.i(TAG, "row_deleted " + String.valueOf(row_deleted)+"for day "+WeatherContract.getDbDateString(new Date()));
            int rowsInserted = this.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, contentValuesArray);
            Log.d("Sunserviec", "in line 298 " + String.valueOf(rowsInserted));
            notifyWeather();
            if (mQueue != null) {
                mQueue.cancelAll(REQUEST_TAG);
            }

            // Use a DEBUG variable to gate whether or not you do this, so you can easily
            // turn it on and off, and so that it's easy to see what you can rip out if
            // you ever want to remove it.
         /*   if (DEBUG) {
                Cursor weatherCursor = this.getContentResolver().query(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                );

                if (weatherCursor.moveToFirst()) {
                    ContentValues resultValues = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(weatherCursor, resultValues);
                    Log.v(TAG, "Query succeeded! **********");
                    for (String key : resultValues.keySet()) {
                        Log.v(TAG, key + ": " + resultValues.getAsString(key));
                    }
                } else {
                    Log.v(TAG, "Query failed! :( **********");
                }
            }*/
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        Log.i(TAG, "error in volley response " + volleyError.toString());

    }

    @Override
    public void onResponse(Object o) {
        Log.i(TAG, "in onresponse method");
        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        // Weather information. Each day's forecast info is an element of the "list" array
        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";
        JSONArray weatherArray=null;
        long locationID=0;
       // JSONObject forecastJson =(JSONObject)o;
        String forecastJson =(String) o;

        try {

          JSONObject jfj=new JSONObject(forecastJson);
            weatherArray = jfj.getJSONArray(OWM_LIST);

            JSONObject cityJson = jfj.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);
            JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = coordJSON.getDouble(OWM_COORD_LAT);
            double cityLongitude = coordJSON.getDouble(OWM_COORD_LONG);
            /** Insert the location into the database. */
            locationID = insertLocationInDatabase(
                    postalCode, cityName, cityLatitude, cityLongitude
            );
        } catch (JSONException e) {
            e.printStackTrace();

        }


        // Get and insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

        for(int i = 0; i < weatherArray.length(); i++) {
            //  These are the values that will be collected

            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            double high;
            double low;

            String description;
            int weatherId;
            try {


                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                dateTime = dayForecast.getLong(OWM_DATETIME);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationID);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            } catch (JSONException e) {

            }
        }
        Log.i(TAG, "cvvector os " + cVVector.toString());

        /** Insert weather data into database */
        insertWeatherIntoDatabase(cVVector);
    }


    public static class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent startServiceIntent = new Intent(context, SunshineService.class);
            startServiceIntent.putExtra(LOCATION_QUERY_EXTRA,
                    intent.getStringExtra(SunshineService.LOCATION_QUERY_EXTRA));
            context.startService(startServiceIntent);
        }
    }
}
/* private String getWeatherForecastData(String postalCode) {
        //These two need to be declared outside the try/catch
        //so that they can be closed in the finally block
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        //Wil contain the raw JSON response as a string
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            String APPID = "appid";
            String appid="aa382dc2c439e05c9cd8e5af6726b756";
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, postalCode)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID,appid)
                    .build();

            URL url = new URL(builtUri.toString());
            Log.d(TAG, "url is  " + url.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                //Nothing to do
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                //Since it's JSON, adding a newline isn't necessary (it won't affect
                //parsing) But it does make debugging a lot easier if you print out the
                //completed buffer for debugging
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                //Stream was empty. No point in parsing
                return null;
            }
            forecastJsonStr = buffer.toString();

        } catch (IOException e) {
            Log.e("MainFragment", "Error: ", e);
            //If the code didn't successfully get the weather data, there's no point in attempting to parse it
            forecastJsonStr = null;
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();

            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("MainFragment", "Error closing stream", e);
                }
            }
        }
        Log.i(TAG, "forcast string is " + forecastJsonStr);
        return forecastJsonStr;
    }*/


/**
 * Take the String representing the complete forecast in JSON Format and
 * pull out the data we need to construct the Strings needed for the wireframes.
 *
 * Fortunately parsing is easy:  constructor takes the JSON string and converts it
 * into an Object hierarchy for us.
 */

   /* private void getWeatherDataFromJson(String forecastJsonStr, String locationSetting)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        // Weather information. Each day's forecast info is an element of the "list" array
        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);
        JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = coordJSON.getDouble(OWM_COORD_LAT);
        double cityLongitude = coordJSON.getDouble(OWM_COORD_LONG);

        *//** Insert the location into the database. *//*
        long locationID = insertLocationInDatabase(
                locationSetting, cityName, cityLatitude, cityLongitude
        );

        // Get and insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

        for(int i = 0; i < weatherArray.length(); i++) {
            //  These are the values that will be collected

            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            double high;
            double low;

            String description;
            int weatherId;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            dateTime = dayForecast.getLong(OWM_DATETIME);

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            high = temperatureObject.getDouble(OWM_MAX);
            low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationID);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            cVVector.add(weatherValues);
        }
      //  Log.i(TAG, "cvvector os " + cVVector.toString());

        *//** Insert weather data into database *//*
        insertWeatherIntoDatabase(cVVector);
    }*/


    /**
     * The date/time conversion code
     */
/*
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }*/
