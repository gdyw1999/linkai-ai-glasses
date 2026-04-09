package com.glasses.app.manager

import android.content.Context
import android.util.Log
import com.oudmon.ble.base.communication.LargeDataHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 录音管理器
 * 管理Linkai星韵AI眼镜端录音的启动、停止和文件下载
 * 实现30秒最长时间限制和5秒静音超时检测
 */
class RecordingManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "RecordingManager"
        
        @Volatile
        private var instance: RecordingManager? = null
        
        fun getInstance(context: Context): RecordingManager {
            return instance ?: synchronized(this) {
                instance ?: RecordingManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // 录音状态
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    // 录音配置
    private var config = RecordingConfig()
    
    // 录音监控Job
    private var monitorJob: Job? = null
    
    // 录音开始时间
    private var recordingStartTime = 0L
    
    // 静音开始时间
    private var silenceStartTime = 0L
    
    /**
     * 设置录音配置
     */
    fun setRecordingConfig(config: RecordingConfig) {
        this.config = config
        Log.d(TAG, "Recording config updated: $config")
    }
    
    /**
     * 开始录音
     */
    suspend fun startRecording(): Result<Unit> = suspendCoroutine { continuation ->
        try {
            Log.d(TAG, "Starting recording...")
            
            // 发送开始录音指令
            // 0x08 = 开始录音
            LargeDataHandler.getInstance().glassesControl(
                byteArrayOf(0x02, 0x01, 0x08)
            ) { _, response ->
                if (response != null) {
                    Log.d(TAG, "Recording started successfully")
                    _recordingState.value = RecordingState.Recording
                    recordingStartTime = System.currentTimeMillis()
                    
                    // 启动监控
                    startRecordingMonitor()
                    
                    continuation.resume(Result.success(Unit))
                } else {
                    Log.e(TAG, "Failed to start recording")
                    continuation.resume(Result.failure(Exception("Failed to start recording")))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 停止录音
     */
    suspend fun stopRecording(): Result<File> = suspendCoroutine { continuation ->
        try {
            Log.d(TAG, "Stopping recording...")
            
            // 停止监控
            monitorJob?.cancel()
            monitorJob = null
            
            // 发送停止录音指令
            // 0x0c = 停止录音
            LargeDataHandler.getInstance().glassesControl(
                byteArrayOf(0x02, 0x01, 0x0c)
            ) { _, response ->
                if (response != null) {
                    Log.d(TAG, "Recording stopped successfully")
                    _recordingState.value = RecordingState.Processing(0f)
                    
                    // 下载录音文件
                    downloadLatestRecording { result ->
                        if (result.isSuccess) {
                            val file = result.getOrNull()!!
                            _recordingState.value = RecordingState.Completed(file)
                            continuation.resume(Result.success(file))
                        } else {
                            val error = result.exceptionOrNull()!!
                            _recordingState.value = RecordingState.Error(error.message ?: "Download failed")
                            continuation.resume(Result.failure(error))
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to stop recording")
                    continuation.resume(Result.failure(Exception("Failed to stop recording")))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * 启动录音监控
     * 检查最长时间限制和静音超时
     */
    private fun startRecordingMonitor() {
        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(100)
                
                val duration = System.currentTimeMillis() - recordingStartTime
                
                // 检查最长时间限制
                if (duration >= config.maxDuration) {
                    Log.d(TAG, "Max duration reached, stopping recording")
                    stopRecording()
                    break
                }
                
                // 检查静音超时（如果启用）
                if (config.enableSilenceDetection) {
                    if (isSilent()) {
                        if (silenceStartTime == 0L) {
                            silenceStartTime = System.currentTimeMillis()
                        } else if (System.currentTimeMillis() - silenceStartTime >= config.silenceTimeout) {
                            Log.d(TAG, "Silence timeout reached, stopping recording")
                            stopRecording()
                            break
                        }
                    } else {
                        silenceStartTime = 0L
                    }
                }
            }
        }
    }
    
    /**
     * 检测是否静音
     * TODO: 需要SDK提供实时音频电平接口
     */
    private fun isSilent(): Boolean {
        // 暂时返回false，等待SDK提供音频电平接口
        return false
    }
    
    /**
     * 下载最新录音文件
     */
    private fun downloadLatestRecording(callback: (Result<File>) -> Unit) {
        try {
            val tempDir = File(context.cacheDir, "recordings")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            val pcmFile = File(tempDir, "recording_${System.currentTimeMillis()}.pcm")
            val wavFile = File(tempDir, "recording_${System.currentTimeMillis()}.wav")
            
            // TODO: 通过SDK下载最新录音文件到pcmFile
            // 这里需要使用WiFi文件下载功能
            // 参考官方demo的importAlbum()方法
            
            // 临时创建空PCM文件用于测试
            pcmFile.createNewFile()
            
            // 将PCM转换为WAV
            CoroutineScope(Dispatchers.IO).launch {
                val result = com.glasses.app.util.AudioConverter.convertPcmToWav(
                    pcmFile = pcmFile,
                    wavFile = wavFile,
                    sampleRate = 16000,
                    channels = 1,
                    bitDepth = 16
                )
                
                if (result.isSuccess) {
                    // 删除临时PCM文件
                    pcmFile.delete()
                    
                    Log.d(TAG, "Recording file converted and downloaded: ${wavFile.absolutePath}")
                    callback(Result.success(wavFile))
                } else {
                    Log.e(TAG, "Failed to convert PCM to WAV")
                    callback(Result.failure(result.exceptionOrNull()!!))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download recording", e)
            callback(Result.failure(e))
        }
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        monitorJob?.cancel()
        monitorJob = null
        recordingStartTime = 0L
        silenceStartTime = 0L
        _recordingState.value = RecordingState.Idle
    }
}

/**
 * 录音配置
 */
data class RecordingConfig(
    val maxDuration: Long = 30_000L,        // 最长录音时间30秒
    val silenceTimeout: Long = 5_000L,      // 静音超时5秒
    val enableSilenceDetection: Boolean = true  // 启用静音检测
)

/**
 * 录音状态
 */
sealed class RecordingState {
    object Idle : RecordingState()
    object Recording : RecordingState()
    data class Processing(val progress: Float) : RecordingState()
    data class Completed(val audioFile: File) : RecordingState()
    data class Error(val message: String) : RecordingState()
    
    val isRecording: Boolean
        get() = this is Recording
}
