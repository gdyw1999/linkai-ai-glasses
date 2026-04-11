package com.glasses.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameras
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glasses.app.data.local.media.MediaFile
import java.io.File

// 颜色常量
private val TextPrimary = Color(0xFF424242)
private val IconColor = Color(0xFF757575)
private val DragHandleColor = Color(0xFFBDBDBD)
private val SurfaceColor = Color(0xFFF5F5F5)
private val SelectedColor = Color(0xFFE3F2FD)
private val ImagePlaceholderColor = Color(0xFFEEEEEE)

/**
 * 图片来源枚举
 */
enum class ImageSource {
    ALBUM_PHONE,    // 手机相册
    ALBUM_GLASSES,  // 眼镜相册
    CAMERA_PHONE,   // 手机拍照
    CAMERA_GLASSES  // 眼镜拍照
}

/**
 * 图片来源项数据类
 */
data class ImageSourceItem(
    val source: ImageSource,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

// 四个功能入口
private val imageSources = listOf(
    ImageSourceItem(ImageSource.ALBUM_PHONE, Icons.Default.Photo, "手机相册"),
    ImageSourceItem(ImageSource.ALBUM_GLASSES, Icons.Default.Image, "眼镜相册"),
    ImageSourceItem(ImageSource.CAMERA_PHONE, Icons.Default.CameraAlt, "手机拍照"),
    ImageSourceItem(ImageSource.CAMERA_GLASSES, Icons.Default.Cameras, "眼镜拍照")
)

/**
 * 图片来源底部弹出面板
 * 包含功能网格入口和最近图片选择
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceBottomSheet(
    sheetState: SheetState,
    recentImages: List<MediaFile>,
    onDismiss: () -> Unit,
    onSourceSelected: (ImageSource) -> Unit,
    onRecentImageSelected: (MediaFile) -> Unit,
    modifier: Modifier = Modifier
) {

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            // 自定义拖拽手柄 - 三个小圆点
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                DragHandle()
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        containerColor = Color.White,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // 功能网格 - 2x2 排列
            ImageSourceGrid(
                items = imageSources,
                onItemClick = onSourceSelected
            )

            // 最近图片网格（横向滚动）
            if (recentImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "最近图片",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                RecentImagesGrid(
                    recentImages = recentImages.take(8),
                    onImageClick = onRecentImageSelected
                )
            }
        }
    }
}

/**
 * 拖拽手柄 - 三个小圆点
 */
@Composable
fun DragHandle() {
    Row(
        modifier = Modifier
            .width(40.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(DragHandleColor),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(DragHandleColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * 图片来源 2x2 网格
 */
@Composable
fun ImageSourceGrid(
    items: List<ImageSourceItem>,
    onItemClick: (ImageSource) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 第一行：手机相册、眼镜相册
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.take(2).forEach { item ->
                ImageSourceButton(
                    item = item,
                    onClick = { onItemClick(item.source) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // 第二行：手机拍照、眼镜拍照
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.drop(2).forEach { item ->
                ImageSourceButton(
                    item = item,
                    onClick = { onItemClick(item.source) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 图片来源按钮
 */
@Composable
fun ImageSourceButton(
    item: ImageSourceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = modifier
            .size(80.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isPressed) SelectedColor else SurfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = IconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 最近图片网格 - 横向滚动
 */
@Composable
fun RecentImagesGrid(
    recentImages: List<MediaFile>,
    onImageClick: (MediaFile) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(recentImages, key = { it.id }) { mediaFile ->
            RecentImageItem(
                mediaFile = mediaFile,
                onClick = { onImageClick(mediaFile) }
            )
        }
    }
}

/**
 * 最近图片项
 */
@Composable
fun RecentImageItem(
    mediaFile: MediaFile,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ImagePlaceholderColor)
            .clickable(onClick = onClick)
    ) {
        val imagePath = mediaFile.thumbnailPath ?: mediaFile.filePath

        // 使用 Coil 加载图片
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(imagePath))
                .crossfade(true)
                .build(),
            contentDescription = mediaFile.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
