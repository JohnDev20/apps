package com.devai.chatapp.api

data class ChatMessage(
    val role: String,
    val content: String
)

enum class AiProvider { OPENAI, ANTHROPIC, GEMINI }

data class Attachment(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray
)

data class AiRequest(
    val provider: AiProvider,
    val apiKey: String,
    val model: String,
    val instruction: String,
    val messages: List<ChatMessage>,
    val attachments: List<Attachment> = emptyList()
)

data class AiResponse(val text: String)
