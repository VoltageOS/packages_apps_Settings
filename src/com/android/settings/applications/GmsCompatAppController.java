package com.android.settings.applications;

import android.annotation.Nullable;
import android.app.compat.gms.GmsCompat;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.gmscompat.GmsCompatApp;
import com.android.internal.gmscompat.GmsInfo;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

public class GmsCompatAppController extends BasePreferenceController {

    public GmsCompatAppController(Context context, String key) {
        super(context, key);
    }

    @Nullable
    private UserHandle getUser() {
        if ("sandboxed_google_play_private_space".equals(getPreferenceKey())) {
            return PrivateSpaceMaintainer.getInstance(mContext).getPrivateProfileHandle();
        }

        UserHandle workProfileUser = getWorkProfileUser();
        if (workProfileUser != null) {
            return workProfileUser;
        }

        return mContext.getUser();
    }

    @Override
    public int getAvailabilityStatus() {
        UserHandle user = getUser();
        if (user == null) {
            return DISABLED_FOR_USER;
        }

        return GmsCompat.isEnabledFor(GmsInfo.PACKAGE_GMS_CORE, user.getIdentifier()) ?
                AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        UserHandle user = getUser();
        if (user == null) {
            throw new IllegalStateException("getUser() returned null, key: " + getPreferenceKey());
        }

        Intent intent = new Intent(GmsCompatApp.PKG_NAME + ".SETTINGS_LINK");
        intent.setPackage(GmsCompatApp.PKG_NAME);
        mContext.startActivityAsUser(intent, user);
        return true;
    }
}
