/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.navigationbar;

import android.provider.Settings;
import android.os.SystemProperties;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.core.BasePreferenceController;

public class KeyboardNoNavigationBarController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String KEY_KEYBOARD_NO_NAVIGATION_BAR = "keyboard_no_navigation_bar";
    private static final String TAG = "NoNavbarController";

    public KeyboardNoNavigationBarController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        int navigationMode = Settings.Secure.getInt(
            mContext.getContentResolver(),
            Settings.Secure.NAVIGATION_MODE,
            0 // Default to 0 if the setting is not found
        );

        return navigationMode == 2
                ? AVAILABLE 
                : UNSUPPORTED_ON_DEVICE;

    }

    public boolean isChecked() {
        String value = SystemProperties.get("persist.sys.keyboard_no_navigation_bar", "1"); 
        boolean result = "1".equals(value); // switch is ON only when overlay is disabled
        Log.d(TAG, "isChecked() reversed logic returning: " + result);
        return result;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(newValue instanceof Boolean)) {
            Log.e(TAG, "Unexpected value type for preference: " + newValue);
            return false;
        }

        boolean enabled = (Boolean) newValue;
        String newPropValue = enabled ? "1" : "0";
        SystemProperties.set("persist.sys.keyboard_no_navigation_bar", newPropValue);
        Log.d(TAG, "onPreferenceChange() set persist.sys.keyboard_no_navigation_bar to: " + newPropValue);

        String command = enabled
                ? "cmd overlay disable com.voltage.overlay.customization.keyboard.nonavbar"
                : "cmd overlay enable com.voltage.overlay.customization.keyboard.nonavbar";

        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            Log.d(TAG, "Executed command: " + command);
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute overlay command", e);
        }

        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof SwitchPreferenceCompat) {
            boolean checked = isChecked();
            ((SwitchPreferenceCompat) preference).setChecked(checked);
            Log.d(TAG, "updateState() setting switch to: " + checked);
        }
    }
}
