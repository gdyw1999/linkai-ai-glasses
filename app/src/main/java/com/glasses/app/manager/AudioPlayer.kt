package com.glasses.app.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 音频播放器
 * 用于播放TTS生成的音频文件
 */
class AudioPlayer(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioPlayer"
        private var instance: AudioPlayer? = null
        
        fun getInstance(context: Context): AudioPlayer {
            return instance ?: AudioPlayer(context).also { instance = it }
        }
    }
    
    private var mediaPlayer: MediaPlayer? = null
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentFile = MutableStateFlow<File?>(null)
    val currentFile: StateFlow<File?> = _currentFile.asStateFlow()
    
    /**
     * 播放音频文件
     */
    fun play(audioFile: File, onCompletion: (() -> Unit)? = null) {
        try {
            // 停止当前播放
            stop()
            
            // 创建新的MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                
                setDataSource(audioFile.absolutePath)
                
                setOnCompletionListener {
                    Log.d(TAG, "Audio playback completed: ${audioFile.name}")
                    _isPlaying.value = false
                    _currentFile.value = null
                    onCompletion?.invoke()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    false
                }
                
                prepare()
                start()
                _isPlaying.value = true
                _currentFile.value = audioFile
                
                Log.d(TAG, "Started playing: ${audioFile.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
            _isPlaying.value = false
        }
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            _isPlaying.value = false
            _currentFile.value = null
            Log.d(TAG, "Audio playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio", e)
        }
    }
    
    /**
     * 暂停播放
     */
    fun pause() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    pause()
                    _isPlaying.value = false
                    Log.d(TAG, "Audio playback paused")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause audio", e)
        }
    }
    
    /**
     * 恢复播放
     */
    fun resume() {
        try {
            mediaPlayer?.apply {
                if (!isPlaying) {
                    start()
                    _isPlaying.value = true
                    Log.d(TAG, "Audio playback resumed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume audio", e)
        }
    }
    
    /**
     * 获取当前播放位置（毫秒）
     */
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }
    
    /**
     * 获取音频总时长（毫秒）
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stop()
    }
}
