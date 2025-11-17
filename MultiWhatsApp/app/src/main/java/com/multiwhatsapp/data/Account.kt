package com.multiwhatsapp.data

data class Account(
    val id: String,
    val username: String,
    val homeserverUrl: String,
    val accessToken: String? = null,
    val userId: String? = null,
    val isActive: Boolean = false
)

data class Chat(
    val roomId: String,
    val displayName: String,
    val lastMessage: String? = null,
    val timestamp: Long = 0L,
    val unreadCount: Int = 0,
    val avatarUrl: String? = null
)

data class Message(
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean = false
)
