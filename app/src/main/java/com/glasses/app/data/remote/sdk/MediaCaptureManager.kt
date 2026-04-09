package com.glasses.app.data.remote.sdk

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

/**
 * 媒体采集管理器
 * 管理Linkai星韵AI眼镜的拍照、录像、录音、智能识图等媒体采集功能
 * 实现媒体采集状态管理和录制时长计时
 */
class MediaCaptureManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MediaCaptureManager"
        private var instance: MediaCaptureManager? = null
        
        fun getInstance(context: Context): MediaCaptureManager {
            return instance ?: synchronized(this) {
                instance ?: MediaCaptureManager(context).also { instance = it }
            }
        }
    }
    
    private val _status = MutableStateFlow(MediaCaptureStatus())
    val status: StateFlow<MediaCaptureStatus> = _status.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var recordingStartTime = 0L
    private var recordingTimerJob: Job? = null
    
    /**
     * 拍照
     */
    fun takePhoto(onResult: (success: Boolean, message: String) -> Unit) {
        try {
            val command = MediaCaptureCommand.takePhoto()
            LargeDataHandler.getInstance().glassesControl(command) { _, response ->
                if (response.dataType == 1) {
                    Log.d(TAG, "拍照成功")
                    onResult(true, "拍照成功")
                } else {
                    Log.e(TAG, "拍照失败: ${response.dataType}")
                    onResult(false, "拍照失败")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "拍照异常: ${e.message}")
            onResult(false, "拍照异常: ${e.message}")
        }
    }
    
    /**
     * 开始录像
     */
    fun startVideo(onResult: (success: Boolean, message: String) -> Unit) {
        try {
            if (_status.value.state != MediaCaptureState.IDLE) {
                onResult(false, "当前有其他采集操作进行中")
                return
            }
            
            val command = MediaCaptureCommand.startVideo()
            LargeDataHandler.getInstance().glassesControl(command) { _, response ->
                if (response.dataType == 1) {
                    Log.d(TAG, "开始录像成功")
                    _status.value = _status.value.copy(
                        state = MediaCaptureState.RECORDING_VIDEO,
                        recordingDuration = 0L
                    )
                    recordingStartTime = System.currentTimeMillis()
                    startRecordingTimer()
                    onResult(true, "开始录像成功")
                } else {
                    Log.e(TAG, "开始录像失败: ${response.dataType}")
                    onResult(false, "开始录像失败")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "开始录像异常: ${e.message}")
            onResult(false, "开始录像异常: ${e.message}")
        }
    }
    
    /**
     * 停止录像
     */
    fun stopVideo(onResult: (success: Boolean, message: String) -> Unit) {
        try {
            if (_status.value.state != MediaCaptureState.RECORDING_VIDEO) {
                onResult(false, "当前未在录像")
                return
            }
            
            val command = MediaCaptureCommand.stopVideo()
            LargeDataHandler.getInstance().glassesControl(command) { _, response ->
                if (response.dataType == 1) {
                    Log.d(TAG, "停止录像成功")
                    stopRecordingTimer()
                    _status.value = _status.value.copy(
                        state = MediaCaptureState.IDLE,
                        recordingDuration = 0L
                    )
                    onResult(true, "停止录像成功")
                } else {
                    Log.e(TAG, "停止录像失败: ${response.dataType}")
                    onResult(false, "停止录像失败")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止录像异常: ${e.message}")
            onResult(false, "停止录像异常: ${e.message}")
        }
    }
    
    /**
     * 开始录音
     */
    fun startAudio(onResult: (success: Boolean, message: String) -> Unit) {
        try {
            if (_status.value.state != MediaCaptureState.IDLE) {
                onResult(false, "当前有其他采集操作进行中")
                return
            }
            
            val command = MediaCaptureCommand.startAudio()
            LargeDataHandler.getInstance().glassesControl(command) { _, response ->
                if (response.dataType == 1) {
                    Log.d(TAG, "开始录音成功")
                    _status.value = _status.value.copy(
                        state = MediaCaptureState.RECORDING_AUDIO,
                        recordingDuration = 0L
                    )
                    recordingStartTime = System.currentTimeMillis()
                    startRecordingTimer()
                    onResult(true, "开始录音成功")
                } else {
                    Log.e(TAG, "开始录音失败: ${response.dataType}")
                    onResult(false, "开始录音失败")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "开始录音异常: ${e.message}")
            onResult(false, "开始录音异常: ${e.message}")
        }
    }
    
    /**
     * 停止录音
     */
    fun stopAudio(onResult: (success: Boolean, message: String) -> Unit) {
        try {
            if (_status.value.state != MediaCaptureState.RECORDING_AUDIO) {
                onResult(false, "当前未在录音")
                return
            }
            
            val command = MediaCaptureCommand.stopAudio()
            LargeDataHandler.getInstance().glassesControl(command) { _, response ->
                if (response.dataType == 1) {
                    Log.d(TAG, "停止录音成功")
                    stopRecordingTimer()
                    _status.value = _status.value.copy(
                        state = MediaCaptureState.IDLE,
                        recordingDuration = 0L
                    )
                    onResult(true, "停止录音成功")
                } else {
                    Log.e(TAG, "停止录音失败: ${response.dataType}")
                    onResult(false, "停止录音失败")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止录音异常: ${e.message}")
            onResult(false, "停止录音异常: ${e.message}")
        }
    }
    
    /**
     * 开始智能识图
     */
    fun startAIRecognition(thumbnailSize: Int = 2, onResult: (success: Boolean, message: String) -> Unit) {
        try {
            if (_status.value.state != MediaCaptureState.IDLE) {
                onResult(false, "当前有其他采集操作进行中")
                return
            }
            
            val command = MediaCaptureCommand.startAIRecognition(thumbnailSize)
            LargeDataHandler.getInstance().glassesControl(command) { _, response ->
                if (response.dataType == 1) {
                    Log.d(TAG, "开始智能识图成功")
                    _status.value = _status.value.copy(state = MediaCaptureState.AI_RECOGNITION)
                    onResult(true, "开始智能识图成功")
                } else {
                    Log.e(TAG, "开始智能识图失败: ${response.dataType}")
                    onResult(false, "开始智能识图失败")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "开始智能识图异常: ${e.message}")
            onResult(false, "开始智能识图异常: ${e.message}")
        }
    }
    
    /**
     * 停止语音（停止当前所有操作）
     */
    fun stopVoice(onResult: (success: Boolean, message: String) -> Unit) {
        try {
            val command = MediaCaptureCommand.stopVoice()
            LargeDataHandler.getInstance().glassesControl(command) { _, _ ->
                Log.d(TAG, "停止语音成功")
                stopRecordingTimer()
                _status.value = _status.value.copy(
                    state = MediaCaptureState.IDLE,
                    recordingDuration = 0L
                )
                onResult(true, "停止语音成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止语音异常: ${e.message}")
            onResult(false, "停止语音异常: ${e.message}")
        }
    }
    
    /**
     * 查询媒体数量
     */
    fun queryMediaCount(onResult: (success: Boolean, imageCount: Int, videoCount: Int, audioCount: Int) -> Unit) {
        try {
            val command = MediaCaptureCommand.queryMediaCount()
            LargeDataHandler.getInstance().glassesControl(command) { _, response ->
                if (response.dataType == 4) {
                    Log.d(TAG, "查询媒体数量成功: 图片=${response.imageCount}, 视频=${response.videoCount}, 录音=${response.recordCount}")
                    val totalCount = response.imageCount + response.videoCount + response.recordCount
                    _status.value = _status.value.copy(
                        mediaCount = totalCount,
                        imageCount = response.imageCount,
                        videoCount = response.videoCount,
                        audioCount = response.recordCount
                    )
                    onResult(true, response.imageCount, response.videoCount, response.recordCount)
                } else {
                    Log.e(TAG, "查询媒体数量失败: ${response.dataType}")
                    onResult(false, 0, 0, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "查询媒体数量异常: ${e.message}")
            onResult(false, 0, 0, 0)
        }
    }
    
    /**
     * 启动录制计时器
     */
    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = scope.launch {
            while (true) {
                delay(100)  // 每100ms更新一次
                val duration = System.currentTimeMillis() - recordingStartTime
                _status.value = _status.value.copy(recordingDuration = duration)
            }
        }
    }
    
    /**
     * 停止录制计时器
     */
    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }
    
    /**
     * 获取当前状态
     */
    fun getCurrentState(): MediaCaptureState {
        return _status.value.state
    }
    
    /**
     * 获取录制时长（秒）
     */
    fun getRecordingDurationSeconds(): Long {
        return _status.value.recordingDuration / 1000
    }
}
