package com.divyansh.cueats.notifirebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.divyansh.cueats.MainActivity
import com.divyansh.cueats.R


class MealNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // If no meal name is provided, exit without showing a notification
        val mealName = inputData.getString("mealName")
        if (mealName.isNullOrEmpty()) {
            return Result.success()
        }
        val message = inputData.getString("message") ?: "Enjoy your meal! 🍽"

        showNotification(mealName, message)
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "meal_reminder_channel"
        val notificationId = title.hashCode()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Create notification channel (For Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Meal Reminders", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // ✅ Intent to open the app when notification is clicked
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ Custom icon added (Replace `R.drawable.ic_meal_notification` with your actual icon)
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.logo33) // Make sure this icon exists in `res/drawable`
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
