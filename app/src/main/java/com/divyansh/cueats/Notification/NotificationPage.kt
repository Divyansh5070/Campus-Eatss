
@file:OptIn(ExperimentalMaterial3Api::class)
package com.divyansh.cueats.Notification
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.navigation.NavController




import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

// Data class for notifications
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val type: String = "general" // general, update, announcement
)
@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val systemTheme = isSystemInDarkTheme()
    val systemUiController = rememberSystemUiController()

    // Theme colors
    val primaryOrange = Color(0xFFFF6B01)
    val lightBackground = Color(0xFFF6F7FB)
    val darkBackground = Color(0xFF121212)
    val surfaceColor = if (systemTheme) Color(0xFF202020) else Color.White
    val textColor = if (systemTheme) Color.White else Color.Black
    val textSecondaryColor = if (systemTheme) Color.LightGray else Color.Gray
    val cardBackground = if (systemTheme) Color(0xFF2A2A2A) else Color(0xFFF8F9FA)

    // Observe ViewModel states
    val notifications by notificationViewModel.notifications.observeAsState(emptyList())
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)
    val isLoading by notificationViewModel.isLoading.observeAsState(false)
    val hasError by notificationViewModel.hasError.observeAsState(false)

    var navigationInProgress by remember { mutableStateOf(false) }

    fun navigateBack() {
        if (!navigationInProgress) {
            navigationInProgress = true
            try {
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                }
            } catch (e: Exception) {
                Log.e("NotificationScreen", "Navigation error", e)
            }
        }
    }

    // Fetch notifications when screen loads
    LaunchedEffect(Unit) {
        notificationViewModel.fetchNotifications()
    }

    BackHandler(enabled = true) {
        navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Notifications",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Unread count badge
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navigateBack() },
                        enabled = !navigationInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (navigationInProgress) Color.White.copy(alpha = 0.5f) else Color.White
                        )
                    }
                },
                actions = {
                    // Mark all as read button
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = {
                                notificationViewModel.markAllNotificationsAsRead()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryOrange,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = if (systemTheme) darkBackground else lightBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = primaryOrange)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading notifications...",
                                color = textSecondaryColor
                            )
                        }
                    }
                }

                hasError -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = textSecondaryColor,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unable to load notifications",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please check your internet connection and try again.",
                                color = textSecondaryColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { notificationViewModel.fetchNotifications() },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryOrange)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }

                notifications.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "No notifications",
                                tint = textSecondaryColor,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notifications yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You'll see app updates and announcements here.",
                                color = textSecondaryColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    // Notifications list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notifications) { notification ->
                            NotificationCard(
                                notification = notification,
                                cardBackground = cardBackground,
                                textColor = textColor,
                                textSecondaryColor = textSecondaryColor,
                                primaryOrange = primaryOrange,
                                onNotificationClick = {
                                    if (!notification.isRead) {
                                        notificationViewModel.markNotificationAsRead(notification.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    cardBackground: Color,
    textColor: Color,
    textSecondaryColor: Color,
    primaryOrange: Color,
    onNotificationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNotificationClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                cardBackground else
                cardBackground.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 1.dp else 3.dp
        ),
        border = if (!notification.isRead) BorderStroke(1.dp, primaryOrange.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Notification type icon
                    Icon(
                        imageVector = when (notification.type) {
                            "update" -> Icons.Default.CheckCircle
                            "announcement" -> Icons.Default.Campaign
                            "order" -> Icons.Default.ShoppingCart
                            else -> Icons.Default.Notifications
                        },
                        contentDescription = notification.type,
                        tint = primaryOrange,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor
                    )
                }

                // Unread indicator
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(primaryOrange)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.message,
                fontSize = 14.sp,
                color = textSecondaryColor,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatTimestamp(notification.timestamp),
                fontSize = 12.sp,
                color = textSecondaryColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun NotificationIconWithBadge(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        // Bell icon positioned slightly left to accommodate badge
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = Color.White,
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.Center)
                .offset(x = (-2).dp) // Slightly left to balance with badge
        )

        // Badge with perfect positioning
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp) // Fine-tuned position
                    .background(
                        color = Color.Red,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = if (unreadCount > 9) 8.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
// Helper function to format timestamp (same as before)
private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown time"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 7 * 86400_000 -> "${diff / 86400_000} days ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
// Function to fetch notifications from Firebase
private fun fetchNotifications(
    onResult: (List<NotificationItem>, String?) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("notifications")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(50) // Limit to recent 50 notifications
        .get()
        .addOnSuccessListener { documents ->
            val notifications = documents.mapNotNull { doc ->
                try {
                    NotificationItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isRead = doc.getBoolean("isRead") ?: false,
                        type = doc.getString("type") ?: "general"
                    )
                } catch (e: Exception) {
                    Log.e("Firestore", "Error parsing notification: ${e.message}")
                    null
                }
            }
            onResult(notifications, null)
        }
        .addOnFailureListener { exception ->
            onResult(emptyList(), exception.message)
        }
}



// Firestore structure example:
// Collection: "notifications"
// Document fields:
// - title: String
// - message: String
// - timestamp: Long (System.currentTimeMillis())
// - isRead: Boolean
// - type: String ("general", "order", "promotion")
// - userId: String (to filter notifications by user)