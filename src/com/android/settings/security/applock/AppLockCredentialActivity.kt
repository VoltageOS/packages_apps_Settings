/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.security.applock

import android.app.Activity
import android.app.AppLockManager
import android.content.Intent
import android.graphics.Color
import android.hardware.biometrics.BiometricConstants
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.hardware.biometrics.PromptInfo
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserHandle.USER_NULL
import android.os.UserManager
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit

import com.android.internal.widget.LockPatternUtils
import com.android.internal.widget.LockPatternView
import com.android.internal.widget.LockPatternView.Cell
import com.android.internal.widget.LockPatternView.DisplayMode
import com.android.internal.widget.LockscreenCredential
import com.android.settings.R
import com.android.settings.password.BiometricFragment
import com.android.settings.password.ConfirmDeviceCredentialUtils

class AppLockCredentialActivity : FragmentActivity() {

    private enum class SetupStage {
        ENTER,
        CONFIRM,
    }

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var lockPatternUtils: LockPatternUtils
    private lateinit var userManager: UserManager
    private lateinit var appLockManager: AppLockManager

    private var mode = MODE_UNLOCK
    private var targetPackageName: String? = null
    private var label: String? = null
    private var userId: Int = USER_NULL
    private var goHomeOnCancel = false
    private var separateCredentialType = AppLockManager.APP_LOCK_CREDENTIAL_TYPE_NONE
    private var setupStage = SetupStage.ENTER
    private var firstPin: String? = null
    private var firstPattern: List<Cell>? = null
    private var pendingPattern: List<Cell>? = null
    private var biometricFragment: BiometricFragment? = null
    private var finishingFlow = false

    // Legacy (no separate credential) biometric state
    private var goingToBackground = false
    private var waitingForBiometricCallback = false

    // Separate credential biometric state
    // True while the BiometricPrompt is showing on top of the PIN/pattern UI.
    // onPause() is called when the overlay appears — we must NOT finish in that case.
    private var waitingForSeparateBiometricCallback = false
    private var separateBiometricSignal: CancellationSignal? = null
    // Set in onStop() (not onPause!) so we know the user truly navigated away.
    private var trulySentToBackground = false

    private var credentialRootView: ViewGroup? = null
    private lateinit var titleView: TextView
    private lateinit var summaryView: TextView
    private lateinit var errorView: TextView
    private lateinit var cancelButton: Button
    private var primaryButton: Button? = null
    private var biometricButton: Button? = null
    private var pinInput: EditText? = null
    private var lockPatternView: LockPatternView? = null
    private var patternSize: Byte = LockPatternUtils.PATTERN_SIZE_DEFAULT

    private val legacyAuthenticationCallback = object : AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (!goingToBackground) {
                waitingForBiometricCallback = false
                if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                        || errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                    cancelAndFinish()
                }
            } else if (waitingForBiometricCallback) {
                waitingForBiometricCallback = false
                finish()
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            waitingForBiometricCallback = false
            completeAuthenticationSuccess()
        }

        override fun onAuthenticationFailed() {
            waitingForBiometricCallback = false
        }

        override fun onSystemEvent(event: Int) {
            if (event == BiometricConstants.BIOMETRIC_SYSTEM_EVENT_EARLY_USER_CANCEL) {
                cancelAndFinish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
        }

        appLockManager = getSystemService(AppLockManager::class.java)!!
        userManager = UserManager.get(this)
        lockPatternUtils = LockPatternUtils(this)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_UNLOCK
        userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId())
        goHomeOnCancel = intent.getBooleanExtra(EXTRA_GO_HOME_ON_CANCEL, false)
        patternSize = lockPatternUtils.getLockPatternSize(userId)

        if (mode == MODE_SETUP) {
            separateCredentialType = intent.getIntExtra(
                EXTRA_SEPARATE_CREDENTIAL_TYPE,
                AppLockManager.APP_LOCK_CREDENTIAL_TYPE_NONE
            )
            when (separateCredentialType) {
                AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PIN -> showPinUi(forSetup = true)
                AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PATTERN -> showPatternUi(forSetup = true)
                else -> {
                    Log.e(TAG, "Invalid setup credential type $separateCredentialType")
                    finish()
                }
            }
            return
        }
        separateCredentialType = if (appLockManager.isSeparateCredentialEnabled()) {
            appLockManager.getSeparateCredentialType()
        } else {
            AppLockManager.APP_LOCK_CREDENTIAL_TYPE_NONE
        }

        if (mode == MODE_VERIFY) {
            if (separateCredentialType == AppLockManager.APP_LOCK_CREDENTIAL_TYPE_NONE) {
                Log.e(TAG, "Failed to load separate app lock credential for verification")
                finish()
                return
            }
        } else {
            targetPackageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
            if (targetPackageName == null) {
                Log.e(TAG, "Failed to get package name, aborting unlock")
                finish()
                return
            }
            label = intent.getStringExtra(AppLockManager.EXTRA_PACKAGE_LABEL)
        }

        if (separateCredentialType == AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PIN) {
            showPinUi(forSetup = false)
        } else if (separateCredentialType == AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PATTERN) {
            showPatternUi(forSetup = false)
        } else {
            showLegacyCredentialPrompt()
        }
    }

    override fun onStart() {
        super.onStart()
        setVisible(true)
    }

    override fun onPause() {
        super.onPause()
        if (mode == MODE_SETUP) return
        if (finishingFlow) return
        if (!isChangingConfigurations()) {
            if (waitingForSeparateBiometricCallback) {
                // The biometric overlay caused onPause — don't do anything here.
                // onStop() will handle the case where the user truly navigates away.
                return
            }
            goingToBackground = true
            if (!waitingForBiometricCallback) {
                cancelAndFinish()
            }
        } else {
            goingToBackground = false
        }
    }

    override fun onStop() {
        super.onStop()
        if (mode == MODE_SETUP) return
        if (finishingFlow) return
        if (!isChangingConfigurations() && waitingForSeparateBiometricCallback) {
            // The user truly navigated away (home/recents) while biometric was showing.
            trulySentToBackground = true
            // Cancel the signal; onAuthenticationError(CANCELED) will fire → finish().
            separateBiometricSignal?.cancel()
            separateBiometricSignal = null
        }
    }

    private fun showLegacyCredentialPrompt() {
        val biometricsAllowed = intent.getBooleanExtra(
            AppLockManager.EXTRA_ALLOW_BIOMETRICS,
            AppLockManager.DEFAULT_BIOMETRICS_ALLOWED
        )
        var allowedAuthenticators = Authenticators.DEVICE_CREDENTIAL
        if (biometricsAllowed) {
            allowedAuthenticators = allowedAuthenticators or Authenticators.BIOMETRIC_WEAK
        }

        val promptInfo = PromptInfo().apply {
            title = getString(com.android.internal.R.string.unlock_application, label)
            isDisallowBiometricsIfPolicyExists = true
            authenticators = allowedAuthenticators
            isAllowBackgroundAuthentication = true
        }

        if (isBiometricAllowed()) {
            showLegacyBiometricPrompt(promptInfo)
            waitingForBiometricCallback = true
        } else {
            cancelAndFinish()
        }
    }

    private fun applyCredentialWindowBackground() {
        // Theme.AppLockCredential has windowBackground=transparent so the biometric prompt
        // can show cleanly when auto-triggered. Now that we're displaying credential UI, make
        // the window opaque using the theme's proper surface color.
        val ta = obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
        val bgColor = ta.getColor(0, android.graphics.Color.WHITE)
        ta.recycle()
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))
    }

    private fun showPinUi(forSetup: Boolean) {
        applyCredentialWindowBackground()
        setContentView(R.layout.app_lock_credential_pin)
        credentialRootView = findViewById(R.id.credential_root)
        bindCommonViews()

        val input = findViewById<EditText>(R.id.pin_input)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        pinInput = input

        // For unlock/verify: submit via IME action key (no on-screen Confirm button needed).
        if (!forSetup) {
            input.imeOptions = EditorInfo.IME_ACTION_DONE
            input.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    verifyPin(input.text?.toString().orEmpty())
                    true
                } else false
            }
            primaryButton?.visibility = View.GONE
        }

        biometricButton = findViewById<Button>(R.id.biometric_button).apply {
            visibility = if (!forSetup && canUseSeparateBiometrics()) View.VISIBLE else View.GONE
            setOnClickListener { showSeparateBiometricPrompt() }
        }
        cancelButton.setOnClickListener { cancelAndFinish() }
        primaryButton?.setOnClickListener {
            val value = input.text?.toString().orEmpty()
            if (forSetup) handleSetupPin(value) else verifyPin(value)
        }
        updatePinUi(forSetup)

        if (!forSetup && shouldAutoShowSeparateBiometrics()) {
            // Hide credential UI while auto-biometric is being shown.
            credentialRootView?.visibility = View.INVISIBLE
            biometricButton?.post { showSeparateBiometricPrompt() }
        }
    }

    private fun showPatternUi(forSetup: Boolean) {
        applyCredentialWindowBackground()
        setContentView(R.layout.app_lock_credential_pattern)
        credentialRootView = findViewById(R.id.credential_root)
        bindCommonViews()

        val patternView = findViewById<LockPatternView>(R.id.lock_pattern)
        patternView.setLockPatternSize(patternSize)
        patternView.setOnPatternListener(object : LockPatternView.OnPatternListener {
            override fun onPatternStart() { hideError() }

            override fun onPatternCleared() {
                if (forSetup && setupStage == SetupStage.ENTER) {
                    primaryButton?.isEnabled = false
                    pendingPattern = null
                }
            }

            override fun onPatternCellAdded(pattern: List<Cell>) = Unit

            override fun onPatternDetected(pattern: List<Cell>, patternSize: Byte) {
                if (pattern.size < LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
                    showPatternError(getString(R.string.app_lock_pattern_too_short))
                    return
                }
                if (forSetup) {
                    if (setupStage == SetupStage.ENTER) {
                        pendingPattern = ArrayList(pattern)
                        primaryButton?.isEnabled = true
                        hideError()
                    } else {
                        confirmPattern(pattern)
                    }
                } else {
                    verifyPattern(pattern)
                }
            }
        })
        lockPatternView = patternView

        biometricButton = findViewById<Button>(R.id.biometric_button).apply {
            visibility = if (!forSetup && canUseSeparateBiometrics()) View.VISIBLE else View.GONE
            setOnClickListener { showSeparateBiometricPrompt() }
        }
        cancelButton.setOnClickListener { cancelAndFinish() }
        primaryButton?.setOnClickListener {
            if (forSetup && setupStage == SetupStage.ENTER) {
                firstPattern = ArrayList(pendingPattern ?: emptyList())
                setupStage = SetupStage.CONFIRM
                pendingPattern = null
                primaryButton?.isEnabled = false
                lockPatternView?.clearPattern()
                updatePatternUi(forSetup = true)
            }
        }
        updatePatternUi(forSetup)

        if (!forSetup && shouldAutoShowSeparateBiometrics()) {
            credentialRootView?.visibility = View.INVISIBLE
            biometricButton?.post { showSeparateBiometricPrompt() }
        }
    }

    private fun bindCommonViews() {
        titleView = findViewById(R.id.title)
        summaryView = findViewById(R.id.summary)
        errorView = findViewById(R.id.error_text)
        cancelButton = findViewById(R.id.cancel_button)
        primaryButton = findViewById(R.id.primary_button)
    }

    private fun updatePinUi(forSetup: Boolean) {
        if (!forSetup) {
            if (mode == MODE_VERIFY) {
                titleView.text = getString(R.string.app_lock_verify_current_pin_title)
                summaryView.text = getString(R.string.app_lock_verify_current_pin_summary)
                primaryButton?.text = getString(R.string.app_lock_continue)
            } else {
                titleView.text = getString(com.android.internal.R.string.unlock_application, label)
                summaryView.text = getString(R.string.app_lock_verify_pin_summary)
                primaryButton?.text = getString(R.string.app_lock_unlock)
            }
            return
        }
        if (setupStage == SetupStage.ENTER) {
            titleView.text = getString(R.string.app_lock_setup_pin_title)
            summaryView.text = getString(R.string.app_lock_setup_pin_summary)
            primaryButton?.text = getString(R.string.app_lock_continue)
        } else {
            titleView.text = getString(R.string.app_lock_confirm_pin_title)
            summaryView.text = getString(R.string.app_lock_confirm_pin_summary)
            primaryButton?.text = getString(R.string.app_lock_save)
        }
    }

    private fun updatePatternUi(forSetup: Boolean) {
        if (!forSetup) {
            if (mode == MODE_VERIFY) {
                titleView.text = getString(R.string.app_lock_verify_current_pattern_title)
                summaryView.text = getString(R.string.app_lock_verify_current_pattern_summary)
            } else {
                titleView.text = getString(com.android.internal.R.string.unlock_application, label)
                summaryView.text = getString(R.string.app_lock_verify_pattern_summary)
            }
            primaryButton?.visibility = View.GONE
            return
        }
        primaryButton?.visibility = View.VISIBLE
        if (setupStage == SetupStage.ENTER) {
            titleView.text = getString(R.string.app_lock_setup_pattern_title)
            summaryView.text = getString(R.string.app_lock_setup_pattern_summary)
            primaryButton?.text = getString(R.string.app_lock_continue)
            primaryButton?.isEnabled = pendingPattern != null
        } else {
            titleView.text = getString(R.string.app_lock_confirm_pattern_title)
            summaryView.text = getString(R.string.app_lock_confirm_pattern_summary)
            primaryButton?.text = getString(R.string.app_lock_save)
            primaryButton?.isEnabled = false
        }
    }

    private fun handleSetupPin(pin: String) {
        val error = validatePin(pin)
        if (error != null) { showError(error); return }
        if (setupStage == SetupStage.ENTER) {
            firstPin = pin
            setupStage = SetupStage.CONFIRM
            pinInput?.setText("")
            hideError()
            updatePinUi(forSetup = true)
            return
        }
        if (pin != firstPin) {
            showError(getString(R.string.app_lock_pin_mismatch))
            pinInput?.setText("")
            return
        }
        saveCredential(LockscreenCredential.createPin(pin))
    }

    private fun confirmPattern(pattern: List<Cell>) {
        val initialPattern = firstPattern
        if (initialPattern == null || !patternsEqual(initialPattern, pattern)) {
            showPatternError(getString(R.string.app_lock_pattern_mismatch))
            return
        }
        saveCredential(LockscreenCredential.createPattern(pattern, patternSize))
    }

    private fun validatePin(pin: String): String? {
        if (pin.length < LockPatternUtils.MIN_LOCK_PASSWORD_SIZE) {
            return getString(R.string.app_lock_pin_too_short)
        }
        if (!pin.all(Char::isDigit)) {
            return getString(R.string.app_lock_pin_digits_only)
        }
        return null
    }

    private fun verifyPin(pin: String) {
        val error = validatePin(pin)
        if (error != null) { showError(error); return }
        verifyCredential(LockscreenCredential.createPin(pin))
    }

    private fun verifyPattern(pattern: List<Cell>) {
        verifyCredential(LockscreenCredential.createPattern(pattern, patternSize))
    }

    private fun verifyCredential(credential: LockscreenCredential) {
        val response = try {
            appLockManager.verifyCredential(credential)
        } finally {
            credential.zeroize()
        }
        if (response.isMatched) {
            completeAuthenticationSuccess()
            return
        }
        if (response.hasTimeout()) {
            val seconds = maxOf(1, response.timeout / 1000)
            showVerificationError(getString(R.string.app_lock_try_again_later, seconds))
            return
        }
        showVerificationError(getString(R.string.app_lock_wrong_credential))
    }

    private fun saveCredential(credential: LockscreenCredential) {
        try {
            appLockManager.setSeparateCredential(credential)
        } finally {
            credential.zeroize()
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun showVerificationError(message: String) {
        showError(message)
        pinInput?.setText("")
        lockPatternView?.clearPattern()
        lockPatternView?.setDisplayMode(DisplayMode.Wrong)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ lockPatternView?.clearPattern() }, 800L)
    }

    private fun showPatternError(message: String) = showVerificationError(message)

    private fun showError(message: String) {
        errorView.text = message
        errorView.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorView.text = null
        errorView.visibility = View.GONE
    }

    private fun revealCredentialUi() {
        credentialRootView?.visibility = View.VISIBLE
    }

    private fun patternsEqual(first: List<Cell>, second: List<Cell>): Boolean {
        if (first.size != second.size) return false
        return first.indices.all { index ->
            first[index].row == second[index].row && first[index].column == second[index].column
        }
    }

    private fun canUseSeparateBiometrics(): Boolean {
        if (mode != MODE_UNLOCK) return false
        if (!intent.getBooleanExtra(
                AppLockManager.EXTRA_ALLOW_BIOMETRICS,
                AppLockManager.DEFAULT_BIOMETRICS_ALLOWED
            )
        ) return false
        return isBiometricAllowed()
    }

    private fun shouldAutoShowSeparateBiometrics(): Boolean =
        canUseSeparateBiometrics() && appLockManager.isBiometricPromptEnabled()

    private fun showSeparateBiometricPrompt() {
        if (!canUseSeparateBiometrics()) {
            revealCredentialUi()
            return
        }
        // Cancel any outstanding signal before starting a new one.
        separateBiometricSignal?.cancel()
        val signal = CancellationSignal()
        separateBiometricSignal = signal
        waitingForSeparateBiometricCallback = true
        trulySentToBackground = false

        val negativeButtonText = if (separateCredentialType == AppLockManager.APP_LOCK_CREDENTIAL_TYPE_PATTERN) {
            getString(R.string.app_lock_biometric_negative_pattern)
        } else {
            getString(R.string.app_lock_biometric_negative_pin)
        }
        val prompt = BiometricPrompt.Builder(this)
            .setTitle(getString(com.android.internal.R.string.unlock_application, label))
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK)
            .setNegativeButton(negativeButtonText, mainExecutor) { _, _ ->
                // User tapped "Use PIN/Pattern" — clear flags and reveal the credential UI.
                // Note: onAuthenticationError(USER_CANCELED) will also fire; let it be a no-op.
                waitingForSeparateBiometricCallback = false
                separateBiometricSignal = null
                if (!trulySentToBackground) revealCredentialUi()
            }
            .build()

        prompt.authenticate(
            signal,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    waitingForSeparateBiometricCallback = false
                    separateBiometricSignal = null
                    completeAuthenticationSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    waitingForSeparateBiometricCallback = false
                    separateBiometricSignal = null
                    when {
                        trulySentToBackground -> {
                            // User navigated away (home/recents) while biometric was showing.
                            finish()
                        }
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED -> {
                            // Fired as a side-effect of the negative button tap — already handled
                            // in the negative button listener. Just reveal UI if not done yet.
                            if (!finishingFlow) revealCredentialUi()
                        }
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED -> {
                            // System cancelled (e.g., our CancellationSignal fired from onStop).
                            if (trulySentToBackground) finish() else revealCredentialUi()
                        }
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT ||
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT -> {
                            // Too many failed attempts — show the PIN/Pattern UI with an error.
                            revealCredentialUi()
                            showError(errString.toString())
                        }
                        else -> {
                            // Any other error (timeout, hw unavailable, etc.) — show credential UI.
                            revealCredentialUi()
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    // Finger not recognized; the prompt stays visible automatically.
                    // No action needed here.
                }
            }
        )
    }

    private fun completeAuthenticationSuccess() {
        finishingFlow = true
        if (mode == MODE_UNLOCK) {
            targetPackageName?.let { appLockManager.unlockPackage(it) }
            ConfirmDeviceCredentialUtils.checkForPendingIntent(this)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun cancelAndFinish() {
        if (finishingFlow) return
        finishingFlow = true
        // Cancel any active separate biometric prompt cleanly.
        separateBiometricSignal?.cancel()
        separateBiometricSignal = null
        waitingForSeparateBiometricCallback = false
        setResult(Activity.RESULT_CANCELED)
        if (goHomeOnCancel) {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
        finish()
    }

    // User could be locked while effective user is unlocked even though the effective owns the
    // credential. In such case, we also wanna show the user message that biometric is disabled
    // due to device restart.
    private fun isStrongAuthRequired() =
        !lockPatternUtils.isBiometricAllowedForUser(userId) ||
            !userManager.isUserUnlocked(userId)

    private fun isBiometricAllowed() =
        !isStrongAuthRequired() && !lockPatternUtils.hasPendingEscrowToken(userId)

    private fun showLegacyBiometricPrompt(promptInfo: PromptInfo) {
        biometricFragment = supportFragmentManager.findFragmentByTag(TAG_BIOMETRIC_FRAGMENT)
            as? BiometricFragment
        var newFragment = false
        if (biometricFragment == null) {
            biometricFragment = BiometricFragment.newInstance(promptInfo)
            newFragment = true
        }
        biometricFragment?.also {
            it.setCallbacks({
                handler.post(it)
            }, legacyAuthenticationCallback)
            it.setUser(userId)
        }
        if (newFragment) {
            biometricFragment?.let {
                supportFragmentManager.commit {
                    add(it, TAG_BIOMETRIC_FRAGMENT)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AppLockCredentialActivity"
        private const val TAG_BIOMETRIC_FRAGMENT = "fragment"
        private const val EXTRA_GO_HOME_ON_CANCEL = "com.android.server.app.extra.GO_HOME_ON_CANCEL"

        const val EXTRA_MODE = "com.android.settings.security.applock.MODE"
        const val EXTRA_SEPARATE_CREDENTIAL_TYPE =
            "com.android.settings.security.applock.SEPARATE_CREDENTIAL_TYPE"

        const val MODE_UNLOCK = "unlock"
        const val MODE_SETUP = "setup"
        const val MODE_VERIFY = "verify"
    }
}
