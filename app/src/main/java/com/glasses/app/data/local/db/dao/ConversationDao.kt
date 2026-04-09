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
}
