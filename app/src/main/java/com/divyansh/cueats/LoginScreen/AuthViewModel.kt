package com.divyansh.cueats.LoginScreen

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel

data class AuthState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _authState = mutableStateOf(AuthState())
    val authState: State<AuthState> = _authState

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        Log.d(TAG, "AuthViewModel initialized")
        checkAuthState()
    }

    private fun checkAuthState() {
        val user = authRepository.currentUser
        Log.d(TAG, "Checking auth state - Current user: ${user?.email ?: "null"}")
        _authState.value = _authState.value.copy(
            user = user,
            isLoggedIn = user != null
        )
        Log.d(TAG, "Auth state updated - isLoggedIn: ${_authState.value.isLoggedIn}")
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "Starting email login for: $email")
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.loginWithEmail(email, password)
            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Email login successful for: ${user.email}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true,
                        error = null
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "Email login failed", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Login failed"
                    )
                }
            )
        }
    }

    fun registerWithEmail(email: String, password: String, name: String) {
        viewModelScope.launch {
            Log.d(TAG, "Starting email registration for: $email")
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.registerWithEmail(email, password, name)
            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Email registration successful for: ${user.email}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true,
                        error = null
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "Email registration failed", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Registration failed"
                    )
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            Log.d(TAG, "Starting Google Sign-In with token: ${idToken.take(20)}...")
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Google Sign-In SUCCESS")
                    Log.d(TAG, "- User: ${user.email}")
                    Log.d(TAG, "- UID: ${user.uid}")
                    Log.d(TAG, "- Display Name: ${user.displayName}")

                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true,
                        error = null
                    )

                    Log.d(TAG, "Auth state updated - isLoggedIn: ${_authState.value.isLoggedIn}")
                    Log.d(TAG, "Auth state user: ${_authState.value.user?.email}")

                    // Force a delay to ensure state propagation
                    kotlinx.coroutines.delay(500)
                    Log.d(TAG, "After delay - isLoggedIn: ${_authState.value.isLoggedIn}")
                },
                onFailure = { exception ->
                    Log.e(TAG, "Google Sign-In FAILED", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Google Sign-In failed: ${exception.message}"
                    )
                }
            )
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            Log.d(TAG, "Starting password reset for: $email")
            _authState.value = _authState.value.copy(isLoading = true, error = null, successMessage = null)

            val result = authRepository.resetPassword(email)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Password reset email sent successfully")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Password reset email sent! Check your inbox."
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "Password reset failed", exception)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to send reset email"
                    )
                }
            )
        }
    }

    fun signOut() {
        Log.d(TAG, "Signing out user")
        authRepository.signOut()
        _authState.value = AuthState()
    }

    fun refreshAuthState() {
        Log.d(TAG, "Refreshing auth state")
        checkAuthState()
    }

    fun clearError() {
        Log.d(TAG, "Clearing error")
        _authState.value = _authState.value.copy(error = null)
    }

    fun clearSuccess() {
        Log.d(TAG, "Clearing success message")
        _authState.value = _authState.value.copy(successMessage = null)
    }

    fun setError(message: String) {
        Log.e(TAG, "Setting error: $message")
        _authState.value = _authState.value.copy(error = message, isLoading = false)
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        Log.d(TAG, "Getting GoogleSignInClient")
        return authRepository.getGoogleSignInClient(context)
    }
}