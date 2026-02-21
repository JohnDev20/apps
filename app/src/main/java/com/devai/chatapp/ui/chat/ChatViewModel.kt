package com.devai.chatapp.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devai.chatapp.api.*
import com.devai.chatapp.data.local.AppDatabase
import com.devai.chatapp.data.preferences.UserPrefs
import com.devai.chatapp.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val repo = ChatRepository(db.messageDao())
    private val prefs = UserPrefs(app)
    private val aiClient = RouterAiClient()

    val messages = repo.observeMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Current selected provider (UI can override)
    private val prefSnapshot = combine(
        prefs.openAiKey, prefs.openAiModel,
        prefs.anthropicKey, prefs.anthropicModel,
        prefs.geminiKey, prefs.geminiModel,
        prefs.defaultAi, prefs.instruction
    ) { ok, om, ak, am, gk, gm, def, instr ->
        Snapshot(ok, om, ak, am, gk, gm, def, instr)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Snapshot())

    private data class Snapshot(
        val openKey: String = "", val openModel: String = "gpt-5.2",
        val antKey: String = "", val antModel: String = "claude-opus-4-6",
        val gemKey: String = "", val gemModel: String = "gemini-2.0-flash",
        val defaultAi: String = "OPENAI",
        val instruction: String = ""
    )

    suspend fun readAttachment(uri: Uri): Attachment? = withContext(Dispatchers.IO) {
        val cr = getApplication<Application>().contentResolver
        val name = runCatching {
            val c = cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            c?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull() ?: "arquivo"

        val mime = cr.getType(uri) ?: "application/octet-stream"
        val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
        Attachment(name = name, mimeType = mime, bytes = bytes)
    }

    fun saveInstruction(text: String) {
        viewModelScope.launch { prefs.saveInstruction(text) }
    }

    fun send(providerOverride: AiProvider?, instructionOverride: String?, userText: String, attachments: List<Attachment>) {
        val text = userText.trim()
        if (text.isBlank() && attachments.isEmpty()) return

        viewModelScope.launch {
            // Persist user message (text + attachments names)
            val attLabel = if (attachments.isNotEmpty()) {
                "\n\nAnexos: " + attachments.joinToString { it.name }
            } else ""
            repo.add("user", text + attLabel)

            val s = prefSnapshot.value

            val provider = providerOverride ?: when (s.defaultAi) {
                "ANTHROPIC" -> AiProvider.ANTHROPIC
                "GEMINI" -> AiProvider.GEMINI
                else -> AiProvider.OPENAI
            }

            val apiKey = when (provider) {
                AiProvider.OPENAI -> s.openKey
                AiProvider.ANTHROPIC -> s.antKey
                AiProvider.GEMINI -> s.gemKey
            }.trim()

            val model = when (provider) {
                AiProvider.OPENAI -> s.openModel
                AiProvider.ANTHROPIC -> s.antModel
                AiProvider.GEMINI -> s.gemModel
            }.trim()

            val instruction = (instructionOverride ?: s.instruction).trim()

            if (apiKey.isBlank()) {
                repo.add("assistant", "A API key da IA selecionada está vazia. Abra Configurações e cole sua key.")
                return@launch
            }

            val history = messages.value.map { ChatMessage(it.role, it.content) }

            try {
                val resp = aiClient.send(
                    AiRequest(
                        provider = provider,
                        apiKey = apiKey,
                        model = model,
                        instruction = instruction,
                        messages = history + ChatMessage("user", text),
                        attachments = attachments
                    )
                )
                repo.add("assistant", resp.text)
            } catch (e: Exception) {
                repo.add("assistant", "Erro ao chamar a IA: ${'$'}{e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun clear() {
        viewModelScope.launch { repo.clear() }
    }
}
