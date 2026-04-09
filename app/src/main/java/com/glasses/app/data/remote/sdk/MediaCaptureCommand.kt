package com.glasses.app.data.remote.sdk

/**
 * 媒体采集控制指令
 * 参考官方demo的控制指令格式
 * 
 * 指令格式: [0x02, 0x01, action, ...]
 * - 0x02: 控制指令类型
 * - 0x01: 媒体采集子类型
 * - action: 具体操作码
 */
object MediaCaptureCommand {
    
    // 操作码定义
    const val ACTION_PHOTO = 0x01              // 拍照
    const val ACTION_VIDEO_START = 0x02        // 开始录像
    const val ACTION_VIDEO_STOP = 0x03         // 停止录像
    const val ACTION_AUDIO_START = 0x08        // 开始录音
    const val ACTION_AUDIO_STOP = 0x0C         // 停止录音
    const val ACTION_AI_RECOGNITION = 0x06     // 智能识图
    const val ACTION_STOP_VOICE = 0x0B         // 停止语音
    const val ACTION_MEDIA_COUNT = 0x04        // 查询媒体数量
    
    /**
     * 生成拍照指令
     */
    fun takePhoto(): ByteArray {
        return byteArrayOf(0x02, 0x01, ACTION_PHOTO.toByte())
    }
    
    /**
     * 生成开始录像指令
     */
    fun startVideo(): ByteArray {
        return byteArrayOf(0x02, 0x01, ACTION_VIDEO_START.toByte())
    }
    
    /**
     * 生成停止录像指令
     */
    fun stopVideo(): ByteArray {
        return byteArrayOf(0x02, 0x01, ACTION_VIDEO_STOP.toByte())
    }
    
    /**
     * 生成开始录音指令
     */
    fun startAudio(): ByteArray {
        return byteArrayOf(0x02, 0x01, ACTION_AUDIO_START.toByte())
    }
    
    /**
     * 生成停止录音指令
     */
    fun stopAudio(): ByteArray {
        return byteArrayOf(0x02, 0x01, ACTION_AUDIO_STOP.toByte())
    }
    
    /**
     * 生成智能识图指令
     * @param thumbnailSize 缩略图大小 (0..6)
     */
    fun startAIRecognition(thumbnailSize: Int = 2): ByteArray {
        val size = thumbnailSize.coerceIn(0, 6).toByte()
        return byteArrayOf(
            0x02, 0x01, ACTION_AI_RECOGNITION.toByte(),
            size, size, 0x02
        )
    }
    
    /**
     * 生成停止语音指令
     */
    fun stopVoice(): ByteArray {
        return byteArrayOf(0x02, 0x01, ACTION_STOP_VOICE.toByte())
    }
    
    /**
     * 生成查询媒体数量指令
     */
    fun queryMediaCount(): ByteArray {
        return byteArrayOf(0x02, 0x04)
    }
}
