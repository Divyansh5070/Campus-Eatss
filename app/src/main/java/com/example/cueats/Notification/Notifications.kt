package com.example.cueats.Notification

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import com.example.cueats.notifirebase.NotificationViewModel

// app id cbcda286-0418-4fca-88d4-836bae34a06c
// Data class to represent a notification
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val read: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val fcmToken by viewModel.fcmToken.collectAsState()

    // Effect to initialize Firebase messaging
    LaunchedEffect(Unit) {
        viewModel.initializeFirebaseMessaging()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1A103E)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Notifications",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2E1D7D)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (notifications.isEmpty()) {
                    // Show empty state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            modifier = Modifier
                                .size(120.dp)
                                .padding(bottom = 24.dp),
                            tint = Color(0xFFFFD700)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3D2A8E)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Stay Tuned!",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "The schedule will be updated whenever I receive the latest meal plan, and the app is currently in development.",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "- Divyansh",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 20.sp
                                    ),
                                    color = Color(0xFFFFD700),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Show list of notifications
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notifications) { notification ->
                            NotificationCard(notification = notification)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2A8E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFFD700)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(notification.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
