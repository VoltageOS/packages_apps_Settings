/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.display.darkmode

import android.content.Context
import android.content.om.OverlayIdentifier
import android.content.om.OverlayManager
import android.content.om.OverlayManagerTransaction
import android.content.res.Configuration
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import com.android.settings.R
import androidx.preference.SwitchPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding
import java.util.concurrent.Executor

private const val BLACK_THEME_KEY = "berry_black_theme"
private const val BLACK_THEME_TAG = "DarkModeBlackTheme"
private const val BLACK_THEME_OVERLAY_PACKAGE = "com.android.system.theme.black"
private const val BLACK_THEME_DISABLE_KEY = "android:neutral"

class DarkModeBlackThemePreference(
    private val context: Context,
    private val darkModeStorage: DarkModeStorage
) : PreferenceMetadata, BooleanValuePreference, PreferenceBinding {

    private val storage = BlackThemeStorage(context)

    override val key: String
        get() = BLACK_THEME_KEY

    override val title: Int
        get() = R.string.berry_black_theme_title

    override val summary: Int
        get() = R.string.berry_black_theme_summary

    override fun storage(context: Context): KeyValueStore = storage

    override fun getReadPermissions(context: Context) = DarkModeStorage.getReadPermissions()

    override fun getWritePermissions(context: Context) = DarkModeStorage.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun isEnabled(context: Context): Boolean {
        return darkModeStorage.getBoolean(DarkModeMainSwitchPreference.KEY) ?: false
    }

    override fun createWidget(context: Context) = SwitchPreference(context)
}

class BlackThemeStorage(private val context: Context) : KeyValueStore {
    private val overlayManager: OverlayManager? =
        context.getSystemService(OverlayManager::class.java)

    override fun contains(key: String): Boolean = key == BLACK_THEME_KEY

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (key != BLACK_THEME_KEY || valueType != Boolean::class.javaObjectType) return null
        val setting = Settings.Secure.getIntForUser(
            context.contentResolver,
            BLACK_THEME_KEY,
            0,
            UserHandle.USER_CURRENT
        )
        return (setting == 1) as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (key != BLACK_THEME_KEY || 
            valueType != Boolean::class.javaObjectType || 
            value !is Boolean) {
            return
        }

        Settings.Secure.putIntForUser(
            context.contentResolver,
            BLACK_THEME_KEY,
            if (value) 1 else 0,
            UserHandle.USER_CURRENT
        )

        if (overlayManager == null) {
            Log.e(BLACK_THEME_TAG, "OverlayManager is null")
            return
        }

        try {
            val transaction = OverlayManagerTransaction.Builder()

            transaction.setEnabled(
                getOverlayIdentifier(BLACK_THEME_OVERLAY_PACKAGE),
                value,
                UserHandle.USER_CURRENT
            )

            val isNightMode = (context.resources.configuration.uiMode
                    and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            transaction.setEnabled(
                getOverlayIdentifier(BLACK_THEME_DISABLE_KEY),
                if (isNightMode) !value else true,
                UserHandle.USER_CURRENT
            )

            overlayManager.commit(transaction.build())
            
        } catch (e: Exception) {
            Log.e(BLACK_THEME_TAG, "Failed to toggle black theme overlay", e)
        }
    }

    private fun getOverlayIdentifier(packageName: String): OverlayIdentifier {
        if (packageName.contains(":")) {
            val split = packageName.split(":")
            val pkg = split[0]
            val name = split[1]
            val infos = overlayManager?.getOverlayInfosForTarget(pkg, UserHandle.CURRENT)
            return infos?.find { it.overlayName == name }?.overlayIdentifier
                ?: throw IllegalStateException("Overlay not found: $packageName")
        }
        return overlayManager?.getOverlayInfo(packageName, UserHandle.CURRENT)
            ?.overlayIdentifier
            ?: throw IllegalStateException("Overlay not found: $packageName")
    }

    override fun addObserver(observer: com.android.settingslib.datastore.KeyedObserver<String?>, executor: Executor) = false
    override fun addObserver(key: String, observer: com.android.settingslib.datastore.KeyedObserver<String>, executor: Executor) = false
    override fun notifyChange(reason: Int) {}
    override fun notifyChange(key: String, reason: Int) {}
    override fun removeObserver(observer: com.android.settingslib.datastore.KeyedObserver<String?>) = false
    override fun removeObserver(key: String, observer: com.android.settingslib.datastore.KeyedObserver<String>) = false
}
