package com.divyansh.cueats.AnnouncementScreen

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing event data from Firebase Firestore
 */
class EventRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")
    
    companion object {
        private const val TAG = "EventRepository"
    }
    
    /**
     * Get all active events (one-time fetch)
     */
    suspend fun getActiveEvents(limit: Int = 50): List<Event> {
        return try {
            val snapshot = eventsCollection
                .whereEqualTo("isActive", true)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    Event(
                        eventId = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "General",
                        date = doc.getString("date") ?: "",
                        endDate = doc.getString("endDate") ?: doc.getString("date") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        venue = doc.getString("venue") ?: "",
                        organizer = doc.getString("organizer") ?: "",
                        organizerIcon = doc.getString("organizerIcon") ?: "👥",
                        clubId = doc.getString("clubId") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        bannerUrl = doc.getString("bannerUrl") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        isActive = doc.getBoolean("isActive") ?: true,
                        registrationUrl = doc.getString("registrationUrl") ?: "",
                        mapUrl = doc.getString("mapUrl") ?: "",
                        prizePool = doc.getString("prizePool") ?: "",
                        targetAudience = doc.getString("targetAudience") ?: "Open to All",
                        registrationCloseDate = doc.getString("registrationCloseDate") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing event: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching events", e)
            emptyList()
        }
    }
    
    /**
     * Get events filtered by category
     */
    suspend fun getEventsByCategory(category: String, limit: Int = 50): List<Event> {
        return try {
            val snapshot = eventsCollection
                .whereEqualTo("isActive", true)
                .whereEqualTo("category", category)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    Event(
                        eventId = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "General",
                        date = doc.getString("date") ?: "",
                        endDate = doc.getString("endDate") ?: doc.getString("date") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        venue = doc.getString("venue") ?: "",
                        organizer = doc.getString("organizer") ?: "",
                        organizerIcon = doc.getString("organizerIcon") ?: "👥",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        bannerUrl = doc.getString("bannerUrl") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        isActive = doc.getBoolean("isActive") ?: true,
                        registrationUrl = doc.getString("registrationUrl") ?: "",
                        mapUrl = doc.getString("mapUrl") ?: "",
                        prizePool = doc.getString("prizePool") ?: "",
                        targetAudience = doc.getString("targetAudience") ?: "Open to All",
                        registrationCloseDate = doc.getString("registrationCloseDate") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing event: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching events by category", e)
            emptyList()
        }
    }
    
    /**
     * Get a single event by ID
     */
    suspend fun getEventById(eventId: String): Event? {
        return try {
            val doc = eventsCollection.document(eventId).get().await()
            if (doc.exists()) {
                Event(
                    eventId = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    category = doc.getString("category") ?: "General",
                    date = doc.getString("date") ?: "",
                    endDate = doc.getString("endDate") ?: doc.getString("date") ?: "",
                    startTime = doc.getString("startTime") ?: "",
                    endTime = doc.getString("endTime") ?: "",
                    venue = doc.getString("venue") ?: "",
                    organizer = doc.getString("organizer") ?: "",
                    organizerIcon = doc.getString("organizerIcon") ?: "👥",
                    clubId = doc.getString("clubId") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    bannerUrl = doc.getString("bannerUrl") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0,
                    isActive = doc.getBoolean("isActive") ?: true,
                    registrationUrl = doc.getString("registrationUrl") ?: "",
                    mapUrl = doc.getString("mapUrl") ?: "",
                    prizePool = doc.getString("prizePool") ?: "",
                    targetAudience = doc.getString("targetAudience") ?: "Open to All",
                    registrationCloseDate = doc.getString("registrationCloseDate") ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching event by ID: $eventId", e)
            null
        }
    }
    
    /**
     * Listen to events in real-time
     */
    fun listenToEvents(limit: Int = 50): Flow<List<Event>> = callbackFlow {
        val listenerRegistration = eventsCollection
            .whereEqualTo("isActive", true)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to events", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event(
                            eventId = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            category = doc.getString("category") ?: "General",
                            date = doc.getString("date") ?: "",
                            endDate = doc.getString("endDate") ?: doc.getString("date") ?: "",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            venue = doc.getString("venue") ?: "",
                            organizer = doc.getString("organizer") ?: "",
                            organizerIcon = doc.getString("organizerIcon") ?: "👥",
                            clubId = doc.getString("clubId") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            bannerUrl = doc.getString("bannerUrl") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0,
                            isActive = doc.getBoolean("isActive") ?: true,
                            registrationUrl = doc.getString("registrationUrl") ?: "",
                            mapUrl = doc.getString("mapUrl") ?: "",
                            prizePool = doc.getString("prizePool") ?: "",
                            targetAudience = doc.getString("targetAudience") ?: "Open to All",
                            registrationCloseDate = doc.getString("registrationCloseDate") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing event: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(events)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Search events by title or description
     */
    fun searchEvents(events: List<Event>, query: String): List<Event> {
        if (query.isBlank()) return events
        
        val lowerQuery = query.lowercase()
        return events.filter { event ->
            event.title.lowercase().contains(lowerQuery) ||
            event.description.lowercase().contains(lowerQuery) ||
            event.organizer.lowercase().contains(lowerQuery) ||
            event.venue.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Get club profile by clubId
     */
    suspend fun getClubById(clubId: String): Club? {
        return try {
            val doc = firestore.collection("clubs")
                .document(clubId)
                .get()
                .await()
            
            if (doc.exists()) {
                Club(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    logoUrl = doc.getString("logoUrl") ?: "",
                    contactEmail = doc.getString("contactEmail") ?: "",
                    contactPhone = doc.getString("contactPhone") ?: "",
                    website = doc.getString("website") ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching club by ID: $clubId", e)
            null
        }
    }
}
