package com.glasses.app.data.remote.sdk

import android.content.Context
import android.util.Log
import com.oudmon.wifi.GlassesControl
import com.oudmon.wifi.bean.GlassAlbumEntity
import com.glasses.app.data.local.media.MediaFile
import com.glasses.app.data.local.media.MediaType
import com.glasses.app.data.local.media.SyncProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 媒体同步管理器
 * 管理Linkai星韵AI眼镜的媒体文件同步
 * 参考官方demo的WiFi文件下载监听器实现
 */
class MediaSyncManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "MediaSyncManager"
        private var instance: MediaSyncManager? = null
        
        fun getInstance(context: Context): MediaSyncManager {
            return instance ?: synchronized(this) {
                instance ?: MediaSyncManager(context).also { instance = it }
            }
        }
    }
    
    private val appContext = context.applicationContext
    
    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    private val _mediaFiles = MutableStateFlow<List<MediaFile>>(emptyList())
    val mediaFiles: StateFlow<List<MediaFile>> = _mediaFiles.asStateFlow()
    
    private val mediaFileMap = mutableMapOf<String, MediaFile>()
    private var isSyncing = false
    
    /**
     * 初始化媒体同步
     * @param albumDirPath 相册目录路径
     */
    fun initSync(albumDirPath: String, onResult: (success: Boolean, message: String) -> Unit) {
        try {
            // 创建相册目录
            val albumDir = File(albumDirPath)
            if (!albumDir.exists()) {
                albumDir.mkdirs()
            }
            
            // 获取Application实例
            val app = appContext as android.app.Application
            val glassesControl = GlassesControl.getInstance(app)
            
            // 初始化GlassesControl
            glassesControl?.initGlasses(albumDirPath)
            
            // 设置WiFi文件下载监听器
            glassesControl?.setWifiDownloadListener(
                object : GlassesControl.WifiFilesDownloadListener {
                    override fun eisEnd(fileName: String, filePath: String) {
                        Log.d(TAG, "文件下载完成: $fileName -> $filePath")
                        handleFileDownloadComplete(fileName, filePath)
                    }
                    
                    override fun eisError(fileName: String, sourcePath: String, errorInfo: String) {
                        Log.e(TAG, "文件下载错误: $fileName, 错误: $errorInfo")
                        handleFileDownloadError(fileName, errorInfo)
                    }
                    
                    override fun fileCount(index: Int, total: Int) {
                        Log.d(TAG, "文件计数: $index/$total")
                        _syncProgress.value = _syncProgress.value.copy(
                            currentIndex = index,
                            totalCount = total
                        )
                    }
                    
                    override fun fileDownloadComplete() {
                        Log.d(TAG, "所有文件下载完成")
                        _syncProgress.value = _syncProgress.value.copy(isComplete = true)
                        isSyncing = false
                    }
                    
                    override fun fileDownloadError(fileType: Int, errorType: Int) {
                        Log.e(TAG, "文件下载错误: fileType=$fileType, errorType=$errorType")
                        _syncProgress.value = _syncProgress.value.copy(
                            hasError = true,
                            errorMessage = "文件下载错误: fileType=$fileType, errorType=$errorType"
                        )
                        isSyncing = false
                    }
                    
                    override fun fileProgress(fileName: String, progress: Int) {
                        Log.d(TAG, "文件下载进度: $fileName -> $progress%")
                        _syncProgress.value = _syncProgress.value.copy(
                            currentFileName = fileName,
                            currentProgress = progress
                        )
                    }
                    
                    override fun fileWasDownloadSuccessfully(entity: GlassAlbumEntity) {
                        Log.d(TAG, "文件下载成功: ${entity.fileName}")
                        addMediaFile(entity)
                    }
                    
                    override fun onGlassesControlSuccess() {
                        Log.d(TAG, "眼镜控制成功")
                    }
                    
                    override fun onGlassesFail(errorCode: Int) {
                        Log.e(TAG, "眼镜控制失败: errorCode=$errorCode")
                        _syncProgress.value = _syncProgress.value.copy(
                            hasError = true,
                            errorMessage = "眼镜控制失败: errorCode=$errorCode"
                        )
                        isSyncing = false
                    }
                    
                    override fun recordingToPcm(fileName: String, filePath: String, duration: Int) {
                        Log.d(TAG, "录音转PCM: $fileName -> $filePath, 时长: ${duration}ms")
                    }
                    
                    override fun recordingToPcmError(fileName: String, errorInfo: String) {
                        Log.e(TAG, "录音转PCM错误: $fileName, 错误: $errorInfo")
                    }
                    
                    override fun voiceFromGlasses(pcmData: ByteArray) {
                        Log.d(TAG, "接收到眼镜语音数据: ${pcmData.size} bytes")
                    }
                    
                    override fun voiceFromGlassesStatus(status: Int) {
                        Log.d(TAG, "眼镜语音状态: $status")
                    }
                    
                    override fun wifiSpeed(wifiSpeed: String) {
                        Log.d(TAG, "WiFi速度: $wifiSpeed")
                    }
                }
            )
            
            Log.d(TAG, "媒体同步初始化成功")
            onResult(true, "媒体同步初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "媒体同步初始化失败: ${e.message}")
            onResult(false, "媒体同步初始化失败: ${e.message}")
        }
    }
    
    /**
     * 开始同步媒体文件
     */
    fun startSync(onResult: (success: Boolean, message: String) -> Unit) {
        try {
            if (isSyncing) {
                onResult(false, "正在同步中，请稍候")
                return
            }
            
            isSyncing = true
            _syncProgress.value = SyncProgress(
                currentIndex = 0,
                totalCount = 0,
                currentFileName = "",
                currentProgress = 0,
                isComplete = false,
                hasError = false,
                errorMessage = ""
            )
            
            // 获取Application实例
            val app = appContext as android.app.Application
            val glassesControl = GlassesControl.getInstance(app)
            
            // 调用SDK的导入相册方法
            glassesControl?.importAlbum()
            
            Log.d(TAG, "开始同步媒体文件")
            onResult(true, "开始同步媒体文件")
        } catch (e: Exception) {
            Log.e(TAG, "开始同步失败: ${e.message}")
            isSyncing = false
            onResult(false, "开始同步失败: ${e.message}")
        }
    }
    
    /**
     * 停止同步
     */
    fun stopSync() {
        isSyncing = false
        Log.d(TAG, "停止同步")
    }
    
    /**
     * 处理文件下载完成
     */
    private fun handleFileDownloadComplete(fileName: String, filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val mediaType = detectMediaType(fileName)
                val mediaFile = MediaFile(
                    id = fileName,
                    fileName = fileName,
                    filePath = filePath,
                    type = mediaType,
                    size = file.length(),
                    createTime = System.currentTimeMillis()
                )
                mediaFileMap[fileName] = mediaFile
                updateMediaFilesList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理文件下载完成异常: ${e.message}")
        }
    }
    
    /**
     * 处理文件下载错误
     */
    private fun handleFileDownloadError(fileName: String, errorInfo: String) {
        Log.e(TAG, "文件下载错误: $fileName - $errorInfo")
        _syncProgress.value = _syncProgress.value.copy(
            hasError = true,
            errorMessage = "文件 $fileName 下载失败: $errorInfo"
        )
    }
    
    /**
     * 添加媒体文件
     */
    private fun addMediaFile(entity: GlassAlbumEntity) {
        try {
            val mediaType = when {
                entity.fileName.endsWith(".jpg", ignoreCase = true) ||
                entity.fileName.endsWith(".png", ignoreCase = true) -> MediaType.IMAGE
                entity.fileName.endsWith(".mp4", ignoreCase = true) ||
                entity.fileName.endsWith(".mov", ignoreCase = true) -> MediaType.VIDEO
                entity.fileName.endsWith(".wav", ignoreCase = true) ||
                entity.fileName.endsWith(".mp3", ignoreCase = true) -> MediaType.AUDIO
                else -> MediaType.IMAGE
            }
            
            // 获取文件大小
            val fileSize = try {
                entity.filePath?.let { File(it).length() } ?: 0L
            } catch (e: Exception) {
                0L
            }
            
            // 获取时长（如果是视频或音频）
            val duration = try {
                if (mediaType == MediaType.VIDEO || mediaType == MediaType.AUDIO) {
                    entity.filePath?.let { filePath ->
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(filePath)
                        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        retriever.release()
                        durationStr?.toLongOrNull() ?: 0L
                    } ?: 0L
                } else {
                    0L
                }
            } catch (e: Exception) {
                0L
            }
            
            val mediaFile = MediaFile(
                id = entity.fileName,
                fileName = entity.fileName,
                filePath = entity.filePath ?: "",
                type = mediaType,
                size = fileSize,
                duration = duration,
                createTime = System.currentTimeMillis()
            )
            
            mediaFileMap[entity.fileName] = mediaFile
            updateMediaFilesList()
        } catch (e: Exception) {
            Log.e(TAG, "添加媒体文件异常: ${e.message}")
        }
    }
    
    /**
     * 更新媒体文件列表
     */
    private fun updateMediaFilesList() {
        _mediaFiles.value = mediaFileMap.values.sortedByDescending { it.createTime }
    }
    
    /**
     * 检测媒体类型
     */
    private fun detectMediaType(fileName: String): MediaType {
        return when {
            fileName.endsWith(".jpg", ignoreCase = true) ||
            fileName.endsWith(".png", ignoreCase = true) -> MediaType.IMAGE
            fileName.endsWith(".mp4", ignoreCase = true) ||
            fileName.endsWith(".mov", ignoreCase = true) -> MediaType.VIDEO
            fileName.endsWith(".wav", ignoreCase = true) ||
            fileName.endsWith(".mp3", ignoreCase = true) -> MediaType.AUDIO
            else -> MediaType.IMAGE
        }
    }
    
    /**
     * 获取指定类型的媒体文件
     */
    fun getMediaFilesByType(type: MediaType): List<MediaFile> {
        return _mediaFiles.value.filter { it.type == type }
    }
    
    /**
     * 删除媒体文件
     */
    fun deleteMediaFile(mediaFile: MediaFile, onResult: (success: Boolean, message: String) -> Unit) {
        try {
            val file = File(mediaFile.filePath)
            if (file.exists()) {
                file.delete()
            }
            
            // 删除缩略图
            mediaFile.thumbnailPath?.let {
                val thumbnailFile = File(it)
                if (thumbnailFile.exists()) {
                    thumbnailFile.delete()
                }
            }
            
            mediaFileMap.remove(mediaFile.id)
            updateMediaFilesList()
            
            Log.d(TAG, "媒体文件删除成功: ${mediaFile.fileName}")
            onResult(true, "媒体文件删除成功")
        } catch (e: Exception) {
            Log.e(TAG, "媒体文件删除失败: ${e.message}")
            onResult(false, "媒体文件删除失败: ${e.message}")
        }
    }
    
    /**
     * 清空所有媒体文件
     */
    fun clearAllMediaFiles(onResult: (success: Boolean, message: String) -> Unit) {
        try {
            mediaFileMap.values.forEach { mediaFile ->
                val file = File(mediaFile.filePath)
                if (file.exists()) {
                    file.delete()
                }
                
                mediaFile.thumbnailPath?.let {
                    val thumbnailFile = File(it)
                    if (thumbnailFile.exists()) {
                        thumbnailFile.delete()
                    }
                }
            }
            
            mediaFileMap.clear()
            updateMediaFilesList()
            
            Log.d(TAG, "所有媒体文件已清空")
            onResult(true, "所有媒体文件已清空")
        } catch (e: Exception) {
            Log.e(TAG, "清空媒体文件失败: ${e.message}")
            onResult(false, "清空媒体文件失败: ${e.message}")
        }
    }
    
    /**
     * 获取同步状态
     */
    fun isSyncingNow(): Boolean = isSyncing
}
