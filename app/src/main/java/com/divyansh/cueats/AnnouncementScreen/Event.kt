package com.divyansh.cueats.AnnouncementScreen

import com.divyansh.cueats.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data model for campus events, hackathons, and competitions
 */
data class Event(
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "General", // Tech, Cultural, Sports, General
    val date: String = "", // Start date e.g., "22/12/2025" (DD/MM/YYYY)
    val endDate: String = "", // End date e.g., "23/12/2025" (DD/MM/YYYY)
    val startTime: String = "", // e.g., "9:00 AM"
    val endTime: String = "", // e.g., "9:00 PM"
    val venue: String = "", // e.g., "Main Auditorium, Block A"
    val organizer: String = "", // e.g., "GDSC"
    val organizerIcon: String = "👥", // Emoji or icon identifier
    val clubId: String = "", // Club ID for fetching club profile
    val imageUrl: String = "", // Event thumbnail/banner image
    val bannerUrl: String = "", // Full-size banner for details screen
    val timestamp: Long = 0, // Unix timestamp for sorting
    val isActive: Boolean = true, // Whether event is currently active/visible
    val registrationUrl: String = "", // URL for registration (optional)
    val mapUrl: String = "", // URL for map location (optional)
    val prizePool: String = "", // Prize pool amount (optional)
    val targetAudience: String = "Open to All", // Target audience for the event
    val registrationCloseDate: String = "", // Registration close date - Format: "DD/MM/YYYY"
    val registrationEnabled: Boolean = false // Whether custom registration is enabled (Phase 1)
) {
    /**
     * Get formatted date and time string
     */
    fun getFormattedDateTime(): String {
        return "$date • $startTime - $endTime"
    }
    
    /**
     * Get category color based on category type
     */
    fun getCategoryColor(): Long {
        return when (category.lowercase()) {
            "tech" -> 0xFF5C7CFA // Blue
            "hackathon" -> 0xFF667EEA // Purple
            "workshop" -> 0xFF51CF66 // Green
            "competition" -> 0xFFFFB800 // Gold
            "seminar" -> 0xFF748FFC // Light Blue
            "cultural" -> 0xFFFF6B9D // Pink
            "sports" -> 0xFF51CF66 // Green
            else -> 0xFFFF8C42 // Orange
        }
    }
    
    /**
     * Get category emoji
     */
    fun getCategoryEmoji(): String {
        return when (category.lowercase()) {
            "tech" -> "💻"
            "hackathon" -> "💻"
            "workshop" -> "🛠️"
            "competition" -> "🏆"
            "seminar" -> "📚"
            "cultural" -> "🎭"
            "sports" -> "⚽"
            else -> "📢"
        }
    }
    
    /**
     * Check if event is upcoming (not past)
     */
    fun isUpcoming(): Boolean {
        return timestamp > System.currentTimeMillis()
    }
    
    /**
     * Get day of month from date string
     */
    fun getDayOfMonth(): String {
        return date.split(" ").getOrNull(1) ?: ""
    }
    
    /**
     * Get month from date string
     */
    fun getMonth(): String {
        return date.split(" ").getOrNull(0) ?: ""
    }
    
    /**
     * Get formatted date (Day, Month Year) from date string
     * Converts "18/12/2025" (DD/MM/YYYY) to "18, December 2025"
     */
    fun getFormattedDate(): String {
        return try {
            // Check if this is a multi-day event with endDate field
            if (endDate.isNotEmpty() && endDate != date && date.contains("/") && endDate.contains("/")) {
                val startParts = date.split("/")
                val endParts = endDate.split("/")
                
                if (startParts.size == 3 && endParts.size == 3) {
                    val startDay = startParts[0].toIntOrNull() ?: 1
                    val startMonth = startParts[1].toIntOrNull() ?: 1
                    val endDay = endParts[0].toIntOrNull() ?: 1
                    val endMonth = endParts[1].toIntOrNull() ?: 1
                    val year = startParts[2].toIntOrNull() ?: 2025
                    
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    
                    return if (startMonth == endMonth) {
                        // Same month: "25-26 Dec 2025"
                        val monthName = monthNames.getOrNull(startMonth - 1) ?: "Jan"
                        "$startDay-$endDay $monthName $year"
                    } else {
                        // Different months: "30 Dec - 2 Jan 2025"
                        val startMonthName = monthNames.getOrNull(startMonth - 1) ?: "Jan"
                        val endMonthName = monthNames.getOrNull(endMonth - 1) ?: "Jan"
                        "$startDay $startMonthName - $endDay $endMonthName $year"
                    }
                }
            }
            
            // Single day event
            if (date.contains("/")) {
                val parts = date.split("/")
                if (parts.size == 3) {
                    // Parse DD/MM/YYYY format from EventManager
                    val day = parts[0].toIntOrNull() ?: 1
                    val month = parts[1].toIntOrNull() ?: 1
                    val year = parts[2].toIntOrNull() ?: 2025
                    
                    // Use abbreviated month names for better display
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    val monthName = monthNames.getOrNull(month - 1) ?: "Jan"
                    "$day $monthName $year"
                } else {
                    date
                }
            } else if (date.contains("-")) {
                // Handle date range format like "22/12/2025 - 23/12/2025"
                val dateParts = date.split("-").map { it.trim() }
                if (dateParts.size == 2) {
                    val startParts = dateParts[0].split("/")
                    val endParts = dateParts[1].split("/")
                    if (startParts.size == 3 && endParts.size == 3) {
                        val startDay = startParts[0].toIntOrNull() ?: 1
                        val startMonth = startParts[1].toIntOrNull() ?: 1
                        val endDay = endParts[0].toIntOrNull() ?: 1
                        val endMonth = endParts[1].toIntOrNull() ?: 1
                        val year = startParts[2].toIntOrNull() ?: 2025
                        
                        val monthNames = listOf(
                            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                        )
                        
                        if (startMonth == endMonth) {
                            // Same month: "22-23 Dec 2025"
                            val monthName = monthNames.getOrNull(startMonth - 1) ?: "Jan"
                            "$startDay-$endDay $monthName $year"
                        } else {
                            // Different months: "30 Dec - 2 Jan 2025"
                            val startMonthName = monthNames.getOrNull(startMonth - 1) ?: "Jan"
                            val endMonthName = monthNames.getOrNull(endMonth - 1) ?: "Jan"
                            "$startDay $startMonthName - $endDay $endMonthName $year"
                        }
                    } else {
                        date
                    }
                } else {
                    date
                }
            } else {
                date
            }
        } catch (e: Exception) {
            date
        }
    }
    
    /**
     * Check if event has prize pool
     */

    /**
     * Get club initials for logo fallback
     */
    fun getClubInitials(): String {
        return organizer.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { organizer.take(1).uppercase() }
    }
    
    /**
     * Check if event has ended (based on endDate)
     */
    fun isCompleted(): Boolean {
        return try {
            val eventEndDate = parseDate(endDate.ifEmpty { date })
            val currentTime = System.currentTimeMillis()
            eventEndDate < currentTime
        } catch (e: Exception) {
            false
        }
    }
    

    
    /**
     * Parse date string (DD/MM/YYYY) to timestamp
     */
    private fun parseDate(dateStr: String): Long {
        return try {
            if (dateStr.contains("/")) {
                val parts = dateStr.split("/")
                if (parts.size == 3) {
                    val day = parts[0].toIntOrNull() ?: 1
                    val month = parts[1].toIntOrNull() ?: 1
                    val year = parts[2].toIntOrNull() ?: 2025
                    
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(year, month - 1, day, 23, 59, 59)
                    calendar.timeInMillis
                } else {
                    0L
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Get days remaining until event completion
     */
    fun getDaysUntilCompletion(): Int {
        return try {
            val eventEndDate = parseDate(endDate.ifEmpty { date })
            val currentTime = System.currentTimeMillis()
            val diffInMillis = eventEndDate - currentTime
            (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get formatted registration close date
     * Converts "20/12/2025" (DD/MM/YYYY) to "Dec 20, 2025"
     */
    fun getFormattedRegistrationCloseDate(): String {
        return try {
            if (registrationCloseDate.isEmpty()) return ""
            
            if (registrationCloseDate.contains("/")) {
                val parts = registrationCloseDate.split("/")
                if (parts.size == 3) {
                    val day = parts[0].toIntOrNull() ?: 1
                    val month = parts[1].toIntOrNull() ?: 1
                    val year = parts[2].toIntOrNull() ?: 2025
                    
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    val monthName = monthNames.getOrNull(month - 1) ?: "Jan"
                    "$monthName $day, $year"
                } else {
                    registrationCloseDate
                }
            } else {
                registrationCloseDate
            }
        } catch (e: Exception) {
            registrationCloseDate
        }
    }
}

/**
 * Filter categories for events
 */
enum class EventCategory(val displayName: String, val iconRes: Int) {
    ALL("All", R.drawable.tool),
    COMPLETED("Completed", R.drawable.checked), // Shows events that have ended
    HACKATHON("Hackathon", R.drawable.hackathon),
    WORKSHOP("Workshop", R.drawable.workshop),
    COMPETITION("Competition", R.drawable.competition),
    SEMINAR("Seminar", R.drawable.training),
    CULTURAL("Cultural", R.drawable.cultural),
    SPORTS("Sports", R.drawable.sports),
    TECH("Tech", R.drawable.technology),
    OTHER("Other", R.drawable.megaphone);
    
    companion object {
        fun fromString(value: String): EventCategory {
            return values().find { it.displayName.equals(value, ignoreCase = true) } ?: ALL
        }
    }
}
