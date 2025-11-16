package com.multiwhatsapp.data.models

data class ChatRoom(
    val roomId: String,
    val accountId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0,
    val isDirect: Boolean = false,
    val isWhatsAppBridged: Boolean = false,
    val whatsappNumber: String? = null
)
