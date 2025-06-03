package com.example.share_everything_project

import android.service.autofill.Validators.or
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.json.JSONObject
import org.json.JSONArray

class SupabaseWrapper {
    companion object {
        @JvmStatic
        fun fetchMessages(client: SupabaseClient, user1: String, user2: String): JSONArray {
            try {
                // Fetch messages from Supabase
                val response = runBlocking(Dispatchers.IO) {
                    client.postgrest["messages"]
                        .select {
                            filter {
                                or {
                                    and {
                                        eq("sender", user1)
                                        eq("receiver", user2)
                                    }
                                    and {
                                        eq("sender", user2)
                                        eq("receiver", user1)
                                    }
                                }
                            }
                            order("timestamp", Order.ASCENDING)
                        }
                        .decodeList<MessageData>()
                }
                
                // Convert response to JSONArray
                val jsonArray = JSONArray()
                response.forEach { messageData ->
                    val jsonObject = JSONObject().apply {
                        put("id", messageData.id)
                        put("sender", messageData.sender)
                        put("receiver", messageData.receiver)
                        put("content", messageData.content)
                        put("type", messageData.type)
                        put("timestamp", messageData.timestamp)
                        put("created_at", messageData.createdAt)
                    }
                    jsonArray.put(jsonObject)
                }
                return jsonArray
            } catch (e: Exception) {
                e.printStackTrace()
                return JSONArray()
            }
        }

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
            val channel = client.realtime.channel("public:messages")

            // Set up the postgres change flow for INSERT events
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
            }

            // Collect the flow and filter messages between the two users
            changeFlow.onEach { insertAction ->
                try {
                    val record = insertAction.record
                    val sender = record["sender"] as? String
                    val receiver = record["receiver"] as? String

                    // Check if the message matches our filter
                    if ((sender == user1 && receiver == user2) ||
                        (sender == user2 && receiver == user1)
                    ) {

                        // Convert the record to JSONObject
                        val messageJson = JSONObject().apply {
                            put("sender", record["sender"])
                            put("receiver", record["receiver"])
                            put("content", record["content"])
                            put("type", record["type"])
                            put("timestamp", record["timestamp"])
                            put("created_at", record["created_at"])
                        }

                        onMessageReceived(messageJson)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.launchIn(CoroutineScope(Dispatchers.IO))

            // Subscribe to the channel
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.subscribe()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return channel
        }

        @JvmStatic
        fun unsubscribeFromChannel(channel: RealtimeChannel) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun uploadFile(
            client: SupabaseClient,
            bucket: String,
            fileName: String,
            fileBytes: ByteArray
        ) {
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