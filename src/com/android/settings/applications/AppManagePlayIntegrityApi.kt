package com.android.settings.applications

import android.app.compat.gms.GmsUtils
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.content.pm.GosPackageStateFlag
import android.content.pm.PackageManager.NameNotFoundException
import android.ext.PackageId
import android.ext.settings.app.AswBlockPlayIntegrityApi
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.widget.TopIntroPreference

object AswAdapterManagePlayIntegrityApi : AswAdapter<AswBlockPlayIntegrityApi>() {
    override fun getAppSwitch() = AswBlockPlayIntegrityApi.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.app_play_integrity_api)
    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.app_play_integrity_api_blocked)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.app_play_integrity_api_not_blocked)

    override fun getDetailFragmentClass() = AppManagePlayIntegrityApiFragment::class

    override fun shouldIncludeInAppListPage(app: ApplicationInfo, gosPs: GosPackageState): Boolean {
        return gosPs.hasFlag(GosPackageStateFlag.PLAY_INTEGRITY_API_USED_AT_LEAST_ONCE)
    }
}

@Composable
fun AppManagePlayIntegrityApiPreference(app: ApplicationInfo) {
    if (GosPackageState.get(app.packageName, app.userId)
            .hasFlag(GosPackageStateFlag.PLAY_INTEGRITY_API_USED_AT_LEAST_ONCE)) {
        AswPreference(LocalContext.current, app, AswAdapterManagePlayIntegrityApi)
    }
}

class AppManagePlayIntegrityApiFragment : AppInfoWithHeader() {
    private lateinit var showUsageNotifsSwitch: SwitchPreferenceCompat
    private lateinit var blockUsageAttemptsSwitch: SwitchPreferenceCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().setTitle(R.string.app_play_integrity_api)

        val prefCtx = prefContext
        val screen: PreferenceScreen = preferenceManager.createPreferenceScreen(prefCtx)

        TopIntroPreference(prefCtx).apply {
            setTitle(R.string.app_play_integrity_top_intro)
            screen.addPreference(this)
        }
        Preference(prefCtx).apply {
            setTitle(R.string.app_play_integrity_contact_app_developer)
            setOnPreferenceClickListener {
                val pkgName = mPackageName
                val intent: Intent
                if (isPlayStoreAvailable()) {
                    intent = GmsUtils.createAppPlayStoreIntent(pkgName)
                } else {
                    val uri = Uri.parse("https://play.google.com/store/apps/details?id=$pkgName")
                    intent = Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), null)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            }
            screen.addPreference(this)
        }
        showUsageNotifsSwitch = SwitchPreferenceCompat(prefCtx).apply {
            setTitle(R.string.app_play_integrity_show_usage_notifications)
            setOnPreferenceChangeListener { _, value ->
                val ed = GosPackageState.edit(mPackageName, mUserId)
                AswBlockPlayIntegrityApi.I.setNotificationEnabled(ed, value as Boolean)
                ed.apply()
            }
            screen.addPreference(this)
        }
        blockUsageAttemptsSwitch = SwitchPreferenceCompat(prefCtx).apply {
            setTitle(R.string.app_play_integrity_block_usage_attempts)
            setSummaryOn(R.string.app_play_integrity_block_usage_attempts_summary_on)
            setOnPreferenceChangeListener { _, value ->
                val ed = GosPackageState.edit(mPackageName, mUserId)
                AswBlockPlayIntegrityApi.I.set(ed, value as Boolean)
                ed.apply()
            }
            screen.addPreference(this)
        }
        AswAdapterManagePlayIntegrityApi.addAppListPageLink(
                screen, getText(R.string.app_play_integrity_see_all_apps))

        setPreferenceScreen(screen)
    }

    override fun refreshUi(): Boolean {
        val appInfo = getAppInfo(mPackageName) ?: return false
        val gosPs = GosPackageState.get(mPackageName, mUserId)

        showUsageNotifsSwitch.isChecked = AswBlockPlayIntegrityApi.I.isNotificationEnabled(gosPs)

        blockUsageAttemptsSwitch.isChecked = AswBlockPlayIntegrityApi.I
                .get(requireContext(), mUserId, appInfo, gosPs)

        return true
    }

    private fun isPlayStoreAvailable(): Boolean {
        val appInfo = getAppInfo(PackageId.PLAY_STORE_NAME) ?: return false
        if (!appInfo.enabled) {
            return false
        }
        return appInfo.ext().packageId == PackageId.PLAY_STORE
    }

    private fun getAppInfo(pkgName: String): ApplicationInfo? {
        val pkgManager = requireContext().packageManager
        try {
            return pkgManager.getApplicationInfoAsUser(pkgName, 0, mUserId)
        } catch (e: NameNotFoundException) {
            return null
        }
    }

    override fun createDialog(id: Int, errorCode: Int): AlertDialog? = null

    override fun getMetricsCategory(): Int = METRICS_CATEGORY_UNKNOWN
}
