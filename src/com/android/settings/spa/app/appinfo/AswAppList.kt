package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.settings.applications.AswAdapter
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.util.asyncFilter
import com.android.settingslib.spa.framework.util.asyncMap
import com.android.settingslib.spa.widget.ui.SpinnerOption
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

open class AswAppListPageProvider(val adapter: AswAdapter<*>) : SettingsPageProvider {
    override val name = adapter.getAppListPageProviderName()

    @Composable
    override fun Page(arguments: Bundle?) {
        AswAppListPage(adapter)
    }
}

@Composable
fun AswAppListPage(adapter: AswAdapter<*>) {
    AppListPage(
        title = rememberContext { ctx ->
            adapter.getShortAswTitle(ctx).toString()
        },
        listModel = rememberContext { ctx ->
            AswAppListModel(ctx, adapter)
        },
    )
}

class AppRecordImpl(override val app: ApplicationInfo, val gosPackageState: GosPackageState?) : AppRecord

class AswAppListModel(val ctx: Context, val adapter: AswAdapter<*>) : AppListModel<AppRecordImpl> {
    companion object {
        const val SPINNER_OPTION_OFF = 0
        const val SPINNER_OPTION_ON = 1
    }

    override fun getSpinnerOptions(recordList: List<AppRecordImpl>): List<SpinnerOption> {
        val options = ArrayList<SpinnerOption>(2)
        options.add(SpinnerOption(SPINNER_OPTION_OFF, adapter.getOffTitle(ctx).toString()))
        options.add(SpinnerOption(SPINNER_OPTION_ON, adapter.getOnTitle(ctx).toString()))
        return options
    }

    override fun transform(
        userIdFlow: Flow<Int>,
        appListFlow: Flow<List<ApplicationInfo>>
    ): Flow<List<AppRecordImpl>> {
        return userIdFlow.combine(appListFlow) { userId, appList ->
            appList.asyncMap { app ->
                AppRecordImpl(app, GosPackageState.get(app.packageName, userId))
            }
        }
    }

    override fun filter(
        userIdFlow: Flow<Int>,
        option: Int,
        recordListFlow: Flow<List<AppRecordImpl>>
    ): Flow<List<AppRecordImpl>> {
        val appSwitch = adapter.getAppSwitch()
        return userIdFlow.combine(recordListFlow) { userId, recordList ->
            recordList.asyncFilter {
                val value = appSwitch.get(ctx, userId, it.app, it.gosPackageState)
                when (option) {
                    SPINNER_OPTION_OFF -> !value
                    SPINNER_OPTION_ON -> value
                    else -> throw IllegalArgumentException(option.toString())
                }
            }
        }
    }

    @Composable
    override fun AppListItemModel<AppRecordImpl>.AppItem() {
        val context = LocalContext.current
        AppListItem(onClick = {
            adapter.openAppPage(context, record.app)
        })
    }
}

