package com.multiwhatsapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiwhatsapp.data.Account
import com.multiwhatsapp.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    onNavigateToAddAccount: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddAccount) {
                Icon(Icons.Filled.Add, contentDescription = "Add Account")
            }
        }
    ) { paddingValues ->
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No accounts added yet")
                    Text(
                        "Add your first WhatsApp account to get started",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(accounts) { account ->
                    AccountItem(
                        account = account,
                        onRemove = { viewModel.logout(account) },
                        onSetActive = { viewModel.setCurrentAccount(account) }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: Account,
    onRemove: () -> Unit,
    onSetActive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = account.homeserverUrl,
                    style = MaterialTheme.typography.bodySmall
                )
                if (account.isActive) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Set as active") },
                        onClick = {
                            onSetActive()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = {
                            onRemove()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
