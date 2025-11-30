package com.divyansh.cueats.HomeScreen

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class MealInfo(
    val name: String,
    val startTime: String,
    val endTime: String,
    val items: List<String>,
    val backgroundType: String // breakfast, lunch, snacks, dinner
)

data class HomeScreenState(
    val userName: String = "",
    val currentMeal: MealInfo? = null,
    val nextMeal: MealInfo? = null,
    val mealCountdown: String = "",
    val announcements: List<Announcement> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

    private val announcementRepository = AnnouncementRepository()

    companion object {
        private const val TAG = "HomeViewModel"
    }

    init {
        loadHomeData()
        startMealCountdown()
    }

    /**
     * Convert 24-hour time to 12-hour format
     */
    private fun convertTo12Hour(time24: String): String {
        val parts = time24.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1]

        val period = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }

        return "$hour12:$minute $period"
    }

    /**
     * Load all home screen data
     */
    fun loadHomeData() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                // Load announcements
                val announcements = announcementRepository.getActiveAnnouncements(5)

                // Update meal info
                updateMealInfo()

                _state.value = _state.value.copy(
                    announcements = announcements,
                    isLoading = false
                )

                Log.d(TAG, "Home data loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading home data", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load data"
                )
            }
        }
    }

    /**
     * Set user name
     */
    fun setUserName(name: String) {
        _state.value = _state.value.copy(userName = name)
    }

    /**
     * Update current/next meal information
     */
    private fun updateMealInfo() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

        // Define meal times (in 24-hour format for internal use)
        val mealsInternal = listOf(
            Triple(
                "Breakfast",
                if (isWeekend) "07:30" else "08:00",
                if (isWeekend) "09:00" else "09:30"
            ),
            Triple(
                "Lunch",
                if (isWeekend) "12:00" else "12:30",
                if (isWeekend) "13:45" else "14:00"
            ),
            Triple(
                "Snacks",
                "16:30",
                "17:15"
            ),
            Triple(
                "Dinner",
                "19:30",
                "21:00"
            )
        )

        // Convert to MealInfo with 12-hour format
        val meals = listOf(
            MealInfo(
                name = "Breakfast",
                startTime = convertTo12Hour(if (isWeekend) "07:30" else "08:00"),
                endTime = convertTo12Hour(if (isWeekend) "09:00" else "09:30"),
                items = listOf("Paratha", "Poha", "Bread", "Butter", "Jam", "Tea"),
                backgroundType = "breakfast"
            ),
            MealInfo(
                name = "Lunch",
                startTime = convertTo12Hour(if (isWeekend) "12:00" else "12:30"),
                endTime = convertTo12Hour(if (isWeekend) "13:45" else "14:00"),
                items = listOf("Rice", "Dal", "Roti", "Sabzi", "Salad"),
                backgroundType = "lunch"
            ),
            MealInfo(
                name = "Snacks",
                startTime = convertTo12Hour("16:30"),
                endTime = convertTo12Hour("17:15"),
                items = listOf("Samosa", "Pakora", "Tea", "Biscuits"),
                backgroundType = "snacks"
            ),
            MealInfo(
                name = "Dinner",
                startTime = convertTo12Hour("19:30"),
                endTime = convertTo12Hour("21:00"),
                items = listOf("Rice", "Dal", "Roti", "Sabzi", "Curd"),
                backgroundType = "dinner"
            )
        )

        // Find current or next meal using internal 24-hour times
        var currentMeal: MealInfo? = null
        var nextMeal: MealInfo? = null

        for (i in mealsInternal.indices) {
            val (name, startTime24, endTime24) = mealsInternal[i]
            val (startHour, startMin) = startTime24.split(":").map { it.toInt() }
            val (endHour, endMin) = endTime24.split(":").map { it.toInt() }

            val startTimeInMinutes = startHour * 60 + startMin
            val endTimeInMinutes = endHour * 60 + endMin

            if (currentTimeInMinutes in startTimeInMinutes until endTimeInMinutes) {
                // Currently in this meal time
                currentMeal = meals[i]
                nextMeal = if (i < meals.size - 1) meals[i + 1] else meals[0]
                break
            } else if (currentTimeInMinutes < startTimeInMinutes) {
                // This is the next meal
                nextMeal = meals[i]
                break
            }
        }

        // If no next meal found, it means we're past dinner, so next meal is tomorrow's breakfast
        if (nextMeal == null) {
            nextMeal = meals[0]
        }

        _state.value = _state.value.copy(
            currentMeal = currentMeal,
            nextMeal = nextMeal
        )
    }

    /**
     * Start countdown timer for meal
     */
    private fun startMealCountdown() {
        viewModelScope.launch {
            while (true) {
                updateMealInfo()
                updateCountdown()
                delay(1000) // Update every second
            }
        }
    }

    /**
     * Parse 12-hour time format back to 24-hour for calculations
     */
    private fun parse12HourTo24Hour(time12: String): Pair<Int, Int> {
        val parts = time12.split(" ")
        val timeParts = parts[0].split(":")
        var hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val period = parts[1]

        hour = when {
            period == "PM" && hour != 12 -> hour + 12
            period == "AM" && hour == 12 -> 0
            else -> hour
        }

        return Pair(hour, minute)
    }

    /**
     * Update countdown string
     */
    private fun updateCountdown() {
        val meal = _state.value.currentMeal ?: _state.value.nextMeal
        if (meal == null) {
            _state.value = _state.value.copy(mealCountdown = "")
            return
        }

        val calendar = Calendar.getInstance()
        val (targetHour, targetMin) = if (_state.value.currentMeal != null) {
            // Countdown to end of current meal
            parse12HourTo24Hour(meal.endTime)
        } else {
            // Countdown to start of next meal
            parse12HourTo24Hour(meal.startTime)
        }

        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMin)
            set(Calendar.SECOND, 0)
        }

        // If target time is in the past, add a day
        if (targetCalendar.timeInMillis < calendar.timeInMillis) {
            targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diffInMillis = targetCalendar.timeInMillis - calendar.timeInMillis
        val hours = (diffInMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((diffInMillis / (1000 * 60)) % 60).toInt()
        val seconds = ((diffInMillis / 1000) % 60).toInt()

        val countdownText = if (_state.value.currentMeal != null) {
            "Ends in ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}"
        } else {
            "Starts in ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}"
        }

        _state.value = _state.value.copy(mealCountdown = countdownText)
    }


}