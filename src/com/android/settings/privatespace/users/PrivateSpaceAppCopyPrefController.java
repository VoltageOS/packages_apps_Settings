
package com.android.settings.privatespace.users;

import static com.android.settings.dashboard.DashboardFragment.CATEGORY;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.privatespace.PrivateSpaceMaintainer;
import com.android.settings.users.AppCopyFragmentHelperExt;
import com.android.settings.users.UserRestrictions;

public class PrivateSpaceAppCopyPrefController extends BasePreferenceController {

    private static final String TAG = "PrivateSpaceAppCopyPrefCtrl";

    private final PrivateSpaceMaintainer privateSpaceMaintainer;

    public PrivateSpaceAppCopyPrefController(Context ctx, String key) {
        super(ctx, key);
        privateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(ctx);
    }

    @Override
    public int getAvailabilityStatus() {
        UserHandle privateSpaceUserHandle = privateSpaceMaintainer.getPrivateProfileHandle();
        if (privateSpaceUserHandle == null) {
            Log.w(getLogTag(), "No private space user fetched, treating as unavailable");
            return CONDITIONALLY_UNAVAILABLE;
        }

        return AVAILABLE;
    }

    private String getLogTag() {
        return TAG;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setVisible(isAvailable());
        UserRestrictions userRestrictions = getUserRestrictions();
        if (userRestrictions != null) {
            preference.setEnabled(!userRestrictions.isSet(UserManager.DISALLOW_INSTALL_APPS));
        }
    }

    private UserRestrictions getUserRestrictions() {
        UserHandle privateSpaceUserHandle = privateSpaceMaintainer.getPrivateProfileHandle();
        if (privateSpaceUserHandle == null) {
            return null;
        }

        return UserRestrictions.createInstance(mContext, privateSpaceUserHandle.getIdentifier());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        UserHandle privateSpaceUserHandle = privateSpaceMaintainer.getPrivateProfileHandle();
        if (privateSpaceUserHandle == null) {
            return false;
        }

        if (privateSpaceMaintainer.isPrivateSpaceLocked()) {
            return false;
        }

        String destName = preference.getFragment();
        if (destName == null || destName.isEmpty()) {
            return false;
        }

        boolean forPrivateSpace = true;
        boolean showUserAppsOnly = true;
        final Bundle args = AppCopyFragmentHelperExt.appendArgs(preference.getExtras(),
                privateSpaceUserHandle.getIdentifier(), forPrivateSpace, showUserAppsOnly);

        new SubSettingLauncher(preference.getContext())
                .setDestination(destName)
                .setSourceMetricsCategory(args.getInt(CATEGORY, SettingsEnums.PAGE_UNKNOWN))
                .setArguments(args)
                .launch();
        return true;
    }
}
