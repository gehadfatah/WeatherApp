package com.example.godaa.sunshine.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.godaa.sunshine.FetchWeatherTask;
import com.example.godaa.sunshine.Forcastfragment;
import com.example.godaa.sunshine.R;
import com.example.godaa.sunshine.Utility;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by godaa on 09/03/2017.
 */
public class sunshine_cursorAdapter extends CursorAdapter {
   public final int VIEW_TYPE_TODAY=0;
   public final int VIEW_TYPE_FUTURE=1;
    private boolean mUseTodayLayout=true;


    public sunshine_cursorAdapter(Context context, Cursor c, int flags ) {
        super(context, c,flags);

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View rootview;
        int layoutId=-1;

        int view_type=getItemViewType(cursor.getPosition());
        if (view_type == VIEW_TYPE_TODAY) {
             layoutId=R.layout.list_item_forcast_today;
        }else if (view_type==VIEW_TYPE_FUTURE){
            layoutId=R.layout.list_item_forcast;

        }
        rootview = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(rootview);
        //should do tag in cursor adapter
        rootview.setTag(viewHolder);
        return rootview;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        //  TextView tvlist_item = (TextView) view.findViewById(R.id.lisview_forecast_textview);
            double min = cursor.getDouble(Forcastfragment.min_index);
            double max = cursor.getDouble(Forcastfragment.max_index);
        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(context);
        String[] highlow = formatHighLows(max, min,context);
            String short_desc = cursor.getString(Forcastfragment.Short_desc_index);
           // long dateInMillis = cursor.getLong(Forcastfragment.Date_index);

        int weather_id_icon = cursor.getInt(Forcastfragment.weather_id_condition_icon);
        int view_type=getItemViewType(cursor.getPosition());
       // ImageView imageView = (ImageView) view.findViewById(R.id.image_view);

        if (view_type == VIEW_TYPE_TODAY) {
            viewHolder. iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weather_id_icon));
        }else if (view_type==VIEW_TYPE_FUTURE){
            viewHolder. iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weather_id_icon));

        }
        viewHolder.iconView.setContentDescription(short_desc);
       // imageView.setImageResource(Utility.getIconResourceForWeatherCondition(weather_id_icon));
      //  TextView tv_day = (TextView) view.findViewById(R.id.day);
       /* viewHolder. dateView.setText(getReadableDateString(dateInMillis));
        Log.i("goda 2 68", "month and date is " + getReadableDateString_2(dateInMillis));*/
        String dateString = cursor.getString(Forcastfragment.Date_index);

        // Find TextView and set formatted date on it
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context,dateString,this.getClass().getSimpleName()));
        // tv_day.setText(Utility.getFriendlyDayString(context,String.valueOf(dateInMillis)));
       // TextView tv_min = (TextView) view.findViewById(R.id.low_temp);
        viewHolder. lowTempView.setText(context.getString(R.string.format_temperature,Double.parseDouble(highlow[1])));
      //  tv_min.setText(Utility.formatTemperature(context,min,isMetric));
      //  TextView tv_description = (TextView) view.findViewById(R.id.description);
        viewHolder. descriptionView.setText(short_desc);
        viewHolder.descriptionView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);

      //  TextView tv_max = (TextView) view.findViewById(R.id.high_temp);
      //  tv_max.setText(Utility.formatTemperature(context,max,isMetric));
        viewHolder. highTempView.setText(context.getString(R.string.format_temperature,Double.parseDouble(highlow[0])));



      /*  String highAndLow = formatHighLows(max,min);
           String resultStrs = getReadableDateString(date) +
                    " - " + short_desc +
                    " - " + highAndLow;
   tvlist_item.setText(resultStrs);
*/
          }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return (position==0&& mUseTodayLayout)?VIEW_TYPE_TODAY:VIEW_TYPE_FUTURE;
    }


    private String[] formatHighLows(double high, double low,Context context) {
        // Data is fetched in Celsius by default.
        // If user prefers to see in Fahrenheit, convert the values here.
        // We do this rather than fetching in Fahrenheit so that the user can
        // change this option without us having to re-fetch the data once
        // we start storing the values in a database.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String units = preferences.getString
                (context.getString(R.string.pref_units_key), context.getString(R.string.pref_units_default));
        boolean isMatric = Utility.isMetric(context);
        double higher=high,lower=low;
        if (!isMatric) {
            higher = (9*high)/5 + 32;
            lower = (9*low)/5 + 32;
        }
        long roundedlow = Math.round(lower);
        long roundedhigh = Math.round(higher);
        //String highlowStr = roundedhigh + "/" + roundedlow;
        String[] highlowStr = new String[]{String.valueOf(roundedhigh), String.valueOf(roundedlow)};
        return  highlowStr;
    }
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }
    private String getReadableDateString_2(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("MMM d");
        return format.format(date).toString();
    }
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.image_view);
            dateView = (TextView) view.findViewById(R.id.day);
            descriptionView = (TextView) view.findViewById(R.id.description);
            highTempView = (TextView) view.findViewById(R.id.high_temp);
            lowTempView = (TextView) view.findViewById(R.id.low_temp);
        }
    }
    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }
}
