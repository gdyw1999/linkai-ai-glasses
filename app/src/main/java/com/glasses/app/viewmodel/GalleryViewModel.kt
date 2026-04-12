package com.glasses.app.viewmodel

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glasses.app.data.local.media.MediaFile
import com.glasses.app.data.local.media.MediaType
import com.glasses.app.data.remote.sdk.ConnectionState
import com.glasses.app.data.remote.sdk.GlassesSDKManager
import com.glasses.app.data.remote.sdk.MediaSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 媒体类型枚举（UI层）
 */
enum class GalleryMediaType {
    ALL, IMAGE, VIDEO, AUDIO
}

/**
 * 相册UI状态
 */
data class GalleryUiState(
    val mediaFiles: List<MediaFile> = emptyList(),
    val selectedMediaType: GalleryMediaType = GalleryMediaType.ALL,
    val isSyncing: Boolean = false,
    val syncProgress: Int = 0,
    val syncMessage: String = "",
    val statusMessage: String = "",
    val selectedMedia: MediaFile? = null,
    val isViewerOpen: Boolean = false
)

/**
 * 相册ViewModel
 * 管理媒体同步、列表显示、类型筛选等
 * 集成真实的后端模块：MediaSyncManager
 */
@android.annotation.SuppressLint("StaticFieldLeak")
class GalleryViewModel(context: Context) : ViewModel() {
    private val context: Context = context.applicationContext
    
    companion object {
        private const val TAG = "GalleryViewModel"
        private const val ALBUM_DIR_NAME = "GlassesAlbum"
        private const val SYNC_TIMEOUT_MS = 30_000L // 30秒同步超时
    }

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    private var syncTimeoutJob: Job? = null
    
    // 后端模块
    private val mediaSyncManager = MediaSyncManager.getInstance(context)
    
    // 相册目录路径（使用 app-private 目录，SDK 内部用 File API 写入，公共目录在 Android 11+ 无权限）
    private val albumDirPath: String by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "星韵AI相册").absolutePath
    }
    
    init {
        // 初始化媒体同步
        initializeMediaSync()
        
        // 监听媒体文件列表
        viewModelScope.launch {
            mediaSyncManager.mediaFiles.collect { files ->
                _uiState.value = _uiState.value.copy(mediaFiles = files)
                Log.d(TAG, "Media files updated: ${files.size} files")
            }
        }
        
        // 监听同步进度
        viewModelScope.launch {
            mediaSyncManager.syncProgress.collect { progress ->
                val overallProgress = progress.overallProgress
                val isSyncing = mediaSyncManager.isSyncingNow()
                
                val syncMessage = when {
                    progress.hasError -> {
                        com.glasses.app.util.AppLogger.e(TAG, "同步失败: ${progress.errorMessage}")
                        "同步失败: ${progress.errorMessage}"
                    }
                    progress.isComplete -> {
                        syncTimeoutJob?.cancel()
                        com.glasses.app.util.AppLogger.i(TAG, "同步完成，共${_uiState.value.mediaFiles.size}个文件")
                        // 同步完成时强制结束同步状态（避免 SDK 回调时序导致 UI 卡在"同步中"）
                        mediaSyncManager.stopSync()
                        // 3秒后自动清除"同步完成"提示
                        viewModelScope.launch {
                            delay(3000)
                            if (_uiState.value.syncMessage == "同步完成") {
                                _uiState.value = _uiState.value.copy(syncMessage = "")
                            }
                        }
                        "同步完成"
                    }
                    isSyncing && progress.currentFileName.isNotEmpty() ->
                        "正在同步: ${progress.currentFileName} (${progress.currentIndex}/${progress.totalCount})"
                    isSyncing -> "正在同步..."
                    else -> ""
                }

                // 同步完成后强制 isSyncing=false，不依赖 SDK 的 isSyncingNow()
                val finalIsSyncing = isSyncing && !progress.isComplete && !progress.hasError

                _uiState.value = _uiState.value.copy(
                    isSyncing = finalIsSyncing,
                    syncProgress = overallProgress,
                    syncMessage = syncMessage
                )
                
                Log.d(TAG, "Sync progress: $overallProgress%, message: $syncMessage")
            }
        }
    }
    
    /**
     * 初始化媒体同步
     */
    private fun initializeMediaSync() {
        viewModelScope.launch {
            try {
                mediaSyncManager.initSync(albumDirPath) { success, message ->
                    if (success) {
                        Log.d(TAG, "Media sync initialized successfully")
                    } else {
                        Log.e(TAG, "Failed to initialize media sync: $message")
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "初始化失败: $message"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize media sync", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "初始化失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 开始同步媒体（30秒超时保护）
     */
    fun startSync() {
        viewModelScope.launch {
            try {
                // 检查眼镜连接状态
                val sdkManager = GlassesSDKManager.getInstance(context)
                if (sdkManager.connectionState.value != ConnectionState.CONNECTED) {
                    _uiState.value = _uiState.value.copy(statusMessage = "请先连接眼镜")
                    return@launch
                }

                if (_uiState.value.isSyncing) {
                    _uiState.value = _uiState.value.copy(statusMessage = "正在同步中，请稍候")
                    return@launch
                }

                com.glasses.app.util.AppLogger.i(TAG, "开始同步媒体文件...")
                mediaSyncManager.startSync { success, message ->
                    com.glasses.app.util.AppLogger.i(TAG, "同步启动结果: success=$success, msg=$message")
                    if (!success) {
                        _uiState.value = _uiState.value.copy(statusMessage = message)
                    }
                }

                // 启动超时保护：30秒后强制结束同步状态
                syncTimeoutJob?.cancel()
                syncTimeoutJob = viewModelScope.launch {
                    delay(SYNC_TIMEOUT_MS)
                    if (mediaSyncManager.isSyncingNow()) {
                        com.glasses.app.util.AppLogger.w(TAG, "同步超时(${SYNC_TIMEOUT_MS}ms)，强制停止")
                        mediaSyncManager.stopSync()
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            statusMessage = "同步超时，已自动停止"
                        )
                        // 3秒后清除提示
                        delay(3000)
                        _uiState.value = _uiState.value.copy(statusMessage = "")
                    }
                }
            } catch (e: Exception) {
                com.glasses.app.util.AppLogger.e(TAG, "同步启动失败", e)
                _uiState.value = _uiState.value.copy(statusMessage = "同步失败: ${e.message}")
            }
        }
    }
    

    /**
     * 筛选媒体类型
     */
    fun filterByType(type: GalleryMediaType) {
        _uiState.value = _uiState.value.copy(selectedMediaType = type)
    }
    
    /**
     * 获取筛选后的媒体列表
     */
    fun getFilteredMedia(): List<MediaFile> {
        val allMedia = _uiState.value.mediaFiles
        return when (_uiState.value.selectedMediaType) {
            GalleryMediaType.ALL -> allMedia
            GalleryMediaType.IMAGE -> allMedia.filter { it.type == MediaType.IMAGE }
            GalleryMediaType.VIDEO -> allMedia.filter { it.type == MediaType.VIDEO }
            GalleryMediaType.AUDIO -> allMedia.filter { it.type == MediaType.AUDIO }
        }
    }
    
    /**
     * 打开媒体查看器
     */
    fun openViewer(media: MediaFile) {
        _uiState.value = _uiState.value.copy(
            selectedMedia = media,
            isViewerOpen = true
        )
    }
    
    /**
     * 关闭媒体查看器
     */
    fun closeViewer() {
        _uiState.value = _uiState.value.copy(
            isViewerOpen = false,
            selectedMedia = null
        )
    }
    
    /**
     * 删除媒体文件
     */
    fun deleteMedia(media: MediaFile) {
        viewModelScope.launch {
            try {
                mediaSyncManager.deleteMediaFile(media) { success, message ->
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "删除成功"
                        )
                        Log.d(TAG, "Media file deleted: ${media.fileName}")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = message
                        )
                        Log.e(TAG, "Failed to delete media file: $message")
                    }
                }
                
                // 清除状态消息
                delay(1500)
                _uiState.value = _uiState.value.copy(statusMessage = "")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete media", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "删除失败: ${e.message}"
                )
            }
        }
    }
}

/**
 * GalleryViewModel工厂类
 */
class GalleryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
