package com.example.cueats.notifirebase

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cueats.Notification.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationViewModel"

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()

    fun initializeFirebaseMessaging() {
        viewModelScope.launch {
            try {
                // Get the FCM token
                val token = FirebaseMessaging.getInstance().token.await()
                _fcmToken.value = token

                // Store the token in Firestore
                saveTokenToFirestore(token)

                // Fetch existing notifications for this user
                fetchNotifications()

                Log.d(TAG, "FCM Token: $token")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firebase Messaging", e)
            }
        }
    }

    // In your NotificationViewModel or dedicated TokenManager class
    private suspend fun saveTokenToFirestore(token: String) {
        try {
            val userId = getUserId()

            val tokenData = hashMapOf(
                "token" to token,
                "device" to android.os.Build.MODEL,
                "platform" to "android",
                "lastUpdated" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(userId)
                .collection("fcmTokens")
                .document(token)
                .set(tokenData)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving token to Firestore", e)
        }
    }

    private suspend fun saveTokenToGlobalCollection(userId: String, token: String) {
        try {
            val tokenData = hashMapOf(
                "userId" to userId,
                "token" to token,
                "device" to android.os.Build.MODEL,
                "platform" to "android",
                "lastUpdated" to System.currentTimeMillis(),
                "active" to true
            )

            firestore.collection("fcmTokens")
                .document(token)
                .set(tokenData)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to global token collection", e)
        }
    }

    private fun getUserId(): String {
        // Replace with your actual user ID retrieval logic
        // For example, if using Firebase Auth:
        // return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        return "user123" // Placeholder
    }

    private suspend fun fetchNotifications() {
        try {
            val userId = getUserId()

            val notificationsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val notificationsList = notificationsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                NotificationItem(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    message = data["message"] as? String ?: "",
                    timestamp = (data["timestamp"] as? Long) ?: 0L,
                    read = (data["read"] as? Boolean) ?: false
                )
            }

            _notifications.value = notificationsList
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications", e)
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val userId = getUserId()

                firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(notificationId)
                    .update("read", true)
                    .await()

                // Update the local state
                val updatedList = _notifications.value.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(read = true)
                    } else {
                        notification
                    }
                }

                _notifications.value = updatedList
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
            }
        }
    }
}