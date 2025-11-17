package com.multiwhatsapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multiwhatsapp.data.Account
import com.multiwhatsapp.data.AccountRepository
import com.multiwhatsapp.data.Chat
import com.multiwhatsapp.data.Message
import com.multiwhatsapp.network.MatrixService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val accountRepository = AccountRepository(application)
    private val matrixService = MatrixService(application)

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _currentAccount = MutableStateFlow<Account?>(null)
    val currentAccount: StateFlow<Account?> = _currentAccount.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Success)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.accounts.collect { accountList ->
                _accounts.value = accountList
                if (_currentAccount.value == null && accountList.isNotEmpty()) {
                    setCurrentAccount(accountList.first())
                }
            }
        }
    }

    fun login(homeserverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            matrixService.login(homeserverUrl, username, password)
                .onSuccess { account ->
                    accountRepository.addAccount(account)
                    _uiState.value = UiState.Success
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Login failed")
                }
        }
    }

    fun logout(account: Account) {
        viewModelScope.launch {
            matrixService.logout(account.id)
            accountRepository.removeAccount(account.id)
        }
    }

    fun setCurrentAccount(account: Account) {
        _currentAccount.value = account
        loadChats(account)
    }

    private fun loadChats(account: Account) {
        viewModelScope.launch {
            matrixService.getChats(account).collect { chatList ->
                _chats.value = chatList
            }
        }
    }

    fun loadMessages(roomId: String) {
        viewModelScope.launch {
            matrixService.getMessages(roomId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    fun sendMessage(roomId: String, message: String) {
        viewModelScope.launch {
            matrixService.sendMessage(roomId, message)
        }
    }
}
