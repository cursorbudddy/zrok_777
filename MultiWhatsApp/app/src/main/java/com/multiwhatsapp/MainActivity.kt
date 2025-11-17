package com.multiwhatsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.multiwhatsapp.ui.MainViewModel
import com.multiwhatsapp.ui.screens.*
import com.multiwhatsapp.ui.theme.MultiWhatsAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiWhatsAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MultiWhatsAppApp()
                }
            }
        }
    }
}

@Composable
fun MultiWhatsAppApp(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        NavigationHost(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Accounts", "Chats", "Settings")
    val icons = listOf(
        Icons.Filled.AccountCircle,
        Icons.Filled.Chat,
        Icons.Filled.Settings
    )
    val routes = listOf("accounts", "chats", "settings")

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    navController.navigate(routes[index]) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "accounts",
        modifier = modifier
    ) {
        composable("accounts") {
            AccountsScreen(
                viewModel = viewModel,
                onNavigateToAddAccount = { navController.navigate("add_account") }
            )
        }
        composable("chats") {
            ChatsScreen(
                viewModel = viewModel,
                onChatClick = { chatId ->
                    navController.navigate("chat/$chatId")
                }
            )
        }
        composable("settings") {
            SettingsScreen()
        }
        composable("add_account") {
            AddAccountScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(
                viewModel = viewModel,
                chatId = chatId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
