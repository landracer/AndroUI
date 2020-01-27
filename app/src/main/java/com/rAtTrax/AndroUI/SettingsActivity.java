package com.rAtTrax.AndroUI;


import java.util.prefs.Preferences;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;

public class SettingsActivity extends PreferenceActivity {

    View root;
    PreferenceScreen CGPreferenceScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preference RpmPreference;
        Preference TraceBlueToothPreference;

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        CGPreferenceScreen = getPreferenceScreen();

        // Setup the click listener for the "Buy Hardware Controller" preference
        getPreferenceManager()
        .findPreference("go_to_site")
        .setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://www.osrss.com/"));
                        startActivity(intent);
                        return true;
                    }
                });

        // Get handles to various preferences
        TraceBlueToothPreference = getPreferenceManager().
                findPreference("blue_tooth_trace_selection");

        // Remove the RPM preference if the RPM flag is disabled
        if ( !PSensor.ENABLE_RPM ){
            RpmPreference = getPreferenceManager().
                    findPreference("rpm_preferencescreen");
            CGPreferenceScreen.removePreference(RpmPreference);
        }


        // Blue tooth trace preference. Setup the listener if ENABLE_TRACE_BLUE_TOOTH, else remove
        // it from the preference screen.
        if ( PSensor.ENABLE_TRACE_BLUE_TOOTH ){
            TraceBlueToothPreference
                    .setOnPreferenceClickListener(
                            new OnPreferenceClickListener() {
                                public boolean onPreferenceClick(Preference preference) {
                                    startActivity(new Intent(getApplicationContext(), BlueToothTrace.class));
                                    return true;
                                }
                            });
        }
        else {
            CGPreferenceScreen.removePreference(TraceBlueToothPreference);
        }
    }
}
