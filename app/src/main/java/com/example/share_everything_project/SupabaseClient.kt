package com.example.share_everything_project

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
    // Define custom JSON config for reference (to be used in SupabaseHelper)
    val customJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://jrkddmvatenszhhmkydc.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impya2RkbXZhdGVuc3poaG1reWRjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDcyMjI4MTYsImV4cCI6MjA2Mjc5ODgxNn0.muSNAjNIZ7sGbeoeCXk_uhP56l3mMKlaHllh1brnUIs"
        ) {
            // Install required plugins
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
} 