package com.multiwhatsapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multiwhatsapp.data.models.Message
import com.multiwhatsapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val roomId: String,
    private val accountId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        loadMessages()
        markAsRead()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                chatRepository.getMessages(roomId, accountId).collect { messages ->
                    _state.value = _state.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load messages"
                )
                Timber.e(e, "Error loading messages")
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true, error = null)

            try {
                val result = chatRepository.sendTextMessage(roomId, accountId, text)

                result.fold(
                    onSuccess = {
                        _state.value = _state.value.copy(isSending = false)
                        Timber.d("Message sent successfully")
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            isSending = false,
                            error = error.message ?: "Failed to send message"
                        )
                        Timber.e(error, "Error sending message")
                    }
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSending = false,
                    error = e.message ?: "Unknown error"
                )
                Timber.e(e, "Error sending message")
            }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            try {
                chatRepository.markAsRead(roomId, accountId)
            } catch (e: Exception) {
                Timber.e(e, "Error marking as read")
            }
        }
    }
}
