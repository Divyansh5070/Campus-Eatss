package com.example.cueats.LoginScreen

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "YOUR_SUPABASE_URL", // Replace with your URL
        supabaseKey = "YOUR_SUPABASE_ANON_KEY" // Replace with your anon key
    ) {
        install(Auth)
        install(Postgrest)
    }

    val auth = client.auth
    val database = client.postgrest
}