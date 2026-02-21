package com.devai.chatapp.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.devai.chatapp.data.local.MessageEntity
import com.devai.chatapp.databinding.ItemMessageAssistantBinding
import com.devai.chatapp.databinding.ItemMessageUserBinding

class MessagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<MessageEntity>()

    fun submit(newItems: List<MessageEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].role == "user") TYPE_USER else TYPE_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserVH(ItemMessageUserBinding.inflate(inflater, parent, false))
        } else {
            AssistantVH(ItemMessageAssistantBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is UserVH -> holder.bind(item)
            is AssistantVH -> holder.bind(item)
        }
    }

    class UserVH(private val b: ItemMessageUserBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MessageEntity) { b.txt.text = item.content }
    }
    class AssistantVH(private val b: ItemMessageAssistantBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MessageEntity) { b.txt.text = item.content }
    }

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_ASSISTANT = 2
    }
}
