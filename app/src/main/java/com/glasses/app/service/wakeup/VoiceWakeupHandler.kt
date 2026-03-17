package com.glasses.app.service.wakeup

import android.util.Log

/**
 * 语音唤醒处理器
 * 处理语音唤醒词"嘿塞恩"的识别
 */
class VoiceWakeupHandler {
    
    companion object {
        private const val TAG = "VoiceWakeupHandler"
        private const val WAKEUP_WORD = "嘿塞恩" // 固定唤醒词
    }
    
    private var callback: (() -> Unit)? = null
    private var isEnabled = true
    
    /**
     * 注册语音唤醒回调
     */
    fun registerCallback(callback: () -> Unit) {
        this.callback = callback
        Log.d(TAG, "Voice wakeup callback registered")
    }
    
    /**
     * 处理语音唤醒事件
     * @param keyword 识别到的唤醒词
     */
    fun handleVoiceWakeup(keyword: String) {
        if (!isEnabled) {
            Log.d(TAG, "Voice wakeup is disabled, ignoring event")
            return
        }
        
        if (keyword == WAKEUP_WORD) {
            Log.d(TAG, "Voice wakeup triggered with keyword: $keyword")
            callback?.invoke()
        } else {
            Log.d(TAG, "Unknown wakeup keyword: $keyword")
        }
    }
    
    /**
     * 启用/禁用语音唤醒
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d(TAG, "Voice wakeup ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 获取语音唤醒状态
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * 取消注册回调
     */
    fun unregisterCallback() {
        callback = null
        Log.d(TAG, "Voice wakeup callback unregistered")
    }
}
