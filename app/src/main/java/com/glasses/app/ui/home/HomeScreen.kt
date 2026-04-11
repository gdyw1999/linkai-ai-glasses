package com.glasses.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glasses.app.data.remote.sdk.MediaCaptureState
import com.glasses.app.viewmodel.HomeViewModel
import com.glasses.app.viewmodel.HomeViewModelFactory

/**
 * 首页屏幕
 * 显示设备连接状态、电量、快捷功能按钮
 */
@Composable
fun HomeScreen(
    innerPadding: PaddingValues = PaddingValues(),
    onNavigateToDeviceScan: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    var showDeviceDetailsDialog by remember { mutableStateOf(false) }
    var hasRequestedBatteryOptimization by remember { mutableStateOf(false) }
    
    // 当设备首次连接时，请求后台保活权限
    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected && !hasRequestedBatteryOptimization) {
            hasRequestedBatteryOptimization = true
            viewModel.requestBackgroundPermissions()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
    ) {
        // 顶部状态栏
        TopStatusBar(
            isConnected = uiState.isConnected,
            deviceName = uiState.deviceName,
            batteryLevel = uiState.batteryLevel,
            isCharging = isCharging
        )
        
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // 低电量警告
            if (uiState.isConnected && uiState.batteryLevel in 1..20 && !isCharging) {
                LowBatteryWarningCard(batteryLevel = uiState.batteryLevel)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 连接状态卡片
            ConnectionStatusCard(
                isConnected = uiState.isConnected,
                deviceName = uiState.deviceName,
                batteryLevel = uiState.batteryLevel,
                isCharging = isCharging,
                onScanClick = onNavigateToDeviceScan,
                isScanning = uiState.isScanning,
                onDeviceClick = { showDeviceDetailsDialog = true }
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // 状态信息（放在快捷功能上方，错误提示更容易被看到）
            if (uiState.statusMessage.isNotEmpty()) {
                StatusMessageCard(message = uiState.statusMessage)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 快捷功能区标题
            Text(
                text = "快捷功能",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 功能按钮网格
            QuickActionGrid(
                isConnected = uiState.isConnected,
                captureState = uiState.captureState,
                onTakePhoto = { viewModel.takePhoto() },
                onStartVideo = { viewModel.startVideo() },
                onStopVideo = { viewModel.stopVideo() },
                onStartAudio = { viewModel.startAudio() },
                onStopAudio = { viewModel.stopAudio() },
                onAIRecognition = { viewModel.startAIRecognition() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // 录制时长显示
            if (uiState.isRecording) {
                RecordingDurationCard(duration = uiState.recordingDuration)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // 设备详情对话框
    if (showDeviceDetailsDialog && uiState.isConnected) {
        DeviceDetailsDialog(
            deviceName = uiState.deviceName,
            batteryLevel = uiState.batteryLevel,
            isCharging = isCharging,
            onDismiss = { showDeviceDetailsDialog = false }
        )
    }
}

/**
 * 顶部状态栏
 */
@Composable
fun TopStatusBar(
    isConnected: Boolean,
    deviceName: String,
    batteryLevel: Int,
    isCharging: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
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
                    text = "Linkai星韵",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
                Text(
                    text = "AI眼镜控制",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConnected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "已连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "未连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 连接状态卡片
 */
@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    deviceName: String,
    batteryLevel: Int,
    isCharging: Boolean = false,
    onScanClick: () -> Unit,
    isScanning: Boolean,
    onDeviceClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                Color(0xFFE8F5E9) 
            else 
                Color(0xFFFFF3E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "设备状态",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isConnected) "已连接" else "未连接",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) 
                            Color(0xFF2E7D32) 
                        else 
                            Color(0xFFE65100),
                        fontSize = 16.sp
                    )
                }
                
                Icon(
                    imageVector = if (isConnected) 
                        Icons.Default.CheckCircle 
                    else 
                        Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isConnected) 
                        Color(0xFF2E7D32) 
                    else 
                        Color(0xFFE65100),
                    modifier = Modifier.size(40.dp)
                )
            }
            
            if (isConnected) {
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFCCE7CC))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDeviceClick)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "设备名称",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "电量",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 电池图标 - 根据充电状态和电量显示不同图标
                            Icon(
                                imageVector = when {
                                    isCharging -> Icons.Default.Star
                                    batteryLevel > 20 -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Warning
                                },
                                contentDescription = if (isCharging) "充电中" else "电量",
                                tint = when {
                                    isCharging -> Color(0xFFFFA726) // 橙色 - 充电中
                                    batteryLevel > 20 -> Color(0xFF2E7D32) // 绿色 - 正常
                                    else -> Color(0xFFC62828) // 红色 - 低电量
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$batteryLevel%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (batteryLevel <= 20 && !isCharging) Color(0xFFC62828) else Color.Unspecified
                            )
                            // 充电中文字提示
                            if (isCharging) {
                                Text(
                                    text = "充电中",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFA726),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(44.dp),
                    enabled = !isScanning,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    }
                    Text(
                        if (isScanning) "扫描中..." else "扫描设备",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * 快捷功能网格
 */
@Composable
fun QuickActionGrid(
    isConnected: Boolean,
    captureState: MediaCaptureState,
    onTakePhoto: () -> Unit,
    onStartVideo: () -> Unit,
    onStopVideo: () -> Unit,
    onStartAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onAIRecognition: () -> Unit
) {
    val isVideoRecording = captureState == MediaCaptureState.RECORDING_VIDEO
    val isAudioRecording = captureState == MediaCaptureState.RECORDING_AUDIO
    val canStartNewCapture = captureState == MediaCaptureState.IDLE

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 第一行：拍照、录像
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "拍照",
                enabled = isConnected,
                onClick = onTakePhoto,
                backgroundColor = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.PlayArrow,
                label = if (isVideoRecording) "停止录像" else "录像",
                enabled = isConnected && (isVideoRecording || canStartNewCapture),
                onClick = if (isVideoRecording) onStopVideo else onStartVideo,
                isActive = isVideoRecording,
                backgroundColor = Color(0xFFFF5722),
                modifier = Modifier.weight(1f)
            )
        }
        
        // 第二行：录音、智能识图
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Settings,
                label = if (isAudioRecording) "停止录音" else "录音",
                enabled = isConnected && (isAudioRecording || canStartNewCapture),
                onClick = if (isAudioRecording) onStopAudio else onStartAudio,
                isActive = isAudioRecording,
                backgroundColor = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Star,
                label = "智能识图",
                enabled = isConnected && canStartNewCapture,
                onClick = onAIRecognition,
                backgroundColor = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 快捷功能按钮
 */
@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isActive: Boolean = false,
    backgroundColor: Color = Color(0xFF2196F3),
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) 
                Color(0xFFE53935)
            else 
                backgroundColor,
            disabledContainerColor = Color(0xFFE0E0E0)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(36.dp),
                tint = if (enabled) Color.White else Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) Color.White else Color(0xFF999999),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 状态信息卡片
 */
@Composable
fun StatusMessageCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                color = Color(0xFF424242),
                softWrap = true,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * 录制时长卡片
 */
@Composable
fun RecordingDurationCard(duration: Long) {
    val seconds = duration / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    val timeString = String.format("%02d:%02d", minutes, remainingSeconds)
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "录制中: $timeString",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828),
                fontSize = 14.sp
            )
        }
    }
}


/**
 * 低电量警告卡片
 */
@Composable
fun LowBatteryWarningCard(batteryLevel: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE) // 浅红色背景
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "低电量警告",
                tint = Color(0xFFC62828),
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "电量不足",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "当前电量仅剩 $batteryLevel%，请及时充电",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF424242),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 设备详情对话框
 * 显示设备的详细信息：MAC地址、版本、电量等
 */
@Composable
fun DeviceDetailsDialog(
    deviceName: String,
    batteryLevel: Int,
    isCharging: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // 从SDK获取设备信息
    val sdkManager = remember { com.glasses.app.data.remote.sdk.GlassesSDKManager.getInstance(context) }
    val currentDevice = sdkManager.getCurrentDevice()
    val deviceAddress = currentDevice?.address ?: "未知"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设备详情",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已连接",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 设备名称
                DeviceDetailItem(
                    label = "设备名称",
                    value = deviceName,
                    icon = Icons.Default.Settings
                )
                
                Divider(color = Color(0xFFE0E0E0))
                
                // MAC地址
                DeviceDetailItem(
                    label = "MAC地址",
                    value = deviceAddress,
                    icon = Icons.Default.Info,
                    copyable = true
                )
                
                Divider(color = Color(0xFFE0E0E0))
                
                // 电量信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "电量",
                            tint = when {
                                isCharging -> Color(0xFFFFA726)
                                batteryLevel > 20 -> Color(0xFF4CAF50)
                                else -> Color(0xFFC62828)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "电量",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF666666),
                                fontSize = 12.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$batteryLevel%",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (batteryLevel <= 20 && !isCharging) Color(0xFFC62828) else Color(0xFF424242)
                                )
                                if (isCharging) {
                                    Text(
                                        text = "充电中",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFA726),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                Divider(color = Color(0xFFE0E0E0))
                
                // 设备型号
                DeviceDetailItem(
                    label = "设备型号",
                    value = "Linkai星韵AI眼镜",
                    icon = Icons.Default.Star
                )
                
                Divider(color = Color(0xFFE0E0E0))
                
                // SDK版本
                DeviceDetailItem(
                    label = "SDK版本",
                    value = "1.1.0",
                    icon = Icons.Default.Info
                )
                
                Divider(color = Color(0xFFE0E0E0))
                
                // 固件版本
                DeviceDetailItem(
                    label = "固件版本",
                    value = "待获取",
                    icon = Icons.Default.Build
                )
                
                // 提示信息
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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
                            text = "点击MAC地址可复制到剪贴板",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("关闭", fontSize = 14.sp)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 设备详情项
 */
@Composable
fun DeviceDetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    copyable: Boolean = false
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (copyable) {
                    Modifier.clickable {
                        // 复制到剪贴板
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(label, value)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "已复制: $value", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF666666),
                    fontSize = 12.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF424242)
                )
            }
        }
        
        if (copyable) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "复制",
                tint = Color(0xFF999999),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
