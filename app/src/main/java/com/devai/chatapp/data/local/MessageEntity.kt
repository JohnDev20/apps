package com.devai.chatapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
