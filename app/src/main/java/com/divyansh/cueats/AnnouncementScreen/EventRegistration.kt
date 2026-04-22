package com.divyansh.cueats.AnnouncementScreen

/**
 * Data model for event registrations
 * Phase 1: Basic registration with minimal fields
 */
data class EventRegistration(
    val registrationId: String = "",
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val registeredAt: Long = System.currentTimeMillis()
)
