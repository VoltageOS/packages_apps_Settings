package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.SpacePreference;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.Collections;
import java.util.List;

public abstract class RadioButtonAppInfoFragment extends AppInfoWithHeader implements SelectorWithWidgetPreference.OnClickListener {

    public static class Entry {
        public final int id;
        public CharSequence title;
        @Nullable
        public CharSequence summary;
        public boolean isChecked;
        public boolean isEnabled;

        public Entry(int id, CharSequence title, @Nullable CharSequence summary, boolean isChecked, boolean isEnabled) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.isChecked = isChecked;
            this.isEnabled = isEnabled;
        }
    }

    public static Entry createEntry(int id, CharSequence title) {
        return new Entry(id, title, null, false, true);
    }

    public Entry createEntry(int id, @StringRes int title) {
        return createEntry(id, getText(title));
    }

    public Entry createEntry(@StringRes int title) {
        return createEntry(title, title);
    }

    public abstract Entry[] getEntries();

    public boolean hasFooter() {
        return true;
    }

    public abstract void updateFooter(FooterPreference fp);

    public void setLearnMoreLink(FooterPreference p, String url) {
        p.setLearnMoreAction(v -> {
            var i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getPrefContext());
        setPreferenceScreen(screen);
        requireActivity().setTitle(getTitle());
    }

    protected abstract CharSequence getTitle();

    @Nullable
    private SelectorWithWidgetPreference[] radioButtons;
    @Nullable
    private FooterPreference footer;

    private Preference[] extraPrefs;

    @Override
    protected boolean refreshUi() {
        Entry[] entries = getEntries();
        int entryCnt = entries.length;

        Context ctx = getPrefContext();
        PreferenceScreen screen = getPreferenceScreen();

        if (radioButtons == null || radioButtons.length != entryCnt) {
            if (radioButtons != null) {
                for (Preference p : radioButtons) {
                    screen.removePreference(p);
                }
            }

            radioButtons = new SelectorWithWidgetPreference[entryCnt];

            for (int i = 0; i < entryCnt; ++i) {
                var p = new SelectorWithWidgetPreference(ctx);
                p.setOnClickListener(this);
                screen.addPreference(p);
                radioButtons[i] = p;
            }
        }

        for (int i = 0; i < entryCnt; ++i) {
            SelectorWithWidgetPreference p = radioButtons[i];
            Entry e = entries[i];
            p.setKey(Integer.toString(e.id));
            p.setTitle(e.title);
            p.setSummary(e.summary);
            p.setEnabled(e.isEnabled);
            p.setChecked(e.isChecked);
        }

        List<PrefModel> extraPrefModels = getExtraPrefModels();

        if (extraPrefs == null) {
            if (extraPrefModels.isEmpty()) {
                extraPrefs = new Preference[0];
            } else {
                var spacer = new SpacePreference(ctx, null);
                int h = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 48f, ctx.getResources().getDisplayMetrics());
                spacer.setHeight(h);
                screen.addPreference(spacer);

                extraPrefs = new Preference[extraPrefModels.size()];
                for (int i = 0; i < extraPrefs.length; ++i) {
                    PrefModel pm = extraPrefModels.get(i);
                    Preference p;
                    if (pm instanceof LinkPrefModel lpm) {
                        p = new Preference(ctx);
                        p.setOnPreferenceClickListener(pref -> {
                            lpm.onClick.run();
                            return true;
                        });
                    } else if (pm instanceof TogglePrefModel tpm) {
                        p = new SwitchPreferenceCompat(ctx);
                        p.setOnPreferenceChangeListener(tpm.onChangeListener);
                    } else {
                        throw new IllegalArgumentException(pm.toString());
                    }
                    p.setKey("extra_pref_" + i);
                    screen.addPreference(p);
                    extraPrefs[i] = p;
                }
            }
        }
        for (int i = 0; i < extraPrefs.length; ++i) {
            PrefModel pm = extraPrefModels.get(i);
            Preference p = extraPrefs[i];
            if (pm instanceof TogglePrefModel tpm) {
                var sp = (SwitchPreferenceCompat) p;
                sp.setChecked(tpm.isChecked);
            }
            p.setTitle(pm.title);
            p.setSummary(pm.summary);
            p.setEnabled(pm.isEnabled);
        }

        if (hasFooter()) {
            if (footer == null) {
                footer = new FooterPreference(ctx);
                screen.addPreference(footer);
            }
            updateFooter(footer);
        } else {
            if (footer != null) {
                screen.removePreference(footer);
                footer = null;
            }
        }

        return true;
    }

    public static class PrefModel {
        public CharSequence title;
        public CharSequence summary;
        public boolean isEnabled = true;
    }

    public static class LinkPrefModel extends PrefModel {
        public Runnable onClick;
    }

    public static class TogglePrefModel extends PrefModel {
        public boolean isChecked;
        public Preference.OnPreferenceChangeListener onChangeListener;
    }

    protected List<PrefModel> getExtraPrefModels() {
        return Collections.emptyList();
    }

    @Override
    public final void onRadioButtonClicked(SelectorWithWidgetPreference emiter) {
        int id = Integer.parseInt(emiter.getKey());
        onEntrySelected(id);
        if (!refreshUi()) {
            setIntentAndFinish(true);
        }
    }

    public abstract void onEntrySelected(int id);

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    protected ApplicationInfo getAppInfo() {
        return mPackageInfo.applicationInfo;
    }
}
