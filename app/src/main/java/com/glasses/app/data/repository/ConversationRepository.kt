package com.glasses.app.data.repository

import android.content.Context
import android.util.Log
import com.glasses.app.data.local.db.AppDatabase
import com.glasses.app.data.local.db.entity.ConversationEntity
import com.glasses.app.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 会话数据仓库
 */
class ConversationRepository(context: Context) {
    
    companion object {
        private const val TAG = "ConversationRepository"
        private var instance: ConversationRepository? = null
        
        fun getInstance(context: Context): ConversationRepository {
            return instance ?: ConversationRepository(context).also { instance = it }
        }
    }
    
    private val database = AppDatabase.getInstance(context)
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    
    /**
     * 创建会话
     */
    suspend fun createConversation(title: String): Long {
        return try {
            val conversation = ConversationEntity(
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = conversationDao.insertConversation(conversation)
            Log.d(TAG, "Created conversation: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create conversation", e)
            throw e
        }
    }
    
    /**
     * 获取会话
     */
    suspend fun getConversation(id: Long): ConversationEntity? {
        return try {
            conversationDao.getConversationById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get conversation", e)
            null
        }
    }
    
    /**
     * 获取所有会话
     */
    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }
    
    /**
     * 搜索会话（标题或消息内容包含关键词）
     */
    fun searchConversations(query: String): Flow<List<ConversationEntity>> {
        return conversationDao.searchConversations(query)
    }

    /**
     * 删除会话
     */
    suspend fun deleteConversation(id: Long) {
        try {
            val conversation = conversationDao.getConversationById(id)
            if (conversation != null) {
                conversationDao.deleteConversation(conversation)
                Log.d(TAG, "Deleted conversation: $id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete conversation", e)
        }
    }
    
    /**
     * 更新会话时间
     */
    suspend fun updateConversationTime(id: Long) {
        try {
            val conversation = conversationDao.getConversationById(id)
            if (conversation != null) {
                conversationDao.updateConversation(
                    conversation.copy(updatedAt = System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update conversation time", e)
        }
    }

    /**
     * 更新会话标题
     */
    suspend fun updateConversationTitle(id: Long, title: String) {
        try {
            val conversation = conversationDao.getConversationById(id)
            if (conversation != null) {
                conversationDao.updateConversation(
                    conversation.copy(
                        title = title,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                Log.d(TAG, "Updated conversation title: $id -> $title")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update conversation title", e)
        }
    }
    
    /**
     * 添加消息
     */
    suspend fun addMessage(
        conversationId: Long,
        content: String,
        role: String,
        audioUrl: String? = null
    ): Long {
        return try {
            val message = MessageEntity(
                conversationId = conversationId,
                content = content,
                role = role,
                audioUrl = audioUrl,
                createdAt = System.currentTimeMillis()
            )
            val messageId = messageDao.insertMessage(message)
            
            // 更新会话的消息数量和更新时间
            val conversation = conversationDao.getConversationById(conversationId)
            if (conversation != null) {
                val messageCount = messageDao.getMessageCount(conversationId)
                conversationDao.updateConversation(
                    conversation.copy(
                        messageCount = messageCount,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            
            Log.d(TAG, "Added message: $messageId")
            messageId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add message", e)
            throw e
        }
    }
    
    /**
     * 获取会话中的消息
     */
    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByConversationId(conversationId)
    }
    
    /**
     * 获取会话中的消息（一次性）
     */
    suspend fun getMessagesOnce(conversationId: Long): List<MessageEntity> {
        return try {
            messageDao.getMessagesByConversationIdOnce(conversationId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get messages", e)
            emptyList()
        }
    }
    
    /**
     * 删除消息
     */
    suspend fun deleteMessage(messageId: Long) {
        try {
            val message = messageDao.getMessageById(messageId)
            if (message != null) {
                messageDao.deleteMessage(message)
                Log.d(TAG, "Deleted message: $messageId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message", e)
        }
    }
    
    /**
     * 清空会话中的所有消息
     */
    suspend fun clearConversationMessages(conversationId: Long) {
        try {
            messageDao.deleteMessagesByConversationId(conversationId)

            // 更新会话的消息数量
            val conversation = conversationDao.getConversationById(conversationId)
            if (conversation != null) {
                conversationDao.updateConversation(
                    conversation.copy(messageCount = 0)
                )
            }

            Log.d(TAG, "Cleared messages for conversation: $conversationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear conversation messages", e)
        }
    }

    // ==================== 导出/导入 ====================

    /**
     * 导出所有会话数据（包含消息）用于备份
     * @return 包含所有会话和消息的列表
     */
    suspend fun exportAllData(): List<Map<String, Any>> {
        return try {
            val conversations = mutableListOf<Map<String, Any>>()
            // 使用一次性查询获取所有会话
            val convList = database.conversationDao().getAllConversationsOnce()
            for (conv in convList) {
                val messages = database.messageDao().getMessagesByConversationIdOnce(conv.id)
                conversations.add(
                    mapOf(
                        "conversation" to mapOf(
                            "id" to conv.id,
                            "title" to conv.title,
                            "createdAt" to conv.createdAt,
                            "updatedAt" to conv.updatedAt,
                            "messageCount" to conv.messageCount
                        ),
                        "messages" to messages.map { msg ->
                            mapOf(
                                "id" to msg.id,
                                "content" to msg.content,
                                "role" to msg.role,
                                "audioUrl" to (msg.audioUrl ?: ""),
                                "createdAt" to msg.createdAt
                            )
                        }
                    )
                )
            }
            conversations
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export data", e)
            emptyList()
        }
    }

    /**
     * 从备份数据导入会话
     * @param data 导出的会话数据列表
     * @param clearFirst 是否先清空现有数据
     */
    suspend fun importFromData(data: List<Map<String, Any>>, clearFirst: Boolean = false) {
        try {
            if (clearFirst) {
                // 先删所有会话（级联删除消息）
                val convList = database.conversationDao().getAllConversationsOnce()
                for (conv in convList) {
                    database.conversationDao().deleteConversation(conv)
                }
            }

            for (item in data) {
                @Suppress("UNCHECKED_CAST")
                val convMap = item["conversation"] as? Map<String, Any> ?: continue
                @Suppress("UNCHECKED_CAST")
                val messagesList = item["messages"] as? List<Map<String, Any>> ?: emptyList()

                val title = convMap["title"] as? String ?: "导入会话"
                val createdAt = (convMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val updatedAt = (convMap["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

                // 创建会话
                val convEntity = ConversationEntity(
                    title = title,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    messageCount = messagesList.size
                )
                val convId = database.conversationDao().insertConversation(convEntity)

                // 插入消息
                for (msgMap in messagesList) {
                    val msgContent = msgMap["content"] as? String ?: ""
                    val msgRole = msgMap["role"] as? String ?: "user"
                    val msgAudioUrl = msgMap["audioUrl"] as? String
                    val msgCreatedAt = (msgMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

                    val msgEntity = MessageEntity(
                        conversationId = convId,
                        content = msgContent,
                        role = msgRole,
                        audioUrl = msgAudioUrl?.takeIf { it.isNotEmpty() },
                        createdAt = msgCreatedAt
                    )
                    database.messageDao().insertMessage(msgEntity)
                }
            }
            Log.d(TAG, "Imported ${data.size} conversations")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import data", e)
        }
    }
}
