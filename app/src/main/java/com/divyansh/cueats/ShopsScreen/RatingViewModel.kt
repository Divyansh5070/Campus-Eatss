package com.divyansh.cueats.ShopsScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class RatingViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // Real-time listeners
    private var shopRatingsListener: ListenerRegistration? = null
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    // Main shop ratings that show on cards
    private val _shopRatings = MutableStateFlow<Map<String, ShopRating>>(emptyMap())
    val shopRatings: StateFlow<Map<String, ShopRating>> = _shopRatings.asStateFlow()

    // Individual ratings for dialog display
    private val _ratingsPerShop = MutableStateFlow<Map<String, List<Rating>>>(emptyMap())
    val ratingsPerShop: StateFlow<Map<String, List<Rating>>> = _ratingsPerShop.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Current shop ratings for dialog
    private val _currentShopRatings = MutableStateFlow<List<Rating>>(emptyList())
    val ratings: StateFlow<List<Rating>> = _currentShopRatings.asStateFlow()

    // Track which shop is currently being viewed in dialog
    private var currentDialogShopId: String? = null

    init {
        startShopRatingsListener()
    }

    init {
        startShopRatingsListener()

        // AUTO-CLEANUP for problematic shops (PASTE HERE)
        viewModelScope.launch {
            delay(2000) // Wait for initial load

            Log.d("RatingViewModel", "Auto-cleaning problematic shops...")

            // List of problematic shop IDs
            val problematicShops = listOf(
                "chai_sutta_bar_shop_id",
                 // Add more if needed
            )

            problematicShops.forEach { shopId ->
                forceRemoveShopFromUI(shopId)
                deleteAllRatingsForShop(shopId)
            }
        }
    }

    /**
     * COMPLETELY FIXED: Real-time listener with metadata changes and proper deletion handling
     */
    private fun startShopRatingsListener() {
        shopRatingsListener?.remove()

        Log.d("RatingViewModel", "Starting enhanced shop ratings listener...")

        // FIXED: Listen to both server and cache changes to catch all deletions
        shopRatingsListener = firestore.collection("shop_ratings")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e("RatingViewModel", "Error listening to shop ratings", error)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    Log.d("RatingViewModel", "Shop ratings snapshot received - Document count: ${querySnapshot.documents.size}")
                    Log.d("RatingViewModel", "Snapshot metadata - hasPendingWrites: ${querySnapshot.metadata.hasPendingWrites()}, isFromCache: ${querySnapshot.metadata.isFromCache}")

                    // FIXED: Always rebuild from scratch to ensure deletions are handled
                    val newRatingsMap = mutableMapOf<String, ShopRating>()

                    // Process all current documents
                    querySnapshot.documents.forEach { document ->
                        try {
                            val shopId = document.id
                            Log.d("RatingViewModel", "Processing shop rating document: $shopId (exists: ${document.exists()})")

                            if (document.exists()) {
                                val rating = document.toObject(ShopRating::class.java)?.copy(shopId = shopId)

                                if (rating != null && rating.totalRatings > 0) {
                                    newRatingsMap[shopId] = rating
                                    Log.d("RatingViewModel", "Added shop rating: $shopId -> ${rating.averageRating} avg, ${rating.totalRatings} total")
                                } else {
                                    Log.w("RatingViewModel", "Skipping invalid rating for shop: $shopId")
                                }
                            } else {
                                Log.d("RatingViewModel", "Document $shopId marked as deleted")
                            }
                        } catch (e: Exception) {
                            Log.e("RatingViewModel", "Error parsing rating for document ${document.id}", e)
                        }
                    }

                    // Log what's being removed
                    val previousShops = _shopRatings.value.keys
                    val currentShops = newRatingsMap.keys
                    val deletedShops = previousShops - currentShops
                    val addedShops = currentShops - previousShops

                    if (deletedShops.isNotEmpty()) {
                        Log.d("RatingViewModel", "REMOVING shops from UI: ${deletedShops.joinToString()}")
                    }
                    if (addedShops.isNotEmpty()) {
                        Log.d("RatingViewModel", "ADDING shops to UI: ${addedShops.joinToString()}")
                    }

                    // FIXED: Force complete state replacement
                    _shopRatings.value = newRatingsMap.toMap()
                    Log.d("RatingViewModel", "Shop ratings state updated: ${newRatingsMap.size} total shops")

                    // Debug log current state
                    _shopRatings.value.forEach { (shopId, rating) ->
                        Log.d("RatingViewModel", "Current state - Shop: $shopId, Rating: ${rating.averageRating}")
                    }
                }
            }
    }

    /**
     * FIXED: Manual load with force refresh from server
     */
    fun loadAllShopRatings() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("RatingViewModel", "Manual loading all shop ratings from server...")

                // FIXED: Force server fetch, not cache
                val ratingsSnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("shop_ratings")
                        .get(com.google.firebase.firestore.Source.SERVER)
                        .await()
                }

                val ratingsMap = mutableMapOf<String, ShopRating>()

                Log.d("RatingViewModel", "Server returned ${ratingsSnapshot.documents.size} shop rating documents")

                ratingsSnapshot.documents.forEach { document ->
                    try {
                        val shopId = document.id
                        Log.d("RatingViewModel", "Processing server document: $shopId (exists: ${document.exists()})")

                        if (document.exists()) {
                            val rating = document.toObject(ShopRating::class.java)?.copy(shopId = shopId)

                            if (rating != null && rating.totalRatings > 0) {
                                ratingsMap[shopId] = rating
                                Log.d("RatingViewModel", "Server loaded rating for shop $shopId: ${rating.averageRating} (${rating.totalRatings} reviews)")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RatingViewModel", "Error parsing rating for document ${document.id}", e)
                    }
                }

                _shopRatings.value = ratingsMap
                Log.d("RatingViewModel", "Server load completed: ${ratingsMap.size} shop ratings")

            } catch (e: Exception) {
                Log.e("RatingViewModel", "Error loading shop ratings from server", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * FIXED: Load individual ratings with better cleanup
     */
    fun loadRatingsForShop(shopId: String) {
        Log.d("RatingViewModel", "Loading ratings specifically for shop: $shopId")

        // Clear current ratings when switching shops
        if (currentDialogShopId != shopId) {
            _currentShopRatings.value = emptyList()
            currentDialogShopId = shopId
        }

        // Remove existing listener for this shop if any
        activeListeners[shopId]?.remove()

        // Add real-time listener for this shop's ratings with metadata changes
        val listener = firestore.collection("ratings")
            .whereEqualTo("shopId", shopId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e("RatingViewModel", "Error listening to ratings for shop $shopId", error)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    Log.d("RatingViewModel", "Ratings snapshot for shop $shopId - Document count: ${querySnapshot.documents.size}")

                    val ratingsList = mutableListOf<Rating>()

                    querySnapshot.documents.forEach { document ->
                        try {
                            if (document.exists()) {
                                val rating = document.toObject(Rating::class.java)?.copy(id = document.id)
                                if (rating?.shopId == shopId) {
                                    ratingsList.add(rating)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("RatingViewModel", "Error parsing rating ${document.id}", e)
                        }
                    }

                    Log.d("RatingViewModel", "Loaded ${ratingsList.size} ratings for shop $shopId")

                    // Only update if this is still the current shop being viewed
                    if (currentDialogShopId == shopId) {
                        _currentShopRatings.value = ratingsList
                    }

                    // Update the per-shop ratings map
                    val currentRatingsPerShop = _ratingsPerShop.value.toMutableMap()
                    if (ratingsList.isEmpty()) {
                        currentRatingsPerShop.remove(shopId)
                    } else {
                        currentRatingsPerShop[shopId] = ratingsList
                    }
                    _ratingsPerShop.value = currentRatingsPerShop
                }
            }

        activeListeners[shopId] = listener
    }

    /**
     * ENHANCED: Force refresh with complete cache clearing
     */
    fun forceRefreshAll() {
        viewModelScope.launch {
            Log.d("RatingViewModel", "FORCE REFRESH: Clearing all cached data...")

            try {
                // STEP 1: Remove all listeners first
                shopRatingsListener?.remove()
                activeListeners.values.forEach { it.remove() }
                activeListeners.clear()

                // STEP 2: Clear all local state
                _shopRatings.value = emptyMap()
                _ratingsPerShop.value = emptyMap()
                _currentShopRatings.value = emptyList()
                currentDialogShopId = null

                // STEP 3: Try to clear Firestore cache (non-blocking)
                try {
                    withContext(Dispatchers.IO) {
                        firestore.clearPersistence().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("RatingViewModel", "Firestore cache cleared successfully")
                            } else {
                                Log.w("RatingViewModel", "Could not clear Firestore cache", task.exception)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("RatingViewModel", "Could not clear Firestore cache (app may be active)", e)
                }

                // STEP 4: Wait for cleanup
                delay(1000)

                // STEP 5: Force fresh server load
                loadAllShopRatings()

                // STEP 6: Restart listeners after server load completes
                delay(500)
                startShopRatingsListener()

                // STEP 7: If dialog is open, reload its data
                currentDialogShopId?.let { shopId ->
                    delay(500)
                    loadRatingsForShop(shopId)
                }

                Log.d("RatingViewModel", "FORCE REFRESH completed")
            } catch (e: Exception) {
                Log.e("RatingViewModel", "Error during force refresh", e)
            }
        }
    }

    /**
     * ENHANCED: Delete rating with immediate UI update
     */
    suspend fun deleteRating(ratingId: String, shopId: String): Boolean {
        return try {
            Log.d("RatingViewModel", "Deleting rating $ratingId from shop $shopId")

            // Delete the rating document from server
            withContext(Dispatchers.IO) {
                firestore.collection("ratings")
                    .document(ratingId)
                    .delete()
                    .await()
            }

            Log.d("RatingViewModel", "Rating deleted from server, updating shop stats...")

            // Immediately update local state for current ratings if it's the active shop
            if (currentDialogShopId == shopId) {
                val currentRatings = _currentShopRatings.value.toMutableList()
                currentRatings.removeAll { it.id == ratingId }
                _currentShopRatings.value = currentRatings
                Log.d("RatingViewModel", "Immediately removed rating from current dialog view")
            }

            // Recalculate and update shop statistics
            updateShopRatingStats(shopId)

            true
        } catch (e: Exception) {
            Log.e("RatingViewModel", "Error deleting rating", e)
            false
        }
    }

    /**
     * FIXED: Submit rating with server-side validation
     */
    suspend fun submitRating(
        shopId: String,
        userId: String,
        userName: String,
        rating: Float,
        review: String
    ): Boolean {
        return try {
            if (shopId.isBlank()) {
                Log.e("RatingViewModel", "Cannot submit rating: shopId is blank")
                return false
            }

            Log.d("RatingViewModel", "Submitting rating for shop '$shopId': $rating stars by '$userName'")

            // Check for existing rating by this user for THIS SPECIFIC SHOP (from server)
            val existingRating = withContext(Dispatchers.IO) {
                firestore.collection("ratings")
                    .whereEqualTo("shopId", shopId)
                    .whereEqualTo("userId", userId)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()
            }

            val ratingData = Rating(
                shopId = shopId,
                userId = userId,
                userName = userName.trim(),
                rating = rating,
                review = review.trim(),
                timestamp = Timestamp.now(),
                isVerified = true
            )

            if (existingRating.documents.isNotEmpty()) {
                // Update existing rating
                val docId = existingRating.documents[0].id
                withContext(Dispatchers.IO) {
                    firestore.collection("ratings")
                        .document(docId)
                        .set(ratingData)
                        .await()
                }
                Log.d("RatingViewModel", "Updated existing rating with ID: $docId for shop $shopId")
            } else {
                // Add new rating
                val ratingDoc = withContext(Dispatchers.IO) {
                    firestore.collection("ratings").add(ratingData).await()
                }
                Log.d("RatingViewModel", "New rating added with ID: ${ratingDoc.id} for shop $shopId")
            }

            // Recalculate shop statistics
            updateShopRatingStats(shopId)

            Log.d("RatingViewModel", "Rating submission completed successfully for shop $shopId")
            true
        } catch (e: Exception) {
            Log.e("RatingViewModel", "Error submitting rating for shop $shopId", e)
            false
        }
    }

    /**
     * ENHANCED: Update shop rating stats with server-side data
     */
    private suspend fun updateShopRatingStats(shopId: String) {
        try {
            if (shopId.isBlank()) {
                Log.e("RatingViewModel", "Cannot update stats: shopId is blank")
                return
            }

            Log.d("RatingViewModel", "Recalculating rating stats for shop: '$shopId'")

            // FIXED: Get ratings from server to ensure accuracy
            val ratingsSnapshot = withContext(Dispatchers.IO) {
                firestore.collection("ratings")
                    .whereEqualTo("shopId", shopId)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()
            }

            val ratings = ratingsSnapshot.documents.mapNotNull { document ->
                try {
                    if (document.exists()) {
                        val rating = document.toObject(Rating::class.java)
                        if (rating?.shopId == shopId) {
                            rating
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("RatingViewModel", "Error parsing rating ${document.id}", e)
                    null
                }
            }

            Log.d("RatingViewModel", "Server returned ${ratings.size} ratings for shop '$shopId'")

            if (ratings.isEmpty()) {
                // Delete the shop rating document if no ratings exist
                Log.d("RatingViewModel", "No ratings found for shop '$shopId', deleting shop rating document")

                withContext(Dispatchers.IO) {
                    firestore.collection("shop_ratings")
                        .document(shopId)
                        .delete()
                        .await()
                }
                Log.d("RatingViewModel", "Deleted shop rating document for '$shopId'")

                // Immediately remove from local state
                val currentRatings = _shopRatings.value.toMutableMap()
                currentRatings.remove(shopId)
                _shopRatings.value = currentRatings
                Log.d("RatingViewModel", "Immediately removed shop '$shopId' from local state")

                return
            }

            val totalRatings = ratings.size
            val averageRating = ratings.map { it.rating }.average().toFloat()

            // Calculate rating breakdown
            val ratingBreakdown = mutableMapOf<Int, Int>()
            for (i in 1..5) {
                ratingBreakdown[i] = ratings.count { it.rating.toInt() == i }
            }

            val shopRating = ShopRating(
                shopId = shopId,
                averageRating = averageRating,
                totalRatings = totalRatings,
                ratingBreakdown = ratingBreakdown,
                lastUpdated = Timestamp.now()
            )

            // Save to Firestore
            withContext(Dispatchers.IO) {
                firestore.collection("shop_ratings")
                    .document(shopId)
                    .set(shopRating)
                    .await()
            }

            Log.d("RatingViewModel", "Updated shop rating stats for '$shopId' - Average: ${String.format("%.1f", averageRating)}, Total: $totalRatings")

        } catch (e: Exception) {
            Log.e("RatingViewModel", "Error updating shop rating stats for shop '$shopId'", e)
        }
    }

    /**
     * Get shop rating with validation
     */
    fun getShopRating(shopId: String): ShopRating? {
        if (shopId.isBlank()) {
            Log.w("RatingViewModel", "getShopRating called with blank shopId")
            return null
        }

        val rating = _shopRatings.value[shopId]
        Log.d("RatingViewModel", "Getting shop rating for '$shopId': ${rating?.let { "${it.averageRating} (${it.totalRatings} reviews)" } ?: "null"}")
        return rating
    }

    /**
     * Get individual ratings for a specific shop
     */
    fun getRatingsForShop(shopId: String): List<Rating> {
        if (shopId.isBlank()) {
            Log.w("RatingViewModel", "getRatingsForShop called with blank shopId")
            return emptyList()
        }

        return _ratingsPerShop.value[shopId] ?: emptyList()
    }

    /**
     * Force refresh data for a specific shop
     */
    fun refreshShopData(shopId: String) {
        if (shopId.isBlank()) {
            Log.w("RatingViewModel", "refreshShopData called with blank shopId")
            return
        }

        viewModelScope.launch {
            Log.d("RatingViewModel", "Force refreshing data for shop: '$shopId'")
            updateShopRatingStats(shopId)
            loadRatingsForShop(shopId)
        }
    }

    /**
     * Clear current ratings and reset dialog state
     */
    fun clearCurrentRatings() {
        _currentShopRatings.value = emptyList()
        currentDialogShopId = null
        Log.d("RatingViewModel", "Cleared current ratings and dialog state")
    }

    /**
     * ENHANCED: Force delete shop rating with immediate UI update
     */
    suspend fun deleteShopRating(shopId: String): Boolean {
        return try {
            if (shopId.isBlank()) {
                Log.e("RatingViewModel", "Cannot delete shop rating: shopId is blank")
                return false
            }

            Log.d("RatingViewModel", "Force deleting shop rating for: '$shopId'")

            // Delete from server
            withContext(Dispatchers.IO) {
                firestore.collection("shop_ratings")
                    .document(shopId)
                    .delete()
                    .await()
            }

            // Immediately remove from local state
            val currentRatings = _shopRatings.value.toMutableMap()
            currentRatings.remove(shopId)
            _shopRatings.value = currentRatings

            Log.d("RatingViewModel", "Shop rating deleted and removed from UI for '$shopId'")
            true
        } catch (e: Exception) {
            Log.e("RatingViewModel", "Error deleting shop rating for '$shopId'", e)
            false
        }
    }

    /**
     * ENHANCED: Delete all ratings for a shop with batch operations
     */
    suspend fun deleteAllRatingsForShop(shopId: String): Boolean {
        return try {
            if (shopId.isBlank()) {
                Log.e("RatingViewModel", "Cannot delete ratings: shopId is blank")
                return false
            }

            Log.d("RatingViewModel", "Deleting all ratings for shop: '$shopId'")

            // Get all ratings for this shop from server
            val ratingsSnapshot = withContext(Dispatchers.IO) {
                firestore.collection("ratings")
                    .whereEqualTo("shopId", shopId)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()
            }

            // Delete all individual ratings using batch
            withContext(Dispatchers.IO) {
                val batch = firestore.batch()
                ratingsSnapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit().await()
            }

            // Delete the shop rating summary
            withContext(Dispatchers.IO) {
                firestore.collection("shop_ratings")
                    .document(shopId)
                    .delete()
                    .await()
            }

            // Immediately update local state
            val currentRatings = _shopRatings.value.toMutableMap()
            currentRatings.remove(shopId)
            _shopRatings.value = currentRatings

            val currentRatingsPerShop = _ratingsPerShop.value.toMutableMap()
            currentRatingsPerShop.remove(shopId)
            _ratingsPerShop.value = currentRatingsPerShop

            if (currentDialogShopId == shopId) {
                _currentShopRatings.value = emptyList()
            }

            Log.d("RatingViewModel", "All ratings deleted for shop '$shopId' and removed from UI")
            true
        } catch (e: Exception) {
            Log.e("RatingViewModel", "Error deleting all ratings for shop '$shopId'", e)
            false
        }
    }

    /**
     * SPECIAL METHOD: Force remove specific shop from UI (use if Firestore sync fails)
     */
    fun forceRemoveShopFromUI(shopId: String) {
        Log.d("RatingViewModel", "FORCE removing shop '$shopId' from UI (emergency cleanup)")

        val currentRatings = _shopRatings.value.toMutableMap()
        currentRatings.remove(shopId)
        _shopRatings.value = currentRatings

        val currentRatingsPerShop = _ratingsPerShop.value.toMutableMap()
        currentRatingsPerShop.remove(shopId)
        _ratingsPerShop.value = currentRatingsPerShop

        if (currentDialogShopId == shopId) {
            _currentShopRatings.value = emptyList()
            currentDialogShopId = null
        }

        Log.d("RatingViewModel", "Shop '$shopId' forcefully removed from all UI state")
    }

    /**
     * Clean up listeners when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        shopRatingsListener?.remove()
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
        Log.d("RatingViewModel", "ViewModel cleared and listeners removed")
    }

    /**
     * Initialize or reinitialize the rating system
     */
    fun initializeRatingSystem() {
        Log.d("RatingViewModel", "Initializing rating system...")
        startShopRatingsListener()
        loadAllShopRatings()
    }

    /**
     * Enhanced debug method
     */
    fun debugCurrentState() {
        Log.d("RatingViewModel", "=== ENHANCED RATING SYSTEM DEBUG ===")
        Log.d("RatingViewModel", "Total shops with ratings: ${_shopRatings.value.size}")
        _shopRatings.value.forEach { (shopId, rating) ->
            Log.d("RatingViewModel", "Shop '$shopId': ${rating.averageRating} avg, ${rating.totalRatings} total")
        }
        Log.d("RatingViewModel", "Current dialog shop: $currentDialogShopId")
        Log.d("RatingViewModel", "Current ratings count: ${_currentShopRatings.value.size}")
        Log.d("RatingViewModel", "Active listeners: ${activeListeners.size}")
        Log.d("RatingViewModel", "=== END DEBUG ===")
    }

    /**
     * ENHANCED: Clear cache and reload with server verification
     */
    fun clearCacheAndReload() {
        viewModelScope.launch {
            Log.d("RatingViewModel", "ENHANCED: Clearing cache and reloading from server...")
            forceRefreshAll()
        }
    }

    /**
     * ADDED: Emergency cleanup for persistent ghost data
     */
    fun emergencyCleanup() {
        viewModelScope.launch {
            Log.d("RatingViewModel", "EMERGENCY CLEANUP: Removing all cached data")

            // Nuclear option - clear everything
            shopRatingsListener?.remove()
            activeListeners.values.forEach { it.remove() }
            activeListeners.clear()

            _shopRatings.value = emptyMap()
            _ratingsPerShop.value = emptyMap()
            _currentShopRatings.value = emptyList()
            currentDialogShopId = null

            delay(2000) // Wait longer

            // Restart from scratch
            startShopRatingsListener()
            loadAllShopRatings()

            Log.d("RatingViewModel", "Emergency cleanup completed")
        }
    }
}