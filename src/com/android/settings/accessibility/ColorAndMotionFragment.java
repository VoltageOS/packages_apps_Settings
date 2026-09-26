/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.view.CrossWindowBlurListeners;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accessibility.colorandmotion.ui.BlurSwitchPreference;
import com.android.settings.accessibility.colorandmotion.ui.ColorAndMotionScreen;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.Collections;
import java.util.List;

// TODO(b/445978289): Use CatalystFragment
/** Accessibility settings for color and motion. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ColorAndMotionFragment extends DashboardFragment {

    private static final String TAG = "ColorAndMotionFragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Nullable
    @Override
    public String getPreferenceScreenBindingKey(@NonNull Context context) {
        return ColorAndMotionScreen.KEY;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    if (!CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED) {
                        return Collections.emptyList();
                    }

                    SearchIndexableRaw raw = new SearchIndexableRaw(context);
                    raw.key = BlurSwitchPreference.KEY;
                    raw.title = context.getString(R.string.blur_switch);
                    raw.summaryOn = context.getString(R.string.blur_switch_summary);
                    raw.summaryOff = raw.summaryOn;
                    raw.screenTitle =
                            context.getString(R.string.accessibility_color_and_motion_title);
                    raw.keywords = context.getString(R.string.keywords_blur_switch);
                    return Collections.singletonList(raw);
                }
            };
}
