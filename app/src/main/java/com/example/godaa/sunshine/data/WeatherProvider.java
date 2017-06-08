
package com.example.godaa.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.example.godaa.sunshine.data.WeatherContract.WeatherEntry;
import static com.example.godaa.sunshine.data.WeatherContract.LocationEntry;
import static com.example.godaa.sunshine.data.WeatherContract.CONTENT_AUTHORITY;
import static com.example.godaa.sunshine.data.WeatherContract.PATH_LOCATION;
import static com.example.godaa.sunshine.data.WeatherContract.PATH_WEATHER;
/**
 * Created by godaa on 15/03/2017.
 */
public class WeatherProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDpHelper mOpenHelper;
    public   static final int WEATHER = 100;
    public static final int WEATHER_WITH_LOCATION = 101;
    public   static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    public static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    public static final int WEATHER_WITH_ID = 104;
//for foriegn key and its just for query
    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static{
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry._ID);
    }

    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME+
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
    //return from this day and so on
    //more than row
    private static final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME+
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";
//just one row
    private static final String sLocationSettingAndDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";
 /*
        Students: Here is where you need to create the UriMatcher. This UriMatcher will
        match each URI to the WEATHER, WEATHER_WITH_LOCATION, WEATHER_WITH_LOCATION_AND_DATE,
        and LOCATION integer constants defined above.  You can test this by uncommenting the
        testUriMatcher test within TestUriMatcher.
     */
    public static UriMatcher buildUriMatcher() {
        // 1) The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case. Add the constructor below.
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        // 2) Use the addURI function to match each of the types.  Use the constants from
        // WeatherContract to help define the types to the UriMatcher.
        matcher.addURI(CONTENT_AUTHORITY, PATH_WEATHER, WEATHER);
        //return multi row
        matcher.addURI(CONTENT_AUTHORITY, PATH_WEATHER + "/*" , WEATHER_WITH_LOCATION);
        //return one row
       // matcher.addURI(CONTENT_AUTHORITY, PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);
        matcher.addURI(CONTENT_AUTHORITY, PATH_LOCATION, LOCATION);
        matcher.addURI(CONTENT_AUTHORITY, WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);
        matcher.addURI(CONTENT_AUTHORITY, PATH_WEATHER + "/#" ,WEATHER_WITH_ID);

       matcher.addURI(CONTENT_AUTHORITY, WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);
        Log.i("in WeatherProvier", "matcher is" + matcher.toString());
        // 3) Return the new matcher!
        return matcher;
    }

    /*
        Students: We've coded this for you.  We just create a new WeatherDbHelper for later use
        here.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDpHelper(getContext());
        return true;
    }

    /*
        Students: Here's where you'll code the getType function that uses the UriMatcher.  You can
        test this by uncommenting testGetType in TestProvider.

     */
    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
           case WEATHER_WITH_LOCATION:
               return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor recursor;
        Log.i("in WeatherProvier", "in query  " + uri.toString()+"     ");

        switch (sUriMatcher.match(uri)) {
            case WEATHER_WITH_ID:
                long id = ContentUris.parseId(uri);
                selection = WeatherEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(id)};
                recursor = mOpenHelper.getReadableDatabase().query(WeatherEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                Log.i("in WeatherProvier", "in query in WEATHER_WITH_ID " + uri.toString()+"     ");

                break;
            // location/*
            case LOCATION_ID:
                recursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        WeatherContract.LocationEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                Log.i("in WeatherProvier", "in query in LOCATION_ID " + uri.toString());

                break;
            case WEATHER_WITH_LOCATION_AND_DATE:
                recursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                Log.i("in WeatherProvier", "in query in WEATHER_WITH_LOCATION_AND_DATE " + uri.toString());

                break;
            case WEATHER_WITH_LOCATION:
                recursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                Log.i("in WeatherProvier", "in query in WEATHER_WITH_LOCATION " + uri.toString());

                break;
            case WEATHER:
                recursor=mOpenHelper.getReadableDatabase().query(WeatherEntry.TABLE_NAME,
                        projection,selection,selectionArgs,null,null,sortOrder);
                Log.i("in WeatherProvier", "in query in WEATHER " + uri.toString());

                break;
            case LOCATION :
                recursor =mOpenHelper.getReadableDatabase().query(LocationEntry.TABLE_NAME,
                        projection,selection,selectionArgs,null,null,sortOrder);
                Log.i("in WeatherProvier", "in query in LOCATION " + uri.toString());

                break;

            default:
                throw new UnsupportedOperationException("unknown uri" + uri);
        }
        recursor.setNotificationUri(getContext().getContentResolver(), uri);
        return recursor;
    }
    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);
        Log.i("in WeatherProvier", "in getWeatherByLocationSetting function" + startDate+" "+locationSetting);

        String[] selectionArgs;
        String selection;
        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, startDate};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getWeatherByLocationSettingAndDate(
            Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String date = WeatherContract.WeatherEntry.getDateFromUri(uri);
//mOpenHelper.getWritableDatabase().query()
        Log.i("in WeatherProvier", "in getWeatherByLocationSettingAndDate function" + date+" "+locationSetting);

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingAndDaySelection,
                new String[]{locationSetting, date},
                null,
                null,
                sortOrder
        );
    }
    //    Student: Add the ability to insert Locations to the implementation of this function.

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case WEATHER:
                normalizeDate(values);
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = WeatherEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;

             case LOCATION:
              long loc_id = mOpenHelper.getWritableDatabase().insert(LocationEntry.TABLE_NAME,
                null, values);
                 if (loc_id > 0)
                     returnUri = WeatherEntry.buildWeatherUri(loc_id);
                 else
                     throw new android.database.SQLException("Failed to insert row into " + uri);
                 break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        mOpenHelper.getWritableDatabase().close();
        return returnUri;
    }
    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Student: Start by getting a writable database
        int rowsDeleted ;

        // Student: Use the uriMatcher to match the WEATHER and LOCATION URI's we are going to
        // handle.  If it doesn't match these, throw an UnsupportedOperationException.
        if (null == selection) {
            selection = "1";
        }
        // Student: A null value deletes all rows.  In my implementation of this, I only notified
        // the uri listeners (using the content resolver) if the rowsDeleted != 0 or the selection
        // is null.
        // Oh, and you should notify the listeners here.
        SQLiteDatabase dp=mOpenHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case WEATHER:
                rowsDeleted = dp.delete(WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = dp.delete(LocationEntry.TABLE_NAME, selection, selectionArgs);

                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri" + uri);
        }


        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri,null);
        }
        dp.close();
        return rowsDeleted;
    }



    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
       int rowsDeleted;
        SQLiteDatabase dp=mOpenHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case WEATHER:
                normalizeDate(values);
                rowsDeleted = dp.update(WeatherEntry.TABLE_NAME, values,
                        selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = dp.update(LocationEntry.TABLE_NAME,values,
                        selection, selectionArgs);

                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri" + uri);
        }


        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri,null);
        }
        dp.close();
        return rowsDeleted;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Log.i("weatherprovider:", "in  338 bulkinsert"+uri.toString());
        int returnCount = 0;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                // Log.i("weatherprovider:", "in  bulkinsert"+uri.toString());
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                       // normalizeDate(value);
                       // Log.d("weatherprovider:", value.toString());
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                        Log.d("weatherprovider:", "_id "+String.valueOf(_id));

                    }
                    db.setTransactionSuccessful();
                } finally {
                   db.endTransaction();
                }
                break;
            default:
                return super.bulkInsert(uri, values);
        }
        if (returnCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return returnCount;
    }


}
