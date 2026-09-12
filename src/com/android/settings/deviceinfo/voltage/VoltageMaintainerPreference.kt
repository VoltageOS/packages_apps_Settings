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

package com.android.settings.deviceinfo.voltage

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

class VoltageMaintainerPreference :
    PreferenceMetadata, PreferenceBinding {

    override val key: String
        get() = "voltage_maintainer"

    override val title: Int
        get() = R.string.voltage_maintainer_title

    override val purpose: Int
        get() = title

    override fun createWidget(context: Context): Preference = MaintainerStatusPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)

        val context = preference.context
        val statusPreference = preference as? MaintainerStatusPreference
        preference.isIconSpaceReserved = false

        val buildStatus = getBuildStatus(context)
        val maintainerLine =
            if (buildStatus != context.getString(R.string.unknown)) {
                "$buildStatus by ${context.getString(R.string.voltage_maintainer)}"
            } else {
                context.getString(R.string.unknown)
            }

        if (!buildStatus.equals("OFFICIAL", ignoreCase = true)) {
            statusPreference?.setStatusIcon(0, animate = false)
            preference.summary = maintainerLine
            preference.isCopyingEnabled = false
            return
        }

        val gpgKey = SystemProperties.get(GPG_KEY_PROPERTY, "")
        val gpgUid = SystemProperties.get(GPG_UID_PROPERTY, "")

        if (!TextUtils.isEmpty(gpgKey) && !TextUtils.isEmpty(gpgUid)) {
            statusPreference?.setStatusIcon(R.drawable.ic_gpg_verified_anim, animate = true)
            preference.summary = buildMergedSummary(preference, maintainerLine, gpgKey, gpgUid)
            preference.isCopyingEnabled = false
        } else {
            statusPreference?.setStatusIcon(R.drawable.ic_gpg_tampered_anim, animate = true)
            preference.summary =
                SpannableStringBuilder(maintainerLine)
                    .append("\n")
                    .append(context.getString(R.string.voltage_tampered_build_summary))
            preference.isCopyingEnabled = false
        }
    }

    private fun getBuildStatus(context: Context): String {
        val buildStatus = SystemProperties.get(BUILD_STATUS_PROPERTY, "")
        if (buildStatus.equals("OFFICIAL", ignoreCase = true) ||
            buildStatus.equals("UNOFFICIAL", ignoreCase = true)) {
            return buildStatus
        }
        return context.getString(R.string.unknown)
    }

    private fun buildMergedSummary(
        preference: Preference,
        maintainerLine: CharSequence,
        gpgKey: String,
        gpgUid: String,
    ): CharSequence {
        val summary = SpannableStringBuilder(maintainerLine)
        summary.append("\n")
        if (!preference.context.resources.getBoolean(R.bool.config_show_gpg_uid)) {
            summary.append(gpgKey)
            return summary
        }
        val uid = decodeHexUid(gpgUid)
        val accent = resolveColor(preference, android.R.attr.colorAccent)
        val start = summary.length
        summary.append(uid)
        summary.setSpan(
            ForegroundColorSpan(accent),
            start,
            summary.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        summary.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            summary.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        summary.append("\n")
        summary.append(gpgKey)
        return summary
    }

    private fun decodeHexUid(hexUid: String): String =
        try {
            val builder = StringBuilder()
            var i = 0
            while (i < hexUid.length) {
                builder.append(hexUid.substring(i, i + 2).toInt(16).toChar())
                i += 2
            }
            builder.toString().trim()
        } catch (e: Exception) {
            hexUid
        }

    private fun resolveColor(preference: Preference, attr: Int): Int {
        val value = TypedValue()
        preference.context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) {
            preference.context.getColor(value.resourceId)
        } else {
            value.data
        }
    }

    private class MaintainerStatusPreference(context: Context) : Preference(context) {
        private var statusIconRes: Int = 0
        private var animateIcon: Boolean = false

        private val holdHandler = Handler(Looper.getMainLooper())
        private var holdFired = false
        private var longPressed = false
        private var boundRow: View? = null
        private val launchEgg = Runnable {
            holdFired = true
            val ctx = context
            if (ActivityManager.isUserAMonkey()) return@Runnable
            boundRow?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val intent = Intent(Intent.ACTION_MAIN)
                .setComponent(
                    ComponentName(
                        EASTER_EGG_PACKAGE,
                        EASTER_EGG_PACKAGE + ".MainActivity",
                    ),
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                ctx.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(ctx, R.string.voltage_crimson_missing, Toast.LENGTH_SHORT).show()
            }
        }

        init {
            widgetLayoutResource = R.layout.voltage_gpg_widget
        }

        fun setStatusIcon(resId: Int, animate: Boolean) {
            if (statusIconRes != resId || animateIcon != animate) {
                statusIconRes = resId
                animateIcon = animate
                notifyChanged()
            }
        }

        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            val row = holder.itemView
            boundRow = row
            holdHandler.removeCallbacks(launchEgg)
            holdFired = false
            longPressed = false
            row.setOnLongClickListener {
                longPressed = true
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                holdHandler.postDelayed(launchEgg, HOLD_TO_LAUNCH_MS)
                true
            }
            row.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP ||
                    event.action == MotionEvent.ACTION_CANCEL
                ) {
                    holdHandler.removeCallbacks(launchEgg)
                    if (!holdFired && longPressed) copySummary()
                    longPressed = false
                }
                false
            }
            val icon = holder.findViewById(R.id.gpg_status_icon)
            if (icon !is ImageView) {
                return
            }
            if (statusIconRes == 0) {
                icon.setImageDrawable(null)
                icon.visibility = View.GONE
                return
            }
            icon.visibility = View.VISIBLE
            icon.setImageResource(statusIconRes)
            val drawable = icon.drawable
            if (animateIcon && drawable is Animatable && !drawable.isRunning) {
                drawable.start()
            }
        }

        private fun copySummary() {
            val text = summary?.toString().orEmpty()
            if (text.isEmpty()) return
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return
            cm.setPrimaryClip(ClipData.newPlainText("build_status", text))
            Toast.makeText(context, context.getString(android.R.string.copy), Toast.LENGTH_SHORT)
                .show()
        }
    }

    companion object {
        const val BUILD_STATUS_PROPERTY: String = "ro.voltage.build.status"
        const val GPG_KEY_PROPERTY: String = "ro.voltage.maintainer.gpg_key"
        const val GPG_UID_PROPERTY: String = "ro.voltage.maintainer.gpg_uid"

        private const val HOLD_TO_LAUNCH_MS = 5000L
        private const val EASTER_EGG_PACKAGE = "com.voltage.os.easteregg"
    }
}
