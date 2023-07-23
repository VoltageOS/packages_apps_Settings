package com.android.settings.users;

import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.List;

final class UserRestrictions {

    final UserManager userManager;
    final UserInfo userInfo;

    UserRestrictions(UserManager userManager, UserInfo userInfo) {
        this.userManager = userManager;
        this.userInfo = userInfo;
    }

    boolean isSet(String restrictionKey) {
        final boolean isSetFromUser = userManager.hasUserRestriction(restrictionKey, userInfo.getUserHandle());
        if (userInfo.isGuest()) {
            return isSetFromUser || userManager.getDefaultGuestRestrictions().getBoolean(restrictionKey);
        }

        return isSetFromUser;
    }

    void set(String restrictionKey, boolean enableRestriction) {
        if (userInfo.isGuest()) {
            Bundle defaultGuestRestrictions = userManager.getDefaultGuestRestrictions();
            defaultGuestRestrictions.putBoolean(restrictionKey, enableRestriction);
            userManager.setDefaultGuestRestrictions(defaultGuestRestrictions);
        } else {
            userManager.setUserRestriction(restrictionKey, enableRestriction, userInfo.getUserHandle());
        }
    }
}
