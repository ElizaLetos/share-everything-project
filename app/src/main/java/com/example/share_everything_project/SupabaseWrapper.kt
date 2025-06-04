package com.example.share_everything_project

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
import org.json.JSONObject
import org.json.JSONArray

class SupabaseWrapper {
    companion object {
        @JvmStatic
        fun fetchAllUserConversations(client: SupabaseClient, username: String): JSONArray {
            try {
                // Fetch all messages where the user is either sender or receiver
                val response = runBlocking(Dispatchers.IO) {
                    client.postgrest["messages"]
                        .select {
                            filter {
                                or {
                                    eq("sender", username)
                                    eq("receiver", username)
                                }
                            }
                            order("timestamp", Order.DESCENDING)
                        }
                        .decodeList<MessageData>()
                }
                
                println("Supabase all conversations response size: ${response.size}")
                
                // Convert response to JSONArray
                val jsonArray = JSONArray()
                response.forEach { messageData ->
                    println("Processing conversation message: sender=${messageData.sender}, receiver=${messageData.receiver}")
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
                
                println("Supabase conversation response size: ${response.size}")
                
                // Convert response to JSONArray
                val jsonArray = JSONArray()
                response.forEach { messageData ->
                    println("Processing message: sender=${messageData.sender}, receiver=${messageData.receiver}")
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
            println("Setting up realtime subscription for users: $user1 and $user2")

            // Set up the postgres change flow for INSERT events
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
            }

            // Subscribe to the channel first
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    println("Subscribing to realtime channel")
                    channel.subscribe()
                    println("Successfully subscribed to realtime channel")
                } catch (e: Exception) {
                    println("Error subscribing to channel: ${e.message}")
                    e.printStackTrace()
                }
            }

            // Collect the flow and filter messages between the two users
            changeFlow.onEach { insertAction ->
                try {
                    val record = insertAction.record
                    println("Received realtime message: $record")
                    
                    // Properly access the record fields using the correct type casting
                    val sender = record["sender"]?.toString()?.trim('"')
                    val receiver = record["receiver"]?.toString()?.trim('"')
                    val content = record["content"]?.toString()?.trim('"')
                    val type = record["type"]?.toString()?.trim('"')
                    val timestamp = (record["timestamp"] as? Number)?.toLong()
                    val id = record["id"]?.toString()?.trim('"')
                    val createdAt = record["created_at"]?.toString()?.trim('"')

                    println("Parsed message fields - Sender: $sender, Receiver: $receiver")

                    // Check if the message matches our filter
                    if ((sender == user1 && receiver == user2) ||
                        (sender == user2 && receiver == user1)
                    ) {
                        println("Message matches filter - Processing: sender=$sender, receiver=$receiver")

                        // Convert the record to JSONObject with all fields
                        val messageJson = JSONObject().apply {
                            put("id", id)
                            put("sender", sender)
                            put("receiver", receiver)
                            put("content", content)
                            put("type", type)
                            put("timestamp", timestamp ?: System.currentTimeMillis())  // Use current time as fallback
                            put("created_at", createdAt)
                        }

                        println("Sending message to callback: $messageJson")
                        onMessageReceived(messageJson)
                    } else {
                        println("Message does not match filter - Ignoring: sender=$sender, receiver=$receiver")
                    }
                } catch (e: Exception) {
                    println("Error in realtime subscription: ${e.message}")
                    e.printStackTrace()
                }
            }.launchIn(CoroutineScope(Dispatchers.IO))

            return channel
        }

        @JvmStatic
        fun unsubscribeFromChannel(channel: RealtimeChannel) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    println("Unsubscribing from channel")
                    channel.unsubscribe()
                    println("Successfully unsubscribed from channel")
                } catch (e: Exception) {
                    println("Error unsubscribing from channel: ${e.message}")
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