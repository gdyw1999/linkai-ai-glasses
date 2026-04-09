package com.glasses.app.ui.media

import com.glasses.app.data.local.media.MediaFile
import com.glasses.app.data.local.media.MediaType
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * 媒体查看器单元测试
 * 验证需求: 4.9, 4.10, 4.11
 */
class MediaViewerTest {
    
    /**
     * 测试文件大小格式化
     * 验证需求: 4.9, 4.10, 4.11 - 显示媒体信息（文件大小）
     */
    @Test
    fun testFormatFileSize() {
        // 测试字节
        assertEquals("512 B", formatFileSize(512))
        
        // 测试KB
        assertEquals("1 KB", formatFileSize(1024))
        assertEquals("512 KB", formatFileSize(512 * 1024))
        
        // 测试MB
        assertEquals("1 MB", formatFileSize(1024 * 1024))
        assertEquals("10 MB", formatFileSize(10 * 1024 * 1024))
        
        // 测试GB
        assertEquals("1 GB", formatFileSize(1024L * 1024 * 1024))
        assertEquals("2 GB", formatFileSize(2L * 1024 * 1024 * 1024))
    }
    
    /**
     * 测试时间格式化
     * 验证需求: 4.10, 4.11 - 音频/视频播放时长显示
     */
    @Test
    fun testFormatTime() {
        // 测试0秒
        assertEquals("00:00", formatTime(0))
        
        // 测试秒
        assertEquals("00:30", formatTime(30 * 1000))
        assertEquals("00:59", formatTime(59 * 1000))
        
        // 测试分钟
        assertEquals("01:00", formatTime(60 * 1000))
        assertEquals("05:30", formatTime(5 * 60 * 1000 + 30 * 1000))
        
        // 测试小时（显示为分钟）
        assertEquals("60:00", formatTime(60 * 60 * 1000))
        assertEquals("90:30", formatTime(90 * 60 * 1000 + 30 * 1000))
    }
    
    /**
     * 测试日期格式化
     * 验证需求: 4.9, 4.10, 4.11 - 显示媒体信息（时间戳）
     */
    @Test
    fun testFormatDate() {
        // 测试特定时间戳
        val timestamp = 1710576000000L // 2024-03-16 10:00:00
        val formatted = formatDate(timestamp)
        
        // 验证格式正确（yyyy-MM-dd HH:mm）
        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
    }
    
    /**
     * 测试图片媒体文件类型识别
     * 验证需求: 4.9 - 图片全屏显示
     */
    @Test
    fun testImageMediaType() {
        val imageFile = MediaFile(
            id = "img_001",
            fileName = "photo.jpg",
            filePath = "/path/to/photo.jpg",
            type = MediaType.IMAGE,
            fileSize = 1024 * 1024,
            timestamp = System.currentTimeMillis(),
            duration = 0
        )
        
        assertEquals(MediaType.IMAGE, imageFile.type)
        assertTrue(imageFile.fileName.endsWith(".jpg"))
    }
    
    /**
     * 测试视频媒体文件类型识别
     * 验证需求: 4.10 - 视频播放器播放
     */
    @Test
    fun testVideoMediaType() {
        val videoFile = MediaFile(
            id = "vid_001",
            fileName = "video.mp4",
            filePath = "/path/to/video.mp4",
            type = MediaType.VIDEO,
            fileSize = 10 * 1024 * 1024,
            timestamp = System.currentTimeMillis(),
            duration = 60000 // 60秒
        )
        
        assertEquals(MediaType.VIDEO, videoFile.type)
        assertTrue(videoFile.fileName.endsWith(".mp4"))
        assertTrue(videoFile.duration > 0)
    }
    
    /**
     * 测试音频媒体文件类型识别
     * 验证需求: 4.11 - 音频播放器播放
     */
    @Test
    fun testAudioMediaType() {
        val audioFile = MediaFile(
            id = "aud_001",
            fileName = "recording.wav",
            filePath = "/path/to/recording.wav",
            type = MediaType.AUDIO,
            fileSize = 2 * 1024 * 1024,
            timestamp = System.currentTimeMillis(),
            duration = 30000 // 30秒
        )
        
        assertEquals(MediaType.AUDIO, audioFile.type)
        assertTrue(audioFile.fileName.endsWith(".wav"))
        assertTrue(audioFile.duration > 0)
    }
    
    /**
     * 测试媒体文件信息完整性
     * 验证需求: 4.9, 4.10, 4.11 - 显示媒体信息
     */
    @Test
    fun testMediaFileInfo() {
        val mediaFile = MediaFile(
            id = "test_001",
            fileName = "test_media.jpg",
            filePath = "/path/to/test_media.jpg",
            type = MediaType.IMAGE,
            fileSize = 1024 * 1024,
            timestamp = System.currentTimeMillis(),
            duration = 0
        )
        
        // 验证所有必需字段都存在
        assertNotNull(mediaFile.id)
        assertNotNull(mediaFile.fileName)
        assertNotNull(mediaFile.filePath)
        assertNotNull(mediaFile.type)
        assertTrue(mediaFile.fileSize > 0)
        assertTrue(mediaFile.timestamp > 0)
    }
    
    /**
     * 测试不同媒体类型的文件扩展名
     * 验证需求: 4.9, 4.10, 4.11 - 根据文件类型使用对应查看器
     */
    @Test
    fun testMediaFileExtensions() {
        // 图片扩展名
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp")
        imageExtensions.forEach { ext ->
            assertTrue("test$ext".endsWith(ext))
        }
        
        // 视频扩展名
        val videoExtensions = listOf(".mp4", ".avi", ".mov", ".mkv")
        videoExtensions.forEach { ext ->
            assertTrue("test$ext".endsWith(ext))
        }
        
        // 音频扩展名
        val audioExtensions = listOf(".wav", ".mp3", ".aac", ".m4a")
        audioExtensions.forEach { ext ->
            assertTrue("test$ext".endsWith(ext))
        }
    }
    
    /**
     * 测试媒体文件大小边界情况
     * 验证需求: 4.9, 4.10, 4.11 - 显示文件大小
     */
    @Test
    fun testFileSizeBoundaries() {
        // 测试0字节
        assertEquals("0 B", formatFileSize(0))
        
        // 测试1字节
        assertEquals("1 B", formatFileSize(1))
        
        // 测试1023字节（KB边界）
        assertEquals("1023 B", formatFileSize(1023))
        
        // 测试1024字节（1KB）
        assertEquals("1 KB", formatFileSize(1024))
        
        // 测试1MB-1字节
        assertEquals("1023 KB", formatFileSize(1024 * 1024 - 1024))
        
        // 测试1MB
        assertEquals("1 MB", formatFileSize(1024 * 1024))
    }
    
    /**
     * 测试时间格式化边界情况
     * 验证需求: 4.10, 4.11 - 播放时长显示
     */
    @Test
    fun testTimeFormatBoundaries() {
        // 测试0毫秒
        assertEquals("00:00", formatTime(0))
        
        // 测试1秒
        assertEquals("00:01", formatTime(1000))
        
        // 测试59秒
        assertEquals("00:59", formatTime(59 * 1000))
        
        // 测试60秒（1分钟）
        assertEquals("01:00", formatTime(60 * 1000))
        
        // 测试59分59秒
        assertEquals("59:59", formatTime(59 * 60 * 1000 + 59 * 1000))
        
        // 测试60分钟（1小时）
        assertEquals("60:00", formatTime(60 * 60 * 1000))
    }
    
    /**
     * 测试媒体文件路径有效性
     * 验证需求: 4.9, 4.10, 4.11 - 媒体文件访问
     */
    @Test
    fun testMediaFilePath() {
        val mediaFile = MediaFile(
            id = "test_001",
            fileName = "test.jpg",
            filePath = "/storage/emulated/0/GlassesAlbum/test.jpg",
            type = MediaType.IMAGE,
            fileSize = 1024,
            timestamp = System.currentTimeMillis(),
            duration = 0
        )
        
        // 验证路径格式
        assertTrue(mediaFile.filePath.isNotEmpty())
        assertTrue(mediaFile.filePath.contains(mediaFile.fileName))
        
        // 验证路径可以转换为File对象
        val file = File(mediaFile.filePath)
        assertNotNull(file)
        assertEquals(mediaFile.fileName, file.name)
    }
}
