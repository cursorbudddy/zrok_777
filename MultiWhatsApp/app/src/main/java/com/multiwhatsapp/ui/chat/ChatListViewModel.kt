package com.multiwhatsapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multiwhatsapp.data.models.ChatRoom
import com.multiwhatsapp.data.models.MatrixAccount
import com.multiwhatsapp.data.repository.AccountRepository
import com.multiwhatsapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class ChatListState(
    val chats: List<ChatRoom> = emptyList(),
    val accounts: List<MatrixAccount> = emptyList(),
    val activeAccountId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        loadAccounts()
        loadChats()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                accountRepository.getAccounts().collect { accounts ->
                    _state.value = _state.value.copy(accounts = accounts)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading accounts")
            }
        }

        viewModelScope.launch {
            try {
                accountRepository.getActiveAccountId().collect { activeId ->
                    _state.value = _state.value.copy(activeAccountId = activeId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading active account")
            }
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                chatRepository.getAllChats().collect { chats ->
                    _state.value = _state.value.copy(
                        chats = chats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load chats"
                )
                Timber.e(e, "Error loading chats")
            }
        }
    }

    fun switchAccount(accountId: String) {
        viewModelScope.launch {
            try {
                accountRepository.setActiveAccount(accountId)
                loadChats() // Reload chats for new account
            } catch (e: Exception) {
                Timber.e(e, "Error switching account")
            }
        }
    }

    fun refresh() {
        loadChats()
    }
}
