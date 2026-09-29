/*
 * Copyright (C) 2020 abcduwhatever
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

package com.android.settings.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.voltage.support.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class GestureTweaksSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private ListPreference mLeftSwipeActions;
    private ListPreference mRightSwipeActions;
    private Preference mLeftSwipeAppSelection;
    private Preference mRightSwipeAppSelection;
    private ListPreference mLeftVerticalSwipeActions;
    private ListPreference mRightVerticalSwipeActions;
    private Preference mLeftVerticalSwipeAppSelection;
    private Preference mRightVerticalSwipeAppSelection;
    private Preference mLeftPieConfig;
    private Preference mRightPieConfig;
    private Preference mLeftVerticalPieConfig;
    private Preference mRightVerticalPieConfig;
    private SystemSettingSwitchPreference mExtendedSwipe;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gesture_nav_tweaks);

        final ContentResolver resolver = getActivity().getContentResolver();

        mExtendedSwipe = (SystemSettingSwitchPreference) findPreference("back_swipe_extended");
        boolean extendedSwipe = Settings.System.getIntForUser(resolver,
            Settings.System.BACK_SWIPE_EXTENDED, 0,
            UserHandle.USER_CURRENT) != 0;
        mExtendedSwipe.setChecked(extendedSwipe);
        mExtendedSwipe.setOnPreferenceChangeListener(this);

        int leftSwipeActions = Settings.System.getIntForUser(resolver,
                Settings.System.LEFT_LONG_BACK_SWIPE_ACTION, 0,
                UserHandle.USER_CURRENT);
        mLeftSwipeActions = (ListPreference) findPreference("left_swipe_actions");
        mLeftSwipeActions.setValue(Integer.toString(leftSwipeActions));
        mLeftSwipeActions.setSummary(mLeftSwipeActions.getEntry());
        mLeftSwipeActions.setOnPreferenceChangeListener(this);

        int rightSwipeActions = Settings.System.getIntForUser(resolver,
                Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION, 0,
                UserHandle.USER_CURRENT);
        mRightSwipeActions = (ListPreference) findPreference("right_swipe_actions");
        mRightSwipeActions.setValue(Integer.toString(rightSwipeActions));
        mRightSwipeActions.setSummary(mRightSwipeActions.getEntry());
        mRightSwipeActions.setOnPreferenceChangeListener(this);

        mLeftSwipeAppSelection = (Preference) findPreference("left_swipe_app_action");
        boolean isAppSelection = Settings.System.getIntForUser(resolver,
                Settings.System.LEFT_LONG_BACK_SWIPE_ACTION, 0, UserHandle.USER_CURRENT) == 4/*action_app_action*/;
        mLeftSwipeAppSelection.setVisible(isAppSelection);

        mRightSwipeAppSelection = (Preference) findPreference("right_swipe_app_action");
        isAppSelection = Settings.System.getIntForUser(resolver,
                Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION, 0, UserHandle.USER_CURRENT) == 4/*action_app_action*/;
        mRightSwipeAppSelection.setVisible(isAppSelection);

        int leftVerticalSwipeActions = Settings.System.getIntForUser(resolver,
                Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION, 0,
                UserHandle.USER_CURRENT);
        mLeftVerticalSwipeActions = (ListPreference) findPreference("left_vertical_swipe_actions");
        mLeftVerticalSwipeActions.setValue(Integer.toString(leftVerticalSwipeActions));
        mLeftVerticalSwipeActions.setSummary(mLeftVerticalSwipeActions.getEntry());
        mLeftVerticalSwipeActions.setEnabled(extendedSwipe);
        mLeftVerticalSwipeActions.setOnPreferenceChangeListener(this);

        int rightVerticalSwipeActions = Settings.System.getIntForUser(resolver,
                Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION, 0,
                UserHandle.USER_CURRENT);
        mRightVerticalSwipeActions = (ListPreference) findPreference("right_vertical_swipe_actions");
        mRightVerticalSwipeActions.setValue(Integer.toString(rightVerticalSwipeActions));
        mRightVerticalSwipeActions.setSummary(mRightVerticalSwipeActions.getEntry());
        mRightVerticalSwipeActions.setEnabled(extendedSwipe);
        mRightVerticalSwipeActions.setOnPreferenceChangeListener(this);

        mLeftVerticalSwipeAppSelection = (Preference) findPreference("left_vertical_swipe_app_action");
        isAppSelection = Settings.System.getIntForUser(resolver,
                Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION, 0, UserHandle.USER_CURRENT) == 4/*action_app_action*/;
        mLeftVerticalSwipeAppSelection.setVisible(extendedSwipe && isAppSelection);

        mRightVerticalSwipeAppSelection = (Preference) findPreference("right_vertical_swipe_app_action");
        isAppSelection = Settings.System.getIntForUser(resolver,
                Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION, 0, UserHandle.USER_CURRENT) == 4/*action_app_action*/;
        mRightVerticalSwipeAppSelection.setVisible(extendedSwipe && isAppSelection);

        mLeftPieConfig = (Preference) findPreference("left_pie_items");
        mRightPieConfig = (Preference) findPreference("right_pie_items");
        mLeftVerticalPieConfig = (Preference) findPreference("left_vertical_pie_items");
        mRightVerticalPieConfig = (Preference) findPreference("right_vertical_pie_items");

        customAppCheck();
        refreshPieVisibility();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLeftSwipeActions) {
            int leftSwipeActions = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.LEFT_LONG_BACK_SWIPE_ACTION, leftSwipeActions,
                    UserHandle.USER_CURRENT);
            int index = mLeftSwipeActions.findIndexOfValue((String) newValue);
            mLeftSwipeActions.setSummary(
                    mLeftSwipeActions.getEntries()[index]);
            mLeftSwipeAppSelection.setVisible(leftSwipeActions == 4);
            actionPreferenceReload();
            customAppCheck();
            refreshPieVisibility();
            return true;
        } else if (preference == mRightSwipeActions) {
            int rightSwipeActions = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION, rightSwipeActions,
                    UserHandle.USER_CURRENT);
            int index = mRightSwipeActions.findIndexOfValue((String) newValue);
            mRightSwipeActions.setSummary(
                    mRightSwipeActions.getEntries()[index]);
            mRightSwipeAppSelection.setVisible(rightSwipeActions == 4);
            actionPreferenceReload();
            customAppCheck();
            refreshPieVisibility();
            return true;
        } else if (preference == mLeftVerticalSwipeActions) {
            int leftVerticalSwipeActions = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION, leftVerticalSwipeActions,
                    UserHandle.USER_CURRENT);
            int index = mLeftVerticalSwipeActions.findIndexOfValue((String) newValue);
            mLeftVerticalSwipeActions.setSummary(
                    mLeftVerticalSwipeActions.getEntries()[index]);
            mLeftVerticalSwipeAppSelection.setVisible(mExtendedSwipe.isChecked() && leftVerticalSwipeActions == 4);
            customAppCheck();
            refreshPieVisibility();
            return true;
        } else if (preference == mRightVerticalSwipeActions) {
            int rightVerticalSwipeActions = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION, rightVerticalSwipeActions,
                    UserHandle.USER_CURRENT);
            int index = mRightVerticalSwipeActions.findIndexOfValue((String) newValue);
            mRightVerticalSwipeActions.setSummary(
                    mRightVerticalSwipeActions.getEntries()[index]);
            mRightVerticalSwipeAppSelection.setVisible(mExtendedSwipe.isChecked() && rightVerticalSwipeActions == 4);
            customAppCheck();
            refreshPieVisibility();
            return true; 
        } else if (preference == mExtendedSwipe) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            mExtendedSwipe.setChecked(enabled);
            mLeftVerticalSwipeActions.setEnabled(enabled);
            mRightVerticalSwipeActions.setEnabled(enabled);
            mLeftVerticalSwipeAppSelection.setVisible(enabled && mLeftVerticalSwipeActions.getValue().equals("4"));
            mRightVerticalSwipeAppSelection.setVisible(enabled && mRightVerticalSwipeActions.getValue().equals("4"));
            return true; 
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ensure preferences sensible to change get updated
        actionPreferenceReload();
        customAppCheck();
        refreshPieVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Ensure preferences sensible to change gets updated
        actionPreferenceReload();
        customAppCheck();
    }

    /* Helper for reloading both short and long gesture as they might change on
       package uninstallation */
    private void actionPreferenceReload() {
        int leftSwipeActions = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.LEFT_LONG_BACK_SWIPE_ACTION, 0,
                UserHandle.USER_CURRENT);

        int rightSwipeActions = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION, 0,
                UserHandle.USER_CURRENT);

        // Reload the action preferences
        mLeftSwipeActions.setValue(Integer.toString(leftSwipeActions));
        mLeftSwipeActions.setSummary(mLeftSwipeActions.getEntry());

        mRightSwipeActions.setValue(Integer.toString(rightSwipeActions));
        mRightSwipeActions.setSummary(mRightSwipeActions.getEntry());

        mLeftSwipeAppSelection.setVisible("4".equals(mLeftSwipeActions.getValue()));
        mRightSwipeAppSelection.setVisible("4".equals(mRightSwipeActions.getValue()));

        int leftVerticalSwipeActions = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION, 0,
                UserHandle.USER_CURRENT);

        int rightVerticalSwipeActions = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION, 0,
                UserHandle.USER_CURRENT);

        // Reload the action preferences
        mLeftVerticalSwipeActions.setValue(Integer.toString(leftVerticalSwipeActions));
        mLeftVerticalSwipeActions.setSummary(mLeftVerticalSwipeActions.getEntry());

        mRightVerticalSwipeActions.setValue(Integer.toString(rightVerticalSwipeActions));
        mRightVerticalSwipeActions.setSummary(mRightVerticalSwipeActions.getEntry());

        mLeftVerticalSwipeAppSelection.setVisible(mExtendedSwipe.isChecked() && "4".equals(mLeftVerticalSwipeActions.getValue()));
        mRightVerticalSwipeAppSelection.setVisible(mExtendedSwipe.isChecked() && "4".equals(mRightVerticalSwipeActions.getValue()));
    }

    private void refreshPieVisibility() {
        int left = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.LEFT_LONG_BACK_SWIPE_ACTION, 0, UserHandle.USER_CURRENT);
        int right = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION, 0, UserHandle.USER_CURRENT);
        int leftV = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION, 0, UserHandle.USER_CURRENT);
        int rightV = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION, 0, UserHandle.USER_CURRENT);
        boolean ext = mExtendedSwipe != null && mExtendedSwipe.isChecked();
        if (mLeftPieConfig != null) {
            mLeftPieConfig.setVisible(left == 18);
            updatePieSummary(mLeftPieConfig, Settings.System.LEFT_LONG_BACK_SWIPE_PIE_ITEMS);
        }
        if (mRightPieConfig != null) {
            mRightPieConfig.setVisible(right == 18);
            updatePieSummary(mRightPieConfig, Settings.System.RIGHT_LONG_BACK_SWIPE_PIE_ITEMS);
        }
        if (mLeftVerticalPieConfig != null) {
            mLeftVerticalPieConfig.setVisible(ext && leftV == 18);
            updatePieSummary(mLeftVerticalPieConfig,
                    Settings.System.LEFT_VERTICAL_BACK_SWIPE_PIE_ITEMS);
        }
        if (mRightVerticalPieConfig != null) {
            mRightVerticalPieConfig.setVisible(ext && rightV == 18);
            updatePieSummary(mRightVerticalPieConfig,
                    Settings.System.RIGHT_VERTICAL_BACK_SWIPE_PIE_ITEMS);
        }
    }

    private void updatePieSummary(Preference pref, String key) {
        String raw = Settings.System.getStringForUser(getContentResolver(), key,
                UserHandle.USER_CURRENT);
        if (raw == null || raw.isEmpty()) {
            pref.setSummary(getString(R.string.pie_menu_empty_warning));
            return;
        }
        String[] slots = raw.split(";", -1);
        int count = 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && !slots[i].isEmpty()) {
                count++;
            }
        }
        if (count == 0) {
            pref.setSummary(getString(R.string.pie_menu_empty_warning));
        } else {
            pref.setSummary(count + " / 5");
        }
    }

    private void customAppCheck() {
        mLeftSwipeAppSelection.setSummary(Settings.System.getStringForUser(getContentResolver(),
                String.valueOf(Settings.System.LEFT_LONG_BACK_SWIPE_APP_FR_ACTION), UserHandle.USER_CURRENT));
        mRightSwipeAppSelection.setSummary(Settings.System.getStringForUser(getContentResolver(),
                String.valueOf(Settings.System.RIGHT_LONG_BACK_SWIPE_APP_FR_ACTION), UserHandle.USER_CURRENT));

        mLeftVerticalSwipeAppSelection.setSummary(Settings.System.getStringForUser(getContentResolver(),
                String.valueOf(Settings.System.LEFT_VERTICAL_BACK_SWIPE_APP_FR_ACTION), UserHandle.USER_CURRENT));
        mRightVerticalSwipeAppSelection.setSummary(Settings.System.getStringForUser(getContentResolver(),
                String.valueOf(Settings.System.RIGHT_VERTICAL_BACK_SWIPE_APP_FR_ACTION), UserHandle.USER_CURRENT));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VOLTAGE;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result = new ArrayList<>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.gesture_nav_tweaks;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
