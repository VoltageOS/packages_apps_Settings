package com.android.settings.location;

import android.content.Context;
import android.content.pm.PackageManager;
import android.ext.settings.GnssSettings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;

public class GnssSuplPrefController extends IntSettingPrefController {
    private final boolean hasGpsFeature;

    public GnssSuplPrefController(Context ctx, String key) {
        super(ctx, key, GnssSettings.SUPL_SETTING);
        hasGpsFeature = ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    @Override
    public void addPrefsAfterList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.pref_gnss_supl_footer);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!hasGpsFeature) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return super.getAvailabilityStatus();
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.supl_enabled_grapheneos_proxy, GnssSettings.SUPL_SERVER_GRAPHENEOS_PROXY);
        entries.add(R.string.supl_enabled_standard_server, GnssSettings.SUPL_SERVER_STANDARD);
        entries.add(R.string.supl_disabled, R.string.supl_disabled_summary, GnssSettings.SUPL_DISABLED);
    }
}
