package com.multiwhatsapp.data.models

import kotlinx.serialization.Serializable

@Serializable
data class MatrixAccount(
    val id: String,
    val userId: String,
    val homeserverUrl: String,
    val accessToken: String,
    val deviceId: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isActive: Boolean = false,
    val whatsappNumber: String? = null
)

@Serializable
data class MatrixAccountConfig(
    val homeserverUrl: String,
    val username: String,
    val password: String
)
