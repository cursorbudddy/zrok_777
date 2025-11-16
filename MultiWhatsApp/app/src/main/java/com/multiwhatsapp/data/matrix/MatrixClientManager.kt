package com.multiwhatsapp.data.matrix

import android.content.Context
import com.multiwhatsapp.data.models.MatrixAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import java.io.File

/**
 * Manages multiple Matrix clients (one per WhatsApp account)
 */
class MatrixClientManager(private val context: Context) {

    private val _sessions = MutableStateFlow<Map<String, Session>>(emptyMap())
    val sessions: StateFlow<Map<String, Session>> = _sessions.asStateFlow()

    private val _activeAccountId = MutableStateFlow<String?>(null)
    val activeAccountId: StateFlow<String?> = _activeAccountId.asStateFlow()

    private lateinit var matrix: Matrix

    init {
        initializeMatrix()
    }

    private fun initializeMatrix() {
        val matrixConfiguration = MatrixConfiguration(
            applicationFlavor = "MultiWhatsApp",
            roomDisplayNameFallbackProvider = { roomId ->
                "Chat ${roomId.takeLast(8)}"
            }
        )

        matrix = Matrix(
            context = context,
            matrixConfiguration = matrixConfiguration
        )

        Timber.d("Matrix SDK initialized")
    }

    fun getAuthenticationService(): AuthenticationService {
        return matrix.authenticationService()
    }

    suspend fun addSession(account: MatrixAccount) {
        try {
            // Create session from existing credentials
            val session = matrix.authenticationService()
                .getLastAuthenticatedSession()

            if (session != null) {
                _sessions.value = _sessions.value + (account.id to session)

                // Start sync for this session
                session.open()
                session.syncService().startSync(true)

                Timber.d("Session added for account: ${account.userId}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to add session for account: ${account.userId}")
            throw e
        }
    }

    suspend fun removeSession(accountId: String) {
        _sessions.value[accountId]?.let { session ->
            session.syncService().stopSync()
            session.close()
            _sessions.value = _sessions.value - accountId

            if (_activeAccountId.value == accountId) {
                _activeAccountId.value = _sessions.value.keys.firstOrNull()
            }

            Timber.d("Session removed for account: $accountId")
        }
    }

    fun setActiveAccount(accountId: String) {
        if (_sessions.value.containsKey(accountId)) {
            _activeAccountId.value = accountId
            Timber.d("Active account changed to: $accountId")
        }
    }

    fun getSession(accountId: String): Session? {
        return _sessions.value[accountId]
    }

    fun getActiveSession(): Session? {
        return _activeAccountId.value?.let { getSession(it) }
    }

    fun getAllSessions(): Map<String, Session> {
        return _sessions.value
    }

    suspend fun stopAllSessions() {
        _sessions.value.values.forEach { session ->
            try {
                session.syncService().stopSync()
                session.close()
            } catch (e: Exception) {
                Timber.e(e, "Error stopping session")
            }
        }
        _sessions.value = emptyMap()
        _activeAccountId.value = null
    }
}
