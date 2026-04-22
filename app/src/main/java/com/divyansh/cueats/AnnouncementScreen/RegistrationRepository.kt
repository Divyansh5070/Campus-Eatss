package com.divyansh.cueats.AnnouncementScreen

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing event registrations
 * Phase 1: Basic registration operations only
 */
class RegistrationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val registrationsCollection = db.collection("event_registrations")
    
    /**
     * Register a user for an event
     * @param registration EventRegistration object with user details
     * @return Result with registration ID or error message
     */
    suspend fun registerForEvent(registration: EventRegistration): Result<String> {
        return try {
            // Check if user is already registered
            val isRegistered = isUserRegistered(registration.eventId, registration.userId)
            if (isRegistered) {
                return Result.failure(Exception("You are already registered for this event"))
            }
            
            // Create new registration document
            val docRef = registrationsCollection.document()
            val registrationWithId = registration.copy(registrationId = docRef.id)
            
            docRef.set(registrationWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if a user is already registered for an event
     * @param eventId Event ID to check
     * @param userId User ID to check
     * @return true if user is registered, false otherwise
     */
    suspend fun isUserRegistered(eventId: String, userId: String): Boolean {
        return try {
            val snapshot = registrationsCollection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}
