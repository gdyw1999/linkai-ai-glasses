package com.glasses.app.service.wakeup

import android.util.Log

/**
 * 按键唤醒处理器
 * 实现200ms防抖机制，避免重复触发
 */
class ButtonWakeupHandler {
    
    companion object {
        private const val TAG = "ButtonWakeupHandler"
        private const val DEBOUNCE_DELAY_MS = 200L // 防抖延迟200ms
    }
    
    private var lastClickTime = 0L
    private var callback: (() -> Unit)? = null
    
    /**
     * 注册按键唤醒回调
     */
    fun registerCallback(callback: () -> Unit) {
        this.callback = callback
        Log.d(TAG, "Button wakeup callback registered")
    }
    
    /**
     * 处理按键点击事件
     * 实现防抖机制，200ms内的重复点击会被忽略
     */
    fun handleButtonClick() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastClick = currentTime - lastClickTime
        
        if (timeSinceLastClick > DEBOUNCE_DELAY_MS) {
            lastClickTime = currentTime
            Log.d(TAG, "Button click handled, triggering callback")
            callback?.invoke()
        } else {
            Log.d(TAG, "Button click ignored (debounce: ${timeSinceLastClick}ms)")
        }
    }
    
    /**
     * 取消注册回调
     */
    fun unregisterCallback() {
        callback = null
        Log.d(TAG, "Button wakeup callback unregistered")
    }
}
