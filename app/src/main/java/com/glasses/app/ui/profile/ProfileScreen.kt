package com.glasses.app.ui.profile

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glasses.app.data.remote.api.model.AliQwenVisionModels
import com.glasses.app.viewmodel.ProfileViewModel
import com.glasses.app.viewmodel.ProfileViewModelFactory

/**
 * 我的页面屏幕
 * 显示设置、关于、常见问题等
 */
@Composable
fun ProfileScreen(
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
    ) {
        // 顶部栏
        ProfileTopBar()
        
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // 设备信息卡片
            DeviceInfoCard(
                isConnected = uiState.deviceConnected,
                deviceName = uiState.deviceName,
                batteryLevel = uiState.batteryLevel,
                isCharging = isCharging
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 功能菜单
            Text(
                text = "功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            MenuItemCard(
                icon = Icons.Default.Info,
                title = "常见问题",
                subtitle = "查看常见问题和解答",
                onClick = { viewModel.showFAQ() }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MenuItemCard(
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "应用版本和开发者信息",
                onClick = { viewModel.showAbout() }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MenuItemCard(
                icon = Icons.Default.Refresh,
                title = "检查更新",
                subtitle = "检查是否有新版本",
                onClick = { viewModel.checkUpdate() }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MenuItemCard(
                icon = Icons.Default.Settings,
                title = "API配置",
                subtitle = "配置LinkAI、阿里Qwen和OpenClaw API Key",
                onClick = { viewModel.showApiConfig() }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MenuItemCard(
                icon = Icons.Default.Info,
                title = "查看日志",
                subtitle = "查看应用崩溃和调试日志",
                onClick = { viewModel.showCrashLog() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 设置菜单
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            MenuItemCard(
                icon = Icons.Default.Settings,
                title = "断开连接",
                subtitle = "断开与眼镜的蓝牙连接",
                onClick = { viewModel.disconnect() },
                isDestructive = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 状态提示
            if (uiState.statusMessage.isNotEmpty()) {
                StatusBar(message = uiState.statusMessage)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // 关于对话框
    if (uiState.showAboutDialog) {
        AboutDialog(
            appVersion = uiState.appVersion,
            sdkVersion = uiState.sdkVersion,
            onDismiss = { viewModel.hideAbout() }
        )
    }
    
    // 常见问题对话框
    if (uiState.showFAQDialog) {
        FAQDialog(
            onDismiss = { viewModel.hideFAQ() }
        )
    }
    
    // API配置对话框
    if (uiState.showApiConfigDialog) {
        ApiConfigDialog(
            onDismiss = { viewModel.hideApiConfig() },
            onSave = { voiceKey, chatKey, aliVisionKey, aliVisionModel, openclawKey, openclawAppId, linkaiAppCode ->
                viewModel.saveApiConfig(voiceKey, chatKey, aliVisionKey, aliVisionModel, openclawKey, openclawAppId, linkaiAppCode)
            }
        )
    }
    
    // 崩溃日志对话框
    if (uiState.showCrashLogDialog) {
        CrashLogDialog(
            onDismiss = { viewModel.hideCrashLog() }
        )
    }
}

/**
 * 顶部栏
 */
@Composable
fun ProfileTopBar() {
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
                    text = "我的",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "设置和关于",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 设备信息卡片
 */
@Composable
fun DeviceInfoCard(
    isConnected: Boolean,
    deviceName: String,
    batteryLevel: Int = 0,
    isCharging: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设备状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                Surface(
                    color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isConnected) "已连接" else "未连接",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            
            if (isConnected) {
                Text(
                    text = "设备: $deviceName",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF424242),
                    fontSize = 12.sp
                )
                if (batteryLevel > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "电量: $batteryLevel%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (batteryLevel <= 20 && !isCharging) Color(0xFFC62828) else Color(0xFF424242),
                            fontSize = 12.sp
                        )
                        if (isCharging) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "充电中",
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "充电中",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFA726),
                                fontSize = 11.sp
                            )
                        } else if (batteryLevel <= 20) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "低电量",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "请在首页连接设备",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 菜单项卡片
 */
@Composable
fun MenuItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) Color(0xFFE53935) else Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDestructive) Color(0xFFE53935) else Color(0xFF424242),
                        fontSize = 14.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999),
                        fontSize = 12.sp
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 状态栏
 */
@Composable
fun StatusBar(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
 * 关于对话框
 */
@Composable
fun AboutDialog(
    appVersion: String,
    sdkVersion: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "关于",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(label = "应用名称", value = "Linkai星韵AI眼镜")
                InfoRow(label = "应用版本", value = appVersion)
                InfoRow(label = "SDK版本", value = sdkVersion)
                Divider()
                Text(
                    text = "开发者: 搏哥 (Linkai Team)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    fontSize = 12.sp
                )
                Text(
                    text = "© 2026 All Rights Reserved",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999),
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("关闭")
            }
        }
    )
}

/**
 * 常见问题对话框
 */
@Composable
fun FAQDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "常见问题",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FAQItem(
                    question = "如何连接眼镜?",
                    answer = "在首页点击\"扫描设备\"按钮，选择您的眼镜设备进行连接。"
                )
                FAQItem(
                    question = "如何使用AI对话功能?",
                    answer = "连接眼镜后，进入AI对话页面，按住\"按住说话\"按钮进行录音，松开后AI会自动处理并回复。"
                )
                FAQItem(
                    question = "如何同步媒体文件?",
                    answer = "进入相册页面，点击\"同步\"按钮即可从眼镜同步最新的照片、视频和录音。"
                )
                FAQItem(
                    question = "如何断开连接?",
                    answer = "在我的页面点击\"断开连接\"按钮即可断开与眼镜的蓝牙连接。"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("关闭")
            }
        }
    )
}

/**
 * 信息行
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666),
            fontSize = 12.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242),
            fontSize = 12.sp
        )
    }
}

/**
 * FAQ项
 */
@Composable
fun FAQItem(question: String, answer: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3),
            fontSize = 12.sp
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666),
            fontSize = 11.sp
        )
    }
}

/**
 * API配置对话框
 * 配置LinkAI和OpenClaw API Keys
 */
@Composable
fun ApiConfigDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val apiKeyManager = remember { com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context) }
    
    // 加载已保存的API Keys
    var linkaiVoiceKey by remember { mutableStateOf(apiKeyManager.getLinkAIVoiceApiKey()) }
    var linkaiChatKey by remember { mutableStateOf(apiKeyManager.getLinkAIChatApiKey()) }
    var aliQwenVisionKey by remember { mutableStateOf(apiKeyManager.getAliQwenVisionApiKey()) }
    var aliQwenVisionModel by remember { mutableStateOf(apiKeyManager.getAliQwenVisionModel()) }
    var openclawKey by remember { mutableStateOf(apiKeyManager.getOpenClawApiKey()) }
    var openclawAppId by remember { mutableStateOf(apiKeyManager.getOpenClawAppId()) }
    var linkaiAppCode by remember { mutableStateOf(apiKeyManager.getLinkAIAppCode()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "API配置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "API配置",
                    tint = Color(0xFF2196F3),
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
                // 说明文字
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
                            text = "请输入您的API Key，留空表示不修改",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            fontSize = 11.sp
                        )
                    }
                }
                
                // LinkAI语音API Key
                Text(
                    text = "LinkAI语音API",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2196F3)
                )
                
                OutlinedTextField(
                    value = linkaiVoiceKey,
                    onValueChange = { linkaiVoiceKey = it },
                    label = { Text("语音API Key (ASR + TTS)", fontSize = 12.sp) },
                    placeholder = { Text("请输入LinkAI语音API Key", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                
                Divider(color = Color(0xFFE0E0E0))
                
                // LinkAI对话API Key
                Text(
                    text = "LinkAI对话API",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2196F3)
                )
                
                OutlinedTextField(
                    value = linkaiChatKey,
                    onValueChange = { linkaiChatKey = it },
                    label = { Text("对话API Key (LLM)", fontSize = 12.sp) },
                    placeholder = { Text("请输入LinkAI对话API Key", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // LinkAI App Code（工作流配置）
                OutlinedTextField(
                    value = linkaiAppCode,
                    onValueChange = { linkaiAppCode = it },
                    label = { Text("App Code (工作流)", fontSize = 12.sp) },
                    placeholder = { Text("可选，填入后在LinkAI后台配置工作流", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Divider(color = Color(0xFFE0E0E0))

                // 阿里Qwen识图 API
                Text(
                    text = "阿里Qwen识图API",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFFF6F00)
                )

                OutlinedTextField(
                    value = aliQwenVisionKey,
                    onValueChange = { aliQwenVisionKey = it },
                    label = { Text("阿里API Key (DashScope)", fontSize = 12.sp) },
                    placeholder = { Text("请输入阿里Qwen视觉API Key", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    text = "阿里视觉模型",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    fontSize = 12.sp
                )

                AliQwenVisionModels.all.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aliQwenVisionModel = model }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = aliQwenVisionModel == model,
                            onClick = { aliQwenVisionModel = model }
                        )
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Divider(color = Color(0xFFE0E0E0))
                
                // OpenClaw API
                Text(
                    text = "OpenClaw API (预留)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50)
                )
                
                Text(
                    text = "OpenClaw是开源AI自动化代理引擎",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999),
                    fontSize = 11.sp
                )
                
                OutlinedTextField(
                    value = openclawKey,
                    onValueChange = { openclawKey = it },
                    label = { Text("OpenClaw API Key", fontSize = 12.sp) },
                    placeholder = { Text("请输入OpenClaw API Key", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                
                OutlinedTextField(
                    value = openclawAppId,
                    onValueChange = { openclawAppId = it },
                    label = { Text("OpenClaw应用ID", fontSize = 12.sp) },
                    placeholder = { Text("请输入OpenClaw应用ID", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Text("取消", fontSize = 14.sp)
                }
                Button(
                    onClick = {
                        onSave(
                            linkaiVoiceKey,
                            linkaiChatKey,
                            aliQwenVisionKey,
                            aliQwenVisionModel,
                            openclawKey,
                            openclawAppId,
                            linkaiAppCode
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("保存", fontSize = 14.sp)
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}


/**
 * 崩溃日志对话框
 */
@Composable
fun CrashLogDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // 0 = 崩溃日志, 1 = 运行日志
    var selectedTab by remember { mutableIntStateOf(0) }
    var crashLog by remember { mutableStateOf("") }
    var appLog by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showCopiedToast by remember { mutableStateOf(false) }

    // 在 IO 线程加载日志，避免主线程阻塞
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val crash = com.glasses.app.util.CrashLogHelper.readCrashLog(context)
            val app = com.glasses.app.util.AppLogger.readLog(context)
            // 限制显示大小：最多显示末尾 64KB，避免大文件卡顿
            val maxDisplay = 64 * 1024
            crashLog = if (crash.length > maxDisplay) "...(已截取尾部)\n" + crash.takeLast(maxDisplay) else crash
            appLog = if (app.length > maxDisplay) "...(已截取尾部)\n" + app.takeLast(maxDisplay) else app
        }
        isLoading = false
    }

    val currentLog = if (selectedTab == 0) crashLog else appLog

    // 复制到剪贴板
    val copyToClipboard = {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("log", currentLog)
        clipboard.setPrimaryClip(clip)
        showCopiedToast = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "应用日志", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab 切换
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("崩溃日志", "运行日志").forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index },
                            color = if (selected) Color(0xFF2196F3) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else Color(0xFF666666),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // 日志路径
                val logPath = if (selectedTab == 0)
                    com.glasses.app.util.CrashLogHelper.getCrashLogPath(context)
                else
                    com.glasses.app.util.AppLogger.getLogFile(context).absolutePath

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = logPath,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2),
                        fontSize = 9.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                // 日志内容
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    val noLogText = if (selectedTab == 0) "暂无崩溃日志" else "暂无运行日志"
                    val isEmpty = currentLog.isEmpty() ||
                            currentLog == "No crash log found" ||
                            currentLog == "No log found"

                    if (isEmpty) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = noLogText, color = Color(0xFF999999))
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            color = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentLog,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = Color(0xFFE0E0E0),
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                // 复制成功提示
                if (showCopiedToast) {
                    LaunchedEffect(showCopiedToast) {
                        kotlinx.coroutines.delay(2000)
                        showCopiedToast = false
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(text = "已复制到剪贴板", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 清空按钮
                TextButton(
                    onClick = {
                        if (selectedTab == 0) {
                            com.glasses.app.util.CrashLogHelper.clearCrashLog(context)
                            crashLog = ""
                        } else {
                            com.glasses.app.util.AppLogger.clearLog(context)
                            appLog = ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空", fontSize = 12.sp)
                }

                // 复制按钮
                val canCopy = currentLog.isNotEmpty() &&
                        currentLog != "No crash log found" &&
                        currentLog != "No log found"
                if (canCopy) {
                    Button(
                        onClick = copyToClipboard,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制", fontSize = 12.sp)
                    }
                }

                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF666666)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("关闭", fontSize = 12.sp)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(0.95f)
    )
}
