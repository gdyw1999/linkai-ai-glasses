package com.glasses.app.data.remote.sdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oudmon.wifi.GlassesControl
import com.oudmon.wifi.bean.GlassAlbumEntity
import com.glasses.app.data.local.media.MediaFile
import com.glasses.app.data.local.media.MediaType
import com.glasses.app.data.local.media.SyncProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 媒体同步管理器
 * 管理Linkai星韵AI眼镜的媒体文件同步
 * 参考官方demo的WiFi文件下载监听器实现
 *
 * 持久化策略：
 * - 媒体文件元数据（MediaFile）通过 SharedPreferences 持久化（已配置自动备份）
 * - 重装后自动恢复文件列表，SDK 的 importAlbum() 增量同步补充新文件
 * - 实际文件存储在 app-private 目录，通过 FileProvider 共享
 */
class MediaSyncManager private constructor(context: Context) {

    companion object {
        private const val TAG = "MediaSyncManager"
        private const val PREFS_NAME = "media_sync_prefs"
        private const val KEY_MEDIA_FILES = "media_files"
        private const val KEY_SYNCED_FILE_NAMES = "synced_file_names"
        private var instance: MediaSyncManager? = null

        fun getInstance(context: Context): MediaSyncManager {
            return instance ?: synchronized(this) {
                instance ?: MediaSyncManager(context).also { instance = it }
            }
        }
    }

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    private val _mediaFiles = MutableStateFlow<List<MediaFile>>(emptyList())
    val mediaFiles: StateFlow<List<MediaFile>> = _mediaFiles.asStateFlow()

    private val mediaFileMap = mutableMapOf<String, MediaFile>()
    private val syncedFileNames = mutableSetOf<String>()  // SDK 端已同步记录，防止重复下载
    private var isSyncing = false
    private var syncCallbackTimeoutJob: Job? = null  // SDK 无回调时的兜底超时
    private val managerScope = CoroutineScope(Dispatchers.Main + Job())

    // 存储已初始化的相册目录路径
    private var albumDirPath: String = ""

    // 是否已完成首次初始化（避免重复加载覆盖内存状态）
    private var isInitialized = false

    /**
     * 初始化媒体同步（幂等：多次调用不会覆盖已有状态）
     * @param albumDirPath 相册目录路径
     */
    fun initSync(albumDirPath: String, onResult: (success: Boolean, message: String) -> Unit) {
        this.albumDirPath = albumDirPath

        // 首次初始化：从持久化恢复 + 扫描本地目录
        // 后续调用仅重新注册监听器，不重复加载避免覆盖内存状态
        if (!isInitialized) {
            isInitialized = true
            loadPersistedMediaFiles()           // 重装后从 SharedPreferences 恢复文件列表
            scanLocalAlbumDirectory(albumDirPath) // 扫描本地目录，补充 SDK 未记录的文件
        }

        try {
            // 创建相册目录
            val albumDir = File(albumDirPath)
            if (!albumDir.exists()) {
                albumDir.mkdirs()
            }

            // 获取Application实例
            val app = appContext as android.app.Application
            val glassesControl = GlassesControl.getInstance(app)

            // 初始化GlassesControl（重复调用无害，SDK 内部会处理）
            glassesControl?.initGlasses(albumDirPath)

            // 设置WiFi文件下载监听器（重复设置会覆盖之前的监听器）
            glassesControl?.setWifiDownloadListener(
                object : GlassesControl.WifiFilesDownloadListener {
                    override fun eisEnd(fileName: String, filePath: String) {
                        com.glasses.app.util.AppLogger.i(TAG, "SDK回调: 文件下载完成 $fileName -> $filePath")
                        handleFileDownloadComplete(fileName, filePath)
                    }

                    override fun eisError(fileName: String, sourcePath: String, errorInfo: String) {
                        com.glasses.app.util.AppLogger.e(TAG, "SDK回调: 文件下载错误 $fileName - $errorInfo")
                        handleFileDownloadError(fileName, errorInfo)
                    }

                    override fun fileCount(index: Int, total: Int) {
                        com.glasses.app.util.AppLogger.i(TAG, "SDK回调: 文件计数 $index/$total")
                        _syncProgress.value = _syncProgress.value.copy(
                            currentIndex = index,
                            totalCount = total
                        )
                        // SDK 报告 0 个文件 = 眼镜上没有新文件，同步立即结束
                        // 取消超时保护，避免 30s 后误触发"强制结束"
                        if (total == 0) {
                            com.glasses.app.util.AppLogger.i(TAG, "SDK 报告 0 个文件，视为同步完成")
                            syncCallbackTimeoutJob?.cancel()
                            isSyncing = false
                            _syncProgress.value = _syncProgress.value.copy(isComplete = true)
                            // 立即停止监听后续回调（避免 fileDownloadError 覆盖正确状态）
                            return
                        }
                    }

                    override fun fileDownloadComplete() {
                        com.glasses.app.util.AppLogger.i(TAG, "SDK回调: 所有文件下载完成")
                        syncCallbackTimeoutJob?.cancel()
                        _syncProgress.value = _syncProgress.value.copy(isComplete = true)
                        isSyncing = false
                    }

                    override fun fileDownloadError(fileType: Int, errorType: Int) {
                        // 忽略同步已结束后的冗余错误回调（眼镜无文件时 SDK 会在 fileCount(0,0) 后跟 fileDownloadError）
                        if (!isSyncing) {
                            com.glasses.app.util.AppLogger.w(TAG, "忽略冗余错误回调（同步已结束）")
                            return
                        }
                        com.glasses.app.util.AppLogger.e(TAG, "SDK回调: 文件下载错误 fileType=$fileType, errorType=$errorType")
                        syncCallbackTimeoutJob?.cancel()
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
                        com.glasses.app.util.AppLogger.i(TAG, "SDK回调: 文件下载成功 ${entity.fileName}")
                        addMediaFile(entity)
                    }

                    override fun onGlassesControlSuccess() {
                        com.glasses.app.util.AppLogger.i(TAG, "SDK回调: 眼镜控制成功")
                    }

                    override fun onGlassesFail(errorCode: Int) {
                        com.glasses.app.util.AppLogger.e(TAG, "SDK回调: 眼镜控制失败 errorCode=$errorCode")
                        syncCallbackTimeoutJob?.cancel()
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

            Log.d(TAG, "媒体同步初始化成功，当前记录文件数: ${mediaFileMap.size}")
            com.glasses.app.util.AppLogger.i(TAG, "初始化完成，当前 ${mediaFileMap.size} 个媒体文件")
            onResult(true, "媒体同步初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "媒体同步初始化失败: ${e.message}")
            onResult(false, "媒体同步初始化失败: ${e.message}")
        }
    }

    /**
     * 开始同步媒体文件（增量同步：SDK 自动处理，仅拉取眼镜上新文件）
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

            // 取消之前的超时任务
            syncCallbackTimeoutJob?.cancel()

            // SDK 无回调时的兜底超时：30秒后强制结束同步
            // （解决眼镜无新文件时 SDK 完全不发 callback 的问题）
            syncCallbackTimeoutJob = managerScope.launch {
                delay(30_000L)
                if (isSyncing) {
                    com.glasses.app.util.AppLogger.w(TAG, "SDK 30s 无回调，强制结束同步")
                    _syncProgress.value = _syncProgress.value.copy(
                        isComplete = true,
                        hasError = false,
                        errorMessage = ""
                    )
                    isSyncing = false
                }
            }

            // 获取Application实例
            val app = appContext as android.app.Application
            val glassesControl = GlassesControl.getInstance(app)

            // 调用SDK的导入相册方法（增量同步，SDK内部跳过已同步文件）
            glassesControl?.importAlbum()

            com.glasses.app.util.AppLogger.i(TAG, "调用SDK importAlbum()，开始增量同步")
            Log.d(TAG, "开始同步媒体文件")
            onResult(true, "开始同步媒体文件")
        } catch (e: Exception) {
            Log.e(TAG, "开始同步失败: ${e.message}")
            isSyncing = false
            syncCallbackTimeoutJob?.cancel()
            onResult(false, "开始同步失败: ${e.message}")
        }
    }

    /**
     * 停止同步（强制结束）
     * 同时更新 _syncProgress，确保 GalleryViewModel 的 collect 能感知到结束
     */
    fun stopSync() {
        syncCallbackTimeoutJob?.cancel()
        syncCallbackTimeoutJob = null
        isSyncing = false
        // 同步更新 _syncProgress，避免 UI 读到过期的 isComplete=false
        _syncProgress.value = _syncProgress.value.copy(
            isComplete = true,
            hasError = false
        )
        com.glasses.app.util.AppLogger.i(TAG, "stopSync: 强制结束同步 isComplete=true")
        Log.d(TAG, "停止同步")
    }

    /**
     * 处理文件下载完成（SDK eisEnd 回调）
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
                    createTime = file.lastModified()
                )
                mediaFileMap[fileName] = mediaFile
                updateMediaFilesList()
                persistMediaFiles()  // 立即持久化
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
     * 添加媒体文件（SDK fileWasDownloadSuccessfully 回调）
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
            persistMediaFiles()  // 立即持久化
            com.glasses.app.util.AppLogger.i(TAG, "新增媒体文件: ${entity.fileName} (${mediaType}), 总计: ${mediaFileMap.size}")
        } catch (e: Exception) {
            Log.e(TAG, "添加媒体文件异常: ${e.message}")
        }
    }

    /**
     * 更新媒体文件列表并通知 Flow
     */
    private fun updateMediaFilesList() {
        _mediaFiles.value = mediaFileMap.values.sortedByDescending { it.createTime }
    }

    /**
     * 扫描本地相册目录，将实际存在的文件补充到列表中
     * 解决：SDK 重装后丢失同步记录，但本地文件仍在的情况
     */
    private fun scanLocalAlbumDirectory(albumDirPath: String) {
        try {
            val albumDir = File(albumDirPath)
            if (!albumDir.exists() || !albumDir.isDirectory) return

            val existingFiles = albumDir.listFiles() ?: return
            var addedCount = 0

            existingFiles.forEach { file ->
                if (file.isFile && !mediaFileMap.containsKey(file.name)) {
                    val mediaType = detectMediaType(file.name)
                    val mediaFile = MediaFile(
                        id = file.name,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        type = mediaType,
                        size = file.length(),
                        createTime = file.lastModified()
                    )
                    mediaFileMap[file.name] = mediaFile
                    addedCount++
                    com.glasses.app.util.AppLogger.i(TAG, "扫描本地补充: ${file.name} (${file.length()} bytes)")
                }
            }

            if (addedCount > 0) {
                updateMediaFilesList()
                persistMediaFiles()
                com.glasses.app.util.AppLogger.i(TAG, "本地目录扫描完成，补充 $addedCount 个文件")
            }
        } catch (e: Exception) {
            Log.e(TAG, "扫描本地相册目录失败: ${e.message}")
        }
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
            persistMediaFiles()  // 持久化更新后的列表

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
            persistMediaFiles()  // 持久化清空后的列表

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

    // ==================== 持久化相关 ====================

    /**
     * 将媒体文件列表持久化到 SharedPreferences
     * SharedPreferences 已配置自动备份，重装后可恢复
     */
    private fun persistMediaFiles() {
        try {
            val json = gson.toJson(mediaFileMap.values.toList())
            prefs.edit().putString(KEY_MEDIA_FILES, json).apply()
            Log.d(TAG, "媒体文件元数据已持久化: ${mediaFileMap.size} 个文件")
        } catch (e: Exception) {
            Log.e(TAG, "持久化媒体文件失败: ${e.message}")
        }
    }

    /**
     * 从 SharedPreferences 恢复媒体文件列表
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadPersistedMediaFiles() {
        try {
            val json = prefs.getString(KEY_MEDIA_FILES, null) ?: return
            val type = object : TypeToken<List<MediaFile>>() {}.type
            val persistedFiles: List<MediaFile> = gson.fromJson(json, type)

            persistedFiles.forEach { mediaFile ->
                // 验证文件是否实际存在（若重装后 app-private 目录被清空则不存在）
                val file = File(mediaFile.filePath)
                if (file.exists()) {
                    mediaFileMap[mediaFile.id] = mediaFile
                } else {
                    // 文件不存在（app-private 被清空），保留元数据供 UI 显示
                    // （实际文件丢失，用户需重新同步）
                    com.glasses.app.util.AppLogger.w(TAG, "文件丢失（重装后被清空）: ${mediaFile.fileName}")
                }
            }

            updateMediaFilesList()
            Log.d(TAG, "从 SharedPreferences 恢复媒体文件: ${mediaFileMap.size} 个")
        } catch (e: Exception) {
            Log.e(TAG, "恢复媒体文件失败: ${e.message}")
        }
    }
}
