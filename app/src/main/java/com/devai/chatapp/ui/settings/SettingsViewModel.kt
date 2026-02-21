package com.devai.chatapp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devai.chatapp.data.preferences.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = UserPrefs(app)

    val state: StateFlow<SettingsState> = combine(
        prefs.openAiKey, prefs.openAiModel,
        prefs.anthropicKey, prefs.anthropicModel,
        prefs.geminiKey, prefs.geminiModel,
        prefs.defaultAi
    ) { ok, om, ak, am, gk, gm, def ->
        SettingsState(ok, om, ak, am, gk, gm, def)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun save(s: SettingsState) {
        viewModelScope.launch {
            prefs.saveAll(
                openAiKey = s.openAiKey,
                openAiModel = s.openAiModel,
                anthropicKey = s.anthropicKey,
                anthropicModel = s.anthropicModel,
                geminiKey = s.geminiKey,
                geminiModel = s.geminiModel,
                defaultAi = s.defaultAi.ifBlank { "OPENAI" }
            )
        }
    }
}

data class SettingsState(
    val openAiKey: String = "",
    val openAiModel: String = "gpt-5.2",
    val anthropicKey: String = "",
    val anthropicModel: String = "claude-opus-4-6",
    val geminiKey: String = "",
    val geminiModel: String = "gemini-2.0-flash",
    val defaultAi: String = "OPENAI"
)
