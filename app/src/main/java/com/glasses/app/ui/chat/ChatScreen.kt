package com.glasses.app.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glasses.app.data.local.media.MediaFile
import com.glasses.app.data.local.media.MediaType
import com.glasses.app.data.remote.sdk.ConnectionState
import com.glasses.app.data.remote.sdk.GlassesSDKManager
import com.glasses.app.data.remote.sdk.MediaSyncManager
import com.glasses.app.viewmodel.ChatViewModel
import com.glasses.app.R
import com.glasses.app.viewmodel.ChatViewModelFactory
import com.glasses.app.viewmodel.Conversation
import com.glasses.app.viewmodel.ChatMessage
import com.glasses.app.viewmodel.SharedRenderViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * AI对话屏幕
 * 显示消息列表、录音按钮、会话管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun ChatScreen(
    innerPadding: PaddingValues = PaddingValues(),
    onBack: (() -> Unit)? = null,
    conversationId: Long = 0L,
    onNavigateToRender: (() -> Unit)? = null,        // 跳转到渲染页面的回调（预留）
    sharedViewModel: SharedRenderViewModel? = null  // 共享渲染数据的 ViewModel（预留）
) {
    // 创建 ViewModel，传入 sharedViewModel
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(LocalContext.current, conversationId, sharedViewModel)
    )
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val context = LocalContext.current

    // 展开/收起面板状态
    var showImageSourceSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 眼镜相册面板状态
    var showGlassesAlbumSheet by remember { mutableStateOf(false) }
    val glassesAlbumSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 最近图片（从 MediaSyncManager 取，自动响应 mediaFiles 变化）
    val recentImages by remember {
        derivedStateOf {
            MediaSyncManager.getInstance(context).mediaFiles.value
                .filter { it.type == MediaType.IMAGE }
                .take(8)
        }
    }

    // 手机相册 - PickVisualMedia Launcher
    val phoneAlbumLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = copyUriToTempFile(context, it)
            file?.let { f ->
                viewModel.recognizeImageAndSend(f.absolutePath, f.name)
            }
        }
        showImageSourceSheet = false
    }

    // 手机拍照 - TakePicture Launcher
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            // The temp photo file is already created before launch.
            // Find it by pattern and send for recognition
            val files = context.cacheDir.listFiles { _, name -> name.startsWith("IMG_") && name.endsWith(".jpg") }
            files?.maxByOrNull { it.lastModified() }?.let { f ->
                viewModel.recognizeImageAndSend(f.absolutePath, f.name)
            }
        }
        pendingCameraUri = null
        showImageSourceSheet = false
    }
    
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // 顶部栏
            ChatTopBar(
                conversationTitle = uiState.conversationTitle,
                onNewChat = { viewModel.newConversation() },
                onShowHistory = { viewModel.toggleConversationList() },
                onBack = onBack
            )
            
            // 消息列表
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(
                        message = message,
                        onPlayAudio = { viewModel.playAudioMessage(message.id) }
                    )
                }
            }
            
            // 状态提示
            if (uiState.statusMessage.isNotEmpty()) {
                StatusBar(message = uiState.statusMessage)
            }

            // 功能栏（快捷入口）
            FeatureBar(
                features = ChatViewModel.quickFeatures,
                selectedFeature = uiState.selectedFeature,
                onFeatureClick = { viewModel.selectFeature(it) }
            )

            // 二级选择栏（学科/年级/试卷类型）
            if (uiState.selectedFeature != "快速对话") {
                SecondarySelectorBar(
                    selectedFeature = uiState.selectedFeature,
                    selectedSubject = uiState.selectedSubject,
                    selectedGrade = uiState.selectedGrade,
                    selectedExamType = uiState.selectedExamType,
                    onSubjectClick = { viewModel.selectSubject(it) },
                    onGradeClick = { viewModel.selectGrade(it) },
                    onExamTypeClick = { viewModel.selectExamType(it) }
                )
            }
            
            // 底部控制栏
            ChatControlBar(
                isRecording = uiState.isRecording,
                isProcessing = uiState.isProcessing,
                isConnected = uiState.isConnected,
                prefixText = viewModel.getFeaturePrefix(),
                onStartRecording = { viewModel.startRecording() },
                onStopRecording = { viewModel.stopRecording() },
                onInterrupt = { viewModel.interrupt() },
                onSendText = { text -> viewModel.sendTextMessage(text) },
                onAddClick = { showImageSourceSheet = true }
            )
        }
        
        // 会话列表侧边栏
        if (uiState.showConversationList) {
            ConversationListDrawer(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversationId,
                onConversationClick = { conversationId ->
                    viewModel.loadConversation(conversationId)
                    viewModel.closeConversationList()
                },
                onConversationDelete = { conversationId ->
                    viewModel.deleteConversation(conversationId)
                },
                onDismiss = { viewModel.closeConversationList() }
            )
        }

        // 附件扩展面板
        if (showImageSourceSheet) {
            ImageSourceBottomSheet(
                sheetState = sheetState,
                recentImages = recentImages,
                onDismiss = { showImageSourceSheet = false },
                onSourceSelected = { source ->
                    when (source) {
                        ImageSource.ALBUM_PHONE -> {
                            phoneAlbumLauncher.launch("image/*")
                        }
                        ImageSource.CAMERA_PHONE -> {
                            val photoFile = createTempImageFile(context)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        }
                        ImageSource.ALBUM_GLASSES -> {
                            val sdkManager = GlassesSDKManager.getInstance(context)
                            if (sdkManager.connectionState.value != ConnectionState.CONNECTED) {
                                Toast.makeText(context, "请先连接眼镜", Toast.LENGTH_SHORT).show()
                                showImageSourceSheet = false
                            } else {
                                showImageSourceSheet = false
                                showGlassesAlbumSheet = true
                            }
                        }
                        ImageSource.CAMERA_GLASSES -> {
                            val sdkManager = GlassesSDKManager.getInstance(context)
                            if (sdkManager.connectionState.value != ConnectionState.CONNECTED) {
                                Toast.makeText(context, "请先连接眼镜", Toast.LENGTH_SHORT).show()
                                showImageSourceSheet = false
                            } else {
                                showImageSourceSheet = false
                                viewModel.glassesCameraAndAnalyze()
                            }
                        }
                    }
                },
                onRecentImageSelected = { media ->
                    showImageSourceSheet = false
                    viewModel.recognizeImageAndSend(media.filePath, media.fileName)
                }
            )
        }

        // 眼镜相册选择面板
        if (showGlassesAlbumSheet) {
            val glassesImageFiles by remember {
                derivedStateOf {
                    MediaSyncManager.getInstance(context).mediaFiles.value
                        .filter { it.type == MediaType.IMAGE }
                }
            }
            GlassesAlbumSheet(
                sheetState = glassesAlbumSheetState,
                mediaFiles = glassesImageFiles,
                onMediaSelected = { media ->
                    showGlassesAlbumSheet = false
                    viewModel.recognizeImageAndSend(media.filePath, media.fileName)
                },
                onDismiss = { showGlassesAlbumSheet = false }
            )
        }
    }
}

/**
 * 顶部栏
 */
@Composable
fun ChatTopBar(
    conversationTitle: String,
    onNewChat: () -> Unit,
    onShowHistory: () -> Unit,
    onBack: (() -> Unit)? = null
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
            // 左侧：返回按钮（全屏模式）或历史记录按钮
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(onClick = onShowHistory) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "历史记录",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 中间：标题
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI对话",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = conversationTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 右侧：新建会话按钮
            IconButton(onClick = onNewChat) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建对话",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 功能栏 - 横向滑动的快捷入口
 * 点击后设置当前功能模式
 */
@Composable
private fun FeatureBar(
    features: List<String>,
    selectedFeature: String,
    onFeatureClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(features) { feature ->
            val isSelected = feature == selectedFeature
            Surface(
                onClick = { onFeatureClick(feature) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Color(0xFF2196F3) else Color(0xFFEEEEEE)
            ) {
                Text(
                    text = feature,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else Color(0xFF424242)
                )
            }
        }
    }
}

/**
 * 二级选择栏 - 学科/年级/试卷类型选择
 * 在功能栏下方显示，用于选择学科、年级等参数
 */
@Composable
private fun SecondarySelectorBar(
    selectedFeature: String,
    selectedSubject: String,
    selectedGrade: String,
    selectedExamType: String,
    onSubjectClick: (String) -> Unit,
    onGradeClick: (String) -> Unit,
    onExamTypeClick: (String) -> Unit
) {
    val isAIProposition = selectedFeature == "AI命题"

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 第一行：学科选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "学科：",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChatViewModel.subjects.forEach { subject ->
                    val isSelected = subject == selectedSubject
                    Surface(
                        onClick = { onSubjectClick(subject) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF4CAF50) else Color(0xFFE8E8E8)
                    ) {
                        Text(
                            text = subject,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else Color(0xFF424242)
                        )
                    }
                }
            }
        }

        // 第二行：年级选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "年级：",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(ChatViewModel.grades) { grade ->
                    val isSelected = grade == selectedGrade
                    Surface(
                        onClick = { onGradeClick(grade) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFFF9800) else Color(0xFFE8E8E8)
                    ) {
                        Text(
                            text = grade.replace("小学", "").replace("年级", ""),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White else Color(0xFF424242)
                        )
                    }
                }
            }
        }

        // 第三行：试卷类型选择（仅AI命题显示）
        if (isAIProposition) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "类型：",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChatViewModel.examTypes.forEach { type ->
                        val isSelected = type == selectedExamType
                        Surface(
                            onClick = { onExamTypeClick(type) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF9C27B0) else Color(0xFFE8E8E8)
                        ) {
                            Text(
                                text = type,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else Color(0xFF424242)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 消息气泡
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    onPlayAudio: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (message.isUser) 12.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 12.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser)
                    Color(0xFF2196F3)
                else
                    Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else Color(0xFF424242),
                    fontSize = 14.sp
                )
                
                // AI消息显示播放按钮
                if (!message.isUser && message.audioUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onPlayAudio,
                        modifier = Modifier
                            .height(28.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "播放",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
 * 底部控制栏
 * 包含文本输入框、语音按钮、发送按钮，支持功能前缀显示
 */
@Composable
fun ChatControlBar(
    isRecording: Boolean,
    isProcessing: Boolean,
    isConnected: Boolean,
    prefixText: String = "",  // 功能前缀文本，如"作文批改、语文、初一："
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onInterrupt: () -> Unit,
    onSendText: (String) -> Unit,
    onAddClick: () -> Unit = {}
) {
    // 文本输入状态，在重组之间保持
    var inputText by remember { mutableStateOf("") }
    // 显示的前缀（带颜色区分）
    val hasPrefix = prefixText.isNotBlank()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isRecording && !isProcessing) {
                // 空闲状态：文本输入框 + 语音/发送按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 带前缀的文本输入框
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                if (!hasPrefix) {
                                    Text(
                                        text = "输入消息...",
                                        color = Color(0xFFBDBDBD),
                                        fontSize = 14.sp
                                    )
                                }
                            },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            cursorColor = Color(0xFF2196F3)
                        ),
                        // 软键盘"发送"动作
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                    } // 关闭 Box

                    // 语音按钮（输入框为空时显示）
                    if (inputText.isBlank()) {
                        IconButton(
                            onClick = onStartRecording,
                            enabled = isConnected,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isConnected) Color(0xFF2196F3) else Color(0xFFBDBDBD),
                                    RoundedCornerShape(22.dp)
                                )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mic),
                                contentDescription = "语音输入",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // 发送按钮（输入框有文字时显示）
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() || prefixText.isNotBlank()) {
                                    // 组合前缀和用户输入发送
                                    val fullText = if (prefixText.isNotBlank() && inputText.isNotBlank()) {
                                        "$prefixText${inputText.trim()}"
                                    } else if (prefixText.isNotBlank()) {
                                        prefixText
                                    } else {
                                        inputText.trim()
                                    }
                                    onSendText(fullText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color(0xFF2196F3),
                                    RoundedCornerShape(22.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "发送",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // 右侧 + 按钮（仅在输入框为空时显示，防止误点）
                    if (inputText.isBlank() && prefixText.isBlank()) {
                        IconButton(
                            onClick = onAddClick,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color(0xFF4CAF50),
                                    RoundedCornerShape(22.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加附件",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            } else if (isRecording) {
                // 录音中 - 显示停止按钮和打断按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "完成",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Button(
                        onClick = onInterrupt,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "打断",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // 录音动画指示
                RecordingIndicator()
            } else {
                // 处理中 - 显示进度和打断按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        text = "处理中...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onInterrupt,
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(min = 80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "打断",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 录音动画指示器
 */
@Composable
fun RecordingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFFFF9800), RoundedCornerShape(3.dp))
            )
            if (index < 2) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "正在录音...",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE65100),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

/**
 * 会话列表抽屉
 */
@Composable
fun ConversationListDrawer(
    conversations: List<Conversation>,
    currentConversationId: Long,
    onConversationClick: (Long) -> Unit,
    onConversationDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // 背景遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    )
    
    // 侧边栏
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1F1F1F)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "历史记录",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // 会话列表
            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "暂无历史记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationListItem(
                            conversation = conversation,
                            isSelected = conversation.id == currentConversationId,
                            onClick = { onConversationClick(conversation.id) },
                            onDelete = { onConversationDelete(conversation.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 会话列表项 - 使用长按删除
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationListItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除会话") },
            text = { Text("确定要删除这个会话吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            ),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(conversation.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = "${conversation.messageCount}条消息",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                        fontSize = 11.sp
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "当前会话",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // 非选中状态显示删除按钮
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        diff < 604800_000 -> "${diff / 86400_000}天前"
        else -> {
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * 复制 URI 到临时文件并返回 File 对象
 */
private fun copyUriToTempFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = createTempImageFile(context)
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        com.glasses.app.util.AppLogger.i("ChatScreen", "手机相册图片已复制到: ${file.absolutePath}")
        file
    } catch (e: Exception) {
        com.glasses.app.util.AppLogger.e("ChatScreen", "图片复制失败: ${e.message}", e)
        Toast.makeText(context, "图片读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    }
}

/**
 * 创建临时图片文件
 */
private fun createTempImageFile(context: android.content.Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.cacheDir
    return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
}

/**
 * 眼镜相册选择面板（内嵌 ModalBottomSheet）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassesAlbumSheet(
    sheetState: SheetState,
    mediaFiles: List<MediaFile>,
    onMediaSelected: (MediaFile) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "眼镜相册",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                TextButton(onClick = onDismiss) {
                    Text("取消", color = Color(0xFF757575))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 相册网格
            if (mediaFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无图片，请先同步", color = Color(0xFF9E9E9E))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(mediaFiles) { media ->
                        val imageModel = File(media.filePath).takeIf { it.exists() }
                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onMediaSelected(media) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageModel)
                                    .crossfade(true).build(),
                                contentDescription = media.fileName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
