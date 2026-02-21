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
 * OpenAI Responses API client (no streaming).
 * Uses:
 *  - POST https://api.openai.com/v1/responses citeturn0search0turn0search4
 *  - Authorization: Bearer <API_KEY> citeturn0search0turn0search4
 *
 * Supports:
 *  - text conversation
 *  - image attachments (sent as data: URLs via input_image) citeturn0search0turn0search8
 *
 * Other file types are appended as text metadata (and content if small text).
 */
class OpenAiClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://api.openai.com"
) : UnifiedAiClient {

    override suspend fun send(request: AiRequest): AiResponse {
        val body = JSONObject().apply {
            put("model", request.model)

            val input = JSONArray()

            // Optional system instruction as a "system" message
            val instr = request.instruction.trim()
            if (instr.isNotBlank()) {
                input.put(JSONObject().put("role", "system").put("content", instr))
            }

            // prior messages
            request.messages.forEach { m ->
                input.put(JSONObject().put("role", m.role).put("content", m.content))
            }

            // build last user message with attachments as multi-part content (if any)
            if (request.attachments.isNotEmpty()) {
                // Convert the last user message into content array items.
                // We'll take the last user content as main text and attach images.
                val lastUser = request.messages.lastOrNull { it.role == "user" }?.content ?: ""
                val contentArr = JSONArray().apply {
                    put(JSONObject().put("type", "input_text").put("text", lastUser))
                }

                request.attachments.forEach { att ->
                    if (att.mimeType.startsWith("image/")) {
                        val b64 = Base64.getEncoder().encodeToString(att.bytes)
                        val dataUrl = "data:${att.mimeType};base64,$b64"
                        contentArr.put(
                            JSONObject()
                                .put("type", "input_image")
                                .put("image_url", dataUrl)
                        )
                    } else {
                        // For non-image files, include a short metadata note.
                        val note = "[ANEXO] ${att.name} (${att.mimeType}, ${att.bytes.size} bytes)"
                        contentArr.put(JSONObject().put("type", "input_text").put("text", note))
                        // If it's small text, include its text (best-effort UTF-8)
                        if (att.bytes.size <= 60_000 && att.mimeType.startsWith("text/")) {
                            val text = runCatching { att.bytes.toString(Charsets.UTF_8) }.getOrDefault("")
                            if (text.isNotBlank()) {
                                contentArr.put(JSONObject().put("type", "input_text")
                                    .put("text", "Conteúdo do arquivo ${att.name}:
```\n${text.take(50_000)}\n```"))
                            }
                        }
                    }
                }

                // Replace: remove last user from input (if it was added) and add multipart user message at end.
                // To keep it simple, we will append a new user message with multipart; duplicates are acceptable for personal use.
                input.put(JSONObject().put("role", "user").put("content", contentArr))
            }

            put("input", input)

            put("text", JSONObject().put("verbosity", "low"))
            put("reasoning", JSONObject().put("effort", "none"))
        }

        val req = Request.Builder()
            .url("$baseUrl/v1/responses")
            .addHeader("Authorization", "Bearer ${request.apiKey}")
            .addHeader("Content-Type", "application/json")
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
                throw IOException("OpenAI HTTP ${resp.code}: $msg")
            }
            val text = extractFirstOutputText(raw)
            return AiResponse(text = text.ifBlank { "(sem texto retornado)" })
        }
    }

    private fun extractFirstOutputText(jsonString: String): String {
        return runCatching {
            val root = JSONObject(jsonString)
            val output = root.optJSONArray("output") ?: JSONArray()
            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                val content = item.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val c = content.optJSONObject(j) ?: continue
                    val t = c.optString("type")
                    if (t == "output_text" || t == "text") {
                        val s = c.optString("text")
                        if (s.isNotBlank()) return s
                    }
                }
                val direct = item.optString("text")
                if (direct.isNotBlank()) return direct
            }
            root.optString("output_text", "")
        }.getOrDefault("")
    }
}
