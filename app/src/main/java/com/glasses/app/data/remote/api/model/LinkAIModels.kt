package com.glasses.app.data.remote.api.model

import com.google.gson.annotations.SerializedName

/**
 * LinkAI API数据模型
 */

// ==================== 超级AI助理（v1/chat/completions）====================

/**
 * 超级AI助理请求（OpenAI 兼容格式）
 */
data class CompletionsRequest(
    @SerializedName("messages")
    val messages: List<ChatMessageItem>,

    @SerializedName("app_code")
    val appCode: String? = null,

    @SerializedName("session_id")
    val sessionId: String? = null,

    @SerializedName("stream")
    val stream: Boolean = false,

    @SerializedName("stream_options")
    val streamOptions: StreamOptions? = null
)

/**
 * 聊天消息项（OpenAI messages 格式）
 */
data class ChatMessageItem(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: String
)

// ==================== ASR（语音识别）====================

/**
 * ASR响应
 */
data class TranscriptionResponse(
    @SerializedName("text")
    val text: String
)

// ==================== LLM（对话）====================

/**
 * 对话请求
 */
data class ChatRequest(
    @SerializedName("question")
    val question: String,
    
    @SerializedName("session_id")
    val sessionId: String? = null,
    
    @SerializedName("app_code")
    val appCode: String? = null,
    
    @SerializedName("model")
    val model: String? = null,
    
    @SerializedName("temperature")
    val temperature: Float? = null,
    
    @SerializedName("stream")
    val stream: Boolean = false,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    // 流式请求选项（请求 token 用量统计）
    @SerializedName("stream_options")
    val streamOptions: StreamOptions? = null
)

/**
 * 对话响应
 */
data class ChatResponse(
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("choices")
    val choices: List<Choice>,
    
    @SerializedName("usage")
    val usage: Usage? = null
)

/**
 * 对话选项
 */
data class Choice(
    @SerializedName("index")
    val index: Int,
    
    @SerializedName("message")
    val message: Message? = null,
    
    @SerializedName("delta")
    val delta: Delta? = null,
    
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

/**
 * 消息
 */
data class Message(
    @SerializedName("role")
    val role: String,
    
    @SerializedName("content")
    val content: String
)

/**
 * 流式增量消息
 */
data class Delta(
    @SerializedName("content")
    val content: String? = null,

    // DeepSeek-R1 等推理模型的思考过程
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

/**
 * 流式请求选项（用于获取 token 用量）
 */
data class StreamOptions(
    @SerializedName("include_usage")
    val includeUsage: Boolean = true
)

/**
 * Token使用情况
 */
data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    
    @SerializedName("total_tokens")
    val totalTokens: Int
)

// ==================== TTS（语音合成）====================

/**
 * TTS请求
 */
data class TTSRequest(
    @SerializedName("input")
    val input: String,
    
    @SerializedName("voice")
    val voice: String? = null,
    
    @SerializedName("app_code")
    val appCode: String? = null
)

// ==================== 错误响应 ====================

/**
 * API错误响应
 */
data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean = false,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: Any? = null,
    
    @SerializedName("error")
    val error: Error? = null
)

/**
 * 错误详情
 */
data class Error(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("type")
    val type: String
)

/**
 * 阿里 Qwen 视觉模型选项
 */
object AliQwenVisionModels {
    const val QWEN_36_PLUS = "qwen3.6-plus-2026-04-02"
    const val QWEN_35_FLASH = "qwen3.5-flash"

    val all = listOf(QWEN_36_PLUS, QWEN_35_FLASH)
}
