package com.glasses.app.data.remote.api

import android.util.Log
import com.glasses.app.data.remote.api.model.ErrorResponse
import com.google.gson.Gson
import retrofit2.Response

/**
 * API错误处理器
 * 统一处理LinkAI API的错误响应
 */
object ApiErrorHandler {
    
    private const val TAG = "ApiErrorHandler"
    
    /**
     * 解析错误响应
     */
    fun <T> parseError(response: Response<T>): ApiError {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                mapErrorResponse(errorResponse, response.code())
            } else {
                ApiError.Unknown("未知错误: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse error response", e)
            ApiError.Unknown("解析错误响应失败: ${e.message}")
        }
    }
    
    /**
     * 映射错误响应到ApiError
     */
    private fun mapErrorResponse(errorResponse: ErrorResponse, statusCode: Int): ApiError {
        return when (statusCode) {
            400 -> ApiError.BadRequest(errorResponse.message)
            401 -> ApiError.Unauthorized("API Key无效或已过期")
            402 -> ApiError.AppNotFound("应用不存在，请检查app_code参数")
            403 -> ApiError.Forbidden("无访问权限")
            406 -> ApiError.InsufficientCredits("账号积分额度不足")
            408 -> ApiError.NoApiAccess("无API访问权限，需要标准版及以上")
            409 -> ApiError.ContentModeration("内容审核不通过，可能存在敏感词")
            503 -> ApiError.ServiceUnavailable("服务暂时不可用")
            else -> ApiError.Unknown("错误码: $statusCode, 消息: ${errorResponse.message}")
        }
    }
    
    /**
     * 获取用户友好的错误消息
     */
    fun getUserFriendlyMessage(error: ApiError): String {
        return when (error) {
            is ApiError.BadRequest -> "请求格式错误，请稍后重试"
            is ApiError.Unauthorized -> "认证失败，请检查API配置"
            is ApiError.AppNotFound -> "应用配置错误"
            is ApiError.Forbidden -> "无访问权限"
            is ApiError.InsufficientCredits -> "账号余额不足，请充值"
            is ApiError.NoApiAccess -> "当前版本不支持此功能"
            is ApiError.ContentModeration -> "内容包含敏感词，请修改后重试"
            is ApiError.ServiceUnavailable -> "服务暂时不可用，请稍后重试"
            is ApiError.NetworkError -> "网络连接失败，请检查网络"
            is ApiError.Timeout -> "请求超时，请重试"
            is ApiError.Unknown -> "发生未知错误: ${error.message}"
        }
    }
    
    /**
     * 处理异常
     */
    fun handleException(e: Exception): ApiError {
        return when (e) {
            is java.net.UnknownHostException -> ApiError.NetworkError("无法连接到服务器")
            is java.net.SocketTimeoutException -> ApiError.Timeout("请求超时")
            is java.io.IOException -> ApiError.NetworkError("网络错误: ${e.message}")
            else -> ApiError.Unknown(e.message ?: "未知错误")
        }
    }
}

/**
 * API错误类型
 */
sealed class ApiError(open val message: String) {
    data class BadRequest(override val message: String) : ApiError(message)
    data class Unauthorized(override val message: String) : ApiError(message)
    data class AppNotFound(override val message: String) : ApiError(message)
    data class Forbidden(override val message: String) : ApiError(message)
    data class InsufficientCredits(override val message: String) : ApiError(message)
    data class NoApiAccess(override val message: String) : ApiError(message)
    data class ContentModeration(override val message: String) : ApiError(message)
    data class ServiceUnavailable(override val message: String) : ApiError(message)
    data class NetworkError(override val message: String) : ApiError(message)
    data class Timeout(override val message: String) : ApiError(message)
    data class Unknown(override val message: String) : ApiError(message)
}
