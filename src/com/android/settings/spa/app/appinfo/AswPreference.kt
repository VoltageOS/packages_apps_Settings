package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.app.AppSwitch
import androidx.compose.runtime.Composable
import com.android.settings.applications.AswAdapter
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.installed

@Composable
fun AswPreference(context: Context, app: ApplicationInfo, adapter: AswAdapter<out AppSwitch>) {
    if (!app.installed) {
        return
    }

    Preference(object : PreferenceModel {
        override val title = adapter.getAswTitle(context).toString()
        override val summary = {
            adapter.getPreferenceSummary(context, app).toString()
        }
        override val onClick = {
            adapter.openAppPage(context, app)
        }
    })
}
