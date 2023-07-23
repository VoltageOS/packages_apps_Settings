package com.android.settings.users;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UserRestrictions {

    @NonNull
    private final UserManager userManager;
    @NonNull
    public final UserInfo userInfo;

    @Nullable
    public static UserRestrictions createInstance(@NonNull Context ctx, int userId) {
        UserManager userManager = ctx.getSystemService(UserManager.class);
        if (userManager == null) {
            return null;
        }

        return createInstance(userManager, userId);
    }

    @Nullable
    public static UserRestrictions createInstance(@NonNull UserManager userManager, int userId) {
        UserInfo userInfo = userManager.getUserInfo(userId);
        if (userInfo == null) {
            return null;
        }

        return new UserRestrictions(userManager, userInfo);
    }

    @NonNull
    public static UserRestrictions createInstance(@NonNull UserManager userManager, @NonNull UserInfo userInfo) {
        return new UserRestrictions(userManager, userInfo);
    }

    private UserRestrictions(@NonNull UserManager userManager, @NonNull UserInfo userInfo) {
        this.userManager = userManager;
        this.userInfo = userInfo;
    }

    public boolean isSet(String restrictionKey) {
        final boolean isSetFromUser = userManager.hasUserRestriction(restrictionKey, userInfo.getUserHandle());
        if (userInfo.isGuest()) {
            return isSetFromUser || userManager.getDefaultGuestRestrictions().getBoolean(restrictionKey);
        }

        return isSetFromUser;
    }

    public void set(String restrictionKey, boolean enableRestriction) {
        if (userInfo.isGuest()) {
            Bundle defaultGuestRestrictions = userManager.getDefaultGuestRestrictions();
            defaultGuestRestrictions.putBoolean(restrictionKey, enableRestriction);
            userManager.setDefaultGuestRestrictions(defaultGuestRestrictions);
        } else {
            userManager.setUserRestriction(restrictionKey, enableRestriction, userInfo.getUserHandle());
        }
    }
}
