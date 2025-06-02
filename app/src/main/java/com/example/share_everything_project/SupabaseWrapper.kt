package com.example.share_everything_project

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class SupabaseWrapper {
    companion object {
        @JvmStatic
        fun insertMessage(client: SupabaseClient, messageJson: JSONObject) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Convert JSONObject to MessageData
                    val messageData = MessageData(
                        sender = messageJson.getString("sender"),
                        receiver = messageJson.getString("receiver"),
                        content = messageJson.getString("content"),
                        type = messageJson.getString("type"),
                        timestamp = messageJson.getLong("timestamp"),
                        createdAt = messageJson.optString("created_at", null)
                    )
                    
                    // Insert a list containing the single message
                    client.postgrest
                        .from("messages")
                        .insert(listOf(messageData))
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
        }

        @JvmStatic
        fun setupRealtimeSubscription(
            client: SupabaseClient,
            user1: String,
            user2: String,
            onMessageReceived: (JSONObject) -> Unit
        ): RealtimeChannel {
            // Create a filter for messages between these two users
            val filter = "(sender=eq.$user1 AND receiver=eq.$user2) OR (sender=eq.$user2 AND receiver=eq.$user1)"

            // Subscribe to real-time updates
            return client.realtime
                .channel("public:messages")
                .on("INSERT") { payload ->
                    try {
                        // Parse the new message
                        val messageJson = JSONObject(payload.toString())
                        // Check if the message matches our filter
                        val sender = messageJson.getString("sender")
                        val receiver = messageJson.getString("receiver")
                        if ((sender == user1 && receiver == user2) || 
                            (sender == user2 && receiver == user1)) {
                            onMessageReceived(messageJson)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .subscribe()
        }

        @JvmStatic
        fun uploadFile(client: SupabaseClient, bucket: String, fileName: String, fileBytes: ByteArray) {
            CoroutineScope(Dispatchers.IO).launch {
                client.storage
                    .from(bucket)
                    .upload(fileName, fileBytes)
            }
        }

        @JvmStatic
        fun getPublicUrl(client: SupabaseClient, bucket: String, fileName: String): String {
            return client.storage
                .from(bucket)
                .publicUrl(fileName)
        }
    }
} 