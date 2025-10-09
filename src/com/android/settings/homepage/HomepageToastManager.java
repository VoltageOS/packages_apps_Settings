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

package com.android.settings.homepage;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;

import java.util.Random;

public class HomepageToastManager {

    private static final String KEY_VOLTAGE_BUILD_STATUS_PROP = "ro.voltage.build.status";
    private static final String KEY_CUSTOM_TEXT = "homepage_toast_custom_text";
    private static final long REFRESH_INTERVAL_MS = 30000;

    private final Context mContext;
    private final ViewGroup mContainer;
    private View mToastCard;
    private TextView mToastTextView;
    private final Random mRandom = new Random();
    private int mLastToastIndex = -1;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRefreshRunnable = this::showNextToast;

    public HomepageToastManager(Context context, ViewGroup container) {
        mContext = context;
        mContainer = container;
    }

    public boolean isEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(), "homepage_toast_messages_enabled", 0) == 1;
    }

    public void showToastCard() {
        if (mToastCard == null) {
            mToastCard = LayoutInflater.from(mContext).inflate(R.layout.homepage_toast_view, mContainer, false);
            mToastTextView = mToastCard.findViewById(android.R.id.title);
            mToastCard.setOnClickListener(v -> showNextToast(true /* resetTimer */));
            mContainer.addView(mToastCard, 0);
        }
        mToastCard.setVisibility(View.VISIBLE);
        startAutoRefresh();
    }

    public void hideToastCard() {
        if (mToastCard != null) {
            mToastCard.setVisibility(View.GONE);
        }
        stopAutoRefresh();
    }

    public void startAutoRefresh() {
        stopAutoRefresh();
        showNextToast(false /* resetTimer */);
        mHandler.postDelayed(mRefreshRunnable, REFRESH_INTERVAL_MS);
    }

    public void stopAutoRefresh() {
        mHandler.removeCallbacks(mRefreshRunnable);
    }

    private void showNextToast() {
        showNextToast(true /* resetTimer */);
    }

    private void showNextToast(boolean resetTimer) {
        if (mToastTextView == null) return;

        String customMessage = Settings.System.getString(mContext.getContentResolver(), KEY_CUSTOM_TEXT);

        if (!TextUtils.isEmpty(customMessage)) {
            mToastTextView.setText(customMessage);
            mToastCard.setClickable(false);
            stopAutoRefresh();
            return;
        }

        if (!mToastCard.isClickable()) {
            mToastCard.setClickable(true);
        }

        final String buildStatus = SystemProperties.get(KEY_VOLTAGE_BUILD_STATUS_PROP, "UNOFFICIAL");
        final String[] toasts = mContext.getResources().getStringArray(
                "OFFICIAL".equalsIgnoreCase(buildStatus)
                        ? R.array.voltage_official_build_toast
                        : R.array.voltage_unofficial_build_toast);

        if (toasts.length > 0) {
            int randomIndex;
            do {
                randomIndex = mRandom.nextInt(toasts.length);
            } while (toasts.length > 1 && randomIndex == mLastToastIndex);
            mLastToastIndex = randomIndex;
            mToastTextView.setText(toasts[randomIndex]);
        }

        if (resetTimer) {
            mHandler.removeCallbacks(mRefreshRunnable);
            mHandler.postDelayed(mRefreshRunnable, REFRESH_INTERVAL_MS);
        }
    }
}
