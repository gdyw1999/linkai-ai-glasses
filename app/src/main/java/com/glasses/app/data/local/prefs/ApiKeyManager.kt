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
 * - 阿里Qwen视觉API (图片识图)
 * - OpenClaw API (AI自动化代理引擎)
 */
class ApiKeyManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "api_keys"
        private const val KEY_LINKAI_VOICE_API_KEY = "linkai_voice_api_key"
        private const val KEY_LINKAI_CHAT_API_KEY = "linkai_chat_api_key"
        private const val KEY_ALI_QWEN_VISION_API_KEY = "ali_qwen_vision_api_key"
        private const val KEY_ALI_QWEN_VISION_MODEL = "ali_qwen_vision_model"
        private const val KEY_OPENCLAW_API_KEY = "openclaw_api_key"
        private const val KEY_OPENCLAW_APP_ID = "openclaw_app_id"
        private const val KEY_LINKAI_APP_CODE = "linkai_app_code"
        
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

    // ==================== 阿里Qwen识图 ====================

    /**
     * 保存阿里Qwen识图 API Key
     */
    fun saveAliQwenVisionApiKey(apiKey: String) {
        prefs.edit().putString(KEY_ALI_QWEN_VISION_API_KEY, apiKey).apply()
    }

    /**
     * 获取阿里Qwen识图 API Key
     */
    fun getAliQwenVisionApiKey(): String {
        return prefs.getString(KEY_ALI_QWEN_VISION_API_KEY, "") ?: ""
    }

    /**
     * 检查是否已配置阿里Qwen识图 API Key
     */
    fun hasAliQwenVisionApiKey(): Boolean {
        return getAliQwenVisionApiKey().isNotEmpty()
    }

    /**
     * 保存阿里Qwen识图模型
     */
    fun saveAliQwenVisionModel(model: String) {
        prefs.edit().putString(KEY_ALI_QWEN_VISION_MODEL, model).apply()
    }

    /**
     * 获取阿里Qwen识图模型
     */
    fun getAliQwenVisionModel(): String {
        val rawModel = prefs.getString(
            KEY_ALI_QWEN_VISION_MODEL,
            com.glasses.app.data.remote.api.model.AliQwenVisionModels.QWEN_36_PLUS
        ) ?: com.glasses.app.data.remote.api.model.AliQwenVisionModels.QWEN_36_PLUS

        return when (rawModel.lowercase()) {
            "qwen3.6-plus-2026-04-02" -> com.glasses.app.data.remote.api.model.AliQwenVisionModels.QWEN_36_PLUS
            "qwen3.5-flash" -> com.glasses.app.data.remote.api.model.AliQwenVisionModels.QWEN_35_FLASH
            else -> rawModel
        }
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
    
    // ==================== LinkAI App Code ====================

    /**
     * 保存LinkAI App Code
     * 用于指定LinkAI平台的工作流/应用
     */
    fun saveLinkAIAppCode(appCode: String) {
        prefs.edit().putString(KEY_LINKAI_APP_CODE, appCode).apply()
    }

    /**
     * 获取LinkAI App Code
     */
    fun getLinkAIAppCode(): String {
        return prefs.getString(KEY_LINKAI_APP_CODE, "") ?: ""
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
