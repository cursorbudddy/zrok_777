package com.multiwhatsapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multiwhatsapp.data.matrix.MatrixAuthService
import com.multiwhatsapp.data.matrix.MatrixClientManager
import com.multiwhatsapp.data.matrix.WhatsAppBridgeService
import com.multiwhatsapp.data.models.MatrixAccount
import com.multiwhatsapp.data.models.MatrixAccountConfig
import com.multiwhatsapp.data.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val qrCodeUrl: String? = null,
    val waitingForQR: Boolean = false
)

class LoginViewModel(
    private val matrixClientManager: MatrixClientManager,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val authService = MatrixAuthService(matrixClientManager)

    fun login(homeserverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val config = MatrixAccountConfig(
                    homeserverUrl = homeserverUrl,
                    username = username,
                    password = password
                )

                val result = authService.login(config)

                result.fold(
                    onSuccess = { account ->
                        // Save account
                        accountRepository.saveAccount(account)
                        accountRepository.setActiveAccount(account.id)

                        _state.value = _state.value.copy(
                            isLoading = false,
                            loginSuccess = true
                        )

                        Timber.d("Login successful, saved account: ${account.userId}")
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = error.message ?: "Login failed"
                        )
                        Timber.e(error, "Login failed")
                    }
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
                Timber.e(e, "Login error")
            }
        }
    }

    fun requestWhatsAppQRCode(account: MatrixAccount) {
        viewModelScope.launch {
            _state.value = _state.value.copy(waitingForQR = true, error = null)

            try {
                val session = matrixClientManager.getSession(account.id)
                if (session == null) {
                    _state.value = _state.value.copy(
                        waitingForQR = false,
                        error = "Session not found"
                    )
                    return@launch
                }

                val bridgeService = WhatsAppBridgeService(session)

                // Request QR code from bridge
                val result = bridgeService.requestWhatsAppLogin()

                result.fold(
                    onSuccess = {
                        // Start listening for QR code
                        bridgeService.observeQRCode().collect { qrUrl ->
                            if (qrUrl != null) {
                                _state.value = _state.value.copy(
                                    waitingForQR = false,
                                    qrCodeUrl = qrUrl
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            waitingForQR = false,
                            error = error.message ?: "Failed to request QR code"
                        )
                    }
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    waitingForQR = false,
                    error = e.message ?: "Unknown error"
                )
                Timber.e(e, "Error requesting QR code")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetState() {
        _state.value = LoginState()
    }
}
