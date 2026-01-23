package com.android.settings.location

import android.content.Context
import android.ext.settings.NetworkLocationSettings
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.ext.IntSettingPrefController
import com.android.settings.ext.RadioButtonPickerFragment2

/**
 * Preference controller for Network location in Location Services.
 */
class LocationServicesNetworkLocationPreferenceController(ctx: Context, key: String?) :
    IntSettingPrefController(ctx, key, NetworkLocationSettings.NETWORK_LOCATION_SETTING) {

    override fun addPrefsAfterList(fragment: RadioButtonPickerFragment2, screen: PreferenceScreen) {
        addFooterPreference(screen, R.string.network_location_footer)
    }

    override fun getEntries(entries: Entries) {
        entries.add(
            R.string.network_location_enabled_grapheneos_apple_proxy,
            NetworkLocationSettings.NETWORK_LOCATION_GRAPHENEOS_APPLE_PROXY
        )
        entries.add(
            R.string.network_location_enabled_apple,
            NetworkLocationSettings.NETWORK_LOCATION_APPLE
        )
        entries.add(
            R.string.network_location_enabled_apple_china,
            NetworkLocationSettings.NETWORK_LOCATION_APPLE_CHINA
        )
        entries.add(
            R.string.network_location_disabled,
            NetworkLocationSettings.NETWORK_LOCATION_DISABLED
        )
    }
}
