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
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

class ButtonNavigationSettingsLayoutPreference(context: Context) :
    PreferenceMetadata, PreferenceBinding {

    private val store = NavbarLayoutStore(context)

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.navbar_layout_title

    override val summary: Int
        get() = R.string.navbar_layout_title

    override fun createWidget(context: Context) = ListPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        val listPref = preference as ListPreference
        listPref.title = preference.context.getString(title)
        listPref.setEntries(R.array.navbar_layout_entries)
        listPref.setEntryValues(R.array.navbar_layout_values)

        val currentValue = store.getValue(KEY, String::class.java)
        if (currentValue != null) {
            listPref.value = currentValue
        } else {
            listPref.setValueIndex(0)
        }

        listPref.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        listPref.setOnPreferenceChangeListener { _, newValue ->
            store.setValue(KEY, String::class.java, newValue as String)
            true
        }
    }

    companion object {
        const val KEY = "navbar_layout_mode"
    }
}

@Suppress("UNCHECKED_CAST")
private class NavbarLayoutStore(val context: Context) :
    KeyValueStore, AbstractKeyedDataObservable<String>(), KeyedObserver<String> {

    private val settingsStore: SettingsStore = SettingsSecureStore.get(context)

    override fun contains(key: String) = key == ButtonNavigationSettingsLayoutPreference.KEY

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (valueType == String::class.java) {
            val intVal = settingsStore.getInt(key) ?: 0
            return intVal.toString() as T
        }
        return null
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value is String) {
            settingsStore.setInt(key, value.toIntOrNull() ?: 0)
        }
    }

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(ButtonNavigationSettingsLayoutPreference.KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(ButtonNavigationSettingsLayoutPreference.KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(DataChangeReason.UPDATE)
    }
}
