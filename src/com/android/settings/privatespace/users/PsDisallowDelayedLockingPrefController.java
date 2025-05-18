package com.android.settings.privatespace.users;

import android.content.Context;
import android.ext.settings.BoolSetting;
import android.ext.settings.ExtSettings;
import android.os.UserHandle;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.ext.BoolSettingPrefController;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

public class PsDisallowDelayedLockingPrefController extends BoolSettingPrefController {

    private static final String TAG = "PsDisallowDelayedLockingPrefCtrl";

    public PsDisallowDelayedLockingPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.DISALLOW_DELAYED_LOCKING_ON_USER_STOP,
                PrivateSpaceMaintainer.getInstance(ctx).getPrivateProfileHandle());
    }

    @Override
    public int getAvailabilityStatus() {
        PrivateSpaceMaintainer privateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(mContext);
        UserHandle privateSpaceUserHandle = privateSpaceMaintainer.getPrivateProfileHandle();
        if (privateSpaceUserHandle == null) {
            Log.w(TAG, "No private space user fetched, treating as unavailable");
            return CONDITIONALLY_UNAVAILABLE;
        }

        return AVAILABLE;
    }
}
