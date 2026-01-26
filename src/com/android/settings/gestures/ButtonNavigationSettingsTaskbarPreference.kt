/*
 * Copyright (C) 2026 VoltageOS
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

package com.android.settings.gestures

import android.content.Context
import androidx.preference.SwitchPreference
import com.android.settings.R
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding

class ButtonNavigationSettingsTaskbarPreference(val context: Context) :
    BooleanValuePreference, PreferenceBinding {

    private val store = TaskbarStore(context)

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.navigation_bar_enable_taskbar_title

    override val summary: Int
        get() = R.string.navigation_bar_enable_taskbar_summary

    override fun storage(context: Context): KeyValueStore = store

    override fun createWidget(context: Context) = SwitchPreference(context)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getReadPermissions(context: Context): Permissions? =
        TaskbarStore.readPermissions

    override fun getWritePermissions(context: Context): Permissions? =
        TaskbarStore.writePermissions

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = "enable_taskbar"
    }
}

private class TaskbarStore(context: Context) : 
    KeyValueStore, AbstractKeyedDataObservable<String>(), KeyedObserver<String> {
    
    private val settingsStore = SettingsSystemStore.get(context)

    override fun contains(key: String) = key == ButtonNavigationSettingsTaskbarPreference.KEY

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return settingsStore.getBoolean(key) as T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        settingsStore.setBoolean(key, value as? Boolean)
    }

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(ButtonNavigationSettingsTaskbarPreference.KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(ButtonNavigationSettingsTaskbarPreference.KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(DataChangeReason.UPDATE)
    }

    companion object {
        val readPermissions: Permissions = SettingsSystemStore.getReadPermissions()
        val writePermissions: Permissions = SettingsSystemStore.getWritePermissions()
    }
}
