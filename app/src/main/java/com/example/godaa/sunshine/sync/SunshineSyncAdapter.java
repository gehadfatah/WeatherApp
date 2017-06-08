package com.example.godaa.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.godaa.sunshine.MainActivity;
import com.example.godaa.sunshine.R;
import com.example.godaa.sunshine.Utility;
import com.example.godaa.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Created by godaa on 24/03/2017.
 */
public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    public final static String TAG=SunshineSyncAdapter.class.getSimpleName();
    private static boolean DEBUG = true;
    // Interval at which to sync with the weather, in seconds
    // 60 seconds (1min) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    public static final String ACCOUNT_TYPE = "sunshine.example.com";
    // The account name
    public static final String ACCOUNT = "sync";
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

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync");
        String postalCode = Utility.getPreferredLocation(getContext());
        if (postalCode == null || postalCode.isEmpty()) {
            return;
        }
        String weatherForecast = getWeatherForecastData(postalCode);

        try {
            getWeatherDataFromJson(weatherForecast, postalCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * Helper method to have the sync adapter sync immediately
     * @param context An app context
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        Account account = getSyncAccount(context);
        ContentResolver.requestSync(account,
                context.getString(R.string.content_authority), bundle);
    }
    /**
     * Helper method to get the fake accounts to be used with SyncAdapter
     */
    public static Account getSyncAccount(Context context) {

        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        /*Account newAccount = new Account(ACCOUNT,
                ACCOUNT_TYPE);*/
        Account newAccount = SunshineAuthenticatorService.GetAccount();
        Log.i(TAG, "new account is " + newAccount.toString());
        // If password doesn't exist, the account doesn't exist
        if (null==accountManager.getPassword(newAccount)  ) {
            // If not successful
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            // If you don't set android:syncable="true" in your <provider> element in the manifest
            // then call context.setIsSyncable(account, AUTHORITY, 1) here
            onAccountCreated(newAccount, context);
        }

        return newAccount;
    }
    private static void onAccountCreated(Account newAccount, Context context) {
        // Since we've created an account
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Without calling setSyncAutomatically, our periodic sync will not be enabled
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        // Finally, do a sync to get things started
        syncImmediately(context);
    }
    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {

        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority)
                    .build();
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }
    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    /*private void notifyWeather() {
        Context context = getContext();

        // Checking the last update and notify if it's the first
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);

        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
            // Last sync was more than 1 day ago, let's send a notification
            String locationQuery = Utility.getPreferredLocation(context);

            Uri weatherUri = WeatherContract.WeatherEntry
                    .buildWeatherLocationWithDate(
                            locationQuery, WeatherContract.getDbDateString(new Date()));

            // We'll query our contentProvider, as always
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
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.commit();
            }
        }
    }*/
    /**
     * The date/time conversion code
     */
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }
    private String getWeatherForecastData(String postalCode) {
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
    }
    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getWeatherDataFromJson(String forecastJsonStr, String locationSetting)
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

        /** Insert the location into the database. */
        long locationID = addLocation(
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
        Log.i(TAG, "cvvector os " + cVVector.toString());

        /** Insert weather data into database */
        insertWeatherIntoDatabase(cVVector);
    }
    public long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        //Uri uri = WeatherContract.WeatherEntry.buildWeatherLocation(locationSetting);
        //Uri uri_location= WeatherContract.LocationEntry.CONTENT_URI;
        Cursor location_cursor=getContext().getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING+"=?",
                new String[]{locationSetting},
                null);
        long id;
        //  String[] projection = {WeatherContract.LocationEntry._ID};
        //  Cursor cursor=mContext.getContentResolver().query(uri_location,projection,null,null,null);
        if (location_cursor.moveToFirst()) {
            Log.d("FetchWeather", "this location is already exist");
            id = location_cursor.getLong(location_cursor.getColumnIndex
                    (WeatherContract.LocationEntry._ID));
            return id;
        }else {
            ContentValues values = new ContentValues();
            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG,lon);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,lat);
            values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,locationSetting);
            Uri uri1= WeatherContract.LocationEntry.CONTENT_URI;
            Uri return_uri = getContext().getContentResolver().insert(uri1, values);
            id = ContentUris.parseId(return_uri);
        }

        Log.e(TAG, "134 " + String.valueOf(id));
        return id;
    }

    private void insertWeatherIntoDatabase(Vector<ContentValues> CVVector) {
        if (CVVector.size() > 0) {
            ContentValues[] contentValuesArray = new ContentValues[CVVector.size()];
            CVVector.toArray(contentValuesArray);

            int rowsInserted = getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, contentValuesArray);
            Log.d("Sunserviec", "in line 298 " + String.valueOf(rowsInserted));

            // Use a DEBUG variable to gate whether or not you do this, so you can easily
            // turn it on and off, and so that it's easy to see what you can rip out if
            // you ever want to remove it.
            if (DEBUG) {
                Cursor weatherCursor = getContext().getContentResolver().query(
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
            }
        }
    }
}
