package com.example.share_everything_project

import kotlinx.serialization.Serializable

@Serializable
data class MessageData(
    val sender: String,
    val receiver: String,
    val content: String,
    val type: String,
    val timestamp: Long,
    val createdAt: String? = null
) 