package com.glasses.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 会话实体
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis(),
    
    val messageCount: Int = 0
)
