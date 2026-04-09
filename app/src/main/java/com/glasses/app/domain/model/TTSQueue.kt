package com.glasses.app.domain.model

import java.io.File

/**
 * TTS队列项
 */
data class TTSQueueItem(
    val id: String,
    val text: String,
    val audioFile: File? = null,
    val status: TTSStatus = TTSStatus.PENDING
)

/**
 * TTS状态
 */
enum class TTSStatus {
    PENDING,      // 待合成
    SYNTHESIZING, // 合成中
    READY,        // 已就绪
    PLAYING,      // 播放中
    COMPLETED,    // 已完成
    ERROR         // 错误
}

/**
 * TTS队列管理器
 */
class TTSQueue {
    private val queue = mutableListOf<TTSQueueItem>()
    
    /**
     * 入队
     */
    fun enqueue(item: TTSQueueItem) {
        queue.add(item)
    }
    
    /**
     * 出队
     */
    fun dequeue(): TTSQueueItem? {
        return if (queue.isNotEmpty()) queue.removeAt(0) else null
    }
    
    /**
     * 获取队列大小
     */
    fun size(): Int = queue.size
    
    /**
     * 清空队列
     */
    fun clear() {
        queue.clear()
    }
    
    /**
     * 获取队列中的所有项
     */
    fun getAll(): List<TTSQueueItem> = queue.toList()
    
    /**
     * 更新项的状态
     */
    fun updateStatus(itemId: String, status: TTSStatus) {
        val index = queue.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            val item = queue[index]
            queue[index] = item.copy(status = status)
        }
    }
    
    /**
     * 更新项的音频文件
     */
    fun updateAudioFile(itemId: String, audioFile: File) {
        val index = queue.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            val item = queue[index]
            queue[index] = item.copy(audioFile = audioFile, status = TTSStatus.READY)
        }
    }
}
