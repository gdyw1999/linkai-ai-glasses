package com.glasses.app.data.remote.api

import com.glasses.app.data.remote.api.model.ChatRequest
import com.glasses.app.data.remote.api.model.ChatResponse
import com.glasses.app.data.remote.api.model.TTSRequest
import com.glasses.app.data.remote.api.model.TranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * LinkAI API服务接口
 * 定义ASR、LLM、TTS三个核心接口
 */
interface LinkAIService {
    
    /**
     * 语音识别（ASR）
     * 将语音文件转换为文本
     * 
     * @param file 语音文件（支持 mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm, flac, amr）
     * @return 识别出的文本内容
     */
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part
    ): Response<TranscriptionResponse>
    
    /**
     * 记忆对话（LLM）
     * 带上下文记忆的对话API
     * 
     * @param request 对话请求
     * @return 对话响应
     */
    @POST("v1/chat/memory/completions")
    suspend fun chat(
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    /**
     * 流式记忆对话（LLM）
     * 流式输出的对话API
     * 
     * @param request 对话请求（stream=true）
     * @return 流式响应
     */
    @Streaming
    @POST("v1/chat/memory/completions")
    suspend fun chatStreaming(
        @Body request: ChatRequest
    ): Response<ResponseBody>
    
    /**
     * 语音合成（TTS）
     * 将文本转换为语音
     * 
     * @param request TTS请求
     * @return 语音文件（二进制流）
     */
    @POST("v1/audio/speech")
    suspend fun synthesizeSpeech(
        @Body request: TTSRequest
    ): Response<ResponseBody>
}
