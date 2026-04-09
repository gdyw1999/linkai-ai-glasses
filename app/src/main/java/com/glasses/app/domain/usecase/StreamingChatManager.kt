package com.glasses.app.domain.usecase

import android.content.Context
import android.util.Log
import com.glasses.app.data.remote.api.AIServiceImpl
import com.glasses.app.domain.model.TTSQueue
import com.glasses.app.domain.model.TTSQueueItem
import com.glasses.app.domain.model.TTSStatus
import com.glasses.app.manager.AudioPlayer
import com.glasses.app.util.AudioConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*

/**
 * 流式对话管理器
 * 管理流式文本累积、攒句切分、TTS合成和播放
 */
class StreamingChatManager(
    private val context: Context,
    private val aiService: AIServiceImpl
) {
    
    companion object {
        private const val TAG = "StreamingChatManager"
        private const val SENTENCE_END_CHARS = "。！？；，"
        private const val MAX_CHARS_PER_SENTENCE = 20
    }
    
    private val ttsQueue = TTSQueue()
    private val audioPlayer = AudioPlayer.getInstance(context)
    
    private val _accumulatedText = MutableStateFlow("")
    val accumulatedText: StateFlow<String> = _accumulatedText.asStateFlow()
    
    private val _displayText = MutableStateFlow("")
    val displayText: StateFlow<String> = _displayText.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()
    
    private var currentStreamJob: kotlinx.coroutines.Job? = null
    
    /**
     * 处理流式文本
     */
    suspend fun processStreamingText(text: String) {
        try {
            _isProcessing.value = true
            
            // 累积文本
            val accumulated = _accumulatedText.value + text
            _accumulatedText.value = accumulated
            _displayText.value = accumulated
            
            Log.d(TAG, "Accumulated text: $accumulated")
            
            // 检查是否需要切分
            checkAndSplitSentences(accumulated)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process streaming text", e)
        } finally {
            _isProcessing.value = false
        }
    }
    
    /**
     * 检查并切分句子
     */
    private suspend fun checkAndSplitSentences(text: String) {
        var remaining = text
        val sentences = mutableListOf<String>()
        
        while (remaining.isNotEmpty()) {
            // 查找句子结束符
            var endIndex = -1
            for (i in remaining.indices) {
                if (remaining[i] in SENTENCE_END_CHARS) {
                    endIndex = i
                    break
                }
            }
            
            // 如果没有找到结束符，检查是否达到最大字符数
            if (endIndex == -1) {
                if (remaining.length >= MAX_CHARS_PER_SENTENCE) {
                    endIndex = MAX_CHARS_PER_SENTENCE - 1
                } else {
                    // 还没有完整的句子，等待更多文本
                    break
                }
            }
            
            // 提取句子
            val sentence = remaining.substring(0, endIndex + 1)
            sentences.add(sentence)
            remaining = remaining.substring(endIndex + 1)
            
            Log.d(TAG, "Split sentence: $sentence")
        }
        
        // 更新剩余文本
        _accumulatedText.value = remaining
        
        // 为每个句子生成TTS
        for (sentence in sentences) {
            synthesizeAndQueue(sentence)
        }
    }
    
    /**
     * 合成并入队
     */
    private suspend fun synthesizeAndQueue(sentence: String) {
        try {
            val itemId = UUID.randomUUID().toString()
            val queueItem = TTSQueueItem(
                id = itemId,
                text = sentence,
                status = TTSStatus.SYNTHESIZING
            )
            
            ttsQueue.enqueue(queueItem)
            _queueSize.value = ttsQueue.size()
            
            Log.d(TAG, "Queued TTS item: $sentence")
            
            // 调用TTS API
            val ttsResult = aiService.synthesizeSpeech(sentence)
            
            if (ttsResult.isFailure) {
                Log.e(TAG, "TTS synthesis failed: ${ttsResult.exceptionOrNull()?.message}")
                return
            }
            
            // 获取返回的音频文件
            val audioFile = ttsResult.getOrNull()
            if (audioFile == null) {
                Log.e(TAG, "TTS audio file is null")
                return
            }
            
            // 更新队列项
            ttsQueue.updateAudioFile(itemId, audioFile)
            
            Log.d(TAG, "TTS synthesis completed: ${audioFile.name}")
            
            // 如果当前没有播放，开始播放
            if (!audioPlayer.isPlaying.value) {
                playNextInQueue()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to synthesize and queue TTS", e)
        }
    }
    
    /**
     * 播放队列中的下一个
     */
    private fun playNextInQueue() {
        val nextItem = ttsQueue.dequeue()
        if (nextItem != null && nextItem.audioFile != null) {
            _queueSize.value = ttsQueue.size()
            
            audioPlayer.play(nextItem.audioFile) {
                // 播放完成后，删除临时文件
                nextItem.audioFile.delete()
                Log.d(TAG, "Deleted temp audio file: ${nextItem.audioFile.name}")
                
                // 播放下一个
                playNextInQueue()
            }
        }
    }
    
    /**
     * 打断对话
     */
    fun interrupt() {
        try {
            Log.d(TAG, "Interrupting streaming chat")
            
            // 停止播放
            audioPlayer.stop()
            
            // 清空队列
            ttsQueue.clear()
            _queueSize.value = 0
            
            // 清空累积文本
            _accumulatedText.value = ""
            _displayText.value = ""
            
            // 取消当前流任务
            currentStreamJob?.cancel()
            
            Log.d(TAG, "Streaming chat interrupted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to interrupt streaming chat", e)
        }
    }
    
    /**
     * 完成对话
     */
    fun finalize() {
        try {
            // 处理剩余的累积文本
            val remaining = _accumulatedText.value
            if (remaining.isNotEmpty()) {
                // 同步处理剩余文本
                Log.d(TAG, "Finalizing with remaining text: $remaining")
            }
            
            // 等待队列播放完成
            Log.d(TAG, "Streaming chat finalized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize streaming chat", e)
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        interrupt()
        audioPlayer.release()
    }
}
