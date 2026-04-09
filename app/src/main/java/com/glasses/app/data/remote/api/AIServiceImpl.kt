package com.glasses.app.data.remote.api

import android.content.Context
import android.util.Log
import com.glasses.app.data.remote.api.model.ChatRequest
import com.glasses.app.data.remote.api.model.ChatResponse
import com.glasses.app.data.remote.api.model.TTSRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * AI服务实现类
 * 封装LinkAI API的调用
 */
class AIServiceImpl(private val context: Context) {
    
    companion object {
        private const val TAG = "AIServiceImpl"
        
        // 默认音色（适合流式输出）
        private const val DEFAULT_VOICE = "BV700_V2_streaming"
        
        @Volatile
        private var instance: AIServiceImpl? = null
        
        fun getInstance(context: Context): AIServiceImpl {
            return instance ?: synchronized(this) {
                instance ?: AIServiceImpl(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val apiService = LinkAIClient.apiService

    /**
     * 获取当前语音 API Key（ASR/TTS 专用）
     */
    private fun getVoiceApiKey(): String {
        val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
        return apiKeyManager.getLinkAIVoiceApiKey()
    }

    /**
     * 获取当前对话 API Key（LLM 专用）
     */
    private fun getChatApiKey(): String {
        val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
        return apiKeyManager.getLinkAIChatApiKey()
    }

    /**
     * 检查 API Key 是否已配置
     */
    private fun checkApiKey(type: String = "voice"): Result<Unit> {
        val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
        val hasKey = if (type == "chat") apiKeyManager.hasLinkAIChatApiKey()
                     else apiKeyManager.hasLinkAIVoiceApiKey()
        return if (hasKey) Result.success(Unit)
               else Result.failure(Exception("请先在「我的」→「API配置」中设置 LinkAI ${if (type == "chat") "对话" else "语音"} API Key"))
    }
    
    /**
     * 语音识别（ASR）
     * 将音频文件转换为文字
     * 
     * @param audioFile 音频文件（WAV格式）
     * @return 识别出的文字
     */
    suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 检查 API Key
            checkApiKey("voice").onFailure { return@withContext Result.failure(it) }

            Log.d(TAG, "Transcribing audio: ${audioFile.absolutePath}")
            
            // 创建multipart请求
            val requestBody = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
            
            // 调用API
            val response = apiService.transcribeAudio(filePart)
            
            if (response.isSuccessful && response.body() != null) {
                val text = response.body()!!.text
                Log.d(TAG, "Transcription successful: $text")
                Result.success(text)
            } else {
                val error = "ASR failed: ${response.code()} - ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Transcription error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 对话生成（LLM）
     * 发送问题并获取AI回复
     * 
     * @param question 用户问题
     * @param sessionId 会话ID
     * @param appCode 应用code（可选）
     * @return AI回复
     */
    suspend fun chat(
        question: String,
        sessionId: String,
        appCode: String? = null
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            checkApiKey("chat").onFailure { return@withContext Result.failure(it) }

            Log.d(TAG, "Sending chat request: question=$question, sessionId=$sessionId")
            
            val request = ChatRequest(
                question = question,
                sessionId = sessionId,
                appCode = appCode,
                stream = false
            )
            
            val response = apiService.chat(request)
            
            if (response.isSuccessful && response.body() != null) {
                val chatResponse = response.body()!!
                Log.d(TAG, "Chat successful: ${chatResponse.choices.firstOrNull()?.message?.content}")
                Result.success(chatResponse)
            } else {
                val error = "Chat failed: ${response.code()} - ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Chat error", e)
            Result.failure(e)
        }
    }

    /**
     * 流式对话生成（LLM）
     * 流式输出AI回复
     * 
     * @param question 用户问题
     * @param sessionId 会话ID
     * @param appCode 应用code（可选）
     * @return 流式文本片段
     */
    fun chatStreaming(
        question: String,
        sessionId: String,
        appCode: String? = null
    ): Flow<String> = flow {
        try {
            val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
            if (!apiKeyManager.hasLinkAIChatApiKey()) {
                throw Exception("请先在「我的」→「API配置」中设置 LinkAI 对话 API Key")
            }

            Log.d(TAG, "Sending streaming chat request: question=$question")
            
            val request = ChatRequest(
                question = question,
                sessionId = sessionId,
                appCode = appCode,
                stream = true
            )
            
            val response = apiService.chatStreaming(request)
            
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val reader = responseBody.byteStream().bufferedReader()
                
                reader.useLines { lines ->
                    for (line in lines) {
                        if (line.startsWith("data: ")) {
                            val data = line.substring(6).trim()
                            
                            // 检查是否结束
                            if (data == "[DONE]") {
                                Log.d(TAG, "Streaming completed")
                                break
                            }
                            
                            // 解析JSON并提取content
                            try {
                                val json = com.google.gson.Gson().fromJson(data, ChatResponse::class.java)
                                val content = json.choices.firstOrNull()?.delta?.content
                                if (!content.isNullOrEmpty()) {
                                    emit(content)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse streaming data: $data", e)
                            }
                        }
                    }
                }
            } else {
                throw Exception("Streaming chat failed: ${response.code()} - ${response.message()}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Streaming chat error", e)
            throw e
        }
    }
    
    /**
     * 语音合成（TTS）
     * 将文字转换为语音
     * 
     * @param text 要合成的文字
     * @param voice 音色（可选，默认使用BV700_V2_streaming）
     * @return 语音文件
     */
    suspend fun synthesizeSpeech(
        text: String,
        voice: String = DEFAULT_VOICE
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            checkApiKey("voice").onFailure { return@withContext Result.failure(it) }

            Log.d(TAG, "Synthesizing speech: text=$text, voice=$voice")
            
            val request = TTSRequest(
                input = text,
                voice = voice
            )
            
            val response = apiService.synthesizeSpeech(request)
            
            if (response.isSuccessful && response.body() != null) {
                // 保存音频文件到临时目录
                val tempDir = File(context.cacheDir, "tts")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }
                
                val audioFile = File(tempDir, "tts_${System.currentTimeMillis()}.mp3")
                
                response.body()!!.byteStream().use { input ->
                    audioFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "TTS successful: ${audioFile.absolutePath}")
                Result.success(audioFile)
            } else {
                val error = "TTS failed: ${response.code()} - ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "TTS error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 完整的对话流程
     * ASR → LLM → TTS
     * 
     * @param audioFile 用户录音文件
     * @param sessionId 会话ID
     * @param appCode 应用code（可选）
     * @return 对话状态流
     */
    fun processVoiceInput(
        audioFile: File,
        sessionId: String,
        appCode: String? = null
    ): Flow<ChatState> = flow {
        try {
            // 1. ASR - 语音识别
            emit(ChatState.Transcribing)
            val asrResult = transcribeAudio(audioFile)
            
            if (asrResult.isFailure) {
                emit(ChatState.Error("语音识别失败: ${asrResult.exceptionOrNull()?.message}"))
                return@flow
            }
            
            val userText = asrResult.getOrNull()!!
            emit(ChatState.Transcribed(userText))
            
            // 2. LLM - 对话生成
            emit(ChatState.Generating)
            val chatResult = chat(userText, sessionId, appCode)
            
            if (chatResult.isFailure) {
                emit(ChatState.Error("对话生成失败: ${chatResult.exceptionOrNull()?.message}"))
                return@flow
            }
            
            val chatResponse = chatResult.getOrNull()!!
            val aiText = chatResponse.choices.firstOrNull()?.message?.content ?: ""
            emit(ChatState.Generated(aiText))
            
            // 3. TTS - 语音合成
            emit(ChatState.Synthesizing)
            val ttsResult = synthesizeSpeech(aiText)
            
            if (ttsResult.isFailure) {
                emit(ChatState.Error("语音合成失败: ${ttsResult.exceptionOrNull()?.message}"))
                return@flow
            }
            
            val audioFile = ttsResult.getOrNull()!!
            emit(ChatState.Completed(audioFile))
            
        } catch (e: Exception) {
            Log.e(TAG, "Voice input processing error", e)
            emit(ChatState.Error(e.message ?: "未知错误"))
        }
    }
}

/**
 * 对话状态
 */
sealed class ChatState {
    object Transcribing : ChatState()
    data class Transcribed(val text: String) : ChatState()
    object Generating : ChatState()
    data class Generated(val response: String) : ChatState()
    object Synthesizing : ChatState()
    data class Completed(val audioFile: File) : ChatState()
    data class Error(val message: String) : ChatState()
}
