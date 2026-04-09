package com.glasses.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * 媒体播放工具类
 * 在APP内部播放媒体文件
 */
object MediaPlayerUtil {
    
    private const val TAG = "MediaPlayerUtil"
    
    /**
     * 获取图片URI
     */
    fun getImageUri(context: Context, imagePath: String): Uri? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e(TAG, "图片文件不存在: $imagePath")
                return null
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            Log.e(TAG, "获取图片URI失败: ${e.message}")
            null
        }
    }
    
    /**
     * 获取视频URI
     */
    fun getVideoUri(context: Context, videoPath: String): Uri? {
        return try {
            val file = File(videoPath)
            if (!file.exists()) {
                Log.e(TAG, "视频文件不存在: $videoPath")
                return null
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            Log.e(TAG, "获取视频URI失败: ${e.message}")
            null
        }
    }
    
    /**
     * 获取音频URI
     */
    fun getAudioUri(context: Context, audioPath: String): Uri? {
        return try {
            val file = File(audioPath)
            if (!file.exists()) {
                Log.e(TAG, "音频文件不存在: $audioPath")
                return null
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            Log.e(TAG, "获取音频URI失败: ${e.message}")
            null
        }
    }
    
    /**
     * 获取媒体文件URI（自动检测类型）
     */
    fun getMediaUri(context: Context, mediaPath: String): Uri? {
        return try {
            val file = File(mediaPath)
            if (!file.exists()) {
                Log.e(TAG, "媒体文件不存在: $mediaPath")
                return null
            }
            getFileUri(context, file)
        } catch (e: Exception) {
            Log.e(TAG, "获取媒体URI失败: ${e.message}")
            null
        }
    }
    
    /**
     * 获取文件的MIME类型
     */
    fun getMimeType(filePath: String): String {
        return when {
            filePath.endsWith(".jpg", ignoreCase = true) ||
            filePath.endsWith(".jpeg", ignoreCase = true) ||
            filePath.endsWith(".png", ignoreCase = true) ||
            filePath.endsWith(".gif", ignoreCase = true) ||
            filePath.endsWith(".bmp", ignoreCase = true) -> "image/*"
            
            filePath.endsWith(".mp4", ignoreCase = true) ||
            filePath.endsWith(".mov", ignoreCase = true) ||
            filePath.endsWith(".avi", ignoreCase = true) ||
            filePath.endsWith(".mkv", ignoreCase = true) ||
            filePath.endsWith(".flv", ignoreCase = true) -> "video/*"
            
            filePath.endsWith(".mp3", ignoreCase = true) ||
            filePath.endsWith(".wav", ignoreCase = true) ||
            filePath.endsWith(".aac", ignoreCase = true) ||
            filePath.endsWith(".flac", ignoreCase = true) ||
            filePath.endsWith(".m4a", ignoreCase = true) -> "audio/*"
            
            else -> "*/*"
        }
    }
    
    /**
     * 检测媒体类型
     */
    fun detectMediaType(filePath: String): String {
        return when {
            filePath.endsWith(".jpg", ignoreCase = true) ||
            filePath.endsWith(".jpeg", ignoreCase = true) ||
            filePath.endsWith(".png", ignoreCase = true) ||
            filePath.endsWith(".gif", ignoreCase = true) ||
            filePath.endsWith(".bmp", ignoreCase = true) -> "image"
            
            filePath.endsWith(".mp4", ignoreCase = true) ||
            filePath.endsWith(".mov", ignoreCase = true) ||
            filePath.endsWith(".avi", ignoreCase = true) ||
            filePath.endsWith(".mkv", ignoreCase = true) ||
            filePath.endsWith(".flv", ignoreCase = true) -> "video"
            
            filePath.endsWith(".mp3", ignoreCase = true) ||
            filePath.endsWith(".wav", ignoreCase = true) ||
            filePath.endsWith(".aac", ignoreCase = true) ||
            filePath.endsWith(".flac", ignoreCase = true) ||
            filePath.endsWith(".m4a", ignoreCase = true) -> "audio"
            
            else -> "unknown"
        }
    }
    
    /**
     * 获取文件URI
     * 使用FileProvider处理Android 7.0+的文件访问限制
     */
    private fun getFileUri(context: Context, file: File): Uri {
        return try {
            // 尝试使用FileProvider（推荐方式）
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            // 如果FileProvider未配置，直接使用file://URI
            Log.w(TAG, "FileProvider未配置，使用file://URI")
            Uri.fromFile(file)
        }
    }
}
