/*
 * Copyright (C) 2025 VoltageOS
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
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class HomepageToastTextPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String SETTING_KEY = "homepage_toast_custom_text";

    public HomepageToastTextPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        // This is managed by dependency, but we can add an extra check
        boolean isToggleEnabled = Settings.System.getInt(mContext.getContentResolver(),
                "homepage_toast_messages_enabled", 0) == 1;
        return isToggleEnabled ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String text = ((String) newValue).trim();

        Settings.System.putString(mContext.getContentResolver(), SETTING_KEY, text);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        String summary = Settings.System.getString(mContext.getContentResolver(), SETTING_KEY);
        if (TextUtils.isEmpty(summary)) {
            return mContext.getString(com.android.settings.R.string.homepage_toast_text_summary);
        }
        return summary;
    }
}
