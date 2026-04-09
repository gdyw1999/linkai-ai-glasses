package com.glasses.app.data.remote.sdk

/**
 * 媒体采集状态枚举
 */
enum class MediaCaptureState {
    IDLE,              // 空闲
    RECORDING_VIDEO,   // 录像中
    RECORDING_AUDIO,   // 录音中
    AI_RECOGNITION,    // 智能识图中
}

/**
 * 媒体采集状态数据类
 */
data class MediaCaptureStatus(
    val state: MediaCaptureState = MediaCaptureState.IDLE,
    val recordingDuration: Long = 0L,  // 录制时长（毫秒）
    val mediaCount: Int = 0,           // 媒体总数
    val imageCount: Int = 0,           // 图片数
    val videoCount: Int = 0,           // 视频数
    val audioCount: Int = 0,           // 录音数
)
