package com.example.cueats.LoginScreen


import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//class AuthRepository {
//    private val auth = SupabaseClient.auth
//    private val database = SupabaseClient.database
//
//    suspend fun signUp(signUpData: SignUpData): Result<UserInfo> = withContext(Dispatchers.IO) {
//        try {
//            // Sign up user with Supabase Auth
//            val user = auth.signUpWith(Email) {
//                email = signUpData.email
//                password = signUpData.password
//            }
//
//            // Update profile with additional information
//            user?.let {
//                updateUserProfile(
//                    userId = it.id,
//                    fullName = signUpData.fullName,
//                    university = signUpData.university
//                )
//            }
//
//            Result.success(user!!)
//        } catch (e: Exception) {
//            Result.failure(e)
//        } as Result<UserInfo>
//    }
//
//    suspend fun signIn(signInData: SignInData): Result<UserInfo> = withContext(Dispatchers.IO) {
//        try {
//            val user = auth.signInWith(Email) {
//                email = signInData.email
//                password = signInData.password
//            }
//            Result.success(user!!)
//        } catch (e: Exception) {
//            Result.failure(e)
//        } as Result<UserInfo>
//    }
//
//    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
//        try {
//            auth.signOut()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    suspend fun getCurrentUser(): UserInfo? = withContext(Dispatchers.IO) {
//        auth.currentUserOrNull()
//    }
//
//    suspend fun getUserProfile(userId: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
//        try {
//            val profile = database.from("profiles")
//                .select()
//                .eq("id", userId)
//                .decodeSingleOrNull<UserProfile>()
//            Result.success(profile)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    private suspend fun updateUserProfile(
//        userId: String,
//        fullName: String,
//        university: String
//    ) {
//        try {
//            database.from("profiles")
//                .update({
//                    set("full_name", fullName)
//                    set("university", university)
//                    set("updated_at", "now()")
//                }) {
//                    eq("id", userId)
//                }
//        } catch (e: Exception) {
//            // Handle error silently or log
//        }
//    }
//
//
//
//    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
//        try {
//            auth.resetPasswordForEmail(email)
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}