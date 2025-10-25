package com.android.settings.deviceinfo.voltage;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class VoltageGpgPreferenceController extends BasePreferenceController {

    private static final String TAG = "VoltageGpgPreferenceController";

    private static final String KEY_VOLTAGE_BUILD_STATUS = "ro.voltage.build.status";
    private static final String KEY_VOLTAGE_MAINTAINER_GPG_KEY = "ro.voltage.maintainer.gpg_key";
    private static final String KEY_VOLTAGE_MAINTAINER_GPG_UID = "ro.voltage.maintainer.gpg_uid";

    public VoltageGpgPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        String buildStatus = SystemProperties.get(KEY_VOLTAGE_BUILD_STATUS, "UNOFFICIAL");
        String gpgKey = SystemProperties.get(KEY_VOLTAGE_MAINTAINER_GPG_KEY, "");
        String hexGpgUid = SystemProperties.get(KEY_VOLTAGE_MAINTAINER_GPG_UID, "");

        if ("OFFICIAL".equalsIgnoreCase(buildStatus)) {
            if (!TextUtils.isEmpty(gpgKey) && !TextUtils.isEmpty(hexGpgUid)) {
                boolean showUid = mContext.getResources().getBoolean(
                        R.bool.config_show_gpg_uid);

                String decodedGpgUid;
                try {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < hexGpgUid.length(); i += 2) {
                        String hex = hexGpgUid.substring(i, i + 2);
                        sb.append((char) Integer.parseInt(hex, 16));
                    }
                    decodedGpgUid = sb.toString().trim();
                } catch (Exception e) {
                    decodedGpgUid = hexGpgUid;
                }

                preference.setVisible(true);
                if (showUid) {
                    preference.setSummary(decodedGpgUid + "\n" + gpgKey);
                } else {
                    preference.setSummary(gpgKey);
                }
                preference.setIcon(R.drawable.maintainer_official);
                preference.setCopyingEnabled(true);
            } else {
                preference.setVisible(true);
                preference.setSummary(mContext.getString(R.string.voltage_tampered_build_summary));
                preference.setIcon(R.drawable.maintainer_unofficial);
                preference.setCopyingEnabled(false);
            }
        } else {
            preference.setVisible(false);
        }
    }
}
