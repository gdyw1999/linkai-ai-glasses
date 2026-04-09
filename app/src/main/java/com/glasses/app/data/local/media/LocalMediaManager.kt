package com.glasses.app.data.local.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 本地媒体管理器
 * 管理本地存储的媒体文件、缩略图生成和媒体查询
 */
class LocalMediaManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "LocalMediaManager"
        private var instance: LocalMediaManager? = null
        
        fun getInstance(context: Context): LocalMediaManager {
            return instance ?: synchronized(this) {
                instance ?: LocalMediaManager(context).also { instance = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val imageLoader = ImageLoader(context)
    
    /**
     * 生成图片缩略图
     */
    fun generateImageThumbnail(
        imagePath: String,
        thumbnailSize: Int = 200,
        onResult: (success: Boolean, thumbnailPath: String?) -> Unit
    ) {
        scope.launch {
            try {
                val imageFile = File(imagePath)
                if (!imageFile.exists()) {
                    onResult(false, null)
                    return@launch
                }
                
                val request = ImageRequest.Builder(context)
                    .data(imageFile)
                    .size(thumbnailSize, thumbnailSize)
                    .build()
                
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    val bitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        // 如果不是BitmapDrawable，创建一个新的Bitmap
                        val bmp = Bitmap.createBitmap(
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bmp)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bmp
                    }
                    val thumbnailPath = saveThumbnail(bitmap, imageFile.nameWithoutExtension)
                    onResult(true, thumbnailPath)
                } else {
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "生成图片缩略图失败: ${e.message}")
                onResult(false, null)
            }
        }
    }
    
    /**
     * 生成视频缩略图
     */
    fun generateVideoThumbnail(
        videoPath: String,
        onResult: (success: Boolean, thumbnailPath: String?) -> Unit
    ) {
        scope.launch {
            try {
                val videoFile = File(videoPath)
                if (!videoFile.exists()) {
                    onResult(false, null)
                    return@launch
                }
                
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoPath)
                
                val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
                retriever.release()
                
                if (bitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                    val thumbnailPath = saveThumbnail(scaledBitmap, videoFile.nameWithoutExtension)
                    onResult(true, thumbnailPath)
                } else {
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "生成视频缩略图失败: ${e.message}")
                onResult(false, null)
            }
        }
    }
    
    /**
     * 保存缩略图
     */
    private fun saveThumbnail(bitmap: Bitmap, fileName: String): String {
        val thumbnailDir = File(context.cacheDir, "thumbnails")
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdirs()
        }
        
        val thumbnailFile = File(thumbnailDir, "${fileName}_thumb.jpg")
        FileOutputStream(thumbnailFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
        }
        
        return thumbnailFile.absolutePath
    }
    
    /**
     * 获取媒体文件的时长（仅用于视频和音频）
     */
    fun getMediaDuration(mediaPath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(mediaPath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "获取媒体时长失败: ${e.message}")
            0L
        }
    }
    
    /**
     * 获取媒体文件大小
     */
    fun getMediaFileSize(mediaPath: String): Long {
        return try {
            File(mediaPath).length()
        } catch (e: Exception) {
            Log.e(TAG, "获取媒体文件大小失败: ${e.message}")
            0L
        }
    }
    
    /**
     * 检查媒体文件是否存在
     */
    fun mediaFileExists(mediaPath: String): Boolean {
        return File(mediaPath).exists()
    }
    
    /**
     * 删除媒体文件
     */
    fun deleteMediaFile(mediaPath: String): Boolean {
        return try {
            File(mediaPath).delete()
        } catch (e: Exception) {
            Log.e(TAG, "删除媒体文件失败: ${e.message}")
            false
        }
    }
    
    /**
     * 删除缩略图
     */
    fun deleteThumbnail(thumbnailPath: String): Boolean {
        return try {
            File(thumbnailPath).delete()
        } catch (e: Exception) {
            Log.e(TAG, "删除缩略图失败: ${e.message}")
            false
        }
    }
    
    /**
     * 获取缓存目录中的所有缩略图
     */
    fun getAllThumbnails(): List<File> {
        return try {
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (thumbnailDir.exists()) {
                thumbnailDir.listFiles()?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取缩略图列表失败: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 清空所有缩略图缓存
     */
    fun clearThumbnailCache(): Boolean {
        return try {
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (thumbnailDir.exists()) {
                thumbnailDir.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "清空缩略图缓存失败: ${e.message}")
            false
        }
    }
}
