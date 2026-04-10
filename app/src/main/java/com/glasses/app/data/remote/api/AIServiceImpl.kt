package com.glasses.app.data.remote.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.glasses.app.data.remote.api.model.ChatRequest
import com.glasses.app.data.remote.api.model.ChatResponse
import com.glasses.app.data.remote.api.model.TTSRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
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
        private const val DEFAULT_IMAGE_RECOGNITION_PROMPT = "识别图片内容，输出开头使用：“图片识别：xxxxxxx”"
        private const val ALI_QWEN_VISION_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        
        @Volatile
        private var instance: AIServiceImpl? = null
        
        fun getInstance(context: Context): AIServiceImpl {
            return instance ?: synchronized(this) {
                instance ?: AIServiceImpl(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val apiService = LinkAIClient.apiService
    private val gson = Gson()
    private val aliVisionClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private fun getAliVisionApiKey(): String {
        val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
        return apiKeyManager.getAliQwenVisionApiKey()
    }

    private fun getAliVisionModel(): String {
        val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
        return apiKeyManager.getAliQwenVisionModel()
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

    private fun checkAliVisionApiKey(): Result<Unit> {
        val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(context)
        return if (apiKeyManager.hasAliQwenVisionApiKey()) Result.success(Unit)
        else Result.failure(Exception("请先在「我的」→「API配置」中设置阿里Qwen识图 API Key"))
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
     * 图像识别（本地图片）
     * 使用阿里Qwen视觉模型（OpenAI兼容接口）
     */
    suspend fun recognizeImage(
        imagePath: String,
        sessionId: String,
        question: String = DEFAULT_IMAGE_RECOGNITION_PROMPT,
        appCode: String? = null,
        model: String? = null
    ): Result<String> {
        return recognizeImage(
            imageFile = File(imagePath),
            sessionId = sessionId,
            question = question,
            appCode = appCode,
            model = model
        )
    }

    /**
     * 图像识别（文件）
     */
    suspend fun recognizeImage(
        imageFile: File,
        sessionId: String,
        question: String = DEFAULT_IMAGE_RECOGNITION_PROMPT,
        appCode: String? = null,
        model: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            checkAliVisionApiKey().onFailure { return@withContext Result.failure(it) }

            if (!imageFile.exists()) {
                return@withContext Result.failure(Exception("图片不存在: ${imageFile.absolutePath}"))
            }

            @Suppress("UNUSED_VARIABLE")
            val ignoredSessionId = sessionId
            @Suppress("UNUSED_VARIABLE")
            val ignoredAppCode = appCode

            val imageDataUrl = buildImageDataUrl(imageFile)
            val finalModel = model ?: getAliVisionModel()
            val requestPayload = mapOf(
                "model" to finalModel,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to question
                            ),
                            mapOf(
                                "type" to "image_url",
                                "image_url" to mapOf(
                                    "url" to imageDataUrl,
                                    "detail" to "high"
                                )
                            )
                        )
                    )
                ),
                "stream" to false
            )

            val requestBody = gson.toJson(requestPayload)
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(ALI_QWEN_VISION_ENDPOINT)
                .header("Authorization", "Bearer ${getAliVisionApiKey()}")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            Log.d(TAG, "Sending Ali Qwen vision request: model=$finalModel")
            aliVisionClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = if (responseBody.isNotBlank()) {
                        "阿里Qwen识图失败: ${response.code} - $responseBody"
                    } else {
                        "阿里Qwen识图失败: ${response.code} - ${response.message}"
                    }
                    Log.e(TAG, message)
                    return@withContext Result.failure(Exception(message))
                }

                val content = parseAliVisionContent(responseBody)
                if (content.isBlank()) {
                    return@withContext Result.failure(Exception("识图结果为空"))
                }
                return@withContext Result.success(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ali Qwen image recognition error", e)
            Result.failure(e)
        }
    }

    private fun parseAliVisionContent(responseBody: String): String {
        if (responseBody.isBlank()) return ""

        return try {
            val root = JsonParser().parse(responseBody).asJsonObject
            val choices = root.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) return ""

            val firstChoice = choices.get(0).asJsonObject
            val messageObj = firstChoice.getAsJsonObject("message")
            val contentElement = messageObj?.get("content") ?: return ""

            when {
                contentElement.isJsonPrimitive -> contentElement.asString
                contentElement.isJsonArray -> {
                    val array = contentElement.asJsonArray
                    buildString {
                        for (i in 0 until array.size()) {
                            val item = array.get(i)
                            val obj = item.asJsonObject
                            val text = when {
                                obj.has("text") -> obj.get("text").asString
                                obj.has("content") -> obj.get("content").asString
                                else -> null
                            }
                            if (text != null) {
                                if (isNotEmpty()) append("\n")
                                append(text)
                            }
                        }
                    }.trim()
                }
                else -> ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Ali vision response", e)
            ""
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

    /**
     * 构建 data:image/...;base64 URL，便于直接上传本地图片
     */
    private fun buildImageDataUrl(imageFile: File): String {
        val mimeType = when (imageFile.extension.lowercase()) {
            "png" -> "png"
            "jpg", "jpeg" -> "jpeg"
            "webp" -> "webp"
            else -> "jpeg"
        }
        val bytes = imageFile.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/$mimeType;base64,$base64"
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
