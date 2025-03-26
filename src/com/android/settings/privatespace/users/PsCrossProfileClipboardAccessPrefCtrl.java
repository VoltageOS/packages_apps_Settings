package com.android.settings.privatespace.users;

import android.content.Context;
import android.ext.settings.IntSetting;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;
import com.android.settings.ext.RadioButtonPickerFragment2;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

import static android.ext.settings.CrossProfileClipboardAccessSettings.*;

public class PsCrossProfileClipboardAccessPrefCtrl extends IntSettingPrefController {


    public PsCrossProfileClipboardAccessPrefCtrl(Context ctx, String key) {
        super(ctx, key, CROSS_PROFILE_CLIPBOARD_ACCESS_SETTINGS,
                PrivateSpaceMaintainer.getInstance(ctx).getPrivateProfileHandle());
    }

    @Override
    public int getAvailabilityStatus() {
        int r = super.getAvailabilityStatus();
        if (r == AVAILABLE) {
            boolean hasPrivateProfile =
                    PrivateSpaceMaintainer.getInstance(mContext).getPrivateProfileHandle() != null;
            return hasPrivateProfile ? AVAILABLE : DISABLED_FOR_USER;
        }

        return r;
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.cross_profile_clipboard_access_allow_defaults_title,
                R.string.cross_profile_clipboard_access_allow_defaults_summary,
                FOLLOW_DEFAULT);
        entries.add(R.string.cross_profile_clipboard_access_allow_import_defaults_title,
                R.string.cross_profile_clipboard_access_allow_import_defaults_summary,
                ALLOW_IMPORT_DEFAULTS_ONLY);
        entries.add(R.string.cross_profile_clipboard_access_allow_export_defaults_title,
                R.string.cross_profile_clipboard_access_allow_export_defaults_summary,
                ALLOW_EXPORT_DEFAULTS_ONLY);
        entries.add(R.string.cross_profile_clipboard_access_disallow_title,
                R.string.cross_profile_clipboard_access_disallow_summary,
                BLOCK);
    }

    @Override
    public void addPrefsBeforeList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.cross_profile_clipboard_access_footer);
    }
}
