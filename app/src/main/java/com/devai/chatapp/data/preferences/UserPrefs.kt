package com.devai.chatapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "devai_prefs")

class UserPrefs(private val context: Context) {

    private object Keys {
        val OPENAI_KEY = stringPreferencesKey("openai_key")
        val ANTHROPIC_KEY = stringPreferencesKey("anthropic_key")
        val GEMINI_KEY = stringPreferencesKey("gemini_key")

        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val ANTHROPIC_MODEL = stringPreferencesKey("anthropic_model")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")

        val DEFAULT_AI = stringPreferencesKey("default_ai") // "OPENAI" | "ANTHROPIC" | "GEMINI"
        val INSTRUCTION = stringPreferencesKey("instruction")
    }

    val openAiKey: Flow<String> = context.dataStore.data.map { it[Keys.OPENAI_KEY] ?: "" }
    val anthropicKey: Flow<String> = context.dataStore.data.map { it[Keys.ANTHROPIC_KEY] ?: "" }
    val geminiKey: Flow<String> = context.dataStore.data.map { it[Keys.GEMINI_KEY] ?: "" }

    val openAiModel: Flow<String> = context.dataStore.data.map { it[Keys.OPENAI_MODEL] ?: "gpt-5.2" }
    val anthropicModel: Flow<String> = context.dataStore.data.map { it[Keys.ANTHROPIC_MODEL] ?: "claude-opus-4-6" }
    val geminiModel: Flow<String> = context.dataStore.data.map { it[Keys.GEMINI_MODEL] ?: "gemini-2.0-flash" }

    val defaultAi: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_AI] ?: "OPENAI" }
    val instruction: Flow<String> = context.dataStore.data.map { it[Keys.INSTRUCTION] ?: "" }

    suspend fun saveAll(
        openAiKey: String,
        openAiModel: String,
        anthropicKey: String,
        anthropicModel: String,
        geminiKey: String,
        geminiModel: String,
        defaultAi: String
    ) {
        context.dataStore.edit { prefs: Preferences ->
            prefs[Keys.OPENAI_KEY] = openAiKey.trim()
            prefs[Keys.OPENAI_MODEL] = openAiModel.trim().ifBlank { "gpt-5.2" }

            prefs[Keys.ANTHROPIC_KEY] = anthropicKey.trim()
            prefs[Keys.ANTHROPIC_MODEL] = anthropicModel.trim().ifBlank { "claude-opus-4-6" }

            prefs[Keys.GEMINI_KEY] = geminiKey.trim()
            prefs[Keys.GEMINI_MODEL] = geminiModel.trim().ifBlank { "gemini-2.0-flash" }

            prefs[Keys.DEFAULT_AI] = defaultAi
        }
    }

    suspend fun saveInstruction(text: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INSTRUCTION] = text
        }
    }
}
