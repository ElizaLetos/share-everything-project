package com.example.share_everything_project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageData(
    @SerialName("id") @JvmField val id: Int? = null,
    @SerialName("sender") @JvmField val sender: String,
    @SerialName("receiver") @JvmField val receiver: String,
    @SerialName("content") @JvmField val content: String,
    @SerialName("type") @JvmField val type: String,
    @SerialName("timestamp") @JvmField val timestamp: Long,
    @SerialName("created_at") @JvmField val createdAt: String? = null
) 