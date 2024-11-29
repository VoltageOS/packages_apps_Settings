package com.android.settings.security;

import android.content.Context;
import android.content.res.Resources;
import android.ext.settings.UsbPortSecurity;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.ext.PortSecurityState;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.ext.IntSettingPrefController;

import java.util.List;

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
                UsbPortSecurity.MODE_DISABLED);
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
                UsbPortSecurity.MODE_ENABLED);
    }

    private void setState(@android.hardware.usb.ext.PortSecurityState int state) {
        var um = mContext.getSystemService(UsbManager.class);
        List<UsbPort> ports = um.getPorts();

        for (UsbPort port : ports) {
            um.setPortSecurityState(port, state, new ResultReceiver(null) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (resultCode != android.hardware.usb.ext.IUsbExt.NO_ERROR) {
                        throw new IllegalStateException("setPortSecurityState failed, " +
                                "resultCode: " + resultCode + ", port: " + port);
                    }
                }
            });
        }
    }

    @Override
    protected boolean setValue(int val) {
        boolean res = super.setValue(val);
        if (!res) {
            return false;
        }

        int pss = switch (val) {
            case UsbPortSecurity.MODE_DISABLED -> PortSecurityState.DISABLED;
            case UsbPortSecurity.MODE_CHARGING_ONLY -> PortSecurityState.CHARGING_ONLY_IMMEDIATE;
            case UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED -> PortSecurityState.ENABLED;
            case UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED_AFU -> PortSecurityState.ENABLED;
            case UsbPortSecurity.MODE_ENABLED -> PortSecurityState.ENABLED;
            default -> throw new IllegalArgumentException(Integer.toString(val));
        };
        setState(pss);
        return true;
    }

    @Override
    protected boolean isCredentialConfirmationRequired() {
        return true;
    }
}
