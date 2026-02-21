package com.devai.chatapp.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.devai.chatapp.R
import com.devai.chatapp.api.AiProvider
import com.devai.chatapp.api.Attachment
import com.devai.chatapp.databinding.ActivityChatBinding
import com.devai.chatapp.ui.settings.SettingsActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val vm: ChatViewModel by viewModels()
    private val adapter = MessagesAdapter()

    private val providers = listOf("OPENAI", "ANTHROPIC", "GEMINI")
    private var selectedProvider: AiProvider? = null

    private val attachments = mutableListOf<Attachment>()

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val att = vm.readAttachment(uri)
            if (att != null) {
                attachments.add(att)
                renderAttachments()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_clear -> {
                    attachments.clear()
                    renderAttachments()
                    vm.clear()
                    true
                }
                else -> false
            }
        }

        val dd = ArrayAdapter(this, android.R.layout.simple_list_item_1, providers)
        binding.spnProvider.setAdapter(dd)
        binding.spnProvider.setText("OPENAI", false)
        selectedProvider = AiProvider.OPENAI
        binding.spnProvider.setOnItemClickListener { _, _, position, _ ->
            selectedProvider = when (providers[position]) {
                "ANTHROPIC" -> AiProvider.ANTHROPIC
                "GEMINI" -> AiProvider.GEMINI
                else -> AiProvider.OPENAI
            }
        }

        binding.btnAttach.setOnClickListener {
            pickFile.launch(arrayOf("*/*"))
        }

        binding.recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recycler.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.input.text?.toString().orEmpty()
            val instr = binding.edtInstruction.text?.toString()
            vm.saveInstruction(instr.orEmpty())

            vm.send(
                providerOverride = selectedProvider,
                instructionOverride = instr,
                userText = text,
                attachments = attachments.toList()
            )
            binding.input.setText("")
            attachments.clear()
            renderAttachments()
        }

        lifecycleScope.launch {
            vm.messages.collectLatest { list ->
                adapter.submit(list)
                if (list.isNotEmpty()) binding.recycler.scrollToPosition(list.size - 1)
            }
        }

        renderAttachments()
    }

    private fun renderAttachments() {
        binding.txtAttachments.text = if (attachments.isEmpty()) {
            "(sem anexos)"
        } else {
            "Anexos: " + attachments.joinToString { it.name }
        }
    }
}
