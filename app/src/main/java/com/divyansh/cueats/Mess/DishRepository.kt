package com.divyansh.cueats.Mess

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Optimized repository to fetch dish data from Firebase Firestore
 * Features:
 * - Batch queries using whereIn (10x faster)
 * - Local caching with 24-hour expiration
 * - Parallel batch fetching for 10+ dishes
 */
class DishRepository(context: Context? = null) {
    private val firestore = FirebaseFirestore.getInstance()
    private val dishesCollection = firestore.collection("dishes")
    
    // Cache management
    private val sharedPrefs: SharedPreferences? = context?.getSharedPreferences("dish_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val CACHE_KEY_PREFIX = "dish_"
        private const val CACHE_TIMESTAMP_PREFIX = "dish_ts_"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val BATCH_SIZE = 10 // Firestore whereIn limit
    }

    /**
     * Fetch a single dish by ID with caching
     * Returns null if dish not found or error occurs
     */
    suspend fun getDishById(dishId: String): Dish? {
        // Try cache first
        getCachedDish(dishId)?.let { return it }
        
        return try {
            Log.d("DishRepository", "Fetching dish from Firebase: $dishId")
            val document = dishesCollection.document(dishId).get().await()
            
            if (document.exists()) {
                val dish = document.toObject(Dish::class.java)
                dish?.let { cacheDish(dishId, it) }
                Log.d("DishRepository", "Found dish: ${dish?.name}")
                dish
            } else {
                Log.w("DishRepository", "Dish not found: $dishId")
                null
            }
        } catch (e: Exception) {
            Log.e("DishRepository", "Error fetching dish $dishId: ${e.message}")
            null
        }
    }

    /**
     * OPTIMIZED: Fetch multiple dishes using batch queries
     * Uses Firestore whereIn for up to 10 dishes per query
     * Splits into parallel batches for 10+ dishes
     */
    suspend fun getDishesByIds(dishIds: List<String>): Map<String, Dish> {
        if (dishIds.isEmpty()) return emptyMap()
        
        val startTime = System.currentTimeMillis()
        val dishes = mutableMapOf<String, Dish>()
        
        // Step 1: Check cache for all dishes
        val uncachedIds = mutableListOf<String>()
        dishIds.forEach { dishId ->
            val cached = getCachedDish(dishId)
            if (cached != null) {
                dishes[dishId] = cached
            } else {
                uncachedIds.add(dishId)
            }
        }
        
        Log.d("DishRepository", "Cache hit: ${dishes.size}/${dishIds.size} dishes")
        
        // Step 2: Fetch uncached dishes in batches
        if (uncachedIds.isNotEmpty()) {
            val fetchedDishes = fetchDishesBatch(uncachedIds)
            dishes.putAll(fetchedDishes)
            
            // Cache the newly fetched dishes
            fetchedDishes.forEach { (dishId, dish) ->
                cacheDish(dishId, dish)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        Log.d("DishRepository", "Fetched ${dishes.size}/${dishIds.size} dishes in ${duration}ms")
        
        return dishes
    }
    
    /**
     * Fetch dishes in batches using Firestore whereIn
     * Splits into chunks of 10 and fetches in parallel
     */
    private suspend fun fetchDishesBatch(dishIds: List<String>): Map<String, Dish> = coroutineScope {
        val dishes = mutableMapOf<String, Dish>()
        
        // Split into batches of 10 (Firestore whereIn limit)
        val batches = dishIds.chunked(BATCH_SIZE)
        
        Log.d("DishRepository", "Fetching ${dishIds.size} dishes in ${batches.size} batches")
        
        // Fetch all batches in parallel
        val results = batches.map { batch ->
            async {
                fetchSingleBatch(batch)
            }
        }.awaitAll()
        
        // Combine results
        results.forEach { batchResult ->
            dishes.putAll(batchResult)
        }
        
        dishes
    }
    
    /**
     * Fetch a single batch using Firestore whereIn
     */
    private suspend fun fetchSingleBatch(dishIds: List<String>): Map<String, Dish> {
        return try {
            Log.d("DishRepository", "Batch query for ${dishIds.size} dishes")
            
            val querySnapshot = dishesCollection
                .whereIn("dishId", dishIds)
                .get()
                .await()
            
            val dishes = mutableMapOf<String, Dish>()
            querySnapshot.documents.forEach { document ->
                val dish = document.toObject(Dish::class.java)
                if (dish != null) {
                    dishes[dish.dishId] = dish
                }
            }
            
            Log.d("DishRepository", "Batch returned ${dishes.size}/${dishIds.size} dishes")
            dishes
        } catch (e: Exception) {
            Log.e("DishRepository", "Error in batch query: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Get dish from cache if not expired
     */
    private fun getCachedDish(dishId: String): Dish? {
        if (sharedPrefs == null) return null
        
        try {
            val timestamp = sharedPrefs.getLong(CACHE_TIMESTAMP_PREFIX + dishId, 0)
            val currentTime = System.currentTimeMillis()
            
            // Check if cache is still valid
            if (currentTime - timestamp < CACHE_DURATION_MS) {
                val json = sharedPrefs.getString(CACHE_KEY_PREFIX + dishId, null)
                if (json != null) {
                    return gson.fromJson(json, Dish::class.java)
                }
            }
        } catch (e: Exception) {
            Log.e("DishRepository", "Error reading cache for $dishId: ${e.message}")
        }
        
        return null
    }
    
    /**
     * Cache a dish with current timestamp
     */
    private fun cacheDish(dishId: String, dish: Dish) {
        if (sharedPrefs == null) return
        
        try {
            val json = gson.toJson(dish)
            sharedPrefs.edit()
                .putString(CACHE_KEY_PREFIX + dishId, json)
                .putLong(CACHE_TIMESTAMP_PREFIX + dishId, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e("DishRepository", "Error caching dish $dishId: ${e.message}")
        }
    }
    
    /**
     * Clear all cached dishes
     */
    fun clearCache() {
        if (sharedPrefs == null) return
        
        try {
            val editor = sharedPrefs.edit()
            sharedPrefs.all.keys.forEach { key ->
                if (key.startsWith(CACHE_KEY_PREFIX) || key.startsWith(CACHE_TIMESTAMP_PREFIX)) {
                    editor.remove(key)
                }
            }
            editor.apply()
            Log.d("DishRepository", "Cache cleared")
        } catch (e: Exception) {
            Log.e("DishRepository", "Error clearing cache: ${e.message}")
        }
    }
}
