package com.loantrack.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.loantrack.app.util.EmailConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val password: String = "",
    val savedSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(SettingsUiState(
        password = EmailConfig.getPassword(app)
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, savedSuccess = false)
    }

    fun save() {
        EmailConfig.savePassword(app, _uiState.value.password)
        _uiState.value = _uiState.value.copy(savedSuccess = true)
    }

    fun clearSaved() {
        _uiState.value = _uiState.value.copy(savedSuccess = false)
    }
}
