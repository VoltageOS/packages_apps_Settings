/*
 * Copyright (C) 2025 VoltageOS
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

package com.android.settings.display

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.internal.util.voltage.rotation.RotationController
import com.android.settings.R
import java.util.HashMap

class PerAppRotationSettings: Fragment(R.layout.per_app_rotation_layout) {

    private lateinit var packageManager: PackageManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var packageList: List<PackageInfo>
    private lateinit var rotationController: RotationController
    private lateinit var rotationEntries: Array<String>
    private lateinit var launcherPackageNames: Set<String>

    private var searchText = ""
    private var showSystem = false
    private var optionsMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().title = getString(R.string.per_app_rotation_title)
        packageManager = requireContext().packageManager
        packageList = packageManager.getInstalledPackages(PackageManager.MATCH_ANY_USER)
        rotationController = RotationController(requireContext())
        rotationEntries = resources.getStringArray(R.array.per_app_rotation_entries)

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val launcherApps = packageManager.queryIntentActivities(mainIntent, 0)
        launcherPackageNames = launcherApps.map { it.activityInfo.packageName }.toSet()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AppListAdapter()
        recyclerView = view.findViewById<RecyclerView>(R.id.apps_list)!!.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PerAppRotationSettings.adapter
        }
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.per_app_rotation_menu, menu)
        optionsMenu = menu
        updateOptionsMenu()

        val searchItem = menu.findItem(R.id.search) ?: return
        val searchView = searchItem.actionView as? SearchView

        searchView?.apply {
            queryHint = getString(R.string.search_apps)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false
                override fun onQueryTextChange(newText: String): Boolean {
                    searchText = newText
                    refreshList()
                    return true
                }
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.show_system, R.id.hide_system -> {
                showSystem = !showSystem
                refreshList()
                updateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateOptionsMenu() {
        optionsMenu?.findItem(R.id.show_system)?.isVisible = !showSystem
        optionsMenu?.findItem(R.id.hide_system)?.isVisible = showSystem
    }

    private fun refreshList() {
        val list = packageList.filter {
            launcherPackageNames.contains(it.packageName)
        }.filter {
            if (showSystem) true else !it.applicationInfo!!.isSystemApp()
        }.filter {
            getLabel(it).contains(searchText, true)
        }.sortedWith { a, b ->
            getLabel(a).compareTo(getLabel(b))
        }
        adapter.submitList(list.map { appInfoFromPackageInfo(it) })
    }

    private fun getLabel(packageInfo: PackageInfo) =
        packageInfo.applicationInfo!!.loadLabel(packageManager).toString()

    private fun appInfoFromPackageInfo(packageInfo: PackageInfo) =
        AppInfo(
            packageInfo.packageName,
            getLabel(packageInfo),
            packageInfo.applicationInfo!!.loadIcon(packageManager)
        )

    private fun showRotationDialog(item: AppInfo) {
        val currentRotation = rotationController.getRotationForApp(item.packageName)

        AlertDialog.Builder(requireActivity())
            .setTitle(item.label)
            .setSingleChoiceItems(R.array.per_app_rotation_entries, currentRotation) { dialog, which ->
                setRotation(item.packageName, which)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setRotation(packageName: String, rotation: Int) {
        if (rotation < RotationController.ROTATION_DEFAULT || rotation > RotationController.ROTATION_FULL_SENSOR) {
            return
        }
        val newSettingsString = buildSettingsString(packageName, rotation)
        Settings.System.putString(context?.contentResolver, Settings.System.PER_APP_ROTATION, newSettingsString)
        refreshList()
    }

    private fun buildSettingsString(packageName: String, rotation: Int): String {
        val currentSetting = Settings.System.getString(context?.contentResolver, Settings.System.PER_APP_ROTATION)
        val rotationMap = HashMap<String, Int>()
        if (!TextUtils.isEmpty(currentSetting)) {
            val entries = currentSetting.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (entry in entries) {
                if (TextUtils.isEmpty(entry)) continue
                val pair = entry.split("=".toRegex(), 2).toTypedArray()
                if (pair.size == 2 && !TextUtils.isEmpty(pair[0])) {
                    try {
                        val value = Integer.parseInt(pair[1])
                        if (value >= RotationController.ROTATION_DEFAULT && value <= RotationController.ROTATION_FULL_SENSOR) {
                            rotationMap[pair[0]] = value
                        }
                    } catch (e: NumberFormatException) {
                        // ignore
                    }
                }
            }
        }

        if (rotation == RotationController.ROTATION_DEFAULT) {
            rotationMap.remove(packageName)
        } else {
            rotationMap[packageName] = rotation
        }

        return rotationMap.entries.joinToString(separator = ",") { (key, value) -> "$key=$value" }
    }

    private inner class AppListAdapter : ListAdapter<AppInfo, AppListViewHolder>(itemCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AppListViewHolder(
                layoutInflater.inflate(
                    R.layout.per_app_rotation_list_item, parent, false
                )
            )

        override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
            val item = getItem(position)
            holder.label.text = item.label
            holder.icon.setImageDrawable(item.icon)

            val currentRotation = rotationController.getRotationForApp(item.packageName)
            holder.summary.text = rotationEntries[currentRotation]

            holder.itemView.setOnClickListener {
                showRotationDialog(item)
            }
        }
    }

    private class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val label: TextView = itemView.findViewById(R.id.label)
        val summary: TextView = itemView.findViewById(R.id.summary)
    }

    private data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable,
    )

    companion object {
        private val itemCallback = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                oldInfo.packageName == newInfo.packageName

            override fun areContentsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                oldInfo == newInfo
        }
    }
}
