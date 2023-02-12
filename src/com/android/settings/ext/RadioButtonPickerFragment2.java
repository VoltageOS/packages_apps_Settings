package com.android.settings.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.utils.CandidateInfoExtra;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;

public class RadioButtonPickerFragment2 extends RadioButtonPickerFragment {

    private final ArrayList<CandidateInfo> candidates = new ArrayList<>();

    static final String KEY_PREF_CONTROLLER_CLASS = "pref_controller";
    static final String KEY_PREF_KEY = "pref_key";

    private AbstractListPreferenceController prefController;

    public static void fillArgs(Preference pref, AbstractListPreferenceController pc, boolean isForWork) {
        Bundle args = pref.getExtras();
        args.putString(KEY_PREF_CONTROLLER_CLASS, pc.getClass().getName());
        args.putString(KEY_PREF_KEY, pc.getPreferenceKey());
        args.putBoolean(EXTRA_FOR_WORK, isForWork);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String prefControllerClass = args.getString(KEY_PREF_CONTROLLER_CLASS);
        String prefKey = args.getString(KEY_PREF_KEY);
        boolean forWork = args.getBoolean(EXTRA_FOR_WORK);

        Context ctx = requireContext();

        prefController = (AbstractListPreferenceController) BasePreferenceController
                .createInstance(ctx,prefControllerClass, prefKey, forWork);
        prefController.fragment = this;

        super.onCreate(savedInstanceState);
    }

    private static final String KEY_USER_CREDENTIAL_CONFIRMED = "user_credential_confirmed";
    private boolean userCredentialConfirmed;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(ps);

        if (savedInstanceState != null) {
            userCredentialConfirmed = savedInstanceState.getBoolean(
                    KEY_USER_CREDENTIAL_CONFIRMED, false);
        }

        updateCandidates();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_USER_CREDENTIAL_CONFIRMED, userCredentialConfirmed);
    }

    private Runnable onAfterCredentialConfirmed;

    private void runAfterCredentialConfirmation(Runnable runnable) {
        if (userCredentialConfirmed) {
            throw new IllegalStateException();
        }

        if (onAfterCredentialConfirmed != null) {
            throw new IllegalStateException();
        }

        onAfterCredentialConfirmed = runnable;

        var b = new ChooseLockSettingsHelper.Builder(requireActivity());
        b.setActivityResultLauncher(credentialConfirmationLauncher);
        b.setForegroundOnly(true);
        b.show();
    }

    private final ActivityResultLauncher<Intent> credentialConfirmationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::onCredentialConfirmationResult);

    private void onCredentialConfirmationResult(@Nullable ActivityResult result) {
        Runnable r = onAfterCredentialConfirmed;
        onAfterCredentialConfirmed = null;

        if (result != null && result.getResultCode() == Activity.RESULT_OK) {
            userCredentialConfirmed = true;
            if (r != null) {
                r.run();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!requireActivity().isChangingConfigurations()) {
            userCredentialConfirmed = false;
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return -1;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        candidates.clear();
        prefController.getEntriesAsCandidates(candidates);
        return candidates;
    }

    @Override
    protected void addPrefsBeforeList(PreferenceScreen screen) {
        prefController.addPrefsBeforeList(this, screen);
    }

    @Override
    protected void addPrefsAfterList(PreferenceScreen screen) {
        prefController.addPrefsAfterList(this, screen);
    }

    @Override
    protected String getDefaultKey() {
        return Integer.toString(prefController.getCurrentValue());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (key.equals(getDefaultKey())) {
            return true;
        }

        if (!userCredentialConfirmed && prefController.isCredentialConfirmationRequired()) {
            Context ctx = requireContext();
            LockPatternUtils lpu = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(ctx);

            if (lpu.isSecure(ctx.getUserId())) {
                runAfterCredentialConfirmation(() -> prefController.setValue(Integer.parseInt(key)));
                return false;
            }
        }

        return prefController.setValue(Integer.parseInt(key));
    }

    @Override
    public void bindPreferenceExtra(SelectorWithWidgetPreference pref, String key, CandidateInfo info,
                                    String defaultKey, String systemDefaultKey) {
        pref.setSingleLineTitle(false);

        if (info instanceof CandidateInfoExtra) {
            var cie = (CandidateInfoExtra) info;
            pref.setSummary(cie.loadSummary());
        }
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    @Override
    public void onPause() {
        super.onPause();
        prefController.onPause(this);
    }

    private boolean updateCandidatesOnResume;

    @Override
    public void onResume() {
        super.onResume();
        prefController.onResume(this);

        if (updateCandidatesOnResume) {
            updateCandidates();
        } else {
            // updateCandidates() is called from onCreatePrefences() right before the first onResume()
            updateCandidatesOnResume = true;
        }
    }
}
