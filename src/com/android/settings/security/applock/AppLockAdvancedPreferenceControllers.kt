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

import android.app.AppLockManager
import android.app.AppLockManager.APP_LOCK_RELOCK_BEHAVIOR_TIMEOUT
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat

import com.android.settings.core.BasePreferenceController
import com.android.settings.dashboard.DashboardFragment

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val KEY_TIMEOUT = "app_lock_timeout"
private const val KEY_BIOMETRIC_PROMPT = "app_lock_biometric_prompt_enabled"

class AppLockRelockBehaviorPreferenceController(
    context: Context,
    key: String,
) : BasePreferenceController(context, key), Preference.OnPreferenceChangeListener {

    private val appLockManager = context.getSystemService(AppLockManager::class.java)!!
    private var timeoutPreference: Preference? = null

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        screen.findPreference<Preference>(preferenceKey)?.onPreferenceChangeListener = this
        timeoutPreference = screen.findPreference(KEY_TIMEOUT)
    }

    override fun updateState(preference: Preference) {
        val relockBehavior = appLockManager.relockBehavior
        (preference as ListPreference).value = relockBehavior.toString()
        timeoutPreference?.isVisible = relockBehavior == APP_LOCK_RELOCK_BEHAVIOR_TIMEOUT
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val relockBehavior = (newValue as String).toInt()
        appLockManager.relockBehavior = relockBehavior
        timeoutPreference?.isVisible = relockBehavior == APP_LOCK_RELOCK_BEHAVIOR_TIMEOUT
        return true
    }
}

class AppLockBiometricPromptPreferenceController(
    context: Context,
    private val host: DashboardFragment,
    private val coroutineScope: CoroutineScope,
) : AppLockTogglePreferenceController(context, KEY_BIOMETRIC_PROMPT), LifecycleEventObserver {

    private val appLockManager = context.getSystemService(AppLockManager::class.java)!!
    private val biometricManager = context.getSystemService(BiometricManager::class.java)!!

    private var preference: SwitchPreferenceCompat? = null
    private var isChecked = false

    init {
        host.lifecycle.addObserver(this)
    }

    override fun getAvailabilityStatus(): Int {
        val result = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
        return if (result == BiometricManager.BIOMETRIC_SUCCESS) AVAILABLE else CONDITIONALLY_UNAVAILABLE
    }

    override fun isChecked() = isChecked

    override fun setChecked(checked: Boolean): Boolean {
        if (isChecked == checked) return false
        isChecked = checked
        coroutineScope.launch(Dispatchers.IO) {
            appLockManager.setBiometricPromptEnabled(checked)
            withContext(Dispatchers.Main) {
                host.refreshSeparateCredentialPreferences(appLockManager)
            }
        }
        return true
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference) {
        val switchPreference = preference as? SwitchPreferenceCompat ?: return
        coroutineScope.launch {
            val enabled = withContext(Dispatchers.IO) {
                appLockManager.isSeparateCredentialEnabled()
            }
            val biometricsAllowed = withContext(Dispatchers.IO) {
                appLockManager.isBiometricsAllowed()
            }
            switchPreference.isVisible = enabled && biometricsAllowed
            isChecked = if (enabled && biometricsAllowed) {
                withContext(Dispatchers.IO) { appLockManager.isBiometricPromptEnabled() }
            } else {
                false
            }
            switchPreference.isChecked = isChecked
        }
    }

    override fun onStateChanged(owner: LifecycleOwner, event: Event) {
        if (event == Event.ON_START) {
            preference?.let(::updateState)
        }
    }
}
