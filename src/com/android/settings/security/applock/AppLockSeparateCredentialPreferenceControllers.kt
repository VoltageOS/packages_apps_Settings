/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.security.applock

import android.app.Activity
import android.app.AlertDialog
import android.app.AppLockManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat

import com.android.settings.R
import com.android.settings.Utils.SETTINGS_PACKAGE_NAME
import com.android.settings.core.BasePreferenceController
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.password.ConfirmDeviceCredentialActivity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val KEY_USE_SEPARATE_CREDENTIAL = "app_lock_use_separate_credential"
private const val KEY_SEPARATE_CREDENTIAL_TYPE = "app_lock_separate_credential_type"
private const val KEY_RESET_SEPARATE_CREDENTIAL = "app_lock_reset_separate_credential"
private const val KEY_BIOMETRIC_PROMPT_ENABLED = "app_lock_biometric_prompt_enabled"

private fun createConfirmDeviceCredentialIntent(context: Context): Intent {
    val title = context.getString(R.string.app_lock_authentication_dialog_title)
    return Intent().apply {
        setClassName(
            SETTINGS_PACKAGE_NAME,
            ConfirmDeviceCredentialActivity::class.qualifiedName!!
        )
        putExtra(KeyguardManager.EXTRA_TITLE, title)
    }
}

private fun createSetupIntent(context: Context, credentialType: Int): Intent =
    Intent(context, AppLockCredentialActivity::class.java).apply {
        putExtra(AppLockCredentialActivity.EXTRA_MODE, AppLockCredentialActivity.MODE_SETUP)
        putExtra(AppLockCredentialActivity.EXTRA_SEPARATE_CREDENTIAL_TYPE, credentialType)
    }

private fun createVerifyIntent(context: Context): Intent =
    Intent(context, AppLockCredentialActivity::class.java).apply {
        putExtra(AppLockCredentialActivity.EXTRA_MODE, AppLockCredentialActivity.MODE_VERIFY)
    }

private fun showCredentialTypeDialog(
    context: Context,
    onSelected: (Int) -> Unit,
    onCancelled: () -> Unit = {},
) {
    val labels = arrayOf(
        context.getString(R.string.app_lock_credential_type_pin),
        context.getString(R.string.app_lock_credential_type_pattern),
    )
    val values = intArrayOf(
        AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PIN,
        AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PATTERN,
    )
    AlertDialog.Builder(context)
        .setTitle(R.string.app_lock_choose_credential_type_title)
        .setItems(labels) { _, which ->
            onSelected(values[which])
        }
        .setOnCancelListener {
            onCancelled()
        }
        .show()
}

private fun getCredentialTypeSummary(context: Context, credentialType: Int): String =
    when (credentialType) {
        AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PIN ->
            context.getString(R.string.app_lock_credential_type_pin)
        AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PATTERN ->
            context.getString(R.string.app_lock_credential_type_pattern)
        else -> context.getString(R.string.app_lock_credential_type_none)
    }

internal fun DashboardFragment.refreshSeparateCredentialPreferences(appLockManager: AppLockManager) {
    lifecycleScope.launch {
        val fragmentContext = context ?: return@launch
        val enabled = withContext(Dispatchers.IO) { appLockManager.isSeparateCredentialEnabled() }
        val biometricsAllowed = withContext(Dispatchers.IO) { appLockManager.isBiometricsAllowed() }
        preferenceScreen?.findPreference<SwitchPreferenceCompat>(KEY_USE_SEPARATE_CREDENTIAL)?.isChecked =
            enabled
        val typePreference =
            preferenceScreen?.findPreference<Preference>(KEY_SEPARATE_CREDENTIAL_TYPE)
        val resetPreference =
            preferenceScreen?.findPreference<Preference>(KEY_RESET_SEPARATE_CREDENTIAL)
        val biometricPromptPreference =
            preferenceScreen?.findPreference<SwitchPreferenceCompat>(KEY_BIOMETRIC_PROMPT_ENABLED)
        typePreference?.isVisible = enabled
        resetPreference?.isVisible = enabled
        biometricPromptPreference?.isVisible = enabled && biometricsAllowed
        if (enabled) {
            val credentialType = withContext(Dispatchers.IO) {
                appLockManager.getSeparateCredentialType()
            }
            typePreference?.summary = fragmentContext.getString(
                R.string.app_lock_separate_credential_type_summary,
                getCredentialTypeSummary(fragmentContext, credentialType)
            )
        }
        if (enabled && biometricsAllowed) {
            biometricPromptPreference?.isChecked = withContext(Dispatchers.IO) {
                appLockManager.isBiometricPromptEnabled()
            }
        } else {
            biometricPromptPreference?.isChecked = false
        }
    }
}

internal abstract class AppLockSeparateCredentialBaseController(
    context: Context,
    preferenceKey: String,
    protected val host: DashboardFragment,
    protected val coroutineScope: CoroutineScope,
) : BasePreferenceController(context, preferenceKey), LifecycleEventObserver {

    protected val appLockManager = context.getSystemService(AppLockManager::class.java)!!
    protected var preference: Preference? = null

    init {
        host.lifecycle.addObserver(this)
    }

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun onStateChanged(owner: LifecycleOwner, event: Event) {
        if (event == Event.ON_START) {
            preference?.let(::updateState)
        }
    }
}

internal class AppLockSeparateCredentialSwitchPreferenceController(
    context: Context,
    host: DashboardFragment,
    coroutineScope: CoroutineScope,
) : AppLockSeparateCredentialBaseController(
    context,
    KEY_USE_SEPARATE_CREDENTIAL,
    host,
    coroutineScope,
), Preference.OnPreferenceChangeListener {

    private val setupLauncher: ActivityResultLauncher<Intent> =
        host.registerForActivityResult(StartActivityForResult()) {
            host.refreshSeparateCredentialPreferences(appLockManager)
        }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference?.onPreferenceChangeListener = this
    }

    override fun updateState(preference: Preference) {
        if (preference !is SwitchPreferenceCompat) return
        coroutineScope.launch {
            preference.isChecked = withContext(Dispatchers.IO) {
                appLockManager.isSeparateCredentialEnabled()
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val enable = newValue as? Boolean ?: return false
        if (enable) {
            showCredentialTypeDialog(
                host.requireContext(),
                onSelected = { credentialType ->
                    setupLauncher.launch(createSetupIntent(mContext, credentialType))
                },
                onCancelled = {
                    this.preference?.let(::updateState)
                }
            )
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                appLockManager.clearSeparateCredential()
                withContext(Dispatchers.Main) {
                    host.refreshSeparateCredentialPreferences(appLockManager)
                }
            }
        }
        return false
    }
}

internal class AppLockSeparateCredentialTypePreferenceController(
    context: Context,
    host: DashboardFragment,
    coroutineScope: CoroutineScope,
) : AppLockSeparateCredentialBaseController(
    context,
    KEY_SEPARATE_CREDENTIAL_TYPE,
    host,
    coroutineScope,
), Preference.OnPreferenceClickListener {

    private val confirmLauncher: ActivityResultLauncher<Intent> =
        host.registerForActivityResult(StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                launchChangeFlow()
            } else {
                preference?.let(::updateState)
            }
        }

    private val setupLauncher: ActivityResultLauncher<Intent> =
        host.registerForActivityResult(StartActivityForResult()) {
            host.refreshSeparateCredentialPreferences(appLockManager)
        }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference?.onPreferenceClickListener = this
    }

    override fun updateState(preference: Preference) {
        coroutineScope.launch {
            val enabled = withContext(Dispatchers.IO) {
                appLockManager.isSeparateCredentialEnabled()
            }
            preference.isVisible = enabled
            if (!enabled) return@launch
            val type = withContext(Dispatchers.IO) {
                appLockManager.getSeparateCredentialType()
            }
            preference.summary = mContext.getString(
                R.string.app_lock_separate_credential_type_summary,
                getCredentialTypeSummary(mContext, type)
            )
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        confirmLauncher.launch(createVerifyIntent(mContext))
        return true
    }

    private fun launchChangeFlow() {
        showCredentialTypeDialog(
            host.requireContext(),
            onSelected = { credentialType ->
                setupLauncher.launch(createSetupIntent(mContext, credentialType))
            },
            onCancelled = {
                preference?.let(::updateState)
            }
        )
    }
}

internal class AppLockSeparateCredentialResetPreferenceController(
    context: Context,
    host: DashboardFragment,
    coroutineScope: CoroutineScope,
) : AppLockSeparateCredentialBaseController(
    context,
    KEY_RESET_SEPARATE_CREDENTIAL,
    host,
    coroutineScope,
), Preference.OnPreferenceClickListener {

    private val confirmLauncher: ActivityResultLauncher<Intent> =
        host.registerForActivityResult(StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                showCredentialTypeDialog(
                    host.requireContext(),
                    onSelected = { credentialType ->
                        setupLauncher.launch(createSetupIntent(mContext, credentialType))
                    },
                    onCancelled = {
                        preference?.let(::updateState)
                    }
                )
            } else {
                preference?.let(::updateState)
            }
        }

    private val setupLauncher: ActivityResultLauncher<Intent> =
        host.registerForActivityResult(StartActivityForResult()) {
            host.refreshSeparateCredentialPreferences(appLockManager)
        }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference?.onPreferenceClickListener = this
    }

    override fun updateState(preference: Preference) {
        coroutineScope.launch {
            preference.isVisible = withContext(Dispatchers.IO) {
                appLockManager.isSeparateCredentialEnabled()
            }
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        confirmLauncher.launch(createConfirmDeviceCredentialIntent(mContext))
        return true
    }
}
