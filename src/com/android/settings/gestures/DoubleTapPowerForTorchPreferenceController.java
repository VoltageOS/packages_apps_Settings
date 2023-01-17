/*
 * Copyright (C) 2023 The PixelDust Project
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

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * Configures the behaviour of the radio selector to configure double tap power button for torch.
 */
public class DoubleTapPowerForTorchPreferenceController extends BasePreferenceController
        implements PowerMenuSettingsUtils.SettingsStateCallback,
                SelectorWithWidgetPreference.OnClickListener,
                LifecycleObserver {

    private SelectorWithWidgetPreference mPreference;
    private final PowerMenuSettingsUtils mUtils;
    private Context mContext;

    private static final String PREF_KEY_POWER_BUTTON_TORCH_LP = "gesture_power_button_torch_long_press";

    public DoubleTapPowerForTorchPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mUtils = new PowerMenuSettingsUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (isQuickOpenCameraGestureEnabled() ||
            (!PowerMenuSettingsUtils.isDoubleTapPowerForTorchEnabled(mContext) &&
                    !PowerMenuSettingsUtils.isLongPressPowerForTorchEnabled(mContext))) {
            return DISABLED_DEPENDENT_SETTING;
        } else if (mContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return AVAILABLE;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    private boolean isQuickOpenCameraGestureEnabled() {
        return (mContext.getResources().getBoolean(
            com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled) && (
            Settings.Secure.getInt(mContext.getContentResolver(),
            Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0) == 0));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setOnClickListener(this);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference)
                    .setChecked(
                            PowerMenuSettingsUtils.isDoubleTapPowerForTorchEnabled(mContext));
        }
        preference.setEnabled(getAvailabilityStatus() == AVAILABLE);
    }

    @Override
    public CharSequence getSummary() {
        return isQuickOpenCameraGestureEnabled() &&
            PowerMenuSettingsUtils.isLongPressPowerForTorchEnabled(mContext)
            ? mContext.getString(R.string.double_tap_power_for_torch_summary_disabled)
            : "";
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        Settings.Secure.putIntForUser(
            mContext.getContentResolver(),
            Settings.Secure.TORCH_LONG_PRESS_POWER,
            0,
            UserHandle.USER_CURRENT
        );
        Settings.Secure.putIntForUser(
            mContext.getContentResolver(),
            Settings.Secure.TORCH_DOUBLE_TAP_POWER_GESTURE_ENABLED,
            1,
            UserHandle.USER_CURRENT
        );
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void onChange(Uri uri) {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_START) */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mUtils.registerObserver(this);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_STOP) */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mUtils.unregisterObserver();
    }
}
