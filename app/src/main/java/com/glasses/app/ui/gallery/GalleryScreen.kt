package com.glasses.app.ui.gallery

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glasses.app.data.local.media.MediaFile
import com.glasses.app.data.local.media.MediaType
import com.glasses.app.ui.media.MediaViewerScreen
import com.glasses.app.viewmodel.GalleryMediaType
import com.glasses.app.viewmodel.GalleryViewModel
import com.glasses.app.viewmodel.GalleryViewModelFactory
import java.io.File

/**
 * 相册屏幕
 * 显示媒体网格、类型筛选、同步功能
 */
@Composable
fun GalleryScreen(
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModelFactory(LocalContext.current)),
    onMediaSelected: ((MediaFile) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val filteredMedia = viewModel.getFilteredMedia()
    
    // 显示媒体查看器
    if (uiState.isViewerOpen && uiState.selectedMedia != null) {
        MediaViewerScreen(
            mediaFile = uiState.selectedMedia!!,
            onClose = { viewModel.closeViewer() },
            onDelete = {
                viewModel.deleteMedia(uiState.selectedMedia!!)
                viewModel.closeViewer()
            },
            onShare = {
                shareMedia(context, uiState.selectedMedia!!)
            }
        )
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(innerPadding)
    ) {
        // 顶部栏
        if (onMediaSelected != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择图片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    TextButton(onClick = { onDismiss?.invoke() }) {
                        Text("取消", color = Color(0xFF757575))
                    }
                }
            }
        } else {
            GalleryTopBar(
                isSyncing = uiState.isSyncing,
                onSync = { viewModel.startSync() }
            )
        }
        
        // 类型筛选Tab
        MediaTypeTabBar(
            selectedType = uiState.selectedMediaType,
            onTypeSelected = { viewModel.filterByType(it) }
        )
        
        // 同步进度条
        if (uiState.isSyncing) {
            SyncProgressBar(progress = uiState.syncProgress)
        }
        
        // 媒体网格
        if (filteredMedia.isEmpty()) {
            EmptyMediaPlaceholder()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMedia) { media ->
                    MediaGridItem(
                        media = media,
                        onClick = {
                            if (onMediaSelected != null) {
                                onMediaSelected.invoke(media)
                            } else {
                                viewModel.openViewer(media)
                            }
                        }
                    )
                }
            }
        }
        
        // 状态提示
        if (uiState.statusMessage.isNotEmpty()) {
            StatusBar(message = uiState.statusMessage)
        }
    }
}

/**
 * 分享媒体文件
 */
fun shareMedia(context: android.content.Context, mediaFile: MediaFile) {
    try {
        val file = File(mediaFile.filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = when (mediaFile.type) {
                MediaType.IMAGE -> "image/*"
                MediaType.VIDEO -> "video/*"
                MediaType.AUDIO -> "audio/*"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "分享媒体"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 顶部栏
 */
@Composable
fun GalleryTopBar(
    isSyncing: Boolean,
    onSync: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        color = Color(0xFF1F1F1F),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "相册",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "媒体管理",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp
                )
            }
            
            Button(
                onClick = onSync,
                enabled = !isSyncing,
                modifier = Modifier
                    .height(40.dp)
                    .widthIn(min = 100.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFFCCCCCC)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 6.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (isSyncing) "同步中" else "同步",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 媒体类型Tab栏
 */
@Composable
fun MediaTypeTabBar(
    selectedType: GalleryMediaType,
    onTypeSelected: (GalleryMediaType) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaTypeTab(
                label = "全部",
                isSelected = selectedType == GalleryMediaType.ALL,
                onClick = { onTypeSelected(GalleryMediaType.ALL) }
            )
            MediaTypeTab(
                label = "图片",
                isSelected = selectedType == GalleryMediaType.IMAGE,
                onClick = { onTypeSelected(GalleryMediaType.IMAGE) }
            )
            MediaTypeTab(
                label = "视频",
                isSelected = selectedType == GalleryMediaType.VIDEO,
                onClick = { onTypeSelected(GalleryMediaType.VIDEO) }
            )
            MediaTypeTab(
                label = "音频",
                isSelected = selectedType == GalleryMediaType.AUDIO,
                onClick = { onTypeSelected(GalleryMediaType.AUDIO) }
            )
        }
    }
}

/**
 * 媒体类型Tab
 */
@Composable
fun MediaTypeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 70.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFFF0F0F0),
            contentColor = if (isSelected) Color.White else Color(0xFF424242)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

/**
 * 同步进度条
 */
@Composable
fun SyncProgressBar(progress: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "同步进度",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF2196F3)
            )
        }
        LinearProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(0xFF2196F3),
            trackColor = Color(0xFFE0E0E0)
        )
    }
}

/**
 * 媒体网格项
 * 图片：Coil 异步加载缩略图
 * 视频：背景色 + 播放图标 + 时长
 * 音频：背景色 + 音符图标 + 时长 + 文件名
 */
@Composable
fun MediaGridItem(
    media: MediaFile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE8E8E8)),
            contentAlignment = Alignment.Center
        ) {
            when (media.type) {
                MediaType.IMAGE -> {
                    // 图片：使用 Coil 异步加载，支持缩略图和原始路径
                    val imageModel = media.thumbnailPath?.let { File(it) }
                        ?.takeIf { it.exists() }
                        ?: File(media.filePath).takeIf { it.exists() }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = media.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                MediaType.VIDEO -> {
                    // 视频：显示播放图标 + 文件名 + 时长
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "视频",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(36.dp)
                    )
                    // 文件名
                    Text(
                        text = media.fileName,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MediaType.AUDIO -> {
                    // 音频：音符图标 + 文件名 + 时长
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "音频",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(32.dp)
                    )
                    // 文件名（去掉扩展名）
                    Text(
                        text = media.fileName.removeSuffix(".wav").removeSuffix(".opus"),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666),
                        fontSize = 8.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 视频/音频时长标签
            if (media.type == MediaType.VIDEO || media.type == MediaType.AUDIO) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = formatDuration(media.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(2.dp, 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * 空媒体占位符
 */
@Composable
fun EmptyMediaPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无媒体文件",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF999999),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击同步按钮获取眼镜中的媒体",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCCCCCC),
            fontSize = 12.sp
        )
    }
}

/**
 * 状态栏
 */
@Composable
fun StatusBar(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF424242),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 格式化时长
 */
fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
