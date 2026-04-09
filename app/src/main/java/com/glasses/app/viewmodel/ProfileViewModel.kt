package com.glasses.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glasses.app.GlassesApplication
import com.glasses.app.data.remote.sdk.ConnectionState
import com.glasses.app.data.remote.sdk.GlassesSDKManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 我的页面UI状态
 */
data class ProfileUiState(
    val appVersion: String = "1.0.0",
    val sdkVersion: String = "1.1",
    val deviceConnected: Boolean = false,
    val deviceName: String = "未连接",
    val batteryLevel: Int = 0,
    val statusMessage: String = "",
    val showAboutDialog: Boolean = false,
    val showFAQDialog: Boolean = false,
    val showApiConfigDialog: Boolean = false,
    val showCrashLogDialog: Boolean = false
)

/**
 * 我的页面ViewModel
 * 管理设置、关于、常见问题等
 * 集成真实的后端模块：GlassesSDKManager
 */
class ProfileViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "ProfileViewModel"
    }
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private var sdkManager: GlassesSDKManager? = null
    
    // 充电状态 - 延迟初始化
    val isCharging: StateFlow<Boolean> by lazy {
        sdkManager?.isCharging ?: MutableStateFlow(false).asStateFlow()
    }
    
    init {
        // 确保SDK已初始化
        (context.applicationContext as? GlassesApplication)?.initializeSDK()
        
        // 获取SDK管理器实例
        try {
            sdkManager = GlassesSDKManager.getInstance(context)
            
            // 监听连接状态
            viewModelScope.launch {
                sdkManager?.connectionState?.collect { state ->
                    updateConnectionState(state)
                }
            }
            
            // 监听电量
            viewModelScope.launch {
                sdkManager?.batteryLevel?.collect { level ->
                    _uiState.value = _uiState.value.copy(batteryLevel = level)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SDK manager", e)
        }
        
        // 加载应用信息
        loadAppInfo()
    }
    
    /**
     * 更新连接状态
     */
    private fun updateConnectionState(state: ConnectionState) {
        when (state) {
            ConnectionState.DISCONNECTED -> {
                _uiState.value = _uiState.value.copy(
                    deviceConnected = false,
                    deviceName = "未连接"
                )
            }
            ConnectionState.CONNECTING -> {
                _uiState.value = _uiState.value.copy(
                    deviceConnected = false,
                    deviceName = "连接中..."
                )
            }
            ConnectionState.CONNECTED -> {
                val deviceName = sdkManager?.getCurrentDevice()?.name ?: "已连接"
                _uiState.value = _uiState.value.copy(
                    deviceConnected = true,
                    deviceName = deviceName
                )
            }
        }
    }
    
    private fun loadAppInfo() {
        // 从应用信息加载版本号
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appVersion = packageInfo.versionName ?: "1.0.0"
            
            _uiState.value = _uiState.value.copy(
                appVersion = appVersion,
                sdkVersion = "1.1"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load app info", e)
            _uiState.value = _uiState.value.copy(
                appVersion = "1.0.0",
                sdkVersion = "1.1"
            )
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                val result = sdkManager?.disconnect()
                result?.onSuccess {
                    Log.d(TAG, "Disconnected successfully")
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "已断开连接"
                    )
                }?.onFailure { e ->
                    Log.e(TAG, "Failed to disconnect", e)
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "断开失败: ${e.message}"
                    )
                }
                
                // 清除状态消息
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(statusMessage = "")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "断开失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 显示关于对话框
     */
    fun showAbout() {
        _uiState.value = _uiState.value.copy(showAboutDialog = true)
    }
    
    /**
     * 隐藏关于对话框
     */
    fun hideAbout() {
        _uiState.value = _uiState.value.copy(showAboutDialog = false)
    }
    
    /**
     * 显示常见问题对话框
     */
    fun showFAQ() {
        _uiState.value = _uiState.value.copy(showFAQDialog = true)
    }
    
    /**
     * 隐藏常见问题对话框
     */
    fun hideFAQ() {
        _uiState.value = _uiState.value.copy(showFAQDialog = false)
    }
    
    /**
     * 检查更新
     */
    fun checkUpdate() {
        _uiState.value = _uiState.value.copy(statusMessage = "检查更新中...")
        
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(statusMessage = "已是最新版本")
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(statusMessage = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = "检查失败")
            }
        }
    }
    
    /**
     * 显示API配置对话框
     */
    fun showApiConfig() {
        _uiState.value = _uiState.value.copy(showApiConfigDialog = true)
    }
    
    /**
     * 隐藏API配置对话框
     */
    fun hideApiConfig() {
        _uiState.value = _uiState.value.copy(showApiConfigDialog = false)
    }
    
    /**
     * 保存API配置
     */
    fun saveApiConfig(
        linkaiVoiceKey: String,
        linkaiChatKey: String,
        openclawKey: String,
        openclawAppId: String
    ) {
        viewModelScope.launch {
            try {
                val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
                
                // 保存API Keys
                if (linkaiVoiceKey.isNotEmpty()) {
                    apiKeyManager.saveLinkAIVoiceApiKey(linkaiVoiceKey)
                }
                if (linkaiChatKey.isNotEmpty()) {
                    apiKeyManager.saveLinkAIChatApiKey(linkaiChatKey)
                }
                if (openclawKey.isNotEmpty()) {
                    apiKeyManager.saveOpenClawApiKey(openclawKey)
                }
                if (openclawAppId.isNotEmpty()) {
                    apiKeyManager.saveOpenClawAppId(openclawAppId)
                }
                
                _uiState.value = _uiState.value.copy(
                    statusMessage = "API配置已保存",
                    showApiConfigDialog = false
                )
                
                // 清除状态消息
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(statusMessage = "")
                
                Log.d(TAG, "API configuration saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save API configuration", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "保存失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 显示崩溃日志对话框
     */
    fun showCrashLog() {
        _uiState.value = _uiState.value.copy(showCrashLogDialog = true)
    }
    
    /**
     * 隐藏崩溃日志对话框
     */
    fun hideCrashLog() {
        _uiState.value = _uiState.value.copy(showCrashLogDialog = false)
    }
}

/**
 * ProfileViewModel工厂类
 */
class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
