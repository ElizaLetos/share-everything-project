package com.example.share_everything_project

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import kotlin.jvm.JvmField

// Use ignoreUnknownKeys to handle extra fields from database
private val jsonConfig = Json { 
    ignoreUnknownKeys = true 
    isLenient = true
    coerceInputValues = true
}

@Serializable
data class MessageData(
    @JvmField val id: Int? = null,
    @JvmField val sender: String,
    @JvmField val receiver: String,
    @JvmField val content: String,
    @JvmField val type: String,
    @JvmField val timestamp: Long,
    @SerialName("created_at") @JvmField val createdAt: String? = null
)

@OptIn(SupabaseExperimental::class)
object SupabaseHelper {
    fun insertMessage(client: SupabaseClient, message: JSONObject) {
        runBlocking {
            try {
                // Convert JSONObject to MessageData
                val messageData = MessageData(
                    sender = message.getString("sender"),
                    receiver = message.getString("receiver"),
                    content = message.getString("content"),
                    type = message.getString("type"),
                    timestamp = message.getLong("timestamp"),
                    createdAt = message.optString("created_at", null)
                )
                
                // Insert a list containing the single message
                client.postgrest["messages"].insert(listOf(messageData))
                println("Message inserted successfully!")
            } catch (e: Exception) {
                println("Error inserting message: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    fun getMessages(client: SupabaseClient, user1: String, user2: String): List<Message> {
        return runBlocking {
            try {
                // Enable real-time for the messages table
                client.realtime.channel("public:messages").subscribe()
                
                // Get PostgrestResult with our query
                val postgrestResult = client.postgrest["messages"]
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
                        order(
                            "timestamp",
                            order = Order.ASCENDING,
                            nullsFirst = false,
                            referencedTable = null
                        )
                    }
                
                try {
                    val messages = mutableListOf<Message>()
                    val supabaseMessages = postgrestResult.decodeList<MessageData>() 
                    
                    // Convert to Message objects
                    for (messageData in supabaseMessages) {
                        messages.add(
                            Message(
                                messageData.sender,
                                messageData.receiver,
                                messageData.content,
                                messageData.type,
                                messageData.timestamp
                            )
                        )
                    }
                    
                    return@runBlocking messages
                } catch (e: Exception) {
                    println("Error parsing messages: ${e.message}")
                    e.printStackTrace()
                    return@runBlocking emptyList<Message>()
                }
            } catch (e: Exception) {
                println("Error getting messages: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    fun uploadFile(client: SupabaseClient, bucket: String, fileName: String, fileBytes: ByteArray) {
        runBlocking {
            client.storage[bucket].upload(fileName, fileBytes)
        }
    }

    fun getPublicUrl(client: SupabaseClient, bucket: String, fileName: String): String {
        return runBlocking {
            client.storage[bucket].publicUrl(fileName)
        }
    }

    fun checkUserExists(client: SupabaseClient, phoneNumber: String): Boolean {
        return runBlocking {
            try {
                // Query the users table to check if the phone number exists
                val result = client.postgrest["users"]
                    .select {
                        filter {
                            eq("phone_number", phoneNumber)
                        }
                    }
                    .decodeList<Map<String, Any>>()
                
                // Return true if any user was found
                result.isNotEmpty()
            } catch (e: Exception) {
                println("Error checking if user exists: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }
} 