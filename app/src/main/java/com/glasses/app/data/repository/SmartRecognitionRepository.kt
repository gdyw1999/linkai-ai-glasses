package com.glasses.app.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 智能识图结果中转仓库
 * 在 Home 产生识图结果后推送，Chat 页面消费并写入会话
 */
class SmartRecognitionRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context
) {

    companion object {
        private const val TAG = "SmartRecognitionRepo"

        @Volatile
        private var instance: SmartRecognitionRepository? = null

        fun getInstance(context: Context): SmartRecognitionRepository {
            return instance ?: synchronized(this) {
                instance ?: SmartRecognitionRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _pendingResult = MutableStateFlow<SmartRecognitionResult?>(null)
    val pendingResult: StateFlow<SmartRecognitionResult?> = _pendingResult.asStateFlow()

    fun publish(result: SmartRecognitionResult) {
        _pendingResult.value = result
        Log.d(TAG, "Published smart recognition result: ${result.id}")
    }

    fun clear(resultId: Long) {
        if (_pendingResult.value?.id == resultId) {
            _pendingResult.value = null
            Log.d(TAG, "Cleared smart recognition result: $resultId")
        }
    }
}

data class SmartRecognitionResult(
    val id: Long = System.currentTimeMillis(),
    val imagePath: String,
    val model: String,
    val question: String,
    val answer: String,
    val createdAt: Long = System.currentTimeMillis()
)
