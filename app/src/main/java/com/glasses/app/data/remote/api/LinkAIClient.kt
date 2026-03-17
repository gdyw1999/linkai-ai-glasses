package com.glasses.app.data.remote.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * LinkAI API客户端
 * 配置Retrofit和OkHttp
 */
object LinkAIClient {
    
    private const val BASE_URL = "https://api.link-ai.tech/"
    
    // API Key - 需要在实际使用时替换
    private const val API_KEY = "YOUR_API_KEY"
    
    // 超时配置
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 60L
    private const val WRITE_TIMEOUT = 60L
    
    /**
     * 创建OkHttpClient
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
     * 创建认证拦截器
     * 自动添加Authorization header
     */
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
    }
    
    /**
     * 创建日志拦截器
     * 用于调试，生产环境应关闭
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    /**
     * 创建Retrofit实例
     */
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * 获取LinkAI API服务
     */
    val apiService: LinkAIService by lazy {
        createRetrofit().create(LinkAIService::class.java)
    }
    
    /**
     * 设置API Key
     * 应该在Application初始化时调用
     */
    fun setApiKey(apiKey: String) {
        // TODO: 实现动态设置API Key
        // 当前版本使用常量，后续可以改为动态配置
    }
}
