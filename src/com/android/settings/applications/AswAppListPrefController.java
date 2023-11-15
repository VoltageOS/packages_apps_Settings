package com.android.settings.applications;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.applications.AswAdapter;
import com.android.settings.core.BasePreferenceController;

public abstract class AswAppListPrefController extends BasePreferenceController {

    protected final AswAdapter aswAdapter;

    public AswAppListPrefController(Context context, String preferenceKey, AswAdapter adapter) {
        super(context, preferenceKey);
        aswAdapter = adapter;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        aswAdapter.openAppListPage(preference.getContext());
        return true;
    }
}
