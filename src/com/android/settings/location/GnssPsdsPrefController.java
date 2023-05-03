package com.android.settings.location;

import android.content.Context;
import android.content.pm.PackageManager;
import android.ext.settings.GnssSettings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

public class GnssPsdsPrefController extends IntSettingPrefController {
    private final String psdsType;
    private final boolean hasGpsFeature;

    public GnssPsdsPrefController(Context ctx, String key) {
        super(ctx, key, GnssSettings.getPsdsSetting(ctx));
        psdsType = ctx.getString(com.android.internal.R.string.config_gnssPsdsType);
        hasGpsFeature = ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!hasGpsFeature) {
            return UNSUPPORTED_ON_DEVICE;
        }

        int result = super.getAvailabilityStatus();
        if (result == AVAILABLE) {
            if (psdsType.isEmpty()) {
                result = UNSUPPORTED_ON_DEVICE;
            }
        }
        return result;
    }

    @Override
    public void addPrefsAfterList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.gnss_psds_footer);
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.psds_enabled_grapheneos_server, GnssSettings.PSDS_SERVER_GRAPHENEOS);
        entries.add(R.string.psds_enabled_standard_server, GnssSettings.PSDS_SERVER_STANDARD);
        entries.add(R.string.psds_disabled, R.string.psds_disabled_summary, GnssSettings.PSDS_DISABLED);
    }
}
