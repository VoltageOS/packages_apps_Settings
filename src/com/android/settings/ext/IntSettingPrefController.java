/*
 * Copyright (C) 2022 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.ext;

import android.content.Context;
import android.ext.settings.IntSetting;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

public abstract class IntSettingPrefController extends AbstractListPreferenceController
        implements ExtSettingPrefController<IntSetting>
{
    private final IntSetting setting;
    private final UserHandle user;

    private final ExtSettingControllerHelper<IntSetting> helper;

    protected IntSettingPrefController(Context ctx, String key, IntSetting setting) {
        this(ctx, key, setting, ctx.getUser());
    }

    protected IntSettingPrefController(Context ctx, String key, IntSetting setting, UserHandle user) {
        super(ctx, key);
        this.setting = setting;
        this.user = user;
        Context ctxForUser = ctx.getUser().equals(user) ? ctx : ctx.createContextAsUser(user, 0);
        helper = new ExtSettingControllerHelper<>(ctxForUser, setting);
    }

    @Override
    public int getAvailabilityStatus() {
        return helper.getAvailabilityStatus();
    }

    @Override
    protected final int getCurrentValue() {
        if (!mContext.getUser().equals(user)) {
            return setting.get(mContext, user.getIdentifier());
        }
        return setting.get(mContext);
    }

    @Override
    protected boolean setValue(int val) {
        if (!mContext.getUser().equals(user)) {
            Context ctxForUser = mContext.createContextAsUser(user, 0);
            return setting.put(ctxForUser, val);
        }
        return setting.put(mContext, val);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        helper.onResume(this);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        helper.onPause(this);
    }

    // called by the setting observer
    @Override
    public void accept(IntSetting intSetting) {
        updatePreference();
    }
}
