package com.multiwhatsapp.data.repository

import com.multiwhatsapp.data.matrix.MatrixClientManager
import com.multiwhatsapp.data.matrix.WhatsAppBridgeService
import com.multiwhatsapp.data.models.ChatRoom
import com.multiwhatsapp.data.models.Message
import com.multiwhatsapp.data.models.MessageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.message.*
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber

class ChatRepository(
    private val matrixClientManager: MatrixClientManager
) {

    /**
     * Get all chats from all accounts
     */
    fun getAllChats(): Flow<List<ChatRoom>> = flow {
        try {
            val allChats = mutableListOf<ChatRoom>()

            matrixClientManager.getAllSessions().forEach { (accountId, session) ->
                val bridgeService = WhatsAppBridgeService(session)
                val whatsappRooms = bridgeService.getWhatsAppChats()

                whatsappRooms.forEach { room ->
                    val chatRoom = roomToChatRoom(room, accountId, bridgeService)
                    if (chatRoom != null) {
                        allChats.add(chatRoom)
                    }
                }
            }

            emit(allChats.sortedByDescending { it.lastMessageTime ?: 0 })

        } catch (e: Exception) {
            Timber.e(e, "Error getting all chats")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get messages for a specific room
     */
    fun getMessages(roomId: String, accountId: String): Flow<List<Message>> = flow {
        try {
            val session = matrixClientManager.getSession(accountId)
            if (session == null) {
                emit(emptyList())
                return@flow
            }

            val room = session.roomService().getRoom(roomId)
            if (room == null) {
                emit(emptyList())
                return@flow
            }

            // Get timeline events
            room.timelineService()
                .getTimelineFlow()
                .collect { timeline ->
                    val messages = timeline.events
                        .mapNotNull { timelineEvent ->
                            timelineEventToMessage(timelineEvent, session)
                        }
                        .sortedBy { it.timestamp }

                    emit(messages)
                }

        } catch (e: Exception) {
            Timber.e(e, "Error getting messages for room: $roomId")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Send text message
     */
    suspend fun sendTextMessage(
        roomId: String,
        accountId: String,
        text: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = matrixClientManager.getSession(accountId)
                ?: return@withContext Result.failure(Exception("Session not found"))

            val room = session.roomService().getRoom(roomId)
                ?: return@withContext Result.failure(Exception("Room not found"))

            room.sendService().sendTextMessage(text)

            Timber.d("Message sent to room: $roomId")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Error sending message")
            Result.failure(e)
        }
    }

    /**
     * Send image message
     */
    suspend fun sendImageMessage(
        roomId: String,
        accountId: String,
        imageUri: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = matrixClientManager.getSession(accountId)
                ?: return@withContext Result.failure(Exception("Session not found"))

            val room = session.roomService().getRoom(roomId)
                ?: return@withContext Result.failure(Exception("Room not found"))

            // Upload and send image
            // Note: This requires proper URI handling and file upload implementation
            // For now, we'll just log it
            Timber.d("Image message would be sent: $imageUri")

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Error sending image")
            Result.failure(e)
        }
    }

    /**
     * Mark room as read
     */
    suspend fun markAsRead(roomId: String, accountId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val session = matrixClientManager.getSession(accountId)
                    ?: return@withContext Result.failure(Exception("Session not found"))

                val room = session.roomService().getRoom(roomId)
                    ?: return@withContext Result.failure(Exception("Room not found"))

                room.readService().setReadMarker(null)

                Result.success(Unit)

            } catch (e: Exception) {
                Timber.e(e, "Error marking room as read")
                Result.failure(e)
            }
        }

    /**
     * Convert Matrix Room to ChatRoom model
     */
    private fun roomToChatRoom(
        room: Room,
        accountId: String,
        bridgeService: WhatsAppBridgeService
    ): ChatRoom? {
        try {
            val roomSummary = room.roomSummary() ?: return null

            val whatsappNumber = roomSummary.otherMemberIds
                .firstNotNullOfOrNull { bridgeService.extractWhatsAppNumber(it) }

            return ChatRoom(
                roomId = room.roomId,
                accountId = accountId,
                displayName = roomSummary.displayName,
                avatarUrl = roomSummary.avatarUrl,
                lastMessage = roomSummary.latestPreviewableEvent?.root?.content
                    ?.toModel<MessageTextContent>()?.body,
                lastMessageTime = roomSummary.latestPreviewableEvent?.root?.originServerTs,
                unreadCount = roomSummary.notificationCount,
                isDirect = roomSummary.isDirect,
                isWhatsAppBridged = bridgeService.isWhatsAppRoom(room),
                whatsappNumber = whatsappNumber
            )
        } catch (e: Exception) {
            Timber.e(e, "Error converting room to ChatRoom")
            return null
        }
    }

    /**
     * Convert TimelineEvent to Message model
     */
    private fun timelineEventToMessage(
        event: TimelineEvent,
        session: Session
    ): Message? {
        try {
            val content = event.root.content.toModel<org.matrix.android.sdk.api.session.room.model.message.MessageContent>()
                ?: return null

            val messageContent = when (content) {
                is MessageTextContent -> MessageContent.Text(content.body)
                is MessageImageContent -> MessageContent.Image(
                    url = content.url ?: "",
                    body = content.body
                )
                is MessageVideoContent -> MessageContent.Video(
                    url = content.url ?: "",
                    body = content.body
                )
                is MessageFileContent -> MessageContent.File(
                    url = content.url ?: "",
                    filename = content.body,
                    mimeType = content.mimeType
                )
                is MessageAudioContent -> MessageContent.Audio(
                    url = content.url ?: "",
                    duration = content.audioInfo?.duration?.toLong()
                )
                else -> return null
            }

            return Message(
                eventId = event.eventId,
                roomId = event.roomId,
                senderId = event.root.senderId ?: "",
                senderName = event.senderInfo.disambiguatedDisplayName,
                content = messageContent,
                timestamp = event.root.originServerTs ?: 0,
                isOutgoing = event.root.senderId == session.myUserId
            )

        } catch (e: Exception) {
            Timber.e(e, "Error converting timeline event to message")
            return null
        }
    }
}
