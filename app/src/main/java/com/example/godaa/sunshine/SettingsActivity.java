package com.example.godaa.sunshine;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.godaa.sunshine.data.WeatherContract;
import com.example.godaa.sunshine.sync.SunshineSyncAdapter;

public class SettingsActivity extends ActionBarActivity


{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        Log.i("SettingsActivity", "in onCreate method");
    }



    public static class SettingPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_main);

         Preference location = findPreference(getString(R.string.pref_location_key));
         Preference units = findPreference(getString(R.string.pref_units_key));
            bindPreferenceSummaryToValue(location);
            bindPreferenceSummaryToValue(units);

        }
         //for set summary of prefernce
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
          /*   boolean mBindingPreferences = false;
           if (!mBindingPreferences) {
                if (preference.getKey().equals(R.string.pref_location_key)) {
                    SunshineSyncAdapter.syncImmediately(getActivity());                }
                else {
                    getActivity().getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI,null);
                }
            }*/
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(stringValue);
                if (prefIndex >= 0) {
                    CharSequence[] labels = listPreference.getEntries();
                    //summary of list
                    preference.setSummary(labels[prefIndex]);
                }
            } else {
                //summary of location
                preference.setSummary(stringValue);
            }
            return true;
        }

        private void bindPreferenceSummaryToValue(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            String preferenceString = preferences.getString(preference.getKey(), "");
            onPreferenceChange(preference, preferenceString);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}