# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Linkai星韵AI眼镜配套 Android App（Kotlin + Jetpack Compose）。MVVM 架构，minSdk 24 / targetSdk 34。

## 构建命令

**用户手动编译，不需要 Claude 执行构建。** 本地已安装 Android Studio，改完代码后用户自行编译安装。

```bash
# 仅在需要验证编译是否通过时使用
./gradlew compileDebugKotlin

# 输出路径
app/build/outputs/apk/debug/app-debug.apk

# 运行单元测试
./gradlew testDebugUnitTest
```

### 本地开发工具路径

| 工具 | 路径 |
|------|------|
| Android Studio | `E:\Program Files\Android\Android Studio\bin` |
| JDK | `E:\Program Files\Android\Android Studio\corretto-19.0.2` |
| Android SDK / adb | `E:\Android\Sdk\platform-tools` |

## 版本号规则

版本号在 `app/build.gradle.kts` 中维护，格式为 `0.xxx`，每次改动 +1：

```kotlin
versionCode = 211   // 与 versionName 同步递增
versionName = "0.211"
```

## 架构要点

### 导航与页面

- `MainActivity.kt` — Scaffold + 底部导航栏，`innerPadding` 必须传给所有页面避免被导航栏遮住
- `NavGraph.kt` — 路由定义，所有页面接收 `innerPadding: PaddingValues`
- 4 个 Tab 页：Home / Chat / Gallery / Profile

### AI 服务链路

| 场景 | API | 关键文件 |
|------|-----|----------|
| 语音对话 | LinkAI（ASR → LLM 流式 → TTS） | `AIServiceImpl.kt` → `ChatViewModel.kt` |
| 智能识图 | 阿里 Qwen 视觉（Base64 直传） | `HomeViewModel.startAIRecognition()` |
| 文本输入对话 | LinkAI LLM 流式 | `ChatViewModel.sendTextMessage()` |

### API Key 与配置

- `ApiKeyManager.kt` — 所有 API Key / App Code 的存取中心，使用 SharedPreferences
- `LinkAIClient.kt` — OkHttp 拦截器按 URL 路径自动选择 API Key（`chat/` 用对话 Key，`audio/` 用语音 Key）
- `app_code`（LinkAI 工作流）通过 `ApiKeyManager.getLinkAIAppCode()` 传入对话请求

### 眼镜 SDK 交互

- `GlassesSDKManager.kt` — 蓝牙连接、设备管理
- `MediaCaptureManager.kt` — 拍照/录像/录音指令
- `MediaSyncManager.kt` — WiFi 相册同步（`importAlbum()` 是异步的，文件通过回调下载）
- SDK AAR 位于 `app/libs/LIB_GLASSES_SDK-release.aar`

### 智能识图完整流程

`HomeViewModel.startAIRecognition()`:
1. `takePhotoSuspend()` → 眼镜拍照
2. `delay(3000)` → 等眼镜保存照片到相册
3. `syncAndGetLatestImagePath()` → 启动 WiFi 同步 + 轮询等待新图片（20s 超时）
4. `AIServiceImpl.recognizeImage()` → Base64 上传到阿里 Qwen 视觉
5. `SmartRecognitionRepository.publish()` → 结果推送到 ChatViewModel

### 数据持久化

- `ConversationRepository` — Room 数据库，会话和消息的 CRUD
- `ApiKeyManager` — SharedPreferences，API 配置
- `StreamingChatManager` — 管理流式文本的分段和显示

## 编码注意事项

- ViewModel 中必须用 `context.applicationContext` 避免内存泄漏；如需弹 Dialog 等操作，单独保存 `activityContext`
- Gson 版本较旧，用 `JsonParser().parse()` 而非 `JsonParser.parseString()`，用 `choices.get(0)` 而非 `choices[0]`
- 基础 Material Icons 集（无 extended 库），`Icons.Default.Mic` 等不可用，需用 `Settings` / `Send` 等替代
- 前台服务和蓝牙操作需 `@SuppressLint("MissingPermission")` 并配合 try-catch SecurityException
- 所有 Scaffold 内的页面必须接收并使用 `innerPadding`，否则底部内容被导航栏遮住

## 已知问题与修复记录

### 蓝牙连接后崩溃（BadTokenException）

**现象**：连接蓝牙后 APP 崩溃，`WindowManager$BadTokenException: Unable to add window -- token null is not valid`

**原因**：`HomeViewModel` 用 `applicationContext` 弹 AlertDialog（华为保活引导），Dialog 需要 Activity context

**修复**：构造时额外保存 `activityContext`（`context as? Activity`），弹 Dialog 时用它，并检查 `isFinishing/isDestroyed`

### 智能识图"未找到可识别的图片"

**现象**：拍照成功后同步完成，但仍提示"未找到可识别的图片"

**原因**：`takePhoto` 返回后立即调 `importAlbum()`，眼镜端还没保存完照片

**修复**：拍照后 `delay(3000)` 再开始同步；`syncAndGetLatestImagePath()` 加了详细日志方便排查

### 首页底部内容被导航栏遮住

**原因**：`Scaffold` 的 `innerPadding` 未传给页面组件

**修复**：`MainActivity` → `NavGraph` → 所有 4 个页面都接收 `innerPadding`
