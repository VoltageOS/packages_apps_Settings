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
    private static final String OVERLAY_PACKAGE = "com.voltage.overlay.customization.keyboard.nonavbar";
    private static final String PROP_KEY = "persist.sys.keyboard_no_navigation_bar";

    public KeyboardNoNavigationBarController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        try {
            int navigationMode = Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE,
                0
            );
            return navigationMode == 2 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        } catch (Exception e) {
            Log.e(TAG, "Error getting navigation mode", e);
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    public boolean isChecked() {
        try {
            String value = SystemProperties.get(PROP_KEY, "1");
            boolean result = "1".equals(value);
            Log.d(TAG, "isChecked() returning: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error checking system property", e);
            return true; // Default to true on error
        }
    }

    private boolean executeOverlayCommand(boolean enable) {
        String command = enable
            ? "cmd overlay disable " + OVERLAY_PACKAGE
            : "cmd overlay enable " + OVERLAY_PACKAGE;

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Log.e(TAG, "Overlay command failed with exit code: " + exitCode);
                return false;
            }
            Log.d(TAG, "Successfully executed overlay command: " + command);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute overlay command", e);
            return false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(newValue instanceof Boolean)) {
            Log.e(TAG, "Invalid preference value type: " + newValue);
            return false;
        }

        boolean enabled = (Boolean) newValue;
        String newPropValue = enabled ? "1" : "0";

        try {
            SystemProperties.set(PROP_KEY, newPropValue);
            Log.d(TAG, "Set system property to: " + newPropValue);

            if (!executeOverlayCommand(enabled)) {
                Log.e(TAG, "Failed to update overlay state");
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating preference", e);
            return false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof SwitchPreferenceCompat) {
            boolean checked = isChecked();
            ((SwitchPreferenceCompat) preference).setChecked(checked);
            Log.d(TAG, "Updated switch state to: " + checked);
        }
    }
}
