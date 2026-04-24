package com.divyansh.cueats.LoginScreen

import com.divyansh.cueats.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.firestore.BuildConfig

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "AuthRepository"
        // Web Client ID is loaded from BuildConfig (set via secrets.properties — never committed)
        // See secrets.properties.example for the required key: GOOGLE_WEB_CLIENT_ID
        private val WEB_CLIENT_ID = BuildConfig.GOOGLE_WEB_CLIENT_ID
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Attempting email login for: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Email login successful")
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Email login failed", e)
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Attempting email registration for: $email")
            // Create user with email and password
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            // Update the user's display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()

            user.updateProfile(profileUpdates).await()
            Log.d(TAG, "Profile updated with name: $name")

            // Save user data to Firestore
            val userInfo = hashMapOf(
                "uid" to user.uid,
                "displayName" to name,
                "email" to email,
                "role" to "user", // Default role is user
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(user.uid)
                .set(userInfo)
                .await()

            Log.d(TAG, "User data saved to Firestore")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Email registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Starting Google Sign-In with idToken")
            Log.d(TAG, "IdToken received: ${idToken.take(20)}...")

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Log.d(TAG, "Created Google credential")

            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            if (user == null) {
                Log.e(TAG, "Firebase returned null user after successful authentication")
                return Result.failure(Exception("Authentication succeeded but user is null"))
            }

            Log.d(TAG, "Firebase sign-in successful:")
            Log.d(TAG, "- User ID: ${user.uid}")
            Log.d(TAG, "- Email: ${user.email}")
            Log.d(TAG, "- Display Name: ${user.displayName}")
            Log.d(TAG, "- Is Email Verified: ${user.isEmailVerified}")

            // Check if user exists in Firestore, if not create entry
            try {
                val userDoc = firestore.collection("users").document(user.uid).get().await()

                if (!userDoc.exists()) {
                    Log.d(TAG, "Creating new user document in Firestore")
                    val userInfo = hashMapOf(
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: "User"),
                        "email" to user.email,
                        "role" to "user", // Default role is user
                        "createdAt" to System.currentTimeMillis(),
                        "provider" to "google"
                    )

                    firestore.collection("users")
                        .document(user.uid)
                        .set(userInfo)
                        .await()
                    Log.d(TAG, "User document created in Firestore")
                } else {
                    Log.d(TAG, "User document already exists in Firestore")
                }
            } catch (firestoreException: Exception) {
                Log.w(TAG, "Firestore operation failed, but authentication was successful", firestoreException)
                // Don't fail the entire operation if Firestore fails
            }

            Log.d(TAG, "Google Sign-In completed successfully")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed with exception: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            Log.d(TAG, "Sending password reset email to: $email")
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send password reset email", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRole(userId: String): String {
        return try {
            Log.d(TAG, "Fetching user role for: $userId")
            val userDoc = firestore.collection("users").document(userId).get().await()
            val role = userDoc.getString("role") ?: "user"
            Log.d(TAG, "User role: $role")
            role
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user role, defaulting to 'user'", e)
            "user" // Default to user role if fetch fails
        }
    }

    fun signOut() {
        Log.d(TAG, "Signing out user")
        auth.signOut()
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        Log.d(TAG, "Creating GoogleSignInClient")
        Log.d(TAG, "Package name: ${context.packageName}")
        
        // Use the hardcoded Web Client ID (verified from google-services.json)
        val webClientId = WEB_CLIENT_ID
        Log.d(TAG, "Using Web Client ID: $webClientId")
        
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .build()

            val client = GoogleSignIn.getClient(context, gso)
            Log.d(TAG, "GoogleSignInClient created successfully")
            
            // Check if there's already a signed-in account
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                Log.d(TAG, "Found existing signed-in account: ${account.email}")
            } else {
                Log.d(TAG, "No existing signed-in account found")
            }
            
            return client
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create GoogleSignInClient", e)
            throw e
        }
    }
}