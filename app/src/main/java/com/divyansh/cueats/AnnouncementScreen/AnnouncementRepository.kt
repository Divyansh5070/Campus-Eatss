package com.divyansh.cueats.AnnouncementScreen

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Announcement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val timestamp: Long = 0,
    val category: String = "general", // competition, event, notice, general
    val isActive: Boolean = true
)

class AnnouncementRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val announcementsCollection = firestore.collection("announcements")
    
    companion object {
        private const val TAG = "AnnouncementRepository"
    }
    
    /**
     * Get active announcements (one-time fetch)
     */
    suspend fun getActiveAnnouncements(limit: Int = 5): List<Announcement> {
        return try {
            val snapshot = announcementsCollection
                .whereEqualTo("isActive", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    Announcement(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        category = doc.getString("category") ?: "general",
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing announcement: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching announcements", e)
            emptyList()
        }
    }
    
    /**
     * Listen to announcements in real-time
     */
    fun listenToAnnouncements(limit: Int = 5): Flow<List<Announcement>> = callbackFlow {
        val listenerRegistration = announcementsCollection
            .whereEqualTo("isActive", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to announcements", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val announcements = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Announcement(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0,
                            category = doc.getString("category") ?: "general",
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing announcement: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(announcements)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Get category icon resource based on category type
     */
    fun getCategoryIcon(category: String): String {
        return when (category.lowercase()) {
            "competition" -> "🏆"
            "event" -> "🎉"
            "notice" -> "📢"
            "important" -> "⚠️"
            "sports" -> "⚽"
            "cultural" -> "🎭"
            else -> "📌"
        }
    }
}
