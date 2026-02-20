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
package com.android.settings.notification

import android.content.Context
import android.provider.Settings.Global.CHARGING_VIBRATION_ENABLED
import com.android.settings.R
import com.android.settings.contract.KEY_CHARGING_VIBRO
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

class ChargingVibroPreference :
    SwitchPreference(CHARGING_VIBRATION_ENABLED, R.string.charging_vibro_title) {

    override fun tags(context: Context) = arrayOf(KEY_CHARGING_VIBRO)

    override fun storage(context: Context) = SettingsGlobalStore.get(context)

    override fun getReadPermissions(context: Context) = SettingsGlobalStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsGlobalStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY
}
