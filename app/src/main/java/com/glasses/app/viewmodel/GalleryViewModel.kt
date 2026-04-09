package com.glasses.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glasses.app.data.local.media.LocalMediaManager
import com.glasses.app.data.local.media.MediaFile
import com.glasses.app.data.local.media.MediaType
import com.glasses.app.data.remote.sdk.MediaSyncManager
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
 * 集成真实的后端模块：MediaSyncManager、LocalMediaManager
 */
class GalleryViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "GalleryViewModel"
        private const val ALBUM_DIR_NAME = "GlassesAlbum"
    }
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    
    // 后端模块
    private val mediaSyncManager = MediaSyncManager.getInstance(context)
    private val localMediaManager = LocalMediaManager.getInstance(context)
    
    // 相册目录路径
    private val albumDirPath: String = File(context.getExternalFilesDir(null), ALBUM_DIR_NAME).absolutePath
    
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
                    progress.hasError -> "同步失败: ${progress.errorMessage}"
                    progress.isComplete -> "同步完成"
                    isSyncing && progress.currentFileName.isNotEmpty() -> 
                        "正在同步: ${progress.currentFileName} (${progress.currentIndex}/${progress.totalCount})"
                    isSyncing -> "正在同步..."
                    else -> ""
                }
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = isSyncing,
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
     * 开始同步媒体
     */
    fun startSync() {
        viewModelScope.launch {
            try {
                if (_uiState.value.isSyncing) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "正在同步中，请稍候"
                    )
                    return@launch
                }
                
                mediaSyncManager.startSync { success, message ->
                    if (!success) {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start sync", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "同步失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 停止同步
     */
    fun stopSync() {
        mediaSyncManager.stopSync()
        _uiState.value = _uiState.value.copy(
            isSyncing = false,
            statusMessage = "已停止同步"
        )
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
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(statusMessage = "")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete media", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "删除失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 生成缩略图
     */
    fun generateThumbnail(media: MediaFile) {
        viewModelScope.launch {
            try {
                when (media.type) {
                    MediaType.IMAGE -> {
                        localMediaManager.generateImageThumbnail(media.filePath) { success, thumbnailPath ->
                            if (success && thumbnailPath != null) {
                                Log.d(TAG, "Image thumbnail generated: $thumbnailPath")
                            } else {
                                Log.e(TAG, "Failed to generate image thumbnail")
                            }
                        }
                    }
                    MediaType.VIDEO -> {
                        localMediaManager.generateVideoThumbnail(media.filePath) { success, thumbnailPath ->
                            if (success && thumbnailPath != null) {
                                Log.d(TAG, "Video thumbnail generated: $thumbnailPath")
                            } else {
                                Log.e(TAG, "Failed to generate video thumbnail")
                            }
                        }
                    }
                    MediaType.AUDIO -> {
                        // 音频不需要缩略图
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate thumbnail", e)
            }
        }
    }
    
    /**
     * 清空所有媒体文件
     */
    fun clearAllMedia() {
        viewModelScope.launch {
            try {
                mediaSyncManager.clearAllMediaFiles { success, message ->
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "已清空所有媒体文件"
                        )
                        Log.d(TAG, "All media files cleared")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = message
                        )
                        Log.e(TAG, "Failed to clear media files: $message")
                    }
                }
                
                // 清除状态消息
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(statusMessage = "")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all media", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "清空失败: ${e.message}"
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
