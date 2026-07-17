package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.repository.PulseStreamRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PulseStreamViewModel
import com.example.ui.viewmodel.PulseStreamViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Database, Repository, and ViewModel
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = PulseStreamRepository(db)
        val factory = PulseStreamViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[PulseStreamViewModel::class.java]

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainLayout(viewModel)
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: PulseStreamViewModel) {
    var currentTab by remember { mutableStateOf(0) }
    var showChatScreen by remember { mutableStateOf(false) }
    val notifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    if (showChatScreen) {
        ChatScreen(
            viewModel = viewModel,
            onBackClick = { showChatScreen = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Tab 0: Home
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_item_home")
                )

                // Tab 1: Shorts
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                            contentDescription = "Shorts"
                        )
                    },
                    label = { Text("Shorts") },
                    modifier = Modifier.testTag("nav_item_shorts")
                )

                // Tab 2: Create
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.AddBox else Icons.Outlined.AddBox,
                            contentDescription = "Create"
                        )
                    },
                    label = { Text("Create") },
                    modifier = Modifier.testTag("nav_item_create")
                )

                // Tab 3: Alerts/Notifications
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.testTag("unread_badge_count")
                                    ) {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Alerts"
                            )
                        }
                    },
                    label = { Text("Alerts") },
                    modifier = Modifier.testTag("nav_item_alerts")
                )

                // Tab 4: Profile
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 4) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_item_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = { userId ->
                        // In high-fidelity we can expand profile navigation,
                        // for now we switch to profile tab
                        currentTab = 4
                    },
                    onNavigateToChat = { showChatScreen = true }
                )
                1 -> ShortsScreen(viewModel = viewModel)
                2 -> CreateScreen(
                    viewModel = viewModel,
                    onPostCreated = { currentTab = 0 }
                )
                3 -> NotificationsScreen(viewModel = viewModel)
                4 -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}
}
