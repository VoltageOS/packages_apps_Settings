/**
 * SPDX-FileCopyrightText: 2025 The LibreMobileOS Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.notification;

import android.content.Context;
import android.net.Uri;
import android.preference.SeekBarVolumizer;
import android.widget.SeekBar;

/**
 * Version of {@link SeekBarVolumizer} that plays sample audio immediately on progress change.
 */
public class SettingsSeekBarVolumizer extends SeekBarVolumizer {

    private final SeekBarVolumizer.Callback mCallback;

    public SettingsSeekBarVolumizer(
            Context context,
            int streamType,
            Uri defaultUri,
            SeekBarVolumizer.Callback callback) {
        super(context, streamType, defaultUri, callback);
        mCallback = callback;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        startSample();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Skip postStartSample() from super method
        if (mCallback != null) {
            mCallback.onStopTrackingTouch(this);
        }
    }
}
