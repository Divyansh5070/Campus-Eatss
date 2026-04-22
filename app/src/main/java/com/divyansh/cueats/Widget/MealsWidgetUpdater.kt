package com.divyansh.cueats.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MealsWidgetUpdater {

    /**
     * Updates all meal widgets with fresh data
     */
    fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val widget = MealsGlanceWidget()

                // Update all instances of the widget
                widget.updateAll(context)

                Log.d("MealsWidget", "Successfully updated all meal widgets")
            } catch (e: Exception) {
                Log.e("MealsWidget", "Error updating widgets: ${e.message}")
            }
        }
    }

    /**
     * Force refresh all widgets by clearing cache and fetching new data
     */
    fun forceRefreshWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clear widget cache
                clearWidgetCache(context)

                // Fetch fresh data
                fetchTodayMealsData(context)

                // Update widgets
                updateWidgets(context)

                Log.d("MealsWidget", "Successfully force refreshed all meal widgets")
            } catch (e: Exception) {
                Log.e("MealsWidget", "Error force refreshing widgets: ${e.message}")
            }
        }
    }

    /**
     * Clear widget cache data
     */
    private fun clearWidgetCache(context: Context) {
        try {
            val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            // Get all keys and remove meal data
            val allKeys = prefs.all.keys
            allKeys.filter { it.startsWith("meals_") || it.startsWith("cache_timestamp_") }
                .forEach { key ->
                    editor.remove(key)
                }

            editor.apply()
            Log.d("MealsWidget", "Widget cache cleared successfully")
        } catch (e: Exception) {
            Log.e("MealsWidget", "Error clearing widget cache: ${e.message}")
        }
    }

    /**
     * Check if widget needs update based on cache age
     */
    fun shouldUpdateWidget(context: Context): Boolean {
        try {
            val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
            val currentTime = System.currentTimeMillis()

            // Check if we have today's data
            val todayDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                .format(java.util.Calendar.getInstance().time)

            val cacheTimestamp = prefs.getLong("cache_timestamp_$todayDate", 0)
            val cacheAgeHours = (currentTime - cacheTimestamp) / (1000 * 60 * 60)

            // Update if cache is older than 2 hours or doesn't exist
            return cacheAgeHours > 2 || !prefs.contains("meals_$todayDate")
        } catch (e: Exception) {
            Log.e("MealsWidget", "Error checking widget update status: ${e.message}")
            return true // Update on error to be safe
        }
    }

    /**
     * Schedule periodic widget updates
     */
    fun schedulePeriodicUpdates(context: Context) {
        // This would typically use WorkManager or AlarmManager
        // for periodic updates, but for simplicity we'll just
        // update when the app starts or when data changes

        if (shouldUpdateWidget(context)) {
            updateWidgets(context)
        }
    }
}

/**
 * Extension function to make widget updates easier from ViewModels
 */
fun Context.updateMealWidgets() {
    MealsWidgetUpdater.updateWidgets(this)
}

/**
 * Widget configuration and theme utilities
 */
object WidgetThemeUtils {

    fun isDarkModeEnabled(context: Context): Boolean {
        return try {
            val configuration = context.resources.configuration
            val currentNightMode = configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        } catch (e: Exception) {
            false // Default to light mode
        }
    }

    fun getWidgetBackgroundColor(context: Context): androidx.compose.ui.graphics.Color {
        return if (isDarkModeEnabled(context)) {
            androidx.compose.ui.graphics.Color(0xFF1E1E1E)
        } else {
            androidx.compose.ui.graphics.Color(0xFFF6F7FB)
        }
    }

    fun getWidgetCardColor(context: Context): androidx.compose.ui.graphics.Color {
        return if (isDarkModeEnabled(context)) {
            androidx.compose.ui.graphics.Color(0xFF2D2D2D)
        } else {
            androidx.compose.ui.graphics.Color.White
        }
    }

    fun getWidgetTextColor(context: Context): androidx.compose.ui.graphics.Color {
        return if (isDarkModeEnabled(context)) {
            androidx.compose.ui.graphics.Color.White
        } else {
            androidx.compose.ui.graphics.Color(0xFF2D3748)
        }
    }
}