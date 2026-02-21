package com.devai.chatapp.ui.settings

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.devai.chatapp.databinding.ActivitySettingsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val vm: SettingsViewModel by viewModels()

    private val providers = listOf("OPENAI", "ANTHROPIC", "GEMINI")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, providers)
        binding.spnDefaultAi.setAdapter(adapter)

        lifecycleScope.launch {
            vm.state.collectLatest { s ->
                if (binding.edtOpenAi.text?.toString() != s.openAiKey) binding.edtOpenAi.setText(s.openAiKey)
                if (binding.edtOpenAiModel.text?.toString() != s.openAiModel) binding.edtOpenAiModel.setText(s.openAiModel)

                if (binding.edtAnthropic.text?.toString() != s.anthropicKey) binding.edtAnthropic.setText(s.anthropicKey)
                if (binding.edtAnthropicModel.text?.toString() != s.anthropicModel) binding.edtAnthropicModel.setText(s.anthropicModel)

                if (binding.edtGemini.text?.toString() != s.geminiKey) binding.edtGemini.setText(s.geminiKey)
                if (binding.edtGeminiModel.text?.toString() != s.geminiModel) binding.edtGeminiModel.setText(s.geminiModel)

                if (binding.spnDefaultAi.text?.toString() != s.defaultAi) binding.spnDefaultAi.setText(s.defaultAi, false)
            }
        }

        binding.btnSave.setOnClickListener {
            vm.save(
                SettingsState(
                    openAiKey = binding.edtOpenAi.text?.toString().orEmpty(),
                    openAiModel = binding.edtOpenAiModel.text?.toString().orEmpty(),
                    anthropicKey = binding.edtAnthropic.text?.toString().orEmpty(),
                    anthropicModel = binding.edtAnthropicModel.text?.toString().orEmpty(),
                    geminiKey = binding.edtGemini.text?.toString().orEmpty(),
                    geminiModel = binding.edtGeminiModel.text?.toString().orEmpty(),
                    defaultAi = binding.spnDefaultAi.text?.toString().ifBlank { "OPENAI" }
                )
            )
            finish()
        }
    }
}
