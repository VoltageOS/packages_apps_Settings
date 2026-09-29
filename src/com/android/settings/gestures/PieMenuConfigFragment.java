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

package com.android.settings.gestures;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PieMenuConfigFragment extends SettingsPreferenceFragment {
    private String mPieKey;
    private PreferenceScreen mScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mPieKey = args != null ? args.getString("pie_key") : null;
        if (!isValidKey(mPieKey)) {
            finish();
            return;
        }
        mScreen = getPreferenceManager().createPreferenceScreen(getContext());
        setPreferenceScreen(mScreen);
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VOLTAGE;
    }

    private boolean isValidKey(String key) {
        return Settings.System.LEFT_LONG_BACK_SWIPE_PIE_ITEMS.equals(key)
                || Settings.System.RIGHT_LONG_BACK_SWIPE_PIE_ITEMS.equals(key)
                || Settings.System.LEFT_VERTICAL_BACK_SWIPE_PIE_ITEMS.equals(key)
                || Settings.System.RIGHT_VERTICAL_BACK_SWIPE_PIE_ITEMS.equals(key);
    }

    private List<String> getSlots() {
        String raw = Settings.System.getStringForUser(getContentResolver(), mPieKey,
                UserHandle.USER_CURRENT);
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        String[] parts = raw.split(";", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null && !parts[i].isEmpty()) {
                out.add(parts[i]);
            }
        }
        return out;
    }

    private void saveSlots(List<String> slots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size() && i < 5; i++) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(slots.get(i));
        }
        Settings.System.putStringForUser(getContentResolver(), mPieKey, sb.toString(),
                UserHandle.USER_CURRENT);
        refresh();
    }

    private void refresh() {
        if (mScreen == null) {
            return;
        }
        mScreen.removeAll();
        List<String> slots = getSlots();
        for (int i = 0; i < slots.size(); i++) {
            String slot = slots.get(i);
            Preference p = new Preference(getContext());
            p.setTitle(slotTitle(slot));
            p.setSummary(slot);
            p.setIconSpaceReserved(true);
            final int index = i;
            p.setOnPreferenceClickListener(pref -> {
                showSlotDialog(index);
                return true;
            });
            mScreen.addPreference(p);
            loadSlotIcon(p, slot);
        }
        if (slots.isEmpty()) {
            Preference empty = new Preference(getContext());
            empty.setTitle(getString(R.string.pie_menu_empty_warning));
            empty.setSelectable(false);
            mScreen.addPreference(empty);
        }
        if (slots.size() < 5) {
            Preference addApp = new Preference(getContext());
            addApp.setTitle(getString(R.string.pie_menu_add_app));
            addApp.setIconSpaceReserved(true);
            addApp.setOnPreferenceClickListener(pref -> {
                showAppPicker();
                return true;
            });
            mScreen.addPreference(addApp);
            Preference addAction = new Preference(getContext());
            addAction.setTitle(getString(R.string.pie_menu_add_action));
            addAction.setIconSpaceReserved(true);
            addAction.setOnPreferenceClickListener(pref -> {
                showActionPicker();
                return true;
            });
            mScreen.addPreference(addAction);
        }
        Preference hint = new Preference(getContext());
        hint.setTitle(getString(R.string.pie_menu_hint));
        hint.setSelectable(false);
        mScreen.addPreference(hint);
    }

    private void loadSlotIcon(Preference pref, String slot) {
        String pkg = null;
        String cls = null;
        try {
            String[] parts = slot.split(":", 3);
            if (parts.length < 2) {
                return;
            }
            if ("app".equals(parts[0])) {
                pkg = parts[1];
            } else if ("activity".equals(parts[0])) {
                String[] comp = parts[1].split("/", 2);
                if (comp.length != 2) {
                    return;
                }
                pkg = comp[0];
                cls = comp[1];
            } else {
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (pkg == null || pkg.isEmpty()) {
            return;
        }
        final String fPkg = pkg;
        final String fCls = cls;
        new Thread(() -> {
            if (getContext() == null) {
                return;
            }
            android.graphics.drawable.Drawable icon = null;
            try {
                PackageManager pm = getContext().getPackageManager();
                if (fCls != null) {
                    try {
                        icon = pm.getActivityIcon(new ComponentName(fPkg, fCls));
                    } catch (Exception e) {
                        icon = pm.getApplicationIcon(fPkg);
                    }
                } else {
                    icon = pm.getApplicationIcon(fPkg);
                }
            } catch (Exception e) {
            }
            if (icon == null || getActivity() == null) {
                return;
            }
            final android.graphics.drawable.Drawable fIcon = icon;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                pref.setIcon(fIcon);
            });
        }).start();
    }

    private String slotTitle(String slot) {
        try {
            String[] parts = slot.split(":", 3);
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                return parts[2];
            }
            if (parts.length >= 2) {
                return parts[1];
            }
        } catch (Exception e) {
        }
        return slot;
    }

    private void showSlotDialog(int index) {
        List<String> slots = getSlots();
        if (index < 0 || index >= slots.size()) {
            return;
        }
        String[] opts = new String[]{
                getString(R.string.pie_menu_move_up),
                getString(R.string.pie_menu_move_down),
                getString(R.string.pie_menu_remove)};
        new AlertDialog.Builder(getContext())
                .setTitle(slotTitle(slots.get(index)))
                .setItems(opts, (d, which) -> {
                    List<String> cur = getSlots();
                    if (index < 0 || index >= cur.size()) {
                        return;
                    }
                    if (which == 2) {
                        cur.remove(index);
                        saveSlots(cur);
                    } else if (which == 0 && index > 0) {
                        Collections.swap(cur, index, index - 1);
                        saveSlots(cur);
                    } else if (which == 1 && index < cur.size() - 1) {
                        Collections.swap(cur, index, index + 1);
                        saveSlots(cur);
                    }
                })
                .show();
    }

    private void showAppPicker() {
        new Thread(() -> {
            PackageManager pm = getContext().getPackageManager();
            List<ApplicationInfo> all = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<ApplicationInfo> apps = new ArrayList<>();
            for (int i = 0; i < all.size(); i++) {
                ApplicationInfo ai = all.get(i);
                try {
                    if (pm.getLaunchIntentForPackage(ai.packageName) != null) {
                        apps.add(ai);
                    }
                } catch (Exception e) {
                }
            }
            Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(pm));
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < apps.size(); i++) {
                try {
                    labels.add(apps.get(i).loadLabel(pm).toString());
                } catch (Exception e) {
                    labels.add(apps.get(i).packageName);
                }
            }
            String[] arr = labels.toArray(new String[0]);
            getActivity().runOnUiThread(() -> {
                new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.pie_menu_add_app))
                        .setItems(arr, (d, which) -> {
                            if (which < 0 || which >= apps.size()) {
                                return;
                            }
                            List<String> cur = getSlots();
                            if (cur.size() >= 5) {
                                return;
                            }
                            cur.add("app:" + apps.get(which).packageName + ":"
                                    + sanitize(arr[which]));
                            saveSlots(cur);
                        })
                        .show();
            });
        }).start();
    }

    private void showActionPicker() {
        String[] entries = getResources().getStringArray(R.array.swipe_actions_values);
        String[] names = getResources().getStringArray(R.array.swipe_actions_entries);
        List<String> entryList = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        for (int i = 0; i < entries.length && i < names.length; i++) {
            if ("18".equals(entries[i]) || "0".equals(entries[i])) {
                continue;
            }
            valueList.add(entries[i]);
            entryList.add(names[i]);
        }
        String[] arr = entryList.toArray(new String[0]);
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.pie_menu_add_action))
                .setItems(arr, (d, which) -> {
                    if (which < 0 || which >= valueList.size()) {
                        return;
                    }
                    List<String> cur = getSlots();
                    if (cur.size() >= 5) {
                        return;
                    }
                    cur.add("action:" + valueList.get(which) + ":" + sanitize(arr[which]));
                    saveSlots(cur);
                })
                .show();
    }

    private String sanitize(String s) {
        if (s == null) {
            return "";
        }
        String r = s.replace(";", "").replace("\n", "").replace("\r", "").trim();
        if (r.length() > 40) {
            r = r.substring(0, 40);
        }
        return r;
    }
}
