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
 * Gemini generateContent REST API.
 * Google docs: generating content endpoint and capabilities. citeturn0search2turn0search14
 *
 * Endpoint (public Gemini API):
 *  POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key=API_KEY
 *
 * This client supports:
 *  - text
 *  - image inline_data (base64) best-effort
 */
class GeminiClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://generativelanguage.googleapis.com"
) : UnifiedAiClient {

    override suspend fun send(request: AiRequest): AiResponse {
        val url = "$baseUrl/v1beta/models/${request.model}:generateContent?key=${request.apiKey}"

        val parts = JSONArray()

        val instr = request.instruction.trim()
        if (instr.isNotBlank()) {
            parts.put(JSONObject().put("text", "INSTRUÇÃO FIXA:\n$instr\n"))
        }

        val chatText = buildString {
            request.messages.forEach { m ->
                append(m.role.uppercase())
                append(": ")
                append(m.content)
                append("\n\n")
            }
        }.trim()

        if (chatText.isNotBlank()) parts.put(JSONObject().put("text", chatText))

        request.attachments.forEach { att ->
            if (att.mimeType.startsWith("image/")) {
                val b64 = Base64.getEncoder().encodeToString(att.bytes)
                parts.put(
                    JSONObject().put("inline_data", JSONObject()
                        .put("mime_type", att.mimeType)
                        .put("data", b64)
                    )
                )
            } else {
                parts.put(JSONObject().put("text", "[ANEXO] ${att.name} (${att.mimeType}, ${att.bytes.size} bytes)"))
                if (att.bytes.size <= 60_000 && att.mimeType.startsWith("text/")) {
                    val t = runCatching { att.bytes.toString(Charsets.UTF_8) }.getOrDefault("")
                    if (t.isNotBlank()) {
                        parts.put(JSONObject().put("text", "Conteúdo do arquivo ${att.name}:
```
${t.take(50_000)}
```"))
                    }
                }
            }
        }

        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))

        val req = Request.Builder()
            .url(url)
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
                throw IOException("Gemini HTTP ${resp.code}: $msg")
            }

            // candidates[0].content.parts[0].text
            val text = runCatching {
                val root = JSONObject(raw)
                val candidates = root.optJSONArray("candidates") ?: JSONArray()
                val c0 = candidates.optJSONObject(0) ?: return@runCatching ""
                val content = c0.optJSONObject("content") ?: return@runCatching ""
                val partsOut = content.optJSONArray("parts") ?: JSONArray()
                for (i in 0 until partsOut.length()) {
                    val p = partsOut.optJSONObject(i) ?: continue
                    val t = p.optString("text")
                    if (t.isNotBlank()) return@runCatching t
                }
                ""
            }.getOrDefault("")

            return AiResponse(text = text.ifBlank { "(sem texto retornado)" })
        }
    }
}
