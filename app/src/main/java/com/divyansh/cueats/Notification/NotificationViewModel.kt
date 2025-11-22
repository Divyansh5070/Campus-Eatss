package com.divyansh.cueats.Notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableLiveData<List<NotificationItem>>(emptyList())
    val notifications: LiveData<List<NotificationItem>> = _notifications

    private val _unreadCount = MutableLiveData<Int>(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasError = MutableLiveData<Boolean>(false)
    val hasError: LiveData<Boolean> = _hasError

    fun fetchNotifications() {
        _isLoading.value = true
        _hasError.value = false

        val db = FirebaseFirestore.getInstance()

        db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
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

                _notifications.value = notifications
                _unreadCount.value = notifications.count { !it.isRead }
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _hasError.value = true
                _isLoading.value = false
                Log.e("Notifications", "Error fetching notifications: ${exception.message}")
            }
    }

    fun markNotificationAsRead(notificationId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                // Update local data
                val updatedNotifications = _notifications.value?.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                } ?: emptyList()

                _notifications.value = updatedNotifications
                _unreadCount.value = updatedNotifications.count { !it.isRead }

                Log.d("Notifications", "Notification marked as read: $notificationId")
            }
            .addOnFailureListener { exception ->
                Log.e("Notifications", "Error marking notification as read: ${exception.message}")
            }
    }

    fun markAllNotificationsAsRead() {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        _notifications.value?.filter { !it.isRead }?.forEach { notification ->
            val docRef = db.collection("notifications").document(notification.id)
            batch.update(docRef, "isRead", true)
        }

        batch.commit()
            .addOnSuccessListener {
                val updatedNotifications = _notifications.value?.map { notification ->
                    notification.copy(isRead = true)
                } ?: emptyList()

                _notifications.value = updatedNotifications
                _unreadCount.value = 0

                Log.d("Notifications", "All notifications marked as read")
            }
            .addOnFailureListener { exception ->
                Log.e("Notifications", "Error marking all notifications as read: ${exception.message}")
            }
    }
}