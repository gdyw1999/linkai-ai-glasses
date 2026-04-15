package com.glasses.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 共享渲染内容 ViewModel
 * 用于 ChatScreen 和 ContentRenderScreen 之间传递数据
 * 通过导航时共享此 ViewModel，实现跨页面数据传递
 */
class SharedRenderViewModel : ViewModel() {

    private val _renderContent = MutableStateFlow<RenderContent?>(null)
    val renderContent: StateFlow<RenderContent?> = _renderContent.asStateFlow()

    /**
     * 设置要渲染的内容
     * @param content Markdown 或 HTML 内容
     * @param type 内容类型
     */
    fun setRenderContent(content: String, type: ContentType) {
        _renderContent.value = RenderContent(
            content = content,
            type = type,
            title = when(type) {
                ContentType.MARKDOWN -> "Markdown 文档"
                ContentType.HTML -> "HTML 页面"
            }
        )
    }

    /**
     * 清空渲染内容
     */
    fun clearRenderContent() {
        _renderContent.value = null
    }
}

/**
 * 渲染内容数据类
 * @param content Markdown 或 HTML 内容
 * @param type 内容类型枚举
 * @param title 页面标题
 */
data class RenderContent(
    val content: String,
    val type: ContentType,
    val title: String
)

/**
 * 内容类型枚举
 */
enum class ContentType {
    MARKDOWN,
    HTML
}
