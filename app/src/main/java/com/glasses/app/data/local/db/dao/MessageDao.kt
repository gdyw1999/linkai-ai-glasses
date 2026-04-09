package com.glasses.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.glasses.app.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 消息DAO
 */
@Dao
interface MessageDao {
    
    /**
     * 插入消息
     */
    @Insert
    suspend fun insertMessage(message: MessageEntity): Long
    
    /**
     * 插入多条消息
     */
    @Insert
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    /**
     * 更新消息
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    /**
     * 删除消息
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    /**
     * 根据ID查询消息
     */
    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): MessageEntity?
    
    /**
     * 查询会话中的所有消息
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversationId(conversationId: Long): Flow<List<MessageEntity>>
    
    /**
     * 查询会话中的所有消息（一次性）
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesByConversationIdOnce(conversationId: Long): List<MessageEntity>
    
    /**
     * 删除会话中的所有消息
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversationId(conversationId: Long)
    
    /**
     * 删除所有消息
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
    
    /**
     * 获取会话中的消息数量
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: Long): Int
}
