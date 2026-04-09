package com.glasses.app.ui.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.glasses.app.data.local.media.MediaFile
import com.glasses.app.data.local.media.MediaType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 媒体查看器屏幕
 * 支持图片全屏查看（缩放、滑动）、视频播放、音频播放
 * 显示媒体信息、支持分享和删除操作
 */
@Composable
fun MediaViewerScreen(
    mediaFile: MediaFile,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    when (mediaFile.type) {
        MediaType.IMAGE -> ImageViewerScreen(
            mediaFile = mediaFile,
            onClose = onClose,
            onDelete = onDelete,
            onShare = onShare
        )
        MediaType.VIDEO -> VideoPlayerScreen(
            mediaFile = mediaFile,
            onClose = onClose,
            onDelete = onDelete,
            onShare = onShare
        )
        MediaType.AUDIO -> AudioPlayerScreen(
            mediaFile = mediaFile,
            onClose = onClose,
            onDelete = onDelete,
            onShare = onShare
        )
    }
}

/**
 * 图片全屏查看器
 * 支持缩放和平移手势
 */
@Composable
fun ImageViewerScreen(
    mediaFile: MediaFile,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showInfo by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 图片显示区域
        AsyncImage(
            model = File(mediaFile.filePath),
            contentDescription = mediaFile.fileName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentScale = ContentScale.Fit
        )
        
        // 顶部工具栏
        if (showInfo) {
            MediaTopBar(
                onClose = onClose,
                onDelete = onDelete,
                onShare = onShare
            )
        }
        
        // 底部信息栏
        if (showInfo) {
            MediaInfoBar(
                mediaFile = mediaFile,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 视频播放器屏幕
 */
@Composable
fun VideoPlayerScreen(
    mediaFile: MediaFile,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(true) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(mediaFile.filePath)))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 视频播放器
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerShowTimeoutMs = 5000
                    controllerHideOnTouch = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 顶部工具栏
        if (showInfo) {
            MediaTopBar(
                onClose = {
                    exoPlayer.pause()
                    onClose()
                },
                onDelete = {
                    exoPlayer.pause()
                    onDelete()
                },
                onShare = onShare
            )
        }
        
        // 底部信息栏
        if (showInfo) {
            MediaInfoBar(
                mediaFile = mediaFile,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 音频播放器屏幕
 */
@Composable
fun AudioPlayerScreen(
    mediaFile: MediaFile,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(mediaFile.filePath)))
            setMediaItem(mediaItem)
            prepare()
        }
    }
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    
    // 监听播放状态
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
    }
    
    // 更新播放进度
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(100)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 音频图标
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "音频",
                tint = Color.White,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 文件名
            Text(
                text = mediaFile.fileName,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 播放进度条
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { value ->
                        exoPlayer.seekTo((value * duration).toLong())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 播放控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 后退10秒
                IconButton(
                    onClick = {
                        exoPlayer.seekTo((currentPosition - 10000).coerceAtLeast(0))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "后退10秒",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // 播放/暂停
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // 前进10秒
                IconButton(
                    onClick = {
                        exoPlayer.seekTo((currentPosition + 10000).coerceAtMost(duration))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "前进10秒",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // 顶部工具栏
        MediaTopBar(
            onClose = {
                exoPlayer.pause()
                onClose()
            },
            onDelete = {
                exoPlayer.pause()
                onDelete()
            },
            onShare = onShare
        )
        
        // 底部信息栏
        MediaInfoBar(
            mediaFile = mediaFile,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * 顶部工具栏
 */
@Composable
fun MediaTopBar(
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 关闭按钮
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 分享按钮
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "分享",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 删除按钮
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * 底部信息栏
 */
@Composable
fun MediaInfoBar(
    mediaFile: MediaFile,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 文件名
            Text(
                text = mediaFile.fileName,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 文件信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = formatFileSize(mediaFile.size),
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp
                )
                Text(
                    text = formatDate(mediaFile.createTime),
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 格式化文件大小
 */
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * 格式化日期
 */
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 格式化时间（毫秒转 mm:ss）
 */
fun formatTime(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
