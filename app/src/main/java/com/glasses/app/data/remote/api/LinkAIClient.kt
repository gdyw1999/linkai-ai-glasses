package com.glasses.app.data.remote.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * LinkAI API客户端
 * 支持动态 API Key 注入，每次请求从 ApiKeyManager 读取最新 Key
 */
object LinkAIClient {

    private const val BASE_URL = "https://api.link-ai.tech/"
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 180L  // 3分钟，适配慢速LLM回复（实测132秒）
    private const val WRITE_TIMEOUT = 60L

    // 当前使用的 API Key（运行时动态更新）
    @Volatile
    private var currentApiKey: String = ""

    // 应用 Context，用于从 ApiKeyManager 读取 Key
    private var appContext: Context? = null

    /**
     * 初始化，传入 Context 以便动态读取 ApiKeyManager
     * 在 Application.onCreate 中调用
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 从 ApiKeyManager 重新加载 API Key
     * 每次 API 调用前会自动调用，确保使用最新 Key
     */
    fun reloadApiKey() {
        val ctx = appContext ?: return
        val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(ctx)
        val voiceKey = apiKeyManager.getLinkAIVoiceApiKey()
        val chatKey = apiKeyManager.getLinkAIChatApiKey()
        // 备用：优先语音Key，再对话Key
        currentApiKey = voiceKey.ifEmpty { chatKey }
    }

    /**
     * 根据请求类型获取对应的 API Key
     * chat/completions → 对话 Key
     * audio/transcriptions 或 audio/speech → 语音 Key
     */
    private fun selectApiKeyForUrl(url: okhttp3.HttpUrl): String {
        val ctx = appContext ?: return currentApiKey
        val apiKeyManager = com.glasses.app.data.local.prefs.ApiKeyManager.getInstance(ctx)
        val path = url.encodedPath

        return when {
            // 对话相关路径使用对话 Key
            path.contains("chat/") -> apiKeyManager.getLinkAIChatApiKey()
            // 语音相关路径使用语音 Key
            path.contains("audio/") -> apiKeyManager.getLinkAIVoiceApiKey()
            // 其他路径使用备用逻辑
            else -> apiKeyManager.getLinkAIVoiceApiKey().ifEmpty { apiKeyManager.getLinkAIChatApiKey() }
        }.ifEmpty { currentApiKey }
    }

    /**
     * 获取当前 API Key（供外部检查）
     */
    fun getCurrentApiKey(): String = currentApiKey

    /**
     * 检查是否已配置 API Key
     */
    fun hasApiKey(): Boolean = currentApiKey.isNotEmpty()

    /**
     * 创建 OkHttpClient
     * 认证拦截器每次请求时动态读取最新 Key
     */
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(createAuthInterceptor())
            .addInterceptor(createLoggingInterceptor())
            .build()
    }

    /**
     * 认证拦截器 - 按请求路径动态选择对应的 API Key
     * chat 路径用对话 Key，audio 路径用语音 Key
     */
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            reloadApiKey()

            val original = chain.request()
            // 按请求 URL 选择对应类型的 Key
            val key = selectApiKeyForUrl(original.url)

            val request = original.newBuilder()
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
    }

    /**
     * 日志拦截器
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Retrofit 实例（懒加载）
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * LinkAI API 服务
     */
    val apiService: LinkAIService by lazy {
        retrofit.create(LinkAIService::class.java)
    }
}
