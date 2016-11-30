/*
 * Copyright 2016 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.audiobookplayer;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceClickListener;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(MainActivity.PREF_NAME);
            addPreferencesFromResource(R.xml.preferences);
            
            final ListPreference listPreference = null;
            //listPreference = (ListPreference) findPreference(getResources().getString(R.string.const_pref_encoding));
            if (listPreference != null) {
                setListPreferenceData(listPreference);
    
                listPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
    
                        setListPreferenceData(listPreference);
                        return false;
                    }
                });
            }            
        }
    }
    
    protected static void setListPreferenceData(ListPreference lp) {
        ArrayList<String> languages = new ArrayList<>(Charset.availableCharsets().keySet());
        Collections.sort(languages);
        
        String[] entries = new String[languages.size()];
        String[] entryValues = new String[languages.size()];
        entries = languages.toArray(entries);
        entryValues = languages.toArray(entryValues);
        
        lp.setEntries(entries);
        lp.setDefaultValue("UTF-8");
        lp.setEntryValues(entryValues);
    }    
}
