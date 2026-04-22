package com.divyansh.cueats.AnnouncementScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * ViewModel for managing event registration
 * Phase 1: Basic form state and submission
 */
class RegistrationViewModel : ViewModel() {
    private val repository = RegistrationRepository()
    private val auth = FirebaseAuth.getInstance()
    
    // Form state
    var userName by mutableStateOf("")
        private set
    var userEmail by mutableStateOf("")
        private set
    var userPhone by mutableStateOf("")
        private set
    
    // UI state
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isRegistrationSuccess by mutableStateOf(false)
        private set
    var isAlreadyRegistered by mutableStateOf(false)
        private set
    
    /**
     * Update form fields
     */
    fun updateUserName(value: String) {
        userName = value
        errorMessage = null
    }
    
    fun updateUserEmail(value: String) {
        userEmail = value
        errorMessage = null
    }
    
    fun updateUserPhone(value: String) {
        userPhone = value
        errorMessage = null
    }
    
    /**
     * Check if user is already registered for an event
     */
    fun checkRegistrationStatus(eventId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                isAlreadyRegistered = repository.isUserRegistered(eventId, userId)
            } catch (e: Exception) {
                // Silently fail, assume not registered
                isAlreadyRegistered = false
            }
        }
    }
    
    /**
     * Submit registration
     */
    fun submitRegistration(eventId: String, eventTitle: String) {
        // Basic validation
        if (userName.isBlank() || userEmail.isBlank() || userPhone.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            errorMessage = "You must be logged in to register"
            return
        }
        
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val registration = EventRegistration(
                    eventId = eventId,
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    userPhone = userPhone
                )
                
                val result = repository.registerForEvent(registration)
                
                if (result.isSuccess) {
                    isRegistrationSuccess = true
                    isAlreadyRegistered = true
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Reset form state
     */
    fun resetForm() {
        userName = ""
        userEmail = ""
        userPhone = ""
        errorMessage = null
        isRegistrationSuccess = false
    }
}
