/*
 * Copyright (C) 2025 The Android Open Source Project
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
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class DoubleTapToSleepPreferenceController extends TogglePreferenceController {

    public DoubleTapToSleepPreferenceController(Context context, String key) {
        super(context, key);
    }

    /**
     * This method is the core of the controller's logic.
     * It checks if the feature is supported by the device.
     * If this returns false, the setting will be hidden from the UI automatically.
     */
    @Override
    public int getAvailabilityStatus() {
        // This checks the build-time config flag you added in the first patch.
        boolean isSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_supportDoubleTapSleep);

        return isSupported ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    /**
     * This is called when the user toggles the switch.
     * It writes the new value to the Secure Setting you created.
     */
    @Override
    public boolean setChecked(boolean isChecked) {
        // The framework handles the storage. `isChecked` is the new state.
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_TO_SLEEP, isChecked ? 1 : 0);
    }

    /**
     * This is called to check the current state of the setting.
     * It reads from the Secure Setting and tells the UI whether the switch should be on or off.
     */
    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_TO_SLEEP, 0) != 0;
    }

    /**
     * Required method for search functionality in Settings.
     */
    @Override
    public int getSliceHighlightMenuRes() {
        // This helps the Settings search find this item.
        // You would point this to the correct menu key for the screen it appears on.
        // For example, if it's on the main Display settings screen:
        return R.string.menu_key_display;
    }
}
