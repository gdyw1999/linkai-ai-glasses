package com.glasses.app.data.local.media

import java.io.File

/**
 * 媒体文件类型
 */
enum class MediaType {
    IMAGE,    // 图片
    VIDEO,    // 视频
    AUDIO,    // 录音
}

/**
 * 媒体文件数据类
 */
data class MediaFile(
    val id: String,                    // 唯一标识
    val fileName: String,              // 文件名
    val filePath: String,              // 本地文件路径
    val type: MediaType,               // 媒体类型
    val size: Long,                    // 文件大小（字节）
    val duration: Long = 0L,           // 时长（毫秒，仅用于视频和音频）
    val createTime: Long,              // 创建时间（毫秒）
    val thumbnailPath: String? = null, // 缩略图路径
)

/**
 * 媒体同步进度
 */
data class SyncProgress(
    val currentIndex: Int = 0,         // 当前同步文件索引
    val totalCount: Int = 0,           // 总文件数
    val currentFileName: String = "",  // 当前同步文件名
    val currentProgress: Int = 0,      // 当前文件进度 (0-100)
    val isComplete: Boolean = false,   // 是否完成
    val hasError: Boolean = false,     // 是否有错误
    val errorMessage: String = "",     // 错误信息
) {
    val overallProgress: Int
        get() {
            if (totalCount == 0) return 0
            return ((currentIndex * 100 + currentProgress) / totalCount).coerceIn(0, 100)
        }
}
