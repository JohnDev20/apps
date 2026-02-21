package com.devai.chatapp.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Anthropic Messages API client (no streaming).
 * Image base64 format shown in official vision docs. citeturn0search9
 *
 * Endpoint: POST https://api.anthropic.com/v1/messages
 * Headers:
 *  - x-api-key: <KEY>
 *  - anthropic-version: 2023-06-01 citeturn0search9
 */
class AnthropicClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://api.anthropic.com"
) : UnifiedAiClient {

    override suspend fun send(request: AiRequest): AiResponse {
        val body = JSONObject().apply {
            put("model", request.model)
            put("max_tokens", 1024)

            val messages = JSONArray()

            // system is a top-level string in Anthropic; we'll prepend into first user message if needed
            val instr = request.instruction.trim()

            // Build a single user message with text + optional images/notes
            val contentArr = JSONArray()

            val joinedText = buildString {
                if (instr.isNotBlank()) {
                    append("INSTRUÇÃO FIXA:\n")
                    append(instr)
                    append("\n\n")
                }
                request.messages.forEach { m ->
                    append(m.role.uppercase())
                    append(": ")
                    append(m.content)
                    append("\n\n")
                }
            }.trim()

            contentArr.put(JSONObject().put("type", "text").put("text", joinedText))

            request.attachments.forEach { att ->
                if (att.mimeType.startsWith("image/")) {
                    val b64 = Base64.getEncoder().encodeToString(att.bytes)
                    contentArr.put(
                        JSONObject()
                            .put("type", "image")
                            .put("source", JSONObject()
                                .put("type", "base64")
                                .put("media_type", att.mimeType)  // must be image/jpeg|png|gif|webp citeturn0search9turn0search1
                                .put("data", b64)
                            )
                    )
                } else {
                    contentArr.put(
                        JSONObject().put("type", "text")
                            .put("text", "[ANEXO] ${att.name} (${att.mimeType}, ${att.bytes.size} bytes)")
                    )
                }
            }

            messages.put(JSONObject().put("role", "user").put("content", contentArr))
            put("messages", messages)
        }

        val req = Request.Builder()
            .url("$baseUrl/v1/messages")
            .addHeader("x-api-key", request.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching {
                    val err = JSONObject(raw)
                    err.optJSONObject("error")?.optString("message")
                        ?: err.optString("message")
                        ?: raw.take(300)
                }.getOrDefault(raw.take(300))
                throw IOException("Anthropic HTTP ${resp.code}: $msg")
            }
            // Response usually: content: [{type:"text", text:"..."}]
            val text = runCatching {
                val root = JSONObject(raw)
                val content = root.optJSONArray("content") ?: JSONArray()
                for (i in 0 until content.length()) {
                    val c = content.optJSONObject(i) ?: continue
                    val t = c.optString("type")
                    if (t == "text") {
                        val s = c.optString("text")
                        if (s.isNotBlank()) return@runCatching s
                    }
                }
                ""
            }.getOrDefault("")
            return AiResponse(text = text.ifBlank { "(sem texto retornado)" })
        }
    }
}
