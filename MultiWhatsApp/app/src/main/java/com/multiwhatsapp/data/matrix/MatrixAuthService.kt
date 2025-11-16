package com.multiwhatsapp.data.matrix

import com.multiwhatsapp.data.models.MatrixAccount
import com.multiwhatsapp.data.models.MatrixAccountConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import java.util.UUID

/**
 * Handles Matrix authentication (login/logout)
 */
class MatrixAuthService(
    private val matrixClientManager: MatrixClientManager
) {

    suspend fun login(config: MatrixAccountConfig): Result<MatrixAccount> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Attempting login to ${config.homeserverUrl} as ${config.username}")

            val authService = matrixClientManager.getAuthenticationService()

            // Create homeserver config
            val homeServerConnectionConfig = HomeServerConnectionConfig
                .Builder()
                .withHomeServerUri(config.homeserverUrl)
                .build()

            // Get login flow
            val loginFlowResult = authService.getLoginFlow(homeServerConnectionConfig)

            if (loginFlowResult !is LoginFlowResult.Success) {
                return@withContext Result.failure(
                    Exception("Failed to get login flow from homeserver")
                )
            }

            // Perform login
            val session = authService.getLoginWizard().login(
                login = config.username,
                password = config.password,
                initialDeviceName = "MultiWhatsApp Android",
                deviceId = null
            )

            // Create account model
            val account = MatrixAccount(
                id = UUID.randomUUID().toString(),
                userId = session.myUserId,
                homeserverUrl = config.homeserverUrl,
                accessToken = session.sessionParams.credentials.accessToken,
                deviceId = session.sessionParams.deviceId ?: "",
                displayName = null,
                avatarUrl = null,
                isActive = true
            )

            // Add session to manager
            matrixClientManager.addSession(account)
            matrixClientManager.setActiveAccount(account.id)

            Timber.d("Login successful for ${account.userId}")
            Result.success(account)

        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            Result.failure(e)
        }
    }

    suspend fun logout(accountId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = matrixClientManager.getSession(accountId)

            if (session != null) {
                // Sign out from homeserver
                session.signOutService().signOut(true)

                // Remove session from manager
                matrixClientManager.removeSession(accountId)

                Timber.d("Logout successful for account: $accountId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Session not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
            Result.failure(e)
        }
    }
}
