package com.android.settings.security;

import android.content.Context;
import android.content.res.Resources;
import android.ext.settings.UsbPortSecurity;
import android.hardware.usb.UsbManager;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;

import static java.util.Objects.requireNonNull;

public class UsbPortSecurityPrefController extends IntSettingPrefController {
    private final boolean appliesToPogoPins;

    public UsbPortSecurityPrefController(Context ctx, String key) {
        super(ctx, key, UsbPortSecurity.MODE_SETTING);

        appliesToPogoPins = ctx.getResources().getBoolean(
                com.android.internal.R.bool.config_usb_port_security_applies_to_pogo_pins);
    }

    @Override
    public int getAvailabilityStatus() {
        String prefKey = getPreferenceKey();
        if (appliesToPogoPins) {
            if ("usbc_port".equals(prefKey)) {
                return UNSUPPORTED_ON_DEVICE;
            }
        } else {
            if ("usbc_port_and_pogo_pins".equals(prefKey)) {
                return UNSUPPORTED_ON_DEVICE;
            }
        }

        int res = super.getAvailabilityStatus();
        if (res == AVAILABLE) {
            int config = com.android.internal.R.bool.config_usbPortSecuritySupported;
            if (!mContext.getResources().getBoolean(config)) {
                res = UNSUPPORTED_ON_DEVICE;
            }
        }
        return res;
    }

    @Override
    protected void getEntries(Entries entries) {
        boolean pogo = appliesToPogoPins;

        Resources res = mContext.getResources();
        entries.add(pogo ? R.string.usbc_port_and_pogo_pins_off_title : R.string.usbc_port_off_title,
                pogo ? R.string.usbc_port_and_pogo_pins_off_summary : R.string.usbc_port_off_summary,
                UsbPortSecurity.MODE_ALL_PORTS_DISABLED);
        entries.add(R.string.usbc_port_charging_only_title,
                pogo ? R.string.usbc_port_and_pogo_pins_charging_only_summary : R.string.usbc_port_charging_only_summary,
                UsbPortSecurity.MODE_CHARGING_ONLY);

        String title = res.getString(R.string.usbc_port_charging_only_when_locked_title);
        CharSequence summary = res.getText(pogo ?
                R.string.usbc_port_and_pogo_pins_charging_only_when_locked_summary :
                R.string.usbc_port_charging_only_when_locked_summary);
        entries.add(title, summary,
                UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED);

        CharSequence titleAfu = res.getText(R.string.usbc_port_charging_only_when_locked_afu_title);
        String summaryAfu = res.getString(R.string.usbc_port_charging_only_when_locked_afu_summary, title);
        entries.add(titleAfu, summaryAfu,
                UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED_AFU);

        entries.add(R.string.usbc_port_on_title, R.string.usbc_port_on_summary,
                UsbPortSecurity.MODE_ALL_PORTS_ENABLED);
    }

    @Override
    protected boolean setValue(int val) {
        var usbManager = requireNonNull(mContext.getSystemService(UsbManager.class));
        usbManager.updatePortSecuritySetting(val);
        return true;
    }

    @Override
    protected boolean isCredentialConfirmationRequired() {
        return true;
    }
}
