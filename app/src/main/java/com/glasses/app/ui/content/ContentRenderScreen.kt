package com.glasses.app.ui.content

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glasses.app.viewmodel.ContentType
import com.glasses.app.viewmodel.RenderContent
import com.glasses.app.viewmodel.SharedRenderViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.syntax.highlight.SyntaxHighlightPlugin

/**
 * 内容渲染页面
 * 支持渲染 Markdown 和 HTML 内容
 *
 * 使用方式：
 * 1. 在调用页面设置内容：sharedViewModel.setRenderContent(content, type)
 * 2. 导航到此页面：navController.navigate(NavRoutes.CONTENT_RENDER)
 * 3. 页面自动读取并渲染内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentRenderScreen(
    sharedViewModel: SharedRenderViewModel = viewModel(),
    onBack: () -> Unit
) {
    val renderContent by sharedViewModel.renderContent.collectAsState()
    val context = LocalContext.current

    // 初始化 Markwon（Markdown 渲染引擎）
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())           // 支持 HTML 标签
            .usePlugin(SyntaxHighlightPlugin.create()) // 支持代码高亮
            .build()
    }

    // 没有内容时显示空状态
    if (renderContent == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "没有可渲染的内容",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(renderContent!!.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true          // 启用 JS（支持交互、游戏）
                        domStorageEnabled = true          // 启用本地存储
                        databaseEnabled = true            // 启用数据库
                        loadWithOverviewMode = true       // 适配屏幕
                        useWideViewPort = true
                        builtInZoomControls = true        // 启用缩放
                        displayZoomControls = false       // 隐藏缩放按钮
                    }

                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()

                    // 生成 HTML 内容
                    val htmlContent = generateHtmlContent(renderContent!!, markwon)

                    // 加载内容
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * 生成最终的 HTML 内容
 * @param renderContent 渲染内容数据
 * @param markwon Markdown 渲染引擎
 * @return 完整的 HTML 字符串
 */
private fun generateHtmlContent(
    renderContent: RenderContent,
    markwon: Markwon
): String {
    return if (renderContent.type == ContentType.MARKDOWN) {
        // Markdown 转 HTML
        val markdownHtml = markwon.toMarkdown(renderContent.content)
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    padding: 16px;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    color: #333;
                    word-wrap: break-word;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 24px;
                    margin-bottom: 16px;
                    font-weight: 600;
                    line-height: 1.25;
                }
                h1 { font-size: 2em; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
                h2 { font-size: 1.5em; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
                h3 { font-size: 1.25em; }
                p { margin-bottom: 16px; }
                ul, ol { padding-left: 2em; margin-bottom: 16px; }
                li { margin-bottom: 4px; }
                pre {
                    background: #f5f5f5;
                    padding: 12px;
                    border-radius: 8px;
                    overflow-x: auto;
                    margin-bottom: 16px;
                }
                code {
                    background: #f5f5f5;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: "SF Mono", Monaco, "Cascadia Code", "Roboto Mono", Consolas, monospace;
                    font-size: 0.9em;
                }
                pre code {
                    background: transparent;
                    padding: 0;
                }
                blockquote {
                    border-left: 4px solid #ddd;
                    padding-left: 16px;
                    color: #666;
                    margin-bottom: 16px;
                }
                img { max-width: 100%; height: auto; }
                a { color: #2196F3; text-decoration: none; }
                a:hover { text-decoration: underline; }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin-bottom: 16px;
                }
                th, td {
                    border: 1px solid #ddd;
                    padding: 8px 12px;
                }
                th { background: #f5f5f5; font-weight: 600; }
                hr {
                    border: none;
                    border-top: 1px solid #eee;
                    margin: 24px 0;
                }
            </style>
        </head>
        <body>$markdownHtml</body>
        </html>
        """.trimIndent()
    } else {
        // 直接渲染 HTML
        renderContent.content
    }
}

import androidx.compose.runtime.remember
