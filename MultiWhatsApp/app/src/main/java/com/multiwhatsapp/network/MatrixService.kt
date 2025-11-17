package com.multiwhatsapp.network

import android.content.Context
import com.multiwhatsapp.data.Account
import com.multiwhatsapp.data.Chat
import com.multiwhatsapp.data.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.session.Session
import java.util.UUID

/**
 * Service for interacting with Matrix homeserver for WhatsApp bridge communication
 */
class MatrixService(private val context: Context) {

    private val matrixInstances = mutableMapOf<String, Matrix>()
    private val sessions = mutableMapOf<String, Session>()

    /**
     * Login to a Matrix homeserver
     */
    suspend fun login(
        homeserverUrl: String,
        username: String,
        password: String
    ): Result<Account> {
        return try {
            // Create Matrix instance if not exists
            val matrix = getOrCreateMatrix(homeserverUrl)

            // For now, return a mock account since full Matrix SDK integration
            // requires async callbacks and lifecycle management
            val accountId = UUID.randomUUID().toString()
            val account = Account(
                id = accountId,
                username = username,
                homeserverUrl = homeserverUrl,
                accessToken = "mock_token_$accountId",
                userId = "@$username:${homeserverUrl.substringAfter("://").substringBefore(":")}",
                isActive = true
            )

            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get chats/rooms for an account
     */
    fun getChats(account: Account): Flow<List<Chat>> = flow {
        // Mock chats for demonstration
        // In production, this would use session.roomService().getRoomSummaries()
        val mockChats = listOf(
            Chat(
                roomId = "!room1:matrix.org",
                displayName = "WhatsApp Contact 1",
                lastMessage = "Hello from WhatsApp!",
                timestamp = System.currentTimeMillis() - 3600000,
                unreadCount = 2
            ),
            Chat(
                roomId = "!room2:matrix.org",
                displayName = "WhatsApp Group",
                lastMessage = "Group message",
                timestamp = System.currentTimeMillis() - 7200000,
                unreadCount = 0
            )
        )
        emit(mockChats)
    }

    /**
     * Get messages for a specific room
     */
    fun getMessages(roomId: String): Flow<List<Message>> = flow {
        // Mock messages for demonstration
        val mockMessages = listOf(
            Message(
                eventId = UUID.randomUUID().toString(),
                senderId = "@user1:matrix.org",
                senderName = "Contact Name",
                content = "Hello!",
                timestamp = System.currentTimeMillis() - 3600000,
                isOutgoing = false
            ),
            Message(
                eventId = UUID.randomUUID().toString(),
                senderId = "@me:matrix.org",
                senderName = "Me",
                content = "Hi there!",
                timestamp = System.currentTimeMillis() - 3000000,
                isOutgoing = true
            )
        )
        emit(mockMessages)
    }

    /**
     * Send a message to a room
     */
    suspend fun sendMessage(roomId: String, message: String): Result<Unit> {
        return try {
            // In production: session.roomService().getRoom(roomId)?.sendTextMessage(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout from a Matrix session
     */
    suspend fun logout(accountId: String) {
        sessions.remove(accountId)?.close()
    }

    private fun getOrCreateMatrix(homeserverUrl: String): Matrix {
        return matrixInstances.getOrPut(homeserverUrl) {
            val config = MatrixConfiguration(
                applicationFlavor = "MultiWhatsApp",
                roomDisplayNameFallbackProvider = { "Room" }
            )
            Matrix(context, config)
        }
    }

    private fun createHomeServerConfig(url: String): HomeServerConnectionConfig {
        return HomeServerConnectionConfig.Builder()
            .withHomeServerUri(url)
            .build()
    }
}
