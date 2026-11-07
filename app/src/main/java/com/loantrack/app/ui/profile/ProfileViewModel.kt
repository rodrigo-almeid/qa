package com.loantrack.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loantrack.app.domain.usecase.UpdateUserProfileUseCase
import com.loantrack.app.util.DateUtils
import com.loantrack.app.util.SecurityPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isBiometricEnabled: Boolean = false,
    val lgpdAcceptedAt: String = "",
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val app: Application,
    private val auth: FirebaseAuth,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val user = auth.currentUser
        val acceptedAtMs = SecurityPrefs.getTermsAcceptedAt(app)
        _uiState.value = ProfileUiState(
            displayName = user?.displayName ?: "",
            email = user?.email ?: "",
            photoUrl = user?.photoUrl?.toString(),
            isBiometricEnabled = SecurityPrefs.isBiometricEnabled(app),
            lgpdAcceptedAt = if (acceptedAtMs > 0L) DateUtils.formatEpochMillis(acceptedAtMs) else ""
        )
    }

    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name)
    }

    fun toggleBiometric(enabled: Boolean) {
        SecurityPrefs.setBiometricEnabled(app, enabled)
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
    }

    fun save() {
        val name = _uiState.value.displayName.trim()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = updateUserProfileUseCase(name)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isSaving = false, savedSuccess = true)
            } else {
                _uiState.value.copy(isSaving = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun clearSaved() {
        _uiState.value = _uiState.value.copy(savedSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
