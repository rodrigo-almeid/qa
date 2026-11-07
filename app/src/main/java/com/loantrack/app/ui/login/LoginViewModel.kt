package com.loantrack.app.ui.login

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.loantrack.app.util.BiometricHelper
import com.loantrack.app.util.SecurityPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Termos e LGPD
    val termsChecked: Boolean = false,
    val hasAcceptedTerms: Boolean = false,
    // Biometria
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
    val hasFirebaseSession: Boolean = false,
    val triggerBiometric: Boolean = false,
    val showEnableBiometricPrompt: Boolean = false,
    // Rate limiting
    val isLocked: Boolean = false,
    val lockRemainingSeconds: Long = 0L,
    val failedAttempts: Int = 0
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val app: Application,
    private val firebaseAuth: FirebaseAuth
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        val hasSession = firebaseAuth.currentUser != null
        val termsAccepted = SecurityPrefs.isTermsAccepted(app)
        val biometricEnabled = SecurityPrefs.isBiometricEnabled(app)
        val biometricAvailable = BiometricHelper.isAvailable(app)

        _uiState.value = LoginUiState(
            hasAcceptedTerms = termsAccepted,
            biometricAvailable = biometricAvailable,
            biometricEnabled = biometricEnabled,
            hasFirebaseSession = hasSession,
            triggerBiometric = hasSession && biometricEnabled
        )

        // Se há sessão e biometria não está ativada, entra direto
        if (hasSession && !biometricEnabled) {
            _isLoggedIn.value = true
        }
    }

    fun updateTermsChecked(checked: Boolean) {
        _uiState.value = _uiState.value.copy(termsChecked = checked)
    }

    fun acceptTermsAndProceed() {
        SecurityPrefs.acceptTerms(app)
        _uiState.value = _uiState.value.copy(hasAcceptedTerms = true)
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential).await()
                SecurityPrefs.clearFailedAttempts(app)

                val biometricAvailable = BiometricHelper.isAvailable(app)
                val biometricAlreadyEnabled = SecurityPrefs.isBiometricEnabled(app)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasFirebaseSession = true,
                    biometricAvailable = biometricAvailable,
                    showEnableBiometricPrompt = biometricAvailable && !biometricAlreadyEnabled
                )

                if (!biometricAvailable || biometricAlreadyEnabled) {
                    _isLoggedIn.value = true
                }
            } catch (e: Exception) {
                val attempts = SecurityPrefs.recordFailedAttempt(app)
                val locked = SecurityPrefs.isLocked(app)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (locked) "Conta bloqueada por tentativas excessivas. Aguarde."
                            else "Erro ao fazer login. Tente novamente. ($attempts/${SecurityPrefs.MAX_ATTEMPTS})",
                    isLocked = locked,
                    lockRemainingSeconds = SecurityPrefs.lockRemainingSeconds(app),
                    failedAttempts = attempts
                )
                if (locked) startLockCountdown()
            }
        }
    }

    fun onBiometricSuccess() {
        SecurityPrefs.clearFailedAttempts(app)
        _uiState.value = _uiState.value.copy(triggerBiometric = false)
        _isLoggedIn.value = true
    }

    fun onBiometricFailed() {
        val attempts = SecurityPrefs.recordFailedAttempt(app)
        val locked = SecurityPrefs.isLocked(app)
        _uiState.value = _uiState.value.copy(
            error = if (locked) "Muitas tentativas. Aguarde ${SecurityPrefs.lockRemainingSeconds(app) / 60} minutos."
                    else "Biometria não reconhecida. Tentativa $attempts/${SecurityPrefs.MAX_ATTEMPTS}.",
            isLocked = locked,
            lockRemainingSeconds = SecurityPrefs.lockRemainingSeconds(app),
            failedAttempts = attempts,
            triggerBiometric = false
        )
        if (locked) startLockCountdown()
    }

    fun enableBiometric() {
        SecurityPrefs.setBiometricEnabled(app, true)
        _uiState.value = _uiState.value.copy(
            biometricEnabled = true,
            showEnableBiometricPrompt = false
        )
        _isLoggedIn.value = true
    }

    fun skipBiometric() {
        _uiState.value = _uiState.value.copy(showEnableBiometricPrompt = false)
        _isLoggedIn.value = true
    }

    fun signOut() {
        firebaseAuth.signOut()
        _isLoggedIn.value = false
        val biometricAvailable = BiometricHelper.isAvailable(app)
        _uiState.value = LoginUiState(
            hasAcceptedTerms = SecurityPrefs.isTermsAccepted(app),
            biometricAvailable = biometricAvailable,
            biometricEnabled = SecurityPrefs.isBiometricEnabled(app),
            hasFirebaseSession = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun retriggerBiometric() {
        if (!SecurityPrefs.isLocked(app)) {
            _uiState.value = _uiState.value.copy(triggerBiometric = true)
        }
    }

    private fun startLockCountdown() {
        viewModelScope.launch {
            while (SecurityPrefs.isLocked(app)) {
                val remaining = SecurityPrefs.lockRemainingSeconds(app)
                _uiState.value = _uiState.value.copy(
                    lockRemainingSeconds = remaining,
                    isLocked = true
                )
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(isLocked = false, lockRemainingSeconds = 0)
            SecurityPrefs.clearFailedAttempts(app)
        }
    }
}
