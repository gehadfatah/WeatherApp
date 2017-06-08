package com.example.godaa.sunshine.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.godaa.sunshine.Forcastfragment;
import com.example.godaa.sunshine.R;
import com.example.godaa.sunshine.Utility;

import java.io.File;

/**
 * Created by godaa on 11/04/2017.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder> {
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    // Flag to determine if we want to use a separate view for "today".
    private boolean mUseTodayLayout = true;

    private Cursor mCursor;
     private Context mContext;
     private ForecastAdapterOnClickHandler mClickHandler;
     private View mEmptyView;

 /*   public ForecastAdapter(Context context, ForecastAdapterOnClickHandler dh, View emptyView, int choiceMode) {
        mContext = context;
        mClickHandler = dh;
        mEmptyView = emptyView;
        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);    }*/
    public ForecastAdapter(Context context,ForecastAdapterOnClickHandler dh,View emptyView) {
        mContext = context;
        mClickHandler = dh;
        mEmptyView = emptyView;
       /* mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode); */   }
    @Override
    public ForecastAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if ( parent instanceof RecyclerView ) {
            int layoutId = -1;
            Log.i("ForecastAdapterwr", "inoncreateviewholder" + String.valueOf(viewType));

            if (viewType == VIEW_TYPE_TODAY) {
                layoutId = R.layout.list_item_forcast_today;

            }else {
                layoutId = R.layout.list_item_forcast;
            }

            View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            view.setFocusable(true);
            return new ForecastAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }    }

    @Override
    public void onBindViewHolder(ForecastAdapterViewHolder holder, int position) {

        mCursor.moveToPosition(position);
        //to insure that mcursor position and viewholder position is identical
       // Log.i("on Bindviewholder", "position is " + String.valueOf(position)+" mCursorPosition "+String.valueOf(mCursor.getPosition()));
        int weatherId = mCursor.getInt(Forcastfragment.weather_id_condition_icon);
        int view_type=getItemViewType(position);
        if (view_type == VIEW_TYPE_TODAY) {
            //using Glide library to loading image for today photo
            Glide.with(mContext)
                    .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                    .crossFade()
                    .into(holder.mIconView);
          //  holder. mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
        }else if (view_type==VIEW_TYPE_FUTURE_DAY){

            holder. mIconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
        }
        // this enables better animations. even if we lose state due to a device rotation,
        // the animator can use this to re-find the original view
        ViewCompat.setTransitionName(holder.mIconView, "iconView" + position);
        String dateString = mCursor.getString(Forcastfragment.Date_index);

        // Find TextView and set formatted date on it
        holder.mDateView.setText(Utility.getFriendlyDayString(mContext,dateString,this.getClass().getSimpleName()));

        String short_desc = mCursor.getString(Forcastfragment.Short_desc_index);
   holder. mDescriptionView.setText(short_desc);
        holder.mDescriptionView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);

      double min = mCursor.getDouble(Forcastfragment.min_index);
        double max = mCursor.getDouble(Forcastfragment.max_index);
        String[] highlow = formatHighLows(max, min,mContext);
        holder. mHighTempView.setText(mContext.getString(R.string.format_temperature,Double.parseDouble(highlow[0])));
        holder. mLowTempView.setText(mContext.getString(R.string.format_temperature,Double.parseDouble(highlow[1])));
        // mICM.onBindViewHolder(holder, position);
    }


    private String[] formatHighLows(double high, double low,Context context) {
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
        String[] highlowStr = new String[]{String.valueOf(roundedhigh), String.valueOf(roundedlow)};
        return  highlowStr;
    }

    public class ForecastAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView mIconView;
        public final TextView mDateView;
        public final TextView mDescriptionView;
        public final TextView mHighTempView;
        public final TextView mLowTempView;
        public ForecastAdapterViewHolder(View itemView) {
            super(itemView);
            mIconView = (ImageView) itemView.findViewById(R.id.image_view);
            mDateView = (TextView) itemView.findViewById(R.id.day);
            mDescriptionView = (TextView) itemView.findViewById(R.id.description);
            mHighTempView = (TextView) itemView.findViewById(R.id.high_temp);
            mLowTempView = (TextView) itemView.findViewById(R.id.low_temp);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
        int position=getAdapterPosition();
            mCursor.moveToPosition(position);
            int dateColumnIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            mClickHandler.onClick(mCursor.getLong(dateColumnIndex), this);
          //  mICM.onClick(this);
        }
    }
    public  static interface ForecastAdapterOnClickHandler{
        void onClick(long data,ForecastAdapterViewHolder viewHolder);
    }
    public void onRestoreInstanceState(Bundle savedInstanceState) {
       // mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
      //  mICM.onSaveInstanceState(outState);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

//    public int getSelectedItemPosition() {
//      //  return mICM.getSelectedItemPosition();
//    }
    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getItemCount() {
        if ( null == mCursor ) return 0;
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
       mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if ( viewHolder instanceof ForecastAdapterViewHolder ) {
            ForecastAdapterViewHolder vfh = (ForecastAdapterViewHolder)viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }
}
