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
}
