package com.glasses.app.manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 临时文件管理器
 * 管理临时文件的创建、删除和自动清理
 */
class TempFileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TempFileManager"
        private const val TEMP_DIR_NAME = "glasses_temp"
        private const val CLEANUP_INTERVAL_HOURS = 24L
        private const val FILE_MAX_AGE_HOURS = 24L
        
        private var instance: TempFileManager? = null
        
        fun getInstance(context: Context): TempFileManager {
            return instance ?: TempFileManager(context).also { instance = it }
        }
    }
    
    private val tempDir: File
    
    init {
        tempDir = File(context.cacheDir, TEMP_DIR_NAME)
        if (!tempDir.exists()) {
            tempDir.mkdirs()
            Log.d(TAG, "Created temp directory: ${tempDir.absolutePath}")
        }
        
        // 启动定期清理任务
        startPeriodicCleanup()
    }
    
    /**
     * 创建临时文件
     */
    fun createTempFile(prefix: String, suffix: String): File {
        return try {
            val file = File.createTempFile(prefix, suffix, tempDir)
            Log.d(TAG, "Created temp file: ${file.name}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create temp file", e)
            throw e
        }
    }
    
    /**
     * 创建临时音频文件
     */
    fun createTempAudioFile(): File {
        return createTempFile("audio_", ".wav")
    }
    
    /**
     * 创建临时视频文件
     */
    fun createTempVideoFile(): File {
        return createTempFile("video_", ".mp4")
    }
    
    /**
     * 创建临时图片文件
     */
    fun createTempImageFile(): File {
        return createTempFile("image_", ".jpg")
    }
    
    /**
     * 删除临时文件
     */
    fun deleteTempFile(file: File): Boolean {
        return try {
            if (file.exists() && file.delete()) {
                Log.d(TAG, "Deleted temp file: ${file.name}")
                true
            } else {
                Log.w(TAG, "Failed to delete temp file: ${file.name}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting temp file", e)
            false
        }
    }
    
    /**
     * 清空所有临时文件
     */
    fun clearAllTempFiles(): Int {
        return try {
            var count = 0
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile && file.delete()) {
                    count++
                }
            }
            Log.d(TAG, "Cleared $count temp files")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing temp files", e)
            0
        }
    }
    
    /**
     * 清理过期的临时文件（超过24小时）
     */
    fun cleanupExpiredFiles(): Int {
        return try {
            val currentTime = System.currentTimeMillis()
            val maxAge = FILE_MAX_AGE_HOURS * 60 * 60 * 1000
            var count = 0
            
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val fileAge = currentTime - file.lastModified()
                    if (fileAge > maxAge) {
                        if (file.delete()) {
                            count++
                            Log.d(TAG, "Deleted expired file: ${file.name}")
                        }
                    }
                }
            }
            
            Log.d(TAG, "Cleaned up $count expired files")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired files", e)
            0
        }
    }
    
    /**
     * 获取临时目录大小（字节）
     */
    fun getTempDirSize(): Long {
        return try {
            var size = 0L
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
            Log.d(TAG, "Temp dir size: $size bytes")
            size
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating temp dir size", e)
            0L
        }
    }
    
    /**
     * 获取临时文件列表
     */
    fun getTempFiles(): List<File> {
        return try {
            tempDir.listFiles()?.filter { it.isFile } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting temp files", e)
            emptyList()
        }
    }
    
    /**
     * 启动定期清理任务
     */
    private fun startPeriodicCleanup() {
        try {
            val cleanupRequest = PeriodicWorkRequestBuilder<TempFileCleanupWorker>(
                CLEANUP_INTERVAL_HOURS,
                TimeUnit.HOURS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "temp_file_cleanup",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            
            Log.d(TAG, "Started periodic cleanup task")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start periodic cleanup", e)
        }
    }
}

/**
 * 临时文件清理Worker
 * 用于后台定期清理过期的临时文件
 */
class TempFileCleanupWorker(
    context: Context,
    params: androidx.work.WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "TempFileCleanupWorker"
    }
    
    override suspend fun doWork(): Result {
        return try {
            val tempFileManager = TempFileManager.getInstance(applicationContext)
            val cleanedCount = tempFileManager.cleanupExpiredFiles()
            Log.d(TAG, "Cleanup worker completed: cleaned $cleanedCount files")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup worker failed", e)
            Result.retry()
        }
    }
}
