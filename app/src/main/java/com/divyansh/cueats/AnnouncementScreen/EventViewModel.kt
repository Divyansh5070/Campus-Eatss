package com.divyansh.cueats.AnnouncementScreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.divyansh.cueats.SettingScreen.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Event tab types
 */
enum class EventTab {
    ACTIVE,
    COMPLETED,
    INTERESTED
}

/**
 * UI state for events screen
 */
data class EventsUiState(
    val events: List<Event> = emptyList(),
    val filteredEvents: List<Event> = emptyList(),
    val activeEvents: List<Event> = emptyList(),
    val completedEvents: List<Event> = emptyList(),
    val interestedEvents: List<Event> = emptyList(),
    val interestedEventIds: Set<String> = emptySet(),
    val selectedCategory: EventCategory = EventCategory.ALL,
    val selectedTab: EventTab = EventTab.ACTIVE,
    val searchQuery: String = "",
    val selectedDate: String? = null, // Format: "DD/MM/YYYY"
    val isLoading: Boolean = false,
    val error: String? = null,
    val clubsMap: Map<String, Club> = emptyMap() // Map of clubId to Club data
)

/**
 * UI state for event details screen
 */
data class EventDetailsUiState(
    val event: Event? = null,
    val club: Club? = null,
    val isLoading: Boolean = false,
    val isLoadingClub: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false
)

/**
 * ViewModel for managing events state
 */
class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository()
    private val userPreferences = UserPreferences(application)
    
    private val _eventsState = MutableStateFlow(EventsUiState())
    val eventsState: StateFlow<EventsUiState> = _eventsState.asStateFlow()
    
    private val _eventDetailsState = MutableStateFlow(EventDetailsUiState())
    val eventDetailsState: StateFlow<EventDetailsUiState> = _eventDetailsState.asStateFlow()
    
    init {
        // Don't call loadEvents() - it was setting events and filteredEvents to the full unfiltered list
        // including completed events, which was overwriting the correct filtering in listenToEventsRealtime()
        // loadEvents()
        listenToEventsRealtime()
        loadInterestedEvents()
    }
    
    /**
     * Load events from Firebase (one-time fetch)
     */
    private fun loadEvents() {
        viewModelScope.launch {
            _eventsState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val events = repository.getActiveEvents()
                _eventsState.update { 
                    it.copy(
                        events = events,
                        filteredEvents = events,
                        isLoading = false
                    )
                }
                // Load clubs for initial events
                loadClubsForEvents(events)
            } catch (e: Exception) {
                _eventsState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load events: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Listen to events in real-time
     */
    private fun listenToEventsRealtime() {
        viewModelScope.launch {
            repository.listenToEvents().collect { events ->
                // Separate active and completed events
                val activeEvents = events.filterNot { it.isCompleted() }
                val completedEvents = events.filter { it.isCompleted() }
                
                _eventsState.update { currentState ->
                    val filtered = filterEvents(
                        events = activeEvents,
                        category = currentState.selectedCategory,
                        searchQuery = currentState.searchQuery,
                        selectedDate = currentState.selectedDate,
                        completedEvents = completedEvents
                    )
                    
                    // Update interested events with latest data
                    val interestedEvents = events.filter { it.eventId in currentState.interestedEventIds }
                    
                    currentState.copy(
                        events = activeEvents,
                        activeEvents = activeEvents,
                        completedEvents = completedEvents,
                        interestedEvents = interestedEvents,
                        filteredEvents = filtered
                    )
                }
                // Load clubs for all events (active + completed)
                loadClubsForEvents(activeEvents + completedEvents)
            }
        }
    }
    
    /**
     * Load club data for all events
     */
    private fun loadClubsForEvents(events: List<Event>) {
        viewModelScope.launch {
            try {
                val clubIds = events.mapNotNull { it.clubId }.filter { it.isNotBlank() }.distinct()
                
                val clubsMap = mutableMapOf<String, Club>()
                
                // Fetch each club
                clubIds.forEach { clubId ->
                    val club = repository.getClubById(clubId)
                    if (club != null) {
                        clubsMap[clubId] = club
                    }
                }
                
                _eventsState.update { it.copy(clubsMap = clubsMap) }
            } catch (e: Exception) {
                // Silently fail - clubs are optional
            }
        }
    }
    
    /**
     * Load interested events from preferences
     */
    private fun loadInterestedEvents() {
        viewModelScope.launch {
            userPreferences.interestedEventIds.collect { ids ->
                _eventsState.update { currentState ->
                    // Get interested events from all events (active + completed)
                    val allEvents = currentState.activeEvents + currentState.completedEvents
                    val interestedEvents = allEvents.filter { it.eventId in ids }
                    currentState.copy(
                        interestedEventIds = ids,
                        interestedEvents = interestedEvents
                    )
                }
            }
        }
    }
    
    /**
     * Mark event as interested
     */
    fun markEventAsInterested(eventId: String) {
        viewModelScope.launch {
            userPreferences.addInterestedEvent(eventId)
        }
    }
    
    /**
     * Mark event as not interested (remove from interested list)
     */
    fun markEventAsNotInterested(eventId: String) {
        viewModelScope.launch {
            userPreferences.removeInterestedEvent(eventId)
        }
    }
    
    /**
     * Remove event from interested list
     */
    fun removeFromInterested(eventId: String) {
        // Immediately update UI state for instant feedback
        _eventsState.update { currentState ->
            val updatedInterestedIds = currentState.interestedEventIds - eventId
            val updatedInterestedEvents = currentState.interestedEvents.filter { it.eventId != eventId }
            
            currentState.copy(
                interestedEventIds = updatedInterestedIds,
                interestedEvents = updatedInterestedEvents,
                filteredEvents = if (currentState.selectedTab == EventTab.INTERESTED) {
                    updatedInterestedEvents
                } else {
                    currentState.filteredEvents
                }
            )
        }
        
        // Then update preferences in background
        viewModelScope.launch {
            userPreferences.removeInterestedEvent(eventId)
        }
    }
    
    /**
     * Select event tab (Active, Completed, or Interested)
     */
    fun selectTab(tab: EventTab) {
        _eventsState.update { currentState ->
            val eventsToFilter = when (tab) {
                EventTab.ACTIVE -> currentState.activeEvents
                EventTab.COMPLETED -> currentState.completedEvents
                EventTab.INTERESTED -> currentState.interestedEvents
            }
            
            val filtered = when (tab) {
                EventTab.INTERESTED, EventTab.COMPLETED -> {
                    // Show interested and completed events directly without filters
                    eventsToFilter
                }
                EventTab.ACTIVE -> {
                    // Apply filters only to active events
                    filterEvents(
                        events = eventsToFilter,
                        category = currentState.selectedCategory,
                        searchQuery = currentState.searchQuery,
                        selectedDate = currentState.selectedDate
                    )
                }
            }
            
            currentState.copy(
                selectedTab = tab,
                filteredEvents = filtered
            )
        }
    }
    
    /**
     * Filter events by category
     */
    fun filterByCategory(category: EventCategory) {
        _eventsState.update { currentState ->
            val eventsToFilter = if (category == EventCategory.COMPLETED) {
                currentState.completedEvents
            } else {
                currentState.activeEvents
            }
            
            val filtered = filterEvents(
                events = eventsToFilter,
                category = category,
                searchQuery = currentState.searchQuery,
                selectedDate = currentState.selectedDate,
                completedEvents = currentState.completedEvents
            )
            currentState.copy(
                selectedCategory = category,
                filteredEvents = filtered
            )
        }
    }
    
    /**
     * Search events
     */
    fun searchEvents(query: String) {
        _eventsState.update { currentState ->
            // Use completed events if COMPLETED category is selected, otherwise use active events
            val eventsToSearch = if (currentState.selectedCategory == EventCategory.COMPLETED) {
                currentState.completedEvents
            } else {
                currentState.activeEvents
            }
            
            val filtered = filterEvents(
                events = eventsToSearch,
                category = currentState.selectedCategory,
                searchQuery = query,
                selectedDate = currentState.selectedDate,
                completedEvents = currentState.completedEvents
            )
            currentState.copy(
                searchQuery = query,
                filteredEvents = filtered
            )
        }
    }
    
    /**
     * Filter events by date
     */
    fun filterByDate(date: String?) {
        _eventsState.update { currentState ->
            // Use completed events if COMPLETED category is selected, otherwise use active events
            val eventsToFilter = if (currentState.selectedCategory == EventCategory.COMPLETED) {
                currentState.completedEvents
            } else {
                currentState.activeEvents
            }
            
            val filtered = filterEvents(
                events = eventsToFilter,
                category = currentState.selectedCategory,
                searchQuery = currentState.searchQuery,
                selectedDate = date,
                completedEvents = currentState.completedEvents
            )
            currentState.copy(
                selectedDate = date,
                filteredEvents = filtered
            )
        }
    }
    
    /**
     * Clear date filter
     */
    fun clearDateFilter() {
        filterByDate(null)
    }

    private fun filterEvents(
        events: List<Event>,
        category: EventCategory,
        searchQuery: String,
        selectedDate: String? = null,
        completedEvents: List<Event> = emptyList()
    ): List<Event> {
        // If COMPLETED category is selected, show completed events
        if (category == EventCategory.COMPLETED) {
            var filtered = completedEvents
            
            // Filter by search query
            if (searchQuery.isNotBlank()) {
                filtered = repository.searchEvents(filtered, searchQuery)
            }
            
            // Filter by date
            if (selectedDate != null) {
                filtered = filtered.filter { event ->
                    event.date == selectedDate || 
                    (event.endDate.isNotEmpty() && event.endDate != event.date && 
                     isDateInRange(selectedDate, event.date, event.endDate))
                }
            }
            
            return filtered
        }
        
        // For other categories (including ALL), filter active events only
        // Explicitly exclude completed events as a safety measure
        var filtered = events.filterNot { it.isCompleted() }
        
        // Filter by category
        if (category != EventCategory.ALL) {
            filtered = filtered.filter { 
                it.category.equals(category.displayName, ignoreCase = true) 
            }
        }
        
        // Filter by date
        if (selectedDate != null) {
            filtered = filtered.filter { event ->
                // Check if event date matches selected date
                event.date == selectedDate || 
                // Or if event is a multi-day event and selected date falls within range
                (event.endDate.isNotEmpty() && event.endDate != event.date && 
                 isDateInRange(selectedDate, event.date, event.endDate))
            }
        }
        
        // Filter by search query
        if (searchQuery.isNotBlank()) {
            filtered = repository.searchEvents(filtered, searchQuery)
        }
        
        return filtered
    }
    
    /**
     * Check if a date falls within a date range
     */
    private fun isDateInRange(date: String, startDate: String, endDate: String): Boolean {
        return try {
            val dateParts = date.split("/")
            val startParts = startDate.split("/")
            val endParts = endDate.split("/")
            
            if (dateParts.size != 3 || startParts.size != 3 || endParts.size != 3) {
                return false
            }
            
            // Convert to comparable format: YYYYMMDD
            val dateInt = dateParts[2].toInt() * 10000 + dateParts[1].toInt() * 100 + dateParts[0].toInt()
            val startInt = startParts[2].toInt() * 10000 + startParts[1].toInt() * 100 + startParts[0].toInt()
            val endInt = endParts[2].toInt() * 10000 + endParts[1].toInt() * 100 + endParts[0].toInt()
            
            dateInt in startInt..endInt
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Load event details by ID
     */
    fun loadEventDetails(eventId: String) {
        viewModelScope.launch {
            _eventDetailsState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val event = repository.getEventById(eventId)
                if (event != null) {
                    _eventDetailsState.update { 
                        it.copy(
                            event = event,
                            isLoading = false
                        )
                    }
                } else {
                    _eventDetailsState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Event not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _eventDetailsState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load event: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Toggle favorite status for an event
     */
    fun toggleFavorite() {
        _eventDetailsState.update { 
            it.copy(isFavorite = !it.isFavorite)
        }
        // TODO: Persist favorite status to Firebase or local storage
    }
    
    /**
     * Refresh events
     */
    fun refreshEvents() {
        loadEvents()
    }
    
    /**
     * Load club profile by clubId
     */
    fun loadClubProfile(clubId: String) {
        if (clubId.isBlank()) return
        
        viewModelScope.launch {
            _eventDetailsState.update { it.copy(isLoadingClub = true) }
            
            try {
                val club = repository.getClubById(clubId)
                _eventDetailsState.update { 
                    it.copy(
                        club = club,
                        isLoadingClub = false
                    )
                }
            } catch (e: Exception) {
                _eventDetailsState.update { 
                    it.copy(
                        isLoadingClub = false,
                        error = "Failed to load club profile: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _eventsState.update { it.copy(error = null) }
    }
}
