package com.divyansh.cueats.HomeScreen

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ShopClickData(
    val shopId: String = "",
    val shopName: String = "",
    val totalClicks: Int = 0,
    val lastUpdated: Long = 0,
    val imageUrl: String = "",
    val rating: Float = 0f
)

data class DailyClickData(
    val date: String = "",
    val clicks: Int = 0,
    val lastClickTime: Long = 0
)

class ShopClickTracker {
    private val firestore = FirebaseFirestore.getInstance()
    private val shopClicksCollection = firestore.collection("shopClicks")

    companion object {
        private const val TAG = "ShopClickTracker"
        private const val CACHE_DURATION_MS = 2 * 60 * 60 * 1000L // 2 hours
        private var lastRankingUpdate: Long = 0
        private var cachedTopShops: List<ShopClickData> = emptyList()
    }

    /**
     * Track a shop click - increments both total and daily clicks
     */
    suspend fun trackShopClick(shopId: String, shopName: String, imageUrl: String = "", rating: Float = 0f) {
        try {
            val today = getTodayDateString()
            val shopDocRef = shopClicksCollection.document(shopId)
            val dailyClickRef = shopDocRef.collection("dailyClicks").document(today)

            // Run transaction to increment clicks
            firestore.runTransaction { transaction ->
                val shopSnapshot = transaction.get(shopDocRef)
                val dailySnapshot = transaction.get(dailyClickRef)

                // Update shop document
                val currentTotal = shopSnapshot.getLong("totalClicks") ?: 0
                transaction.set(shopDocRef, mapOf(
                    "shopId" to shopId,
                    "shopName" to shopName,
                    "totalClicks" to (currentTotal + 1),
                    "lastUpdated" to System.currentTimeMillis(),
                    "imageUrl" to imageUrl,
                    "rating" to rating
                ), com.google.firebase.firestore.SetOptions.merge())

                // Update daily clicks
                val currentDailyClicks = dailySnapshot.getLong("clicks") ?: 0
                transaction.set(dailyClickRef, mapOf(
                    "date" to today,
                    "clicks" to (currentDailyClicks + 1),
                    "lastClickTime" to System.currentTimeMillis()
                ), com.google.firebase.firestore.SetOptions.merge())
            }.await()

            Log.d(TAG, "Successfully tracked click for shop: $shopName")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking shop click", e)
        }
    }

    /**
     * Get today's top shops based on daily clicks
     */
    suspend fun getTodayTopShops(limit: Int = 5): List<ShopClickData> {
        try {
            // Check if cache is still valid
            if (shouldUseCachedRanking()) {
                Log.d(TAG, "Using cached rankings")
                return cachedTopShops.take(limit)
            }

            val today = getTodayDateString()
            val topShops = mutableListOf<ShopClickData>()

            // Get all shops
            val shopsSnapshot = shopClicksCollection.get().await()

            // For each shop, get today's clicks
            for (shopDoc in shopsSnapshot.documents) {
                val shopId = shopDoc.id
                val shopName = shopDoc.getString("shopName") ?: ""
                val imageUrl = shopDoc.getString("imageUrl") ?: ""
                val rating = shopDoc.getDouble("rating")?.toFloat() ?: 0f

                // Get today's clicks
                val dailyClickDoc = shopClicksCollection
                    .document(shopId)
                    .collection("dailyClicks")
                    .document(today)
                    .get()
                    .await()

                val todayClicks = dailyClickDoc.getLong("clicks")?.toInt() ?: 0

                if (todayClicks > 0) {
                    topShops.add(ShopClickData(
                        shopId = shopId,
                        shopName = shopName,
                        totalClicks = todayClicks,
                        lastUpdated = System.currentTimeMillis(),
                        imageUrl = imageUrl,
                        rating = rating
                    ))
                }
            }

            // Sort by clicks descending
            val sortedShops = topShops.sortedByDescending { it.totalClicks }

            // Update cache
            cachedTopShops = sortedShops
            lastRankingUpdate = System.currentTimeMillis()

            Log.d(TAG, "Fetched ${sortedShops.size} shops with clicks today")
            return sortedShops.take(limit)

        } catch (e: Exception) {
            Log.e(TAG, "Error getting top shops", e)
            return emptyList()
        }
    }

    /**
     * Check if we should use cached ranking (within 2-3 hours)
     */
    private fun shouldUseCachedRanking(): Boolean {
        val timeSinceLastUpdate = System.currentTimeMillis() - lastRankingUpdate
        return cachedTopShops.isNotEmpty() && timeSinceLastUpdate < CACHE_DURATION_MS
    }

    /**
     * Force refresh rankings (clears cache)
     */
    fun forceRefreshRankings() {
        lastRankingUpdate = 0
        cachedTopShops = emptyList()
    }

    /**
     * Get today's date string in YYYY-MM-DD format
     */
    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}