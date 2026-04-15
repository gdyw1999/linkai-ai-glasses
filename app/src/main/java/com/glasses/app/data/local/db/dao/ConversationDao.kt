package com.glasses.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.glasses.app.data.local.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * 会话DAO
 */
@Dao
interface ConversationDao {
    
    /**
     * 插入会话
     */
    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long
    
    /**
     * 更新会话
     */
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    /**
     * 删除会话
     */
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
    
    /**
     * 根据ID查询会话
     */
    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?
    
    /**
     * 查询所有会话（按更新时间倒序）
     */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    /**
     * 查询所有会话（一次性）
     */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversationsOnce(): List<ConversationEntity>
    
    /**
     * 删除所有会话
     */
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    /**
     * 搜索会话（标题或消息内容包含关键词，按更新时间倒序）
     * 使用 DISTINCT 去重，因为一条消息匹配就会导致会话出现多次
     */
    @Query("""
        SELECT DISTINCT c.* FROM conversations c
        LEFT JOIN messages m ON c.id = m.conversationId
        WHERE c.title LIKE '%' || :query || '%'
           OR m.content LIKE '%' || :query || '%'
        ORDER BY c.updatedAt DESC
    """)
    fun searchConversations(query: String): Flow<List<ConversationEntity>>
}
