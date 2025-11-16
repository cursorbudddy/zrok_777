package com.multiwhatsapp.data.matrix

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import timber.log.Timber

/**
 * Service to interact with mautrix-whatsapp bridge
 * The bridge bot typically has a user ID like @whatsappbot:yourdomain.com
 */
class WhatsAppBridgeService(
    private val session: Session,
    private val bridgeBotId: String = "@whatsappbot:yourdomain.com" // Configure this
) {

    companion object {
        const val COMMAND_LOGIN = "login"
        const val COMMAND_LOGOUT = "logout"
        const val COMMAND_HELP = "help"
        const val COMMAND_PING = "ping"
    }

    /**
     * Get or create the bridge bot room (where you send commands and receive QR codes)
     */
    suspend fun getBridgeBotRoom(): Room? = withContext(Dispatchers.IO) {
        try {
            // Try to find existing DM with bridge bot
            val existingRoom = session.roomService()
                .getRoomSummaries(emptyList())
                .find { roomSummary ->
                    roomSummary.isDirect &&
                    roomSummary.otherMemberIds.contains(bridgeBotId)
                }

            if (existingRoom != null) {
                Timber.d("Found existing bridge bot room: ${existingRoom.roomId}")
                return@withContext session.roomService().getRoom(existingRoom.roomId)
            }

            // Create new DM with bridge bot
            Timber.d("Creating new DM with bridge bot: $bridgeBotId")
            val roomId = session.roomService().createDirectRoom(
                otherUserId = bridgeBotId,
                enableEncryption = false
            )

            session.roomService().getRoom(roomId)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get/create bridge bot room")
            null
        }
    }

    /**
     * Send login command to bridge bot to get QR code
     */
    suspend fun requestWhatsAppLogin(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val room = getBridgeBotRoom()
                ?: return@withContext Result.failure(Exception("Failed to get bridge bot room"))

            // Send login command
            room.sendService().sendTextMessage(COMMAND_LOGIN)

            Timber.d("Login command sent to bridge bot")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to request WhatsApp login")
            Result.failure(e)
        }
    }

    /**
     * Listen for QR code from bridge bot
     * The bridge will send the QR code as an image message
     */
    fun observeQRCode(): Flow<String?> = flow {
        try {
            val room = getBridgeBotRoom()
            if (room == null) {
                emit(null)
                return@flow
            }

            // Listen for new messages in the bridge bot room
            room.timelineService()
                .getTimelineFlow()
                .collect { timeline ->
                    timeline.events.forEach { timelineEvent ->
                        val content = timelineEvent.root.content.toModel<MessageContent>()

                        when (content) {
                            is MessageImageContent -> {
                                // QR code received as image
                                val imageUrl = content.url
                                Timber.d("QR code image received: $imageUrl")
                                emit(imageUrl)
                            }
                            is MessageTextContent -> {
                                // Check if message contains QR code text/link
                                if (content.body.contains("scan", ignoreCase = true) ||
                                    content.body.contains("QR", ignoreCase = true)) {
                                    Timber.d("QR code instruction received: ${content.body}")
                                }
                            }
                            else -> {
                                // Ignore other message types
                            }
                        }
                    }
                }

        } catch (e: Exception) {
            Timber.e(e, "Error observing QR code")
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Send command to bridge bot
     */
    suspend fun sendBridgeCommand(command: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val room = getBridgeBotRoom()
                ?: return@withContext Result.failure(Exception("Failed to get bridge bot room"))

            room.sendService().sendTextMessage(command)

            Timber.d("Bridge command sent: $command")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to send bridge command: $command")
            Result.failure(e)
        }
    }

    /**
     * Get all WhatsApp bridged rooms (portals)
     * These are identified by ghost users with WhatsApp prefix
     */
    suspend fun getWhatsAppChats(): List<Room> = withContext(Dispatchers.IO) {
        try {
            val allRooms = session.roomService().getRoomSummaries(emptyList())

            // Filter rooms that are WhatsApp portals
            // Typically, WhatsApp ghost users have IDs like @whatsapp_1234567890:domain.com
            val whatsappRooms = allRooms
                .filter { roomSummary ->
                    roomSummary.otherMemberIds.any { userId ->
                        userId.contains("whatsapp_") || userId.contains("_whatsapp")
                    }
                }
                .mapNotNull { roomSummary ->
                    session.roomService().getRoom(roomSummary.roomId)
                }

            Timber.d("Found ${whatsappRooms.size} WhatsApp chats")
            whatsappRooms

        } catch (e: Exception) {
            Timber.e(e, "Failed to get WhatsApp chats")
            emptyList()
        }
    }

    /**
     * Check if a room is a WhatsApp portal
     */
    fun isWhatsAppRoom(room: Room): Boolean {
        val roomSummary = room.roomSummary() ?: return false

        return roomSummary.otherMemberIds.any { userId ->
            userId.contains("whatsapp_") || userId.contains("_whatsapp")
        }
    }

    /**
     * Extract WhatsApp phone number from ghost user ID
     * Example: @whatsapp_1234567890:domain.com -> +1234567890
     */
    fun extractWhatsAppNumber(userId: String): String? {
        val pattern = Regex("@whatsapp[_-]?(\\d+):")
        val match = pattern.find(userId)
        return match?.groupValues?.get(1)?.let { "+$it" }
    }
}
