package com.divyansh.cueats

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCM_Service"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // ✅ Check if the message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val type = remoteMessage.data["type"]
            
            // Handle meal notifications
            if (type == "meal_notification") {
                val mealName = remoteMessage.data["mealName"] ?: "Meal"
                val message = remoteMessage.data["message"] ?: "Time to eat!"
                
                // Show notification (works in both foreground and background)
                sendMealNotification(mealName, message)
                return
            }
            
            // Handle other notification types
            val title = remoteMessage.data["title"] ?: "New Notification"
            val message = remoteMessage.data["message"] ?: "You have a new notification"
            val timestamp = remoteMessage.data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
            
            // ✅ Store notification in Firestore
            storeNotification(title, message, timestamp)
            // ✅ Show notification
            sendNotification(title, message)
        }

        // ✅ Check if the message contains a notification payload (for non-meal notifications)
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            
            // Only handle if it's not a meal notification (those are auto-handled)
            val isMealNotification = remoteMessage.data["type"] == "meal_notification"
            if (!isMealNotification) {
                val title = it.title ?: "New Notification"
                val message = it.body ?: "You have a new notification"

                // ✅ Store notification in Firestore
                storeNotification(title, message, System.currentTimeMillis())

                // ✅ Show notification
                sendNotification(title, message)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // ✅ Save the new token to Firestore
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = getUserId()

                val tokenData = hashMapOf(
                    "token" to token,
                    "device" to Build.MODEL,
                    "platform" to "android",
                    "lastUpdated" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .collection("fcmTokens")
                    .document(token)
                    .set(tokenData)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving new token", e)
            }
        }
    }

    private fun storeNotification(title: String, message: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = getUserId()

                val notificationData = hashMapOf(
                    "title" to title,
                    "message" to message,
                    "timestamp" to timestamp,
                    "read" to false
                )

                FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .collection("notifications")
                    .add(notificationData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Notification stored with ID: ${it.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error storing notification", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in storeNotification", e)
            }
        }
    }

    private fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun sendMealNotification(mealName: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("navigate_to", "meals")

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "meal_reminder_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo33)
            .setContentTitle("$mealName Time! 🍽️")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Meal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for meal times"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(mealName.hashCode(), notificationBuilder.build())
    }
}
