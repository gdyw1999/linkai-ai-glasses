package com.glasses.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glasses.app.data.remote.api.AIServiceImpl
import com.glasses.app.data.repository.ConversationRepository
import com.glasses.app.domain.usecase.StreamingChatManager
import com.glasses.app.manager.AudioPlayer
import com.glasses.app.manager.RecordingManager
import com.glasses.app.manager.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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
    val showConversationList: Boolean = false
)

/**
 * AI对话ViewModel
 * 管理对话流程、消息列表、会话切换等
 * 集成真实的后端模块：RecordingManager、AIServiceImpl、StreamingChatManager、ConversationRepository
 */
class ChatViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"
    }
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 后端模块
    private val recordingManager = RecordingManager.getInstance(context)
    private val aiService = AIServiceImpl.getInstance(context)
    private val conversationRepository = ConversationRepository.getInstance(context)
    private val audioPlayer = AudioPlayer.getInstance(context)
    private lateinit var streamingChatManager: StreamingChatManager
    
    // 当前会话ID
    private var currentConversationId: Long = 0L
    
    init {
        // 初始化对话
        initializeChat()
        
        // 监听录音状态
        viewModelScope.launch {
            recordingManager.recordingState.collect { state ->
                handleRecordingState(state)
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
                        statusMessage = "⚠️ 请先在「我的」→「API配置」中设置 LinkAI API Key"
                    )
                }

                // 创建新会话
                currentConversationId = conversationRepository.createConversation("新对话")
                
                _uiState.value = _uiState.value.copy(
                    currentConversationId = currentConversationId,
                    conversationTitle = "新对话"
                )
                
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
                
                // 2. LLM - 流式对话生成
                val sessionId = currentConversationId.toString()
                aiService.chatStreaming(userText, sessionId).collect { textChunk ->
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
     * 更新AI消息（流式更新）
     */
    private fun updateAIMessage(content: String) {
        val currentMessages = _uiState.value.messages.toMutableList()
        
        // 查找最后一条AI消息
        val lastAIMessageIndex = currentMessages.indexOfLast { !it.isUser }
        
        if (lastAIMessageIndex >= 0) {
            // 更新现有AI消息
            val updatedMessage = currentMessages[lastAIMessageIndex].copy(content = content)
            currentMessages[lastAIMessageIndex] = updatedMessage
        } else {
            // 添加新的AI消息
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
class ChatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
