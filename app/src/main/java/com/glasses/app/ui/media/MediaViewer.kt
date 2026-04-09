package com.glasses.app.ui.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * 图片查看器
 */
@Composable
fun ImageViewer(
    imageUri: Uri,
    fileName: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }
        
        Text(
            text = fileName,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

/**
 * 视频播放器
 */
@Composable
fun VideoPlayer(
    videoUri: Uri,
    fileName: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 使用Android原生VideoView的Compose包装
        AndroidVideoView(
            uri = videoUri,
            modifier = Modifier.fillMaxSize()
        )
        
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }
        
        Text(
            text = fileName,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

/**
 * 音频播放器
 */
@Composable
fun AudioPlayer(
    audioUri: Uri,
    fileName: String,
    onClose: () -> Unit
) {
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
            // 音频播放器UI
            AndroidAudioPlayer(
                uri = audioUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = fileName,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }
    }
}

/**
 * Android原生VideoView的Compose包装
 */
@Composable
fun AndroidVideoView(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    // 这里需要使用AndroidView来包装原生VideoView
    // 实现细节在实际项目中需要根据具体需求调整
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "视频播放器\n${uri.lastPathSegment}",
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Android原生MediaPlayer的Compose包装
 */
@Composable
fun AndroidAudioPlayer(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    // 这里需要使用AndroidView来包装原生MediaPlayer控制UI
    // 实现细节在实际项目中需要根据具体需求调整
    Box(
        modifier = modifier
            .background(Color.DarkGray)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "音频播放器\n${uri.lastPathSegment}",
            color = Color.White
        )
    }
}
