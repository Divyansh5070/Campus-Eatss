package com.divyansh.cueats.Notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing user notification preferences in Firestore
 */
class NotificationPreferences {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "NotificationPreferences"
        private const val USERS_COLLECTION = "users"
        private const val NOTIFICATIONS_ENABLED_FIELD = "notificationsEnabled"
    }
    
    /**
     * Get current user ID
     */
    private fun getUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get notification preference for current user
     * @return true if notifications are enabled, false otherwise
     */
    suspend fun getNotificationPreference(): Boolean {
        val userId = getUserId() ?: return false
        
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            // Default to true if field doesn't exist
            document.getBoolean(NOTIFICATIONS_ENABLED_FIELD) ?: true
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification preference", e)
            true // Default to enabled on error
        }
    }
    
    /**
     * Set notification preference for current user
     * @param enabled true to enable notifications, false to disable
     */
    suspend fun setNotificationPreference(enabled: Boolean): Boolean {
        val userId = getUserId() ?: return false
        
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(NOTIFICATIONS_ENABLED_FIELD, enabled)
                .await()
            
            Log.d(TAG, "Notification preference updated: $enabled")
            true
        } catch (e: Exception) {
            // If document doesn't exist, create it
            try {
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(mapOf(NOTIFICATIONS_ENABLED_FIELD to enabled))
                    .await()
                
                Log.d(TAG, "Notification preference created: $enabled")
                true
            } catch (e2: Exception) {
                Log.e(TAG, "Error setting notification preference", e2)
                false
            }
        }
    }
    
    /**
     * Observe notification preference changes in real-time
     * @return Flow of boolean values representing notification state
     */
    fun observeNotificationPreference(): Flow<Boolean> = callbackFlow {
        val userId = getUserId()
        
        if (userId == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing notification preference", error)
                    trySend(true) // Default to enabled on error
                    return@addSnapshotListener
                }
                
                val enabled = snapshot?.getBoolean(NOTIFICATIONS_ENABLED_FIELD) ?: true
                trySend(enabled)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Initialize notification preference for new users
     * Sets default to enabled
     */
    suspend fun initializeForNewUser() {
        val userId = getUserId() ?: return
        
        try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            // Only set if field doesn't exist
            if (!document.exists() || !document.contains(NOTIFICATIONS_ENABLED_FIELD)) {
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(mapOf(NOTIFICATIONS_ENABLED_FIELD to true), 
                         com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                Log.d(TAG, "Initialized notification preference for new user")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing notification preference", e)
        }
    }
}
