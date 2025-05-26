package com.example.cueats.LoginScreen



import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.launch

//class AuthViewModel(
//    private val repository: AuthRepository = AuthRepository()
//) : ViewModel() {
//
//    var uiState by mutableStateOf(AuthUiState())
//        private set
//
//    var currentUser by mutableStateOf<UserInfo?>(null)
//        private set
//
//    var userProfile by mutableStateOf<UserProfile?>(null)
//        private set
//
//    init {
//        checkCurrentUser()
//    }
//
//    fun signUp(signUpData: SignUpData) {
//        viewModelScope.launch {
//            uiState = uiState.copy(isLoading = true, error = null)
//
//            repository.signUp(signUpData)
//                .onSuccess { user ->
//                    currentUser = user
//                    uiState = uiState.copy(
//                        isLoading = false,
//                        isSignedIn = true
//                    )
//                }
//                .onFailure { exception ->
//                    uiState = uiState.copy(
//                        isLoading = false,
//                        error = exception.message
//                    )
//                }
//        }
//    }
//
//    fun signIn(signInData: SignInData) {
//        viewModelScope.launch {
//            uiState = uiState.copy(isLoading = true, error = null)
//
//            repository.signIn(signInData)
//                .onSuccess { user ->
//                    currentUser = user
//                    getUserProfile(user.id)
//                    uiState = uiState.copy(
//                        isLoading = false,
//                        isSignedIn = true
//                    )
//                }
//                .onFailure { exception ->
//                    uiState = uiState.copy(
//                        isLoading = false,
//                        error = exception.message
//                    )
//                }
//        }
//    }
//
//    fun signOut() {
//        viewModelScope.launch {
//            repository.signOut()
//                .onSuccess {
//                    currentUser = null
//                    userProfile = null
//                    uiState = AuthUiState() // Reset to initial state
//                }
//        }
//    }
//
//    fun resetPassword(email: String) {
//        viewModelScope.launch {
//            uiState = uiState.copy(isLoading = true, error = null)
//
//            repository.resetPassword(email)
//                .onSuccess {
//                    uiState = uiState.copy(
//                        isLoading = false,
//                        message = "Password reset email sent!"
//                    )
//                }
//                .onFailure { exception ->
//                    uiState = uiState.copy(
//                        isLoading = false,
//                        error = exception.message
//                    )
//                }
//        }
//    }
//
//    private fun checkCurrentUser() {
//        viewModelScope.launch {
//            currentUser = repository.getCurrentUser()
//            currentUser?.let { user ->
//                getUserProfile(user.id)
//                uiState = uiState.copy(isSignedIn = true)
//            }
//        }
//    }
//
//    private fun getUserProfile(userId: String) {
//        viewModelScope.launch {
//            repository.getUserProfile(userId)
//                .onSuccess { profile ->
//                    userProfile = profile
//                }
//        }
//    }
//
//    fun clearError() {
//        uiState = uiState.copy(error = null)
//    }
//
//    fun clearMessage() {
//        uiState = uiState.copy(message = null)
//    }
//}
//
//data class AuthUiState(
//    val isLoading: Boolean = false,
//    val isSignedIn: Boolean = false,
//    val error: String? = null,
//    val message: String? = null
//)