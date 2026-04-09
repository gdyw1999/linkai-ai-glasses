package com.glasses.app.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 权限请求界面
 * 显示需要的权限及其用途说明
 */
@Composable
fun PermissionScreen(
    onRequestPermissions: () -> Unit,
    onSkip: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // 标题
        Text(
            text = "需要您的授权",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "为了正常使用眼镜功能，我们需要以下权限",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 权限列表
        PermissionItem(
            icon = Icons.Default.Settings,
            title = "蓝牙权限",
            description = "用于扫描和连接AI眼镜设备",
            color = Color(0xFF2196F3)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PermissionItem(
            icon = Icons.Default.Place,
            title = "位置权限",
            description = "Android系统要求，用于蓝牙设备扫描",
            color = Color(0xFF4CAF50)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PermissionItem(
            icon = Icons.Default.Star,
            title = "存储权限",
            description = "用于保存和查看照片、视频、录音文件",
            color = Color(0xFF9C27B0)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PermissionItem(
            icon = Icons.Default.Settings,
            title = "麦克风权限",
            description = "用于语音对话和录音功能",
            color = Color(0xFFFF5722)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 授权按钮
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Text(
                text = "授予权限",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 说明文字
        Text(
            text = "我们承诺保护您的隐私，不会收集或上传您的个人信息",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * 权限项
 */
@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F1F1F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * 权限被拒绝的对话框
 */
@Composable
fun PermissionDeniedDialog(
    deniedPermissions: List<String>,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "权限被拒绝",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "以下权限被拒绝，部分功能可能无法正常使用：",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                deniedPermissions.forEach { permission ->
                    Text(
                        text = "• ${getPermissionName(permission)}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "您可以在设置中手动开启这些权限",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("去设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}

/**
 * 获取权限的友好名称
 */
private fun getPermissionName(permission: String): String {
    return when {
        permission.contains("BLUETOOTH") -> "蓝牙权限"
        permission.contains("LOCATION") -> "位置权限"
        permission.contains("STORAGE") || permission.contains("MEDIA") -> "存储权限"
        permission.contains("AUDIO") -> "麦克风权限"
        else -> permission
    }
}
