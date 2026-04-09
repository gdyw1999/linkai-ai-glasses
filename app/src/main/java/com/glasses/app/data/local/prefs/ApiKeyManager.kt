package com.glasses.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * API Key管理器
 * 使用SharedPreferences存储API Keys
 * 
 * 支持的API:
 * - LinkAI语音API (ASR + TTS)
 * - LinkAI对话API (LLM)
 * - OpenClaw API (AI自动化代理引擎)
 */
class ApiKeyManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "api_keys"
        private const val KEY_LINKAI_VOICE_API_KEY = "linkai_voice_api_key"
        private const val KEY_LINKAI_CHAT_API_KEY = "linkai_chat_api_key"
        private const val KEY_OPENCLAW_API_KEY = "openclaw_api_key"
        private const val KEY_OPENCLAW_APP_ID = "openclaw_app_id"
        
        @Volatile
        private var instance: ApiKeyManager? = null
        
        fun getInstance(context: Context): ApiKeyManager {
            return instance ?: synchronized(this) {
                instance ?: ApiKeyManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // ==================== LinkAI语音API ====================
    
    /**
     * 保存LinkAI语音API Key
     * 用于ASR（语音识别）和TTS（语音合成）
     */
    fun saveLinkAIVoiceApiKey(apiKey: String) {
        prefs.edit().putString(KEY_LINKAI_VOICE_API_KEY, apiKey).apply()
    }
    
    /**
     * 获取LinkAI语音API Key
     */
    fun getLinkAIVoiceApiKey(): String {
        return prefs.getString(KEY_LINKAI_VOICE_API_KEY, "") ?: ""
    }
    
    /**
     * 检查是否已配置LinkAI语音API Key
     */
    fun hasLinkAIVoiceApiKey(): Boolean {
        return getLinkAIVoiceApiKey().isNotEmpty()
    }
    
    // ==================== LinkAI对话API ====================
    
    /**
     * 保存LinkAI对话API Key
     * 用于LLM（大语言模型对话）
     */
    fun saveLinkAIChatApiKey(apiKey: String) {
        prefs.edit().putString(KEY_LINKAI_CHAT_API_KEY, apiKey).apply()
    }
    
    /**
     * 获取LinkAI对话API Key
     */
    fun getLinkAIChatApiKey(): String {
        return prefs.getString(KEY_LINKAI_CHAT_API_KEY, "") ?: ""
    }
    
    /**
     * 检查是否已配置LinkAI对话API Key
     */
    fun hasLinkAIChatApiKey(): Boolean {
        return getLinkAIChatApiKey().isNotEmpty()
    }
    
    // ==================== OpenClaw API ====================
    
    /**
     * 保存OpenClaw API Key
     * OpenClaw是开源AI自动化代理引擎
     */
    fun saveOpenClawApiKey(apiKey: String) {
        prefs.edit().putString(KEY_OPENCLAW_API_KEY, apiKey).apply()
    }
    
    /**
     * 获取OpenClaw API Key
     */
    fun getOpenClawApiKey(): String {
        return prefs.getString(KEY_OPENCLAW_API_KEY, "") ?: ""
    }
    
    /**
     * 保存OpenClaw应用ID
     */
    fun saveOpenClawAppId(appId: String) {
        prefs.edit().putString(KEY_OPENCLAW_APP_ID, appId).apply()
    }
    
    /**
     * 获取OpenClaw应用ID
     */
    fun getOpenClawAppId(): String {
        return prefs.getString(KEY_OPENCLAW_APP_ID, "") ?: ""
    }
    
    /**
     * 检查是否已配置OpenClaw API Key
     */
    fun hasOpenClawApiKey(): Boolean {
        return getOpenClawApiKey().isNotEmpty()
    }
    
    /**
     * 检查是否已配置OpenClaw应用ID
     */
    fun hasOpenClawAppId(): Boolean {
        return getOpenClawAppId().isNotEmpty()
    }
    
    // ==================== 通用方法 ====================
    
    /**
     * 清空所有API Keys
     */
    fun clearAllApiKeys() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 检查是否已配置所有必需的API Keys
     */
    fun hasAllRequiredApiKeys(): Boolean {
        return hasLinkAIVoiceApiKey() && hasLinkAIChatApiKey()
    }
}
