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
    private final ExtSettingControllerHelper<IntSetting> helper;

    protected IntSettingPrefController(Context ctx, String key, IntSetting setting) {
        this(ctx, key, setting, ctx.getUser());
    }

    protected IntSettingPrefController(Context ctx, String key, IntSetting setting, UserHandle user) {
        super(ctx, key);
        this.setting = setting;
        Context userContext = ctx.getUser().equals(user) ? ctx : ctx.createContextAsUser(user, 0);
        helper = new ExtSettingControllerHelper<>(userContext, setting);
    }

    @Override
    public int getAvailabilityStatus() {
        return helper.getAvailabilityStatus();
    }

    @Override
    protected final int getCurrentValue() {
        return setting.get(helper.context);
    }

    @Override
    protected boolean setValue(int val) {
        return setting.put(helper.context, val);
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
