package com.divyansh.cueats.Mess

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple repository to track user's daily calorie consumption
 * Uses SharedPreferences for quick implementation
 */
class UserCaloriesRepository(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("user_calories", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    data class DailyCalories(
        val date: String,
        var totalCalories: Int,
        val dishes: MutableMap<String, Int> = mutableMapOf() // dishName -> count
    )
    
    /**
     * Get today's date as string
     */
    private fun getTodayDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
    
    /**
     * Get today's calorie data
     */
    fun getTodayCalories(): DailyCalories {
        val today = getTodayDate()
        val json = prefs.getString("calories_$today", null)
        
        return if (json != null) {
            try {
                gson.fromJson(json, DailyCalories::class.java)
            } catch (e: Exception) {
                Log.e("UserCalories", "Error parsing data: ${e.message}")
                DailyCalories(today, 0)
            }
        } else {
            DailyCalories(today, 0)
        }
    }
    
    /**
     * Add a dish to today's calories
     */
    fun addDish(dishName: String, calories: Int) {
        val data = getTodayCalories()
        val currentCount = data.dishes[dishName] ?: 0
        data.dishes[dishName] = currentCount + 1
        data.totalCalories += calories
        
        saveTodayCalories(data)
        Log.d("UserCalories", "Added $dishName (+$calories cal). Total: ${data.totalCalories}")
    }
    
    /**
     * Add a dish to a specific date's calories
     */
    fun addDishToDate(date: String, dishName: String, calories: Int) {
        val data = getCaloriesForDate(date)
        val currentCount = data.dishes[dishName] ?: 0
        data.dishes[dishName] = currentCount + 1
        data.totalCalories += calories
        
        saveCaloriesForDate(date, data)
        Log.d("UserCalories", "Added $dishName to $date (+$calories cal). Total: ${data.totalCalories}")
    }
    
    /**
     * Remove a dish from today's calories
     */
    fun removeDish(dishName: String, calories: Int) {
        val data = getTodayCalories()
        val currentCount = data.dishes[dishName] ?: 0
        
        if (currentCount > 0) {
            data.dishes[dishName] = currentCount - 1
            data.totalCalories -= calories
            
            if (data.dishes[dishName] == 0) {
                data.dishes.remove(dishName)
            }
            
            saveTodayCalories(data)
            Log.d("UserCalories", "Removed $dishName (-$calories cal). Total: ${data.totalCalories}")
        }
    }
    
    /**
     * Remove a dish from a specific date's calories
     */
    fun removeDishFromDate(date: String, dishName: String, calories: Int) {
        val data = getCaloriesForDate(date)
        val currentCount = data.dishes[dishName] ?: 0
        
        if (currentCount > 0) {
            data.dishes[dishName] = currentCount - 1
            data.totalCalories -= calories
            
            if (data.dishes[dishName] == 0) {
                data.dishes.remove(dishName)
            }
            
            saveCaloriesForDate(date, data)
            Log.d("UserCalories", "Removed $dishName from $date (-$calories cal). Total: ${data.totalCalories}")
        }
    }
    
    /**
     * Get count for a specific dish today
     */
    fun getDishCount(dishName: String): Int {
        val data = getTodayCalories()
        return data.dishes[dishName] ?: 0
    }
    
    /**
     * Get count for a specific dish on a specific date
     */
    fun getDishCountForDate(date: String, dishName: String): Int {
        val data = getCaloriesForDate(date)
        return data.dishes[dishName] ?: 0
    }
    
    /**
     * Save today's calorie data
     */
    private fun saveTodayCalories(data: DailyCalories) {
        val today = getTodayDate()
        val json = gson.toJson(data)
        prefs.edit().putString("calories_$today", json).apply()
    }
    
    /**
     * Save calorie data for a specific date
     */
    private fun saveCaloriesForDate(date: String, data: DailyCalories) {
        val json = gson.toJson(data)
        prefs.edit().putString("calories_$date", json).apply()
    }
    
    /**
     * Get calorie data for a specific date
     */
    fun getCaloriesForDate(date: String): DailyCalories {
        val json = prefs.getString("calories_$date", null)
        
        return if (json != null) {
            try {
                gson.fromJson(json, DailyCalories::class.java)
            } catch (e: Exception) {
                Log.e("UserCalories", "Error parsing data for $date: ${e.message}")
                DailyCalories(date, 0)
            }
        } else {
            DailyCalories(date, 0)
        }
    }
    
    /**
     * Check if goal was achieved on a specific date
     */
    fun wasGoalAchieved(date: String): Boolean {
        val data = getCaloriesForDate(date)
        val goal = getDailyGoal()
        return data.totalCalories >= goal
    }
    
    /**
     * Calculate streak based on consecutive days where goal was achieved
     */
    fun getStreak(): Int {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var streak = 0
        
        // Start from yesterday and count backwards
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        
        // Check up to 365 days back (reasonable limit)
        for (i in 0 until 365) {
            val dateStr = formatter.format(calendar.time)
            
            if (wasGoalAchieved(dateStr)) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // Streak broken
                break
            }
        }
        
        return streak
    }
    
    /**
     * Get daily goal (can be made customizable later)
     */
    fun getDailyGoal(): Int {
        return prefs.getInt("daily_goal", 2000)
    }
    
    /**
     * Set daily goal
     */
    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt("daily_goal", goal).apply()
    }
}
