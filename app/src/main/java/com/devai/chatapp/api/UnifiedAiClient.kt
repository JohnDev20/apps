package com.devai.chatapp.api

interface UnifiedAiClient { suspend fun send(request: AiRequest): AiResponse }
