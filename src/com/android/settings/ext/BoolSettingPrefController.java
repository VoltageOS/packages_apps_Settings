/*
 * Copyright (C) 2022 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.ext;

import android.content.Context;
import android.ext.settings.BoolSetting;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.core.BasePreferenceController;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_BOOL_SETTING_FIELD;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_KEY;

public class BoolSettingPrefController extends AbstractTogglePrefController
        implements ExtSettingPrefController<BoolSetting> {
    private final BoolSetting setting;
    private final ExtSettingControllerHelper<BoolSetting> helper;

    public BoolSettingPrefController(Context ctx, String key, BoolSetting setting) {
        this(ctx, key, setting, ctx.getUser());
    }

    public BoolSettingPrefController(Context ctx, String key, BoolSetting setting, UserHandle user) {
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
    public final boolean isChecked() {
        return setting.get(helper.context);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return setting.put(helper.context, isChecked);
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
    public void accept(BoolSetting boolSetting) {
        if (preference != null) {
            updateState(preference);
        }
    }

    // called when PreferenceScreen XML is parsed
    public static void maybeAdd(Context context, Bundle metadata,
                                List<BasePreferenceController> dest) {
        String boolSettingField = metadata.getString(METADATA_BOOL_SETTING_FIELD);
        if (boolSettingField == null) {
            return;
        }
        String[] split = boolSettingField.split(" ");

        BoolSetting boolSetting;
        try {
            Class c = Class.forName(split[0]);
            Field field = c.getField(split[1]);
            boolSetting = (BoolSetting) Objects.requireNonNull(field.get(null));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid BoolSetting field " + boolSettingField);
        }

        String key = Objects.requireNonNull(metadata.getString(METADATA_KEY));

        dest.add(new BoolSettingPrefController(context, key, boolSetting));
    }
}
