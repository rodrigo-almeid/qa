package com.loantrack.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.loantrack.app.util.EmailConfig
import com.loantrack.app.worker.EmailReportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val email: String = "",
    val password: String = "",
    val savedSuccess: Boolean = false,
    val sendingNow: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val workManager: WorkManager
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(SettingsUiState(
        email = EmailConfig.getSmtpFrom(app),
        password = EmailConfig.getPassword(app)
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value, savedSuccess = false)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, savedSuccess = false)
    }

    fun save() {
        EmailConfig.saveEmail(app, _uiState.value.email)
        EmailConfig.savePassword(app, _uiState.value.password)
        _uiState.value = _uiState.value.copy(savedSuccess = true)
    }

    fun sendNow() {
        val request = OneTimeWorkRequestBuilder<EmailReportWorker>().build()
        workManager.enqueue(request)
        _uiState.value = _uiState.value.copy(sendingNow = true)
    }

    fun clearSaved() {
        _uiState.value = _uiState.value.copy(savedSuccess = false, sendingNow = false)
    }
}
