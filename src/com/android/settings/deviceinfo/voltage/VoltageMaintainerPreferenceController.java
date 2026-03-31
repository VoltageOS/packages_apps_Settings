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

package com.android.settings.deviceinfo.voltage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.Random;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class VoltageMaintainerPreferenceController extends BasePreferenceController {

    private static final String TAG = "VoltageMaintainerPreferenceController";
    private static final String KEY_VOLTAGE_BUILD_STATUS_PROP = "ro.voltage.build.status";

    private final Random mRandom = new Random();
    private int mLastToastIndex = -1;
    private Toast mToast = null;

    private boolean mIsVerifying = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public VoltageMaintainerPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        
        if (preference instanceof VoltageMaintainerPreference) {
            ((VoltageMaintainerPreference) preference).setOnLongClickListener(() -> {
                if (!mIsVerifying) {
                    performSignatureCheck();
                }
            });
        }
    }

    @Override
    public CharSequence getSummary() {
        String buildStatus = getBuildStatus();
        String maintainer = mContext.getResources().getString(R.string.voltage_maintainer);

        if (!TextUtils.isEmpty(buildStatus) && !buildStatus.equals(mContext.getString(R.string.unknown))) {
            return buildStatus + " by " + maintainer;
        }

        return mContext.getString(R.string.unknown);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        String buildStatus = getBuildStatus();

        if ("OFFICIAL".equalsIgnoreCase(buildStatus)) {
            preference.setIcon(R.drawable.maintainer_official);
        } else {
            preference.setIcon(R.drawable.maintainer_unofficial);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        if (mIsVerifying) {
            return true; 
        }

        if (mToast != null && mToast.getView() != null && mToast.getView().isShown()) {
            return true;
        }

        final String buildStatus = getBuildStatus();
        final String[] toasts;
        if ("OFFICIAL".equalsIgnoreCase(buildStatus)) {
            toasts = mContext.getResources().getStringArray(R.array.voltage_official_build_toast);
        } else {
            toasts = mContext.getResources().getStringArray(R.array.voltage_unofficial_build_toast);
        }

        if (toasts.length > 0) {
            int randomIndex;
            if (toasts.length > 1) {
                do {
                    randomIndex = mRandom.nextInt(toasts.length);
                } while (randomIndex == mLastToastIndex);
            } else {
                randomIndex = 0;
            }
            mLastToastIndex = randomIndex;
            notifyState(toasts[randomIndex], Toast.LENGTH_LONG);
        }
        return true;
    }

    private void performSignatureCheck() {
        mIsVerifying = true;
        final boolean isValidated = "OFFICIAL".equalsIgnoreCase(getBuildStatus());

        if (isValidated) {
            notifyState(new String(Base64.decode("4pqhIEluaXRpYXRpbmcgdm9sdGFnZSBjaGFyZ2UuLi4=", Base64.DEFAULT)), 0);
            syncHwRampSegment(20, 100, 1000);

            mHandler.postDelayed(() -> {
                notifyState(new String(Base64.decode("4pqg77iPIFdBUk5JTkc6IENhcGFjaXR5IHJlYWNoaW5nIDIwMCUh", Base64.DEFAULT)), 0);
                syncHwRampSegment(100, 200, 1000);
            }, 1000);

            mHandler.postDelayed(() -> {
                notifyState(new String(Base64.decode("8J+UpSBTWVNURU0gT1ZFUkxPQUQh", Base64.DEFAULT)), 0);
                syncHwRampSegment(200, 255, 500);
            }, 2000);

            mHandler.postDelayed(() -> {
                resolveFallbackTarget();
                mIsVerifying = false;
            }, 2500);

        } else {
            notifyState(new String(Base64.decode("8J+UjCBVbm9mZmljaWFsIGdyb3VuZCBkZXRlY3RlZC4gQnlwYXNzaW5nIHNhZmV0eS4uLg==", Base64.DEFAULT)), 0);
            syncHwRampSegment(10, 50, 1500); 

            mHandler.postDelayed(() -> {
                notifyState(new String(Base64.decode("4pqg77iPIFVuc3RhYmxlIGN1cnJlbnQuIFJlZ3VsYXRpbmcgdm9sdGFnZS4uLg==", Base64.DEFAULT)), 0);
                syncHwRampSegment(50, 120, 1500); 
            }, 1500);

            mHandler.postDelayed(() -> {
                notifyState(new String(Base64.decode("4pqg77iPIENyaXRpY2FsIGZsdWN0dWF0aW9uISBCcmFjZSB5b3Vyc2VsZi4uLg==", Base64.DEFAULT)), 0);
                syncHwRampSegment(120, 200, 1500); 
            }, 3000);

            mHandler.postDelayed(() -> {
                notifyState(new String(Base64.decode("4pqhIENPTlRBSU5NRU5UIEZBSUxFRCE=", Base64.DEFAULT)), 0);
                syncHwRampSegment(200, 255, 500); 
            }, 4500);

            mHandler.postDelayed(() -> {
                resolveFallbackTarget();
                mIsVerifying = false;
            }, 5000);
        }
    }

    private void resolveFallbackTarget() {
        try {
            Intent intent = new Intent();
            String targetPkg = new StringBuilder("ggeretsae.so.egatlov.moc").reverse().toString();
            String targetCls = new StringBuilder("ytivitcAniaM.ggeretsae.so.egatlov.moc").reverse().toString();
            
            intent.setClassName(targetPkg, targetCls);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            mContext.startActivity(intent);
        } catch (Exception e) {
            notifyState(new String(Base64.decode("SW50ZWdyaXR5IHBheWxvYWQgbWlzc2luZy4=", Base64.DEFAULT)), 1);
        }
    }

    private void notifyState(String payload, int dur) {
        if (mToast != null) {
            mToast.cancel();
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View layout = inflater.inflate(R.layout.custom_toast_layout, null);
        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(payload);

        final int yOffset = mContext.getResources().getDimensionPixelSize(R.dimen.toast_y_offset);

        mToast = new Toast(mContext);
        mToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, yOffset);
        mToast.setDuration(dur);
        mToast.setView(layout);
        mToast.show();
    }

    private void syncHwRampSegment(int startAmp, int endAmp, int durationMs) {
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            
            vibrator.cancel();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int stepDuration = 30;
                int steps = Math.max(1, durationMs / stepDuration);
                long[] timings = new long[steps];
                int[] amplitudes = new int[steps];
                
                for (int i = 0; i < steps; i++) {
                    timings[i] = stepDuration;
                    
                    int baseAmp = startAmp + (int) (((float) i / (steps - 1)) * (endAmp - startAmp));
                    
                    int shudder = mRandom.nextInt(30) - 15; 
                    
                    amplitudes[i] = Math.max(1, Math.min(255, baseAmp + shudder));
                }
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
            } else {
                vibrator.vibrate(durationMs);
            }
        }
    }

    private String getBuildStatus() {
        String buildStatus = SystemProperties.get(KEY_VOLTAGE_BUILD_STATUS_PROP, null);

        if ("OFFICIAL".equalsIgnoreCase(buildStatus) || "UNOFFICIAL".equalsIgnoreCase(buildStatus)) {
            return buildStatus;
        }

        return mContext.getString(R.string.unknown);
    }
}
