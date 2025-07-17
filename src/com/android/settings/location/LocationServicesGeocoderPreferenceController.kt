package com.android.settings.location

import android.content.Context
import android.ext.settings.GeocoderSettings
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.ext.IntSettingPrefController
import com.android.settings.ext.RadioButtonPickerFragment2

/**
 * Preference controller for Geocoder in Location Services.
 */
class LocationServicesGeocoderPreferenceController(ctx: Context, key: String?) :
    IntSettingPrefController(ctx, key, GeocoderSettings.GEOCODER_SETTING) {

    override fun addPrefsAfterList(fragment: RadioButtonPickerFragment2, screen: PreferenceScreen) {
        addFooterPreference(screen, R.string.geocoder_footer)
    }

    override fun getEntries(entries: Entries) {
        entries.add(
            R.string.geocoder_enabled_grapheneos_proxy,
            GeocoderSettings.GEOCODER_SERVER_GRAPHENEOS_PROXY
        )
        entries.add(
            R.string.geocoder_enabled_nominatim_server,
            GeocoderSettings.GEOCODER_SERVER_NOMINATIM
        )
        entries.add(
            R.string.geocoder_disabled,
            GeocoderSettings.GEOCODER_DISABLED
        )
    }
}
