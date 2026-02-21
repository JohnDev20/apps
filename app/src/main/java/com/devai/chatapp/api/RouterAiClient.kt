package com.devai.chatapp.api

class RouterAiClient(
    private val openai: UnifiedAiClient = OpenAiClient(),
    private val anthropic: UnifiedAiClient = AnthropicClient(),
    private val gemini: UnifiedAiClient = GeminiClient()
) : UnifiedAiClient {
    override suspend fun send(request: AiRequest): AiResponse {
        return when (request.provider) {
            AiProvider.OPENAI -> openai.send(request)
            AiProvider.ANTHROPIC -> anthropic.send(request)
            AiProvider.GEMINI -> gemini.send(request)
        }
    }
}
