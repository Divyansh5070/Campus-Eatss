package com.divyansh.cueats.LoginScreen


import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val full_name: String? = null,
    val university: String? = null,
    val email: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class SignUpData(
    val fullName: String,
    val email: String,
    val password: String,
    val university: String
)

data class SignInData(
    val email: String,
    val password: String
)