package com.glasses.app.service.wakeup

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 唤醒管理器
 * 统一管理按键唤醒和语音唤醒
 */
class WakeupManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "WakeupManager"
        
        @Volatile
        private var instance: WakeupManager? = null
        
        fun getInstance(context: Context): WakeupManager {
            return instance ?: synchronized(this) {
                instance ?: WakeupManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val buttonHandler = ButtonWakeupHandler()
    private val voiceHandler = VoiceWakeupHandler()
    
    // 唤醒状态
    private val _wakeupState = MutableStateFlow(WakeupState.IDLE)
    val wakeupState: StateFlow<WakeupState> = _wakeupState.asStateFlow()
    
    // 唤醒回调
    private var wakeupCallback: (() -> Unit)? = null
    
    /**
     * 初始化唤醒管理器
     * 注册按键和语音唤醒回调
     */
    fun initialize(callback: () -> Unit) {
        wakeupCallback = callback
        
        // 统一的唤醒处理
        val unifiedCallback = {
            handleWakeup()
        }
        
        // 注册按键唤醒
        buttonHandler.registerCallback(unifiedCallback)
        
        // 注册语音唤醒
        voiceHandler.registerCallback(unifiedCallback)
        
        Log.d(TAG, "WakeupManager initialized")
    }
    
    /**
     * 处理唤醒事件
     */
    private fun handleWakeup() {
        Log.d(TAG, "Wakeup triggered")
        _wakeupState.value = WakeupState.TRIGGERED
        
        // 调用外部回调（通常是启动录音）
        wakeupCallback?.invoke()
    }
    
    /**
     * 处理按键点击
     * 由GlassesDeviceListener调用
     */
    fun onButtonClick() {
        buttonHandler.handleButtonClick()
    }
    
    /**
     * 处理语音唤醒
     * 由GlassesDeviceListener调用
     */
    fun onVoiceWakeup(keyword: String) {
        voiceHandler.handleVoiceWakeup(keyword)
    }
    
    /**
     * 启用/禁用语音唤醒
     */
    fun setVoiceWakeupEnabled(enabled: Boolean) {
        voiceHandler.setEnabled(enabled)
    }
    
    /**
     * 获取语音唤醒状态
     */
    fun isVoiceWakeupEnabled(): Boolean {
        return voiceHandler.isEnabled()
    }
    
    /**
     * 重置唤醒状态
     */
    fun resetState() {
        _wakeupState.value = WakeupState.IDLE
    }
    
    /**
     * 取消所有监听
     */
    fun unregisterAll() {
        buttonHandler.unregisterCallback()
        voiceHandler.unregisterCallback()
        wakeupCallback = null
        Log.d(TAG, "All wakeup callbacks unregistered")
    }
}

/**
 * 唤醒状态
 */
enum class WakeupState {
    IDLE,       // 空闲
    TRIGGERED   // 已触发
}
