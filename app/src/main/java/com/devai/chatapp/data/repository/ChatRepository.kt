package com.devai.chatapp.data.repository

import com.devai.chatapp.data.local.MessageDao
import com.devai.chatapp.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: MessageDao) {
    fun observeMessages(): Flow<List<MessageEntity>> = dao.observeAll()

    suspend fun add(role: String, text: String) {
        dao.insert(MessageEntity(role = role, content = text))
    }

    suspend fun clear() = dao.clearAll()
}
