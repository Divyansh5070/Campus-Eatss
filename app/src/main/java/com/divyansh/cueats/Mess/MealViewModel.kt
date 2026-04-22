package com.divyansh.cueats.Mess

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.divyansh.cueats.widget.MealsWidgetUpdater
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MealViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Replace your existing MealViewModel like/dislike related code with this simplified version
class MealViewModel(private val context: Context) : ViewModel() {
    // Keep ALL your existing properties
    private val _weeklyMenuData = MutableLiveData<List<DayData>>()
    val weeklyMenuData: LiveData<List<DayData>> get() = _weeklyMenuData

    private val _mealTimings = MutableLiveData<Map<String, String>>()

    private val sharedPrefs = context.getSharedPreferences("meal_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // NEW: Dish repository and state for calorie tracking
    private val dishRepository = DishRepository(context)
    private val _dishesMap = MutableLiveData<Map<String, Dish>>()
    val dishesMap: LiveData<Map<String, Dish>> get() = _dishesMap

    // NEW: User calories repository and state
    private val userCaloriesRepository = UserCaloriesRepository(context)
    private val _dailyCalories = MutableLiveData<Int>()
    val dailyCalories: LiveData<Int> get() = _dailyCalories
    
    private val _dailyGoal = MutableLiveData<Int>()
    val dailyGoal: LiveData<Int> get() = _dailyGoal
    
    // NEW: Trigger for calorie updates
    private val _calorieUpdateTrigger = MutableLiveData<Long>()
    val calorieUpdateTrigger: LiveData<Long> get() = _calorieUpdateTrigger

    // Store listener reference for cleanup
    private var databaseListener: ValueEventListener? = null
    private var databaseReference: DatabaseReference? = null


    private companion object {
        const val CACHE_KEY_MEAL_DATA = "cached_meal_data"
        const val CACHE_KEY_MEAL_TIMINGS = "cached_meal_timings"
        const val CACHE_KEY_TIMESTAMP = "cache_timestamp"
        const val CACHE_DURATION_HOURS = 11
        const val CACHE_DURATION_MS = CACHE_DURATION_HOURS * 60 * 60 * 1000L
    }

    init {
        // Initialize meal timings
        _mealTimings.value = emptyMap()
        
        // Initialize dishes map
        _dishesMap.value = emptyMap()
        
        // NEW: Load today's calories
        refreshCalories()
        
        // Load meal data with cache
        loadMealDataWithCache()
    }

    override fun onCleared() {
        super.onCleared()
        databaseListener?.let { listener ->
            databaseReference?.removeEventListener(listener)
        }
    }

    // Keep all your existing cache functions
    private fun loadMealDataWithCache() {
        val cachedTimestamp = sharedPrefs.getLong(CACHE_KEY_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val isCacheValid = (currentTime - cachedTimestamp) < CACHE_DURATION_MS

        if (isCacheValid && hasCachedData()) {
            Log.d("MealCache", "Loading from cache...")
            loadFromCache()
        } else {
            Log.d("MealCache", "Cache expired or empty, fetching from server...")
            fetchDailyMenuFromServer()
        }
    }

    private fun hasCachedData(): Boolean {
        return sharedPrefs.contains(CACHE_KEY_MEAL_DATA) &&
                sharedPrefs.contains(CACHE_KEY_MEAL_TIMINGS)
    }

    private fun loadFromCache() {
        try {
            val cachedMealDataJson = sharedPrefs.getString(CACHE_KEY_MEAL_DATA, null)
            if (cachedMealDataJson != null) {
                val type = object : TypeToken<List<DayData>>() {}.type
                val cachedMealData: List<DayData> = gson.fromJson(cachedMealDataJson, type)
                _weeklyMenuData.value = cachedMealData
            }

            val cachedTimingsJson = sharedPrefs.getString(CACHE_KEY_MEAL_TIMINGS, null)
            if (cachedTimingsJson != null) {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val cachedTimings: Map<String, String> = gson.fromJson(cachedTimingsJson, type)
                _mealTimings.value = cachedTimings
            }

            // NEW: Fetch dishes for cached meals
            _weeklyMenuData.value?.let { meals ->
                fetchDishesForMeals(meals)
            }

            Log.d("MealCache", "Successfully loaded data from cache")
        } catch (e: Exception) {
            Log.e("MealCache", "Error loading from cache: ${e.message}")
            fetchDailyMenuFromServer()
        }
    }


    private fun getDefaultTiming(mealType: String): String {
        val isWeekend = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        return if (isWeekend) {
            when (mealType.lowercase()) {
                "breakfast" -> "8:00 AM - 9:30 AM"
                "lunch" -> "12:30 PM - 2:00 PM"
                "snacks" -> "4:30 PM - 5:15 PM"
                "dinner" -> "7:30 PM - 9:00 PM"
                "south indian dinner" -> "7:30 PM - 9:00 PM" // NEW: Slightly later timing
                else -> ""
            }
        } else {
            when (mealType.lowercase()) {
                "breakfast" -> "7:30 AM - 9:00 AM"
                "lunch" -> "12:00 PM - 1:45 PM"
                "snacks" -> "4:30 PM - 5:15 PM"
                "dinner" -> "7:30 PM - 9:00 PM"
                "south indian dinner" -> "7:30 PM - 9:00 PM" // NEW: Slightly later timing
                else -> ""
            }
        }
    }

    fun getTimingForMeal(mealType: String): String {
        return _mealTimings.value?.get(mealType) ?: getDefaultTiming(mealType)
    }

    // Keep all your existing meal data functions
    private fun fetchDailyMenuFromServer() {
        _isLoading.value = true
        databaseReference =
            FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
                .reference.child("meals")

        databaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseData", "Snapshot value: ${snapshot.value}")
                _isLoading.value = false

                if (snapshot.exists()) {
                    // UPDATED: Added South Indian Dinner to the meal order
                    val mealOrder = listOf("Breakfast", "Lunch", "Snacks", "Dinner", "South Indian Dinner")
                    val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val timingsMap = mutableMapOf<String, String>()
                    val allDays = mutableListOf<DayData>()

                    snapshot.children.forEach { weekSnapshot ->
                        Log.d("FirebaseData", "Processing Week: ${weekSnapshot.key}")

                        dayOrder.forEach { dayName ->
                            val daySnapshot = weekSnapshot.child(dayName)
                            if (daySnapshot.exists()) {
                                val dateStr = daySnapshot.child("date").getValue(String::class.java)
                                    ?: "No date"

                                val meals = daySnapshot.children.mapNotNull { mealSnapshot ->
                                    if (mealSnapshot.key == "date") return@mapNotNull null
                                    val meal = mealSnapshot.getValue(Meal::class.java)
                                    meal?.let {
                                        val mealTiming = it.getActualTiming()
                                        if (mealTiming.isNotEmpty()) {
                                            timingsMap[it.type] = mealTiming
                                            Log.d(
                                                "TimingExtracted",
                                                "Meal: ${it.type}, Timing: $mealTiming"
                                            )
                                        }
                                        it
                                    }
                                }.sortedBy { meal ->
                                    // SAFE: Use indexOf with fallback to prevent crashes
                                    val index = mealOrder.indexOf(meal.type)
                                    if (index == -1) mealOrder.size else index // Put unknown meals at the end
                                }

                                if (meals.isNotEmpty()) {
                                    allDays.add(
                                        DayData(
                                            day = dayName,
                                            date = dateStr,
                                            meals = meals
                                        )
                                    )
                                }
                            }
                        }
                    }

                    _mealTimings.value = timingsMap
                    Log.d("AllTimings", "Stored timings: $timingsMap")

                    val sortedDays = allDays.sortedBy { dayData ->
                        try {
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            formatter.parse(dayData.date)?.time ?: Long.MAX_VALUE
                        } catch (e: Exception) {
                            Log.e("DateParsing", "Error parsing date: ${e.message}")
                            Long.MAX_VALUE
                        }
                    }

                    _weeklyMenuData.value = sortedDays
                    saveToCache(sortedDays, timingsMap)

                    // NEW: Fetch dish data for calorie tracking
                    fetchDishesForMeals(sortedDays)

                    // Update widget after successful data load
                    updateWidget()

                    Log.d(
                        "FirebaseData",
                        "Processed ${sortedDays.size} days with timings and cached"
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
                _isLoading.value = false
            }
        }

        databaseReference?.addValueEventListener(databaseListener!!)
    }

    // ✅ Add this to MealViewModel
    private fun updateWidget() {
        try {
            MealsWidgetUpdater.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("WidgetUpdate", "Error updating widget: ${e.message}")
        }
    }


    private fun saveToCache(mealData: List<DayData>, timings: Map<String, String>) {
        try {
            val editor = sharedPrefs.edit()
            val mealDataJson = gson.toJson(mealData)
            editor.putString(CACHE_KEY_MEAL_DATA, mealDataJson)
            val timingsJson = gson.toJson(timings)
            editor.putString(CACHE_KEY_MEAL_TIMINGS, timingsJson)
            editor.putLong(CACHE_KEY_TIMESTAMP, System.currentTimeMillis())
            editor.apply()
            Log.d("MealCache", "Data saved to cache successfully")
        } catch (e: Exception) {
            Log.e("MealCache", "Error saving to cache: ${e.message}")
        }
    }

    fun forceRefreshMealData() {
        Log.d("MealCache", "Force refreshing meal data...")
        clearCache()
        // Also clear dish cache
        dishRepository.clearCache()
        fetchDailyMenuFromServer()
    }

    private fun clearCache() {
        val editor = sharedPrefs.edit()
        editor.remove(CACHE_KEY_MEAL_DATA)
        editor.remove(CACHE_KEY_MEAL_TIMINGS)
        editor.remove(CACHE_KEY_TIMESTAMP)
        editor.apply()
        Log.d("MealCache", "Cache cleared")
    }

    fun isCacheValid(): Boolean {
        val cachedTimestamp = sharedPrefs.getLong(CACHE_KEY_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - cachedTimestamp) < CACHE_DURATION_MS
    }

    /**
     * Helper function to fetch dish data from Firestore
     * Converts dish names to IDs and fetches calorie info
     */
    fun fetchDishesForMeals(meals: List<DayData>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Collect all unique dish names from all meals
                val allDishNames = mutableSetOf<String>()
                meals.forEach { day ->
                    day.meals.forEach { meal ->
                        allDishNames.addAll(meal.items)
                    }
                }

                // Convert dish names to IDs (lowercase, replace spaces with underscores)
                val dishIds = allDishNames.map { dishName ->
                    dishName.lowercase()
                        .replace(" ", "_")
                        .replace("(", "")
                        .replace(")", "")
                }

                Log.d("DishFetch", "Fetching ${dishIds.size} dishes")

                // Fetch dishes from Firestore
                val dishes = dishRepository.getDishesByIds(dishIds)
                
                // Update the dishes map on main thread
                _dishesMap.postValue(dishes)
                
                Log.d("DishFetch", "Successfully fetched ${dishes.size} dishes")
            } catch (e: Exception) {
                Log.e("DishFetch", "Error fetching dishes: ${e.message}")
            }
        }
    }


    fun getCacheAgeHours(): Long {
        val cachedTimestamp = sharedPrefs.getLong(CACHE_KEY_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - cachedTimestamp) / (1000 * 60 * 60)
    }

    /**
     * Refresh calorie data from repository
     */
    fun refreshCalories() {
        val todayData = userCaloriesRepository.getTodayCalories()
        _dailyCalories.value = todayData.totalCalories
        _dailyGoal.value = userCaloriesRepository.getDailyGoal()
    }
    
    /**
     * Get calories for a specific date (format: dd/MM/yyyy)
     */
    fun getCaloriesForDate(dateStr: String): Int {
        return try {
            // Convert from dd/MM/yyyy to yyyy-MM-dd
            val inputFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormatter.parse(dateStr)
            val formattedDate = outputFormatter.format(date ?: return 0)
            
            val data = userCaloriesRepository.getCaloriesForDate(formattedDate)
            data.totalCalories
        } catch (e: Exception) {
            Log.e("MealViewModel", "Error getting calories for date: ${e.message}")
            0
        }
    }

    /**
     * Add a dish to today's calories
     */
    fun addDishToToday(dishName: String, calories: Int) {
        userCaloriesRepository.addDish(dishName, calories)
        refreshCalories()
    }
    
    /**
     * Add a dish to a specific date's calories (format: dd/MM/yyyy)
     */
    fun addDishToDate(dateStr: String, dishName: String, calories: Int) {
        try {
            // Convert from dd/MM/yyyy to yyyy-MM-dd
            val inputFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormatter.parse(dateStr)
            val formattedDate = outputFormatter.format(date ?: return)
            
            userCaloriesRepository.addDishToDate(formattedDate, dishName, calories)
            _calorieUpdateTrigger.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("MealViewModel", "Error adding dish to date: ${e.message}")
        }
    }

    /**
     * Remove a dish from today's calories
     */
    fun removeDishFromToday(dishName: String, calories: Int) {
        userCaloriesRepository.removeDish(dishName, calories)
        refreshCalories()
    }
    
    /**
     * Remove a dish from a specific date's calories (format: dd/MM/yyyy)
     */
    fun removeDishFromDate(dateStr: String, dishName: String, calories: Int) {
        try {
            // Convert from dd/MM/yyyy to yyyy-MM-dd
            val inputFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormatter.parse(dateStr)
            val formattedDate = outputFormatter.format(date ?: return)
            
            userCaloriesRepository.removeDishFromDate(formattedDate, dishName, calories)
            _calorieUpdateTrigger.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("MealViewModel", "Error removing dish from date: ${e.message}")
        }
    }

    /**
     * Get count for a specific dish
     */
    fun getDishCount(dishName: String): Int {
        return userCaloriesRepository.getDishCount(dishName)
    }
    
    /**
     * Get count for a specific dish on a specific date (format: dd/MM/yyyy)
     */
    fun getDishCountForDate(dateStr: String, dishName: String): Int {
        return try {
            // Convert from dd/MM/yyyy to yyyy-MM-dd
            val inputFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormatter.parse(dateStr)
            val formattedDate = outputFormatter.format(date ?: return 0)
            
            userCaloriesRepository.getDishCountForDate(formattedDate, dishName)
        } catch (e: Exception) {
            Log.e("MealViewModel", "Error getting dish count for date: ${e.message}")
            0
        }
    }

    /**
     * Get streak
     */
    fun getStreak(): Int {
        return userCaloriesRepository.getStreak()
    }
    
    /**
     * Update daily calorie goal
     */
    fun updateDailyGoal(newGoal: Int) {
        userCaloriesRepository.setDailyGoal(newGoal)
        refreshCalories()
    }
}



// Keep existing data classes
data class DayData(
    val day: String,
    val date: String,
    val meals: List<Meal>
)

data class Meal(
    val type: String = "",
    val timing: String = "", // Keep for backward compatibility
    val time: String = "",   // Add to match your database structure
    val items: List<String> = listOf(),
    val hostels: Map<String, List<String>> = mapOf(),
    val commonItems: List<String> = listOf()
) {
    // Helper to get timing from either field
    fun getActualTiming(): String = if (time.isNotEmpty()) time else timing
}

// Helper function for default timings
fun getDefaultTimingForMeal(mealType: String): String {
    val isWeekend = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
    return if (isWeekend) {
        when (mealType.lowercase()) {
            "breakfast" -> "8:00 AM - 9:30 AM"
            "lunch" -> "12:30 PM - 2:00 PM"
            "snacks" -> "4:30 PM - 5:15 PM"
            "dinner" -> "7:30 PM - 9:00 PM"
            "south indian dinner" -> "7:30 PM - 9:00 PM"
            else -> ""
        }
    } else {
        when (mealType.lowercase()) {
            "breakfast" -> "7:30 AM - 9:00 AM"
            "lunch" -> "12:00 PM - 1:45 PM"
            "snacks" -> "4:30 PM - 5:15 PM"
            "dinner" -> "7:30 PM - 9:00 PM"
            "south indian dinner" -> "7:30 PM - 9:00 PM"
            else -> ""
        }
    }
}
