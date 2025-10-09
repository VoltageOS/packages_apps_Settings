/*
 * Copyright (C) 2024 VoltageOS
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

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;

public class HomepageToastPreferenceController extends TogglePreferenceController {

    private static final String SETTING_KEY = "homepage_toast_messages_enabled";
    private static final String DEPENDENT_KEY = "homepage_toast_custom_text";

    private Preference mDependentPref;

    public HomepageToastPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean changed = Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, isChecked ? 1 : 0);
        if (mDependentPref != null) {
            mDependentPref.setVisible(isChecked);
        }
        return changed;
    }

   @Override
    public void displayPreference(PreferenceScreen screen) {
       super.displayPreference(screen);
        mDependentPref = screen.findPreference(DEPENDENT_KEY);
        if (mDependentPref != null) {
            mDependentPref.setVisible(isChecked());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
