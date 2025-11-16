package com.multiwhatsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.multiwhatsapp.data.matrix.MatrixClientManager
import com.multiwhatsapp.data.repository.AccountRepository
import com.multiwhatsapp.data.repository.ChatRepository
import com.multiwhatsapp.ui.chat.ChatListScreen
import com.multiwhatsapp.ui.chat.ChatListViewModel
import com.multiwhatsapp.ui.chat.ChatScreen
import com.multiwhatsapp.ui.chat.ChatViewModel
import com.multiwhatsapp.ui.login.LoginScreen
import com.multiwhatsapp.ui.login.LoginViewModel
import com.multiwhatsapp.ui.theme.MultiWhatsAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var matrixClientManager: MatrixClientManager
    private lateinit var accountRepository: AccountRepository
    private lateinit var chatRepository: ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repositories
        matrixClientManager = MatrixClientManager(applicationContext)
        accountRepository = AccountRepository(applicationContext)
        chatRepository = ChatRepository(matrixClientManager)

        setContent {
            MultiWhatsAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MultiWhatsAppApp(
                        matrixClientManager = matrixClientManager,
                        accountRepository = accountRepository,
                        chatRepository = chatRepository
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        kotlinx.coroutines.MainScope().launch {
            matrixClientManager.stopAllSessions()
        }
    }
}

@Composable
fun MultiWhatsAppApp(
    matrixClientManager: MatrixClientManager,
    accountRepository: AccountRepository,
    chatRepository: ChatRepository
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
    val scope = rememberCoroutineScope()

    // Check if user has accounts on startup
    LaunchedEffect(Unit) {
        val accounts = accountRepository.getAccounts().first()
        currentScreen = if (accounts.isEmpty()) {
            Screen.Login
        } else {
            // Restore sessions for all accounts
            accounts.forEach { account ->
                try {
                    matrixClientManager.addSession(account)
                } catch (e: Exception) {
                    // If session restoration fails, show login
                    currentScreen = Screen.Login
                    return@LaunchedEffect
                }
            }

            // Set active account
            val activeAccountId = accountRepository.getActiveAccountId().first()
            if (activeAccountId != null) {
                matrixClientManager.setActiveAccount(activeAccountId)
            }

            Screen.ChatList
        }
    }

    when (val screen = currentScreen) {
        Screen.Loading -> {
            // Show loading indicator
        }

        Screen.Login -> {
            val viewModel = remember {
                LoginViewModel(matrixClientManager, accountRepository)
            }
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    currentScreen = Screen.ChatList
                }
            )
        }

        Screen.ChatList -> {
            val viewModel = remember {
                ChatListViewModel(chatRepository, accountRepository)
            }
            ChatListScreen(
                viewModel = viewModel,
                onChatClick = { chat ->
                    currentScreen = Screen.Chat(chat.roomId, chat.accountId, chat.displayName)
                },
                onAddAccountClick = {
                    currentScreen = Screen.Login
                }
            )
        }

        is Screen.Chat -> {
            val viewModel = remember(screen.roomId) {
                ChatViewModel(chatRepository, screen.roomId, screen.accountId)
            }
            ChatScreen(
                viewModel = viewModel,
                chatName = screen.chatName,
                onBackClick = {
                    currentScreen = Screen.ChatList
                }
            )
        }
    }
}

sealed class Screen {
    object Loading : Screen()
    object Login : Screen()
    object ChatList : Screen()
    data class Chat(val roomId: String, val accountId: String, val chatName: String) : Screen()
}
