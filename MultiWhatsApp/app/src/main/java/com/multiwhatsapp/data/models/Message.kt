package com.multiwhatsapp.data.models

sealed class MessageContent {
    data class Text(val body: String) : MessageContent()
    data class Image(val url: String, val body: String? = null) : MessageContent()
    data class Video(val url: String, val body: String? = null) : MessageContent()
    data class File(val url: String, val filename: String, val mimeType: String?) : MessageContent()
    data class Audio(val url: String, val duration: Long? = null) : MessageContent()
}

data class Message(
    val eventId: String,
    val roomId: String,
    val senderId: String,
    val senderName: String?,
    val content: MessageContent,
    val timestamp: Long,
    val isOutgoing: Boolean = false,
    val isSending: Boolean = false,
    val sendFailed: Boolean = false
)
