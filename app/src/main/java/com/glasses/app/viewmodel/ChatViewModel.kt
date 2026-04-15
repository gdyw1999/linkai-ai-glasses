package com.glasses.app.viewmodel

import android.content.Context
import android.util.Log
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glasses.app.data.remote.api.AIServiceImpl
import com.glasses.app.data.remote.sdk.ConnectionState
import com.glasses.app.data.remote.sdk.GlassesSDKManager
import com.glasses.app.data.remote.sdk.MediaCaptureManager
import com.glasses.app.data.remote.sdk.MediaSyncManager
import com.glasses.app.data.repository.ConversationRepository
import com.glasses.app.data.repository.SmartRecognitionRepository
import com.glasses.app.data.repository.SmartRecognitionResult
import com.glasses.app.domain.usecase.StreamingChatManager
import com.glasses.app.manager.AudioPlayer
import com.glasses.app.manager.RecordingManager
import com.glasses.app.manager.RecordingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * 消息数据类
 */
data class Message(
    val id: String = "",
    val content: String = "",
    val isUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val audioUrl: String? = null
)

/**
 * 会话数据类
 */
data class Conversation(
    val id: Long = 0L,
    val title: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

/**
 * AI对话UI状态
 */
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0L,
    val isProcessing: Boolean = false,
    val currentConversationId: Long = 0L,
    val conversationTitle: String = "新对话",
    val statusMessage: String = "",
    val isListening: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val showConversationList: Boolean = false,
    val isConnected: Boolean = false,
    // 功能栏相关状态
    val selectedFeature: String = "快速对话",  // 当前选中的功能
    val selectedSubject: String = "语文",     // 当前选中的学科（默认语文）
    val selectedGrade: String = "小学一年级", // 当前选中的年级（默认小学一年级）
    val selectedExamType: String = "期中"      // 当前选中的试卷类型（仅AI命题使用）
)

/**
 * AI对话ViewModel
 * 管理对话流程、消息列表、会话切换等
 * 集成真实的后端模块：RecordingManager、AIServiceImpl、StreamingChatManager、ConversationRepository
 */
class ChatViewModel(
    private val context: Context,
    private val initialConversationId: Long = 0L,
    private val sharedRenderViewModel: SharedRenderViewModel? = null  // 共享渲染数据的 ViewModel
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"

        /** 功能栏快捷入口列表 */
        val quickFeatures = listOf(
            "快速对话",
            "教学游戏",
            "作文批改",
            "作业解析",
            "AI命题",
            "AI组题"
        )

        /** 学科列表（固定3科） */
        val subjects = listOf("语文", "数学", "英语")

        /** 年级列表（小学到高中） */
        val grades = listOf(
            "小学一年级", "小学二年级", "小学三年级",
            "小学四年级", "小学五年级", "小学六年级",
            "初一", "初二", "初三",
            "高一", "高二", "高三"
        )

        /** 试卷类型列表（仅AI命题使用） */
        val examTypes = listOf("期中", "期末", "月考", "专项练习")
    }
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 后端模块
    private val recordingManager = RecordingManager.getInstance(context)
    private val sdkManager = GlassesSDKManager.getInstance(context)
    private val aiService = AIServiceImpl.getInstance(context)
    private val conversationRepository = ConversationRepository.getInstance(context)
    private val smartRecognitionRepository = SmartRecognitionRepository.getInstance(context)
    private val audioPlayer = AudioPlayer.getInstance(context)
    private lateinit var streamingChatManager: StreamingChatManager
    
    // 当前会话ID
    private var currentConversationId: Long = 0L

    // 从 ApiKeyManager 读取 LinkAI App Code
    private val apiKeyManager by lazy { com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context) }
    
    init {
        // 初始化对话
        initializeChat()
        
        // 监听录音状态
        viewModelScope.launch {
            recordingManager.recordingState.collect { state ->
                handleRecordingState(state)
            }
        }

        // 监听眼镜连接状态
        viewModelScope.launch {
            sdkManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(isConnected = state == ConnectionState.CONNECTED)
            }
        }

        // 监听会话列表变化
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { entities ->
                val conversations = entities.map { entity ->
                    Conversation(
                        id = entity.id,
                        title = entity.title,
                        updatedAt = entity.updatedAt,
                        messageCount = entity.messageCount
                    )
                }
                _uiState.value = _uiState.value.copy(conversations = conversations)
            }
        }
    }
    
    private fun initializeChat() {
        viewModelScope.launch {
            try {
                // 检查 API Key 配置
                val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
                if (!apiKeyManager.hasAllRequiredApiKeys()) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "⚠️ 未配置LinkAI语音/对话 Key，语音对话不可用（智能识图不受影响）"
                    )
                }

                if (initialConversationId > 0) {
                    // 从对话列表进入：加载已有会话
                    loadExistingConversation(initialConversationId)
                } else {
                    // 创建新会话
                    currentConversationId = conversationRepository.createConversation("新对话")
                    _uiState.value = _uiState.value.copy(
                        currentConversationId = currentConversationId,
                        conversationTitle = "新对话"
                    )
                }
                
                // 初始化流式对话管理器
                streamingChatManager = StreamingChatManager(context, aiService)
                
                // 监听流式文本
                viewModelScope.launch {
                    streamingChatManager.displayText.collect { text ->
                        if (text.isNotEmpty()) {
                            updateAIMessage(text)
                        }
                    }
                }

                // 监听首页智能识图结果，自动注入当前会话
                viewModelScope.launch {
                    smartRecognitionRepository.pendingResult.collect { result ->
                        if (result != null) {
                            consumeSmartRecognitionResult(result)
                        }
                    }
                }
                
                Log.d(TAG, "Chat initialized with conversation ID: $currentConversationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize chat", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "初始化失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载已有会话：设置当前会话 ID、标题，并加载历史消息到消息列表
     */
    private suspend fun loadExistingConversation(conversationId: Long) {
        val conversation = conversationRepository.getConversation(conversationId)
        if (conversation != null) {
            currentConversationId = conversationId
            _uiState.value = _uiState.value.copy(
                currentConversationId = conversationId,
                conversationTitle = conversation.title.ifBlank { "新对话" }
            )
            // 加载该会话的所有历史消息到 UI
            val messages = conversationRepository.getMessagesOnce(conversationId)
            val uiMessages = messages.map { entity ->
                Message(
                    id = entity.id.toString(),
                    content = entity.content,
                    isUser = entity.role == "user",
                    timestamp = entity.createdAt,
                    audioUrl = entity.audioUrl
                )
            }
            _uiState.value = _uiState.value.copy(messages = uiMessages)
            Log.d(TAG, "Loaded conversation $conversationId with ${uiMessages.size} messages")
        } else {
            // 会话不存在，创建新的
            currentConversationId = conversationRepository.createConversation("新对话")
            _uiState.value = _uiState.value.copy(
                currentConversationId = currentConversationId,
                conversationTitle = "新对话"
            )
            Log.w(TAG, "Conversation $conversationId not found, created new one")
        }
    }

    /**
     * 处理录音状态变化
     */
    private fun handleRecordingState(state: RecordingState) {
        when (state) {
            is RecordingState.Idle -> {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    isListening = false,
                    statusMessage = ""
                )
            }
            is RecordingState.Recording -> {
                _uiState.value = _uiState.value.copy(
                    isRecording = true,
                    isListening = true,
                    statusMessage = "正在录音..."
                )
            }
            is RecordingState.Processing -> {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    isListening = false,
                    isProcessing = true,
                    statusMessage = "处理中..."
                )
            }
            is RecordingState.Completed -> {
                // 录音完成，开始处理
                processRecordedAudio(state.audioFile)
            }
            is RecordingState.Error -> {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    isListening = false,
                    isProcessing = false,
                    statusMessage = "录音失败: ${state.message}"
                )
            }
        }
    }
    
    /**
     * 开始录音
     */
    fun startRecording() {
        if (!_uiState.value.isConnected) {
            _uiState.value = _uiState.value.copy(statusMessage = "请先连接眼镜")
            return
        }
        com.glasses.app.util.AppLogger.i(TAG, "用户操作: 开始录音(对话页)")
        viewModelScope.launch {
            try {
                val result = recordingManager.startRecording()
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "启动录音失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "启动录音失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 停止录音
     */
    fun stopRecording() {
        com.glasses.app.util.AppLogger.i(TAG, "用户操作: 停止录音")
        viewModelScope.launch {
            try {
                val result = recordingManager.stopRecording()
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "停止录音失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "停止录音失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 发送文本消息（手动输入）
     * 跳过 ASR，直接进入 LLM 流式对话
     */
    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        com.glasses.app.util.AppLogger.i(TAG, "用户操作: 发送文本消息(长度=${text.length})")
        viewModelScope.launch {
            try {
                val trimmedText = text.trim()

                // 如果是初始标题"新对话"，用第一条消息内容更新标题
                if (_uiState.value.conversationTitle == "新对话") {
                    val title = if (trimmedText.length > 20) trimmedText.substring(0, 20) + "…" else trimmedText
                    conversationRepository.updateConversationTitle(currentConversationId, title)
                    _uiState.value = _uiState.value.copy(conversationTitle = title)
                }

                // 添加用户消息到UI
                addUserMessage(trimmedText)

                // 保存用户消息到数据库
                conversationRepository.addMessage(
                    conversationId = currentConversationId,
                    content = trimmedText,
                    role = "user"
                )

                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    statusMessage = "AI思考中..."
                )

                // 流式对话生成（携带 app_code 以指定 LinkAI 工作流）
                val sessionId = currentConversationId.toString()
                val appCode = apiKeyManager.getLinkAIAppCode().ifEmpty { null }
                aiService.chatStreaming(trimmedText, sessionId, appCode).collect { textChunk ->
                    streamingChatManager.processStreamingText(textChunk)
                }

                // 流式对话完成
                streamingChatManager.finalize()

                // 保存AI消息到数据库
                val aiText = streamingChatManager.displayText.value
                conversationRepository.addMessage(
                    conversationId = currentConversationId,
                    content = aiText,
                    role = "assistant"
                )

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    statusMessage = ""
                )

            } catch (e: Exception) {
                // 详细的错误分类和日志
                val (errorMsg, detailLog) = when {
                    e is java.net.SocketTimeoutException -> {
                        val isConnect = e.message?.contains("failed to connect", true) == true
                        if (isConnect) {
                            "连接服务器超时" to "连接LinkAI API超时 - 可能原因: 网络不稳定、API服务器负载高、DNS解析慢"
                        } else {
                            "读取响应超时" to "读取LinkAI响应超时 - 可能原因: LLM生成时间过长、网络传输慢"
                        }
                    }
                    e is java.net.UnknownHostException -> {
                        "无法连接到服务器" to "DNS解析失败或无法访问 - 请检查网络连接、API地址是否正确"
                    }
                    e is java.io.IOException -> {
                        "网络错误" to "IO异常: ${e.message} - 可能原因: 网络中断、连接被重置"
                    }
                    e.message?.contains("timeout", true) == true -> {
                        "请求超时" to "请求超时: ${e.message}"
                    }
                    else -> {
                        "发送失败" to "未知异常: ${e.javaClass.simpleName} - ${e.message}"
                    }
                }

                // 详细日志
                Log.e(TAG, "发送文本消息失败 - $detailLog", e)
                Log.e(TAG, "异常类型: ${e.javaClass.simpleName}, 消息: ${e.message}")
                Log.e(TAG, "会话ID: $currentConversationId, 消息长度: ${text.trim().length}")

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    statusMessage = errorMsg
                )
            }
        }
    }

    /**
     * 处理录音文件
     */
    private fun processRecordedAudio(audioFile: File) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    statusMessage = "语音识别中..."
                )
                
                // 1. ASR - 语音识别
                val asrResult = aiService.transcribeAudio(audioFile)
                if (asrResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = "语音识别失败: ${asrResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                
                val userText = asrResult.getOrNull()!!
                Log.d(TAG, "ASR result: $userText")

                // 如果是初始标题"新对话"，用第一条消息内容更新标题
                if (_uiState.value.conversationTitle == "新对话") {
                    val title = if (userText.length > 20) userText.substring(0, 20) + "…" else userText
                    conversationRepository.updateConversationTitle(currentConversationId, title)
                    _uiState.value = _uiState.value.copy(conversationTitle = title)
                }

                // 添加用户消息到UI
                addUserMessage(userText)
                
                // 保存用户消息到数据库
                conversationRepository.addMessage(
                    conversationId = currentConversationId,
                    content = userText,
                    role = "user"
                )
                
                _uiState.value = _uiState.value.copy(
                    statusMessage = "AI思考中..."
                )
                
                // 2. LLM - 流式对话生成（携带 app_code 以指定 LinkAI 工作流）
                val sessionId = currentConversationId.toString()
                val appCode = apiKeyManager.getLinkAIAppCode().ifEmpty { null }
                aiService.chatStreaming(userText, sessionId, appCode).collect { textChunk ->
                    // 处理流式文本
                    streamingChatManager.processStreamingText(textChunk)
                }
                
                // 流式对话完成
                streamingChatManager.finalize()
                
                // 保存AI消息到数据库
                val aiText = streamingChatManager.displayText.value
                conversationRepository.addMessage(
                    conversationId = currentConversationId,
                    content = aiText,
                    role = "assistant"
                )
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    statusMessage = ""
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process recorded audio", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    statusMessage = "处理失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 添加用户消息
     */
    private fun addUserMessage(content: String) {
        val message = Message(
            id = System.currentTimeMillis().toString(),
            content = content,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        _uiState.value = _uiState.value.copy(messages = currentMessages)
    }

    /**
     * 添加AI消息（非流式）
     */
    private fun addAssistantMessage(content: String) {
        val message = Message(
            id = System.currentTimeMillis().toString(),
            content = content,
            isUser = false,
            timestamp = System.currentTimeMillis()
        )

        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        _uiState.value = _uiState.value.copy(messages = currentMessages)
    }
    
    /**
     * 更新AI消息（流式更新）
     */
    private fun updateAIMessage(content: String) {
        val currentMessages = _uiState.value.messages.toMutableList()

        // 仅在最后一条本身就是AI消息时做流式覆盖，避免误改历史消息
        val lastMessage = currentMessages.lastOrNull()
        if (lastMessage != null && !lastMessage.isUser) {
            val lastIndex = currentMessages.lastIndex
            val updatedMessage = currentMessages[lastIndex].copy(content = content)
            currentMessages[lastIndex] = updatedMessage
        } else {
            val message = Message(
                id = System.currentTimeMillis().toString(),
                content = content,
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            currentMessages.add(message)
        }
        
        _uiState.value = _uiState.value.copy(messages = currentMessages)
    }
    
    /**
     * 打断对话
     */
    fun interrupt() {
        com.glasses.app.util.AppLogger.i(TAG, "用户操作: 打断对话")
        try {
            Log.d(TAG, "Interrupting conversation")
            
            // 停止录音
            if (_uiState.value.isRecording) {
                recordingManager.reset()
            }
            
            // 打断流式对话
            if (::streamingChatManager.isInitialized) {
                streamingChatManager.interrupt()
            }
            
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                isProcessing = false,
                isListening = false,
                statusMessage = "已打断"
            )
            
            viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(statusMessage = "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to interrupt", e)
        }
    }
    
    /**
     * 播放AI消息的语音
     */
    fun playAudioMessage(messageId: String) {
        viewModelScope.launch {
            try {
                // 查找消息
                val message = _uiState.value.messages.find { it.id == messageId }
                if (message == null || message.isUser) {
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(statusMessage = "合成语音中...")
                
                // 使用TTS合成语音
                val ttsResult = aiService.synthesizeSpeech(message.content)
                if (ttsResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "语音合成失败: ${ttsResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                
                val audioFile = ttsResult.getOrNull()!!
                
                _uiState.value = _uiState.value.copy(statusMessage = "播放中...")
                
                // 播放音频
                audioPlayer.play(audioFile) {
                    // 播放完成后删除临时文件
                    audioFile.delete()
                    _uiState.value = _uiState.value.copy(statusMessage = "")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play audio message", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "播放失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 新建对话
     */
    fun newConversation() {
        com.glasses.app.util.AppLogger.i(TAG, "用户操作: 新建对话")
        viewModelScope.launch {
            try {
                // 创建新会话
                currentConversationId = conversationRepository.createConversation("新对话")

                _uiState.value = ChatUiState(
                    currentConversationId = currentConversationId,
                    conversationTitle = "新对话"
                )

                // 重置流式对话管理器
                if (::streamingChatManager.isInitialized) {
                    streamingChatManager.interrupt()
                }

                Log.d(TAG, "New conversation created: $currentConversationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create new conversation", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "创建新对话失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 选择功能
     * @param feature 功能名称
     */
    fun selectFeature(feature: String) {
        _uiState.value = _uiState.value.copy(selectedFeature = feature)
        com.glasses.app.util.AppLogger.i(TAG, "用户选择功能: $feature")
    }

    /**
     * 选择学科
     * @param subject 学科名称
     */
    fun selectSubject(subject: String) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
        com.glasses.app.util.AppLogger.i(TAG, "用户选择学科: $subject")
    }

    /**
     * 选择年级
     * @param grade 年级名称
     */
    fun selectGrade(grade: String) {
        _uiState.value = _uiState.value.copy(selectedGrade = grade)
        com.glasses.app.util.AppLogger.i(TAG, "用户选择年级: $grade")
    }

    /**
     * 选择试卷类型（仅AI命题使用）
     * @param examType 试卷类型
     */
    fun selectExamType(examType: String) {
        _uiState.value = _uiState.value.copy(selectedExamType = examType)
        com.glasses.app.util.AppLogger.i(TAG, "用户选择试卷类型: $examType")
    }

    /**
     * 获取当前功能的前缀文本
     * 根据当前选中的功能、学科、年级生成前缀
     */
    fun getFeaturePrefix(): String {
        val state = _uiState.value
        val feature = state.selectedFeature

        return when (feature) {
            "快速对话" -> ""
            "教学游戏" -> "教学游戏、${state.selectedSubject}、${state.selectedGrade}："
            "作文批改" -> "作文批改、${state.selectedSubject}、${state.selectedGrade}："
            "作业解析" -> "作业解析、${state.selectedSubject}、${state.selectedGrade}："
            "AI命题" -> "AI命题、${state.selectedSubject}、${state.selectedGrade}、${state.selectedExamType}："
            "AI组题" -> "AI组题、${state.selectedSubject}、${state.selectedGrade}："
            else -> ""
        }
    }

    /**
     * 加载会话消息
     */
    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                currentConversationId = conversationId
                
                // 获取会话信息
                val conversation = conversationRepository.getConversation(conversationId)
                if (conversation == null) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "会话不存在"
                    )
                    return@launch
                }
                
                // 获取消息列表
                val messages = conversationRepository.getMessagesOnce(conversationId)
                val uiMessages = messages.map { entity ->
                    Message(
                        id = entity.id.toString(),
                        content = entity.content,
                        isUser = entity.role == "user",
                        timestamp = entity.createdAt,
                        audioUrl = entity.audioUrl
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    currentConversationId = conversationId,
                    conversationTitle = conversation.title,
                    messages = uiMessages
                )
                
                Log.d(TAG, "Loaded conversation: $conversationId with ${messages.size} messages")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversation", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "加载会话失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 清空消息
     */
    fun clearMessages() {
        viewModelScope.launch {
            try {
                conversationRepository.clearConversationMessages(currentConversationId)
                _uiState.value = _uiState.value.copy(messages = emptyList())
                Log.d(TAG, "Messages cleared for conversation: $currentConversationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear messages", e)
            }
        }
    }
    
    /**
     * 切换会话列表显示
     */
    fun toggleConversationList() {
        _uiState.value = _uiState.value.copy(
            showConversationList = !_uiState.value.showConversationList
        )
    }
    
    /**
     * 关闭会话列表
     */
    fun closeConversationList() {
        _uiState.value = _uiState.value.copy(showConversationList = false)
    }
    
    /**
     * 删除会话
     */
    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                conversationRepository.deleteConversation(conversationId)
                
                // 如果删除的是当前会话，创建新会话
                if (conversationId == currentConversationId) {
                    newConversation()
                }
                
                Log.d(TAG, "Deleted conversation: $conversationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete conversation", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "删除会话失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 消费首页智能识图结果并写入当前对话
     */
    private suspend fun consumeSmartRecognitionResult(result: SmartRecognitionResult) {
        com.glasses.app.util.AppLogger.i(TAG, "收到智能识图结果: model=${result.model}, 答案长度=${result.answer.length}")
        try {
            if (currentConversationId == 0L) {
                currentConversationId = conversationRepository.createConversation("智能识图")
                _uiState.value = _uiState.value.copy(
                    currentConversationId = currentConversationId,
                    conversationTitle = "智能识图"
                )
            }

            addUserMessage(result.question)
            addAssistantMessage(result.answer)

            conversationRepository.addMessage(
                conversationId = currentConversationId,
                content = result.question,
                role = "user"
            )
            conversationRepository.addMessage(
                conversationId = currentConversationId,
                content = result.answer,
                role = "assistant"
            )

            _uiState.value = _uiState.value.copy(
                statusMessage = "已接收首页智能识图结果（模型：${result.model}）"
            )

            Log.d(TAG, "Smart recognition result consumed: ${result.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to consume smart recognition result", e)
            _uiState.value = _uiState.value.copy(
                statusMessage = "识图结果写入失败: ${e.message}"
            )
        } finally {
            smartRecognitionRepository.clear(result.id)
        }
    }

    /**
     * 识别图片并发送到对话（Qwen 多模态分析）
     * @param imagePath 图片文件路径
     * @param fileName 显示用的文件名
     */
    fun recognizeImageAndSend(imagePath: String, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                statusMessage = "正在分析图片..."
            )
            com.glasses.app.util.AppLogger.i(TAG, "用户操作: 分析图片 $fileName")

            try {
                // 1. 写用户消息
                val userContent = "📷 分析图片: $fileName"
                if (currentConversationId == 0L) {
                    currentConversationId = conversationRepository.createConversation("图片分析")
                    _uiState.value = _uiState.value.copy(
                        currentConversationId = currentConversationId,
                        conversationTitle = "图片分析"
                    )
                }
                conversationRepository.addMessage(
                    conversationId = currentConversationId,
                    content = userContent,
                    role = "user"
                )
                _uiState.value = _uiState.value.copy(
                    messages = conversationRepository.getMessagesOnce(currentConversationId).map { entity ->
                        Message(
                            id = entity.id.toString(),
                            content = entity.content,
                            isUser = entity.role == "user",
                            timestamp = entity.createdAt,
                            audioUrl = entity.audioUrl
                        )
                    }
                )

                // 2. 调用 Qwen 分析
                val appCode = apiKeyManager.getLinkAIAppCode().ifEmpty { null }
                val result = aiService.recognizeImage(
                    imagePath = imagePath,
                    sessionId = "vision_${System.currentTimeMillis()}",
                    appCode = appCode
                )

                // 3. 处理分析结果
                result.fold(
                    onSuccess = { analysisText ->
                        com.glasses.app.util.AppLogger.i(TAG, "Qwen 分析完成，结果长度=${analysisText.length}")
                        conversationRepository.addMessage(
                            conversationId = currentConversationId,
                            content = analysisText,
                            role = "assistant"
                        )
                        _uiState.value = _uiState.value.copy(
                            messages = conversationRepository.getMessagesOnce(currentConversationId).map { entity ->
                                Message(
                                    id = entity.id.toString(),
                                    content = entity.content,
                                    isUser = entity.role == "user",
                                    timestamp = entity.createdAt,
                                    audioUrl = entity.audioUrl
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        val errorMsg = "图片分析失败: ${error.message}"
                        com.glasses.app.util.AppLogger.e(TAG, errorMsg, error)
                        conversationRepository.addMessage(
                            conversationId = currentConversationId,
                            content = "图片分析失败，请重试。错误：${error.message}",
                            role = "assistant"
                        )
                        _uiState.value = _uiState.value.copy(
                            messages = conversationRepository.getMessagesOnce(currentConversationId).map { entity ->
                                Message(
                                    id = entity.id.toString(),
                                    content = entity.content,
                                    isUser = entity.role == "user",
                                    timestamp = entity.createdAt,
                                    audioUrl = entity.audioUrl
                                )
                            }
                        )
                    }
                )

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    statusMessage = ""
                )
            } catch (e: Exception) {
                val errorMsg = "图片分析异常: ${e.message}"
                com.glasses.app.util.AppLogger.e(TAG, errorMsg, e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    statusMessage = errorMsg
                )
            }
        }
    }

    /**
     * 眼镜拍照并分析图片
     * 完整流程：拍照 -> 等3秒 -> 同步 -> 获取最新图片 -> Qwen 分析
     */
    fun glassesCameraAndAnalyze() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                statusMessage = "正在拍照..."
            )
            com.glasses.app.util.AppLogger.i(TAG, "用户操作: 眼镜拍照并分析")

            try {
                // 1. 获取 MediaCaptureManager 和 MediaSyncManager
                val captureManager = MediaCaptureManager.getInstance(context)
                val syncManager = MediaSyncManager.getInstance(context)

                // 2. 拍照
                val photoResult = suspendCancellableCoroutine { continuation ->
                    captureManager.takePhoto { success, message ->
                        if (continuation.isActive) {
                            continuation.resume(success to message)
                        }
                    }
                }
                if (!photoResult.first) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = "拍照失败: ${photoResult.second}"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(statusMessage = "拍照成功，等待同步...")

                // 3. 等眼镜端保存照片
                delay(3000)

                // 4. 启动同步
                syncManager.startSync { success, message ->
                    // SDK 的 startSync 是异步的，这里用 suspendCancellableCoroutine 包装
                }

                // 5. 轮询获取最新图片（最多等20秒）
                val latestImagePath = withTimeoutOrNull<String?>(20_000L) {
                    val albumDirPath = File(context.getExternalFilesDir(null), "GlassesAlbum").absolutePath
                    val albumDir = File(albumDirPath)
                    val filesBefore = albumDir.listFiles()
                        ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
                        ?.map { it.absolutePath }
                        ?.toSet()
                        ?: emptySet()

                    while (isActive) {
                        delay(500)
                        val filesAfter = albumDir.listFiles()
                            ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
                            ?.map { it.absolutePath }
                            ?.toSet()
                            ?: emptySet()
                        val newFiles = filesAfter - filesBefore
                        if (newFiles.isNotEmpty()) {
                            return@withTimeoutOrNull newFiles.maxByOrNull { File(it).lastModified() }
                        }
                    }
                    null
                }

                if (latestImagePath.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = "未找到新拍的照片，请重试"
                    )
                    return@launch
                }

                // 6. 调用 Qwen 分析
                recognizeImageAndSend(latestImagePath, File(latestImagePath).name)
            } catch (e: Exception) {
                val errorMsg = "眼镜拍照分析异常: ${e.message}"
                com.glasses.app.util.AppLogger.e(TAG, errorMsg, e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    statusMessage = errorMsg
                )
            }
        }
    }

    /**
     * 渲染 HTML/Markdown 内容
     * 设置内容到共享 ViewModel，然后由调用方触发导航跳转
     *
     * @param content HTML 或 Markdown 内容
     * @param type 内容类型（HTML 或 MARKDOWN）
     */
    fun renderContent(content: String, type: ContentType = ContentType.MARKDOWN) {
        sharedRenderViewModel?.setRenderContent(content, type)
        Log.d(TAG, "Content set for rendering: type=$type, length=${content.length}")
    }

    /**
     * 检测内容类型并自动渲染
     * 检测规则：
     * - 包含 <!DOCTYPE html 或 <html 标签 → HTML
     * - 其他 → Markdown
     *
     * @param content 要渲染的内容
     */
    fun detectAndRenderContent(content: String): Boolean {
        val type = when {
            content.contains("<!DOCTYPE html", ignoreCase = true) ||
            content.contains("<html", ignoreCase = true) -> ContentType.HTML
            else -> ContentType.MARKDOWN
        }

        renderContent(content, type)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        // 释放资源
        if (::streamingChatManager.isInitialized) {
            streamingChatManager.release()
        }
        audioPlayer.release()
    }
}

/**
 * ChatViewModel工厂类
 */
class ChatViewModelFactory(
    private val context: Context,
    private val conversationId: Long = 0L,
    private val sharedRenderViewModel: SharedRenderViewModel? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context, conversationId, sharedRenderViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
