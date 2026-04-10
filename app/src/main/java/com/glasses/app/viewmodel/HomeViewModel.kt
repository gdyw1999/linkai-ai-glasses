package com.glasses.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glasses.app.GlassesApplication
import com.glasses.app.data.local.media.MediaType
import com.glasses.app.data.local.prefs.ApiKeyManager
import com.glasses.app.data.remote.api.AIServiceImpl
import com.glasses.app.data.remote.sdk.ConnectionState
import com.glasses.app.data.remote.sdk.GlassesSDKManager
import com.glasses.app.data.remote.sdk.MediaCaptureManager
import com.glasses.app.data.remote.sdk.MediaCaptureState
import com.glasses.app.data.remote.sdk.MediaSyncManager
import com.glasses.app.data.remote.sdk.ScannedDevice
import com.glasses.app.data.repository.SmartRecognitionRepository
import com.glasses.app.data.repository.SmartRecognitionResult
import com.glasses.app.service.GlassesConnectionService
import com.glasses.app.util.BatteryOptimizationHelper
import com.glasses.app.util.HuaweiProtectedAppsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/**
 * 首页UI状态
 */
data class HomeUiState(
    val isConnected: Boolean = false,
    val deviceName: String = "未连接",
    val batteryLevel: Int = 0,
    val isScanning: Boolean = false,
    val isRecording: Boolean = false,
    val captureState: MediaCaptureState = MediaCaptureState.IDLE,
    val recordingDuration: Long = 0L,
    val statusMessage: String = "",
    val isLoading: Boolean = false,
)

/**
 * 首页ViewModel
 * 管理设备连接状态、电量、媒体采集等功能
 */
class HomeViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
        private const val ALBUM_DIR_NAME = "GlassesAlbum"
        private const val IMAGE_RECOGNITION_PROMPT = "识别图片内容，输出开头使用：“图片识别：xxxxxxx”"
        private const val SYNC_WAIT_TIMEOUT_MS = 20_000L
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // 扫描到的设备列表
    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()
    
    private var sdkManager: GlassesSDKManager? = null
    private var mediaCaptureManager: MediaCaptureManager? = null
    private var mediaSyncManager: MediaSyncManager? = null
    private var scanJob: Job? = null
    private val apiKeyManager by lazy { ApiKeyManager.getInstance(context) }
    private val aiService by lazy { AIServiceImpl.getInstance(context) }
    private val smartRecognitionRepository by lazy { SmartRecognitionRepository.getInstance(context) }
    private val albumDirPath: String by lazy {
        File(context.getExternalFilesDir(null), ALBUM_DIR_NAME).absolutePath
    }
    
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
            mediaCaptureManager = MediaCaptureManager.getInstance(context)
            mediaSyncManager = MediaSyncManager.getInstance(context)
            initializeMediaSync()

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

            // 监听媒体采集状态（统一监听，避免重复 collect）
            viewModelScope.launch {
                mediaCaptureManager?.status?.collect { status ->
                    val isRecording =
                        status.state == MediaCaptureState.RECORDING_VIDEO ||
                        status.state == MediaCaptureState.RECORDING_AUDIO
                    val autoStatusMessage = when (status.state) {
                        MediaCaptureState.RECORDING_VIDEO -> "录像中..."
                        MediaCaptureState.RECORDING_AUDIO -> "录音中..."
                        MediaCaptureState.AI_RECOGNITION -> "智能识图中..."
                        MediaCaptureState.IDLE -> null
                    }
                    val currentMessage = _uiState.value.statusMessage
                    val shouldClearAutoMessage = status.state == MediaCaptureState.IDLE &&
                        currentMessage in setOf("录像中...", "录音中...", "智能识图中...")

                    _uiState.value = _uiState.value.copy(
                        isRecording = isRecording,
                        captureState = status.state,
                        recordingDuration = if (isRecording) status.recordingDuration else 0L,
                        statusMessage = when {
                            autoStatusMessage != null -> autoStatusMessage
                            shouldClearAutoMessage -> ""
                            else -> currentMessage
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SDK manager", e)
            _uiState.value = _uiState.value.copy(statusMessage = "SDK初始化失败")
        }
    }
    
    /**
     * 更新连接状态
     */
    private fun updateConnectionState(state: ConnectionState) {
        when (state) {
            ConnectionState.DISCONNECTED -> {
                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    deviceName = "未连接",
                    statusMessage = "设备已断开"
                )
                // 停止前台服务
                stopForegroundService()
            }
            ConnectionState.CONNECTING -> {
                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    statusMessage = "正在连接..."
                )
            }
            ConnectionState.CONNECTED -> {
                val deviceName = sdkManager?.getCurrentDevice()?.name ?: "已连接"
                _uiState.value = _uiState.value.copy(
                    isConnected = true,
                    deviceName = deviceName,
                    statusMessage = "连接成功"
                )
                // 连接成功后查询电量
                queryBattery()
                // 启动前台服务
                startForegroundService()
            }
        }
    }
    
    /**
     * 启动前台服务
     * 保持蓝牙连接和语音唤醒监听
     */
    private fun startForegroundService() {
        try {
            Log.d(TAG, "Starting foreground service...")
            GlassesConnectionService.startService(context)
            Log.d(TAG, "Foreground service start request sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            writeCrashLog("Foreground service start failed", e)
            _uiState.value = _uiState.value.copy(
                statusMessage = "后台服务启动失败，部分功能可能受限"
            )
        }
    }

    /**
     * 请求后台保活权限
     * 包括电池优化和华为设备保活引导
     */
    fun requestBackgroundPermissions() {
        // 如果是华为设备，显示保活引导
        if (HuaweiProtectedAppsHelper.isHuaweiDevice()) {
            HuaweiProtectedAppsHelper.showProtectedAppsGuide(context)
        }
        
        // 请求忽略电池优化
        if (context is android.app.Activity) {
            BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
        }
    }
    
    /**
     * 显示厂商特定的保活引导
     */
    fun showManufacturerGuide() {
        BatteryOptimizationHelper.showManufacturerSpecificGuide(context)
    }
    
    /**
     * 停止前台服务
     */
    private fun stopForegroundService() {
        try {
            GlassesConnectionService.stopService(context)
            Log.d(TAG, "Foreground service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop foreground service", e)
        }
    }
    
    /**
     * 开始扫描设备
     */
    fun startScan() {
        // 取消之前的扫描
        scanJob?.cancel()
        
        // 清空设备列表
        _scannedDevices.value = emptyList()
        
        _uiState.value = _uiState.value.copy(isScanning = true, statusMessage = "正在扫描设备...")
        
        scanJob = viewModelScope.launch {
            try {
                sdkManager?.scanDevices()
                    ?.catch { e ->
                        Log.e(TAG, "Scan error", e)
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            statusMessage = "扫描失败: ${e.message}"
                        )
                    }
                    ?.collect { device ->
                        // 添加到设备列表
                        val currentList = _scannedDevices.value.toMutableList()
                        if (!currentList.any { it.address == device.address }) {
                            currentList.add(device)
                            _scannedDevices.value = currentList
                            Log.d(TAG, "Device added: ${device.name}")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start scan", e)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    statusMessage = "扫描失败: ${e.message}"
                )
            }
        }
        
        // 10秒后自动停止扫描
        viewModelScope.launch {
            kotlinx.coroutines.delay(10000)
            if (_uiState.value.isScanning) {
                stopScan()
            }
        }
    }
    
    /**
     * 停止扫描
     */
    fun stopScan() {
        scanJob?.cancel()
        sdkManager?.stopScan()
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            statusMessage = "扫描完成，找到 ${_scannedDevices.value.size} 个设备"
        )
    }
    
    /**
     * 连接设备
     */
    fun connectDevice(device: ScannedDevice) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusMessage = "正在连接 ${device.name}...")
            
            val result = sdkManager?.connect(device.address)
            result?.onSuccess {
                Log.d(TAG, "Connection request sent successfully")
                // 连接状态会通过connectionState Flow更新
            }?.onFailure { e ->
                Log.e(TAG, "Failed to connect", e)
                _uiState.value = _uiState.value.copy(statusMessage = "连接失败: ${e.message}")
            }
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            val result = sdkManager?.disconnect()
            result?.onSuccess {
                Log.d(TAG, "Disconnected successfully")
            }?.onFailure { e ->
                Log.e(TAG, "Failed to disconnect", e)
                _uiState.value = _uiState.value.copy(statusMessage = "断开失败: ${e.message}")
            }
        }
    }
    
    /**
     * 查询电量
     * 电量通过 GlassesDeviceNotifyListener 回调自动更新，
     * 此处仅记录日志
     */
    private fun queryBattery() {
        val level = sdkManager?.getBatteryLevel() ?: 0
        Log.d(TAG, "Current battery level: $level%")
    }

    /**
     * 初始化媒体同步模块，用于识图后读取最新照片
     */
    private fun initializeMediaSync() {
        mediaSyncManager?.initSync(albumDirPath) { success, message ->
            if (success) {
                Log.d(TAG, "Media sync initialized: $albumDirPath")
            } else {
                Log.e(TAG, "Media sync init failed: $message")
            }
        }
    }
    
    /**
     * 拍照
     */
    fun takePhoto() {
        _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "正在拍照...")
        mediaCaptureManager?.takePhoto { success, message ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                statusMessage = message
            )
        }
    }

    /**
     * 开始录像
     */
    fun startVideo() {
        if (!_uiState.value.isConnected) {
            _uiState.value = _uiState.value.copy(statusMessage = "请先连接眼镜")
            return
        }
        if (_uiState.value.captureState == MediaCaptureState.RECORDING_AUDIO) {
            _uiState.value = _uiState.value.copy(statusMessage = "请先停止录音")
            return
        }
        _uiState.value = _uiState.value.copy(statusMessage = "正在开始录像...")
        mediaCaptureManager?.startVideo { success, message ->
            if (!success) {
                _uiState.value = _uiState.value.copy(statusMessage = message)
            }
        }
    }

    /**
     * 停止录像
     */
    fun stopVideo() {
        mediaCaptureManager?.stopVideo { success, message ->
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                statusMessage = message
            )
        }
    }

    /**
     * 开始录音
     */
    fun startAudio() {
        if (!_uiState.value.isConnected) {
            _uiState.value = _uiState.value.copy(statusMessage = "请先连接眼镜")
            return
        }
        if (_uiState.value.captureState == MediaCaptureState.RECORDING_VIDEO) {
            _uiState.value = _uiState.value.copy(statusMessage = "请先停止录像")
            return
        }
        _uiState.value = _uiState.value.copy(statusMessage = "正在开始录音...")
        mediaCaptureManager?.startAudio { success, message ->
            if (!success) {
                _uiState.value = _uiState.value.copy(statusMessage = message)
            }
        }
    }

    /**
     * 停止录音
     */
    fun stopAudio() {
        mediaCaptureManager?.stopAudio { success, message ->
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                statusMessage = message
            )
        }
    }

    /**
     * 开始智能识图
     */
    fun startAIRecognition() {
        if (!_uiState.value.isConnected) {
            _uiState.value = _uiState.value.copy(statusMessage = "请先连接眼镜")
            return
        }
        if (!apiKeyManager.hasAliQwenVisionApiKey()) {
            _uiState.value = _uiState.value.copy(statusMessage = "请先在「我的」→「API配置」设置阿里Qwen识图 API Key")
            return
        }
        if (_uiState.value.isLoading) {
            _uiState.value = _uiState.value.copy(statusMessage = "任务执行中，请稍候")
            return
        }
        if (_uiState.value.captureState != MediaCaptureState.IDLE) {
            _uiState.value = _uiState.value.copy(statusMessage = "请先停止当前采集任务")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "正在拍照...")
            try {
                val photoResult = takePhotoSuspend()
                if (!photoResult.first) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "拍照失败: ${photoResult.second}"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(statusMessage = "拍照成功，正在同步图片...")
                val imagePath = syncAndGetLatestImagePath()
                if (imagePath.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "未找到可识别的图片，请先在相册同步后重试"
                    )
                    return@launch
                }

                val selectedModel = apiKeyManager.getAliQwenVisionModel()
                _uiState.value = _uiState.value.copy(statusMessage = "正在智能识图...")

                val recognitionResult = aiService.recognizeImage(
                    imagePath = imagePath,
                    sessionId = "vision_${System.currentTimeMillis()}",
                    question = IMAGE_RECOGNITION_PROMPT,
                    model = selectedModel
                )

                if (recognitionResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "识图失败: ${recognitionResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val answer = recognitionResult.getOrNull().orEmpty()
                smartRecognitionRepository.publish(
                    SmartRecognitionResult(
                        imagePath = imagePath,
                        model = selectedModel,
                        question = IMAGE_RECOGNITION_PROMPT,
                        answer = answer
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "识图完成，结果已发送到 AI 对话页"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Smart recognition failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "识图失败: ${e.message}"
                )
            }
        }
    }

    private suspend fun takePhotoSuspend(): Pair<Boolean, String> {
        return suspendCancellableCoroutine { continuation ->
            val captureManager = mediaCaptureManager
            if (captureManager == null) {
                continuation.resume(false to "媒体模块未初始化")
                return@suspendCancellableCoroutine
            }
            captureManager.takePhoto { success, message ->
                if (continuation.isActive) {
                    continuation.resume(success to message)
                }
            }
        }
    }

    private suspend fun startSyncSuspend(): Pair<Boolean, String> {
        return suspendCancellableCoroutine { continuation ->
            val syncManager = mediaSyncManager
            if (syncManager == null) {
                continuation.resume(false to "媒体同步模块未初始化")
                return@suspendCancellableCoroutine
            }
            syncManager.startSync { success, message ->
                if (continuation.isActive) {
                    continuation.resume(success to message)
                }
            }
        }
    }

    private suspend fun syncAndGetLatestImagePath(): String? {
        val latestBefore = getLatestImagePath()
        val syncStartResult = startSyncSuspend()
        if (!syncStartResult.first) {
            Log.w(TAG, "Start sync failed, fallback to local image: ${syncStartResult.second}")
            return latestBefore ?: getLatestImagePath()
        }

        // 轮询等待新图片同步完成，超时后返回最新可用图片
        return withTimeoutOrNull<String?>(SYNC_WAIT_TIMEOUT_MS) {
            while (true) {
                val latest = getLatestImagePath()
                if (!latest.isNullOrEmpty() && latest != latestBefore) {
                    return@withTimeoutOrNull latest
                }
                delay(500)
            }
            @Suppress("UNREACHABLE_CODE") null // while(true) 不会执行到此处，仅满足类型推断
        } ?: getLatestImagePath()
    }

    private suspend fun getLatestImagePath(): String? = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val syncImagePath = mediaSyncManager?.mediaFiles
            ?.value
            ?.firstOrNull { it.type == MediaType.IMAGE && File(it.filePath).exists() }
            ?.filePath
        if (!syncImagePath.isNullOrEmpty()) {
            return@withContext syncImagePath
        }

        val albumDir = File(albumDirPath)
        if (!albumDir.exists() || !albumDir.isDirectory) {
            return@withContext null
        }

        albumDir.listFiles()
            ?.filter { file ->
                file.isFile && file.extension.lowercase() in setOf("jpg", "jpeg", "png")
            }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath
    }
    
    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        sdkManager?.stopScan()
    }
    
    /**
     * 写入崩溃日志到文件
     */
    private fun writeCrashLog(message: String, throwable: Throwable) {
        try {
            val crashDir = java.io.File(context.getExternalFilesDir(null), ".")
            if (!crashDir.exists()) crashDir.mkdirs()
            val crashFile = java.io.File(crashDir, "crash_log.txt")
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            java.io.FileWriter(crashFile, true).use { writer ->
                writer.append("=== VIEWMODEL ERROR $timestamp ===\n")
                writer.append("Message: $message\n")
                writer.append("Exception: ${throwable.javaClass.name}\n")
                writer.append("Error: ${throwable.message}\n")
                writer.append("Stack trace:\n")
                throwable.stackTrace.forEach { element ->
                    writer.append("    at $element\n")
                }
                var cause = throwable.cause
                var depth = 1
                while (cause != null && depth <= 5) {
                    writer.append("Caused by ($depth): ${cause.javaClass.name}: ${cause.message}\n")
                    cause.stackTrace.take(5).forEach { element ->
                        writer.append("    at $element\n")
                    }
                    cause = cause.cause
                    depth++
                }
                writer.append("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }
    }
}

/**
 * HomeViewModel工厂类
 */
class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
