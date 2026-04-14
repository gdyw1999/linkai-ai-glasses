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
versionCode = 214   // 与 versionName 同步递增
versionName = "0.214"
```

## 架构要点

### 导航与页面

- `MainActivity.kt` — Scaffold + 底部导航栏（Chat 页面全屏隐藏导航栏，其他页面正常显示）
- `NavGraph.kt` — 路由定义，所有页面接收 `innerPadding: PaddingValues`；Chat 页面传 `PaddingValues(0)` 实现全屏
- 4 个 Tab 页：Home / Chat / Gallery / Profile
- Chat 页面：顶部返回按钮（`onBack`）导航回首页，导航栏随之恢复

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
- 基础 Material Icons 集（无 extended 库），`Icons.Default.Mic` 不可用，已用 `painterResource(R.drawable.ic_mic)` 替代；`ArrowBack` / `Menu` / `Add` 等可用
- 前台服务和蓝牙操作需 `@SuppressLint("MissingPermission")` 并配合 try-catch SecurityException
- 所有 Scaffold 内的页面必须接收并使用 `innerPadding`，否则底部内容被导航栏遮住

## 已知问题与修复记录

### ChatScreen 功能栏与二级选择器（v0.214）

**功能**：对话页面新增功能栏快捷入口，支持教学场景快捷操作

**功能列表**：
- 快速对话 — 无前缀，正常自由对话
- 教学游戏 — 前缀 `教学游戏、{学科}、{年级}：`
- 作文批改 — 前缀 `作文批改、{学科}、{年级}：`
- 作业解析 — 前缀 `作业解析、{学科}、{年级}：`
- AI命题 — 前缀 `AI命题、{学科}、{年级}、{试卷类型}：`
- AI组题 — 前缀 `AI组题、{学科}、{年级}：`

**二级选择器**：
- 学科：语文 / 数学 / 英语
- 年级：小学一年级~六年级、初一~初三、高一~高三
- 试卷类型（仅AI命题）：期中 / 期末 / 月考 / 专项练习

**交互逻辑**：
1. 点击功能按钮切换当前功能（高亮蓝色显示）
2. 非"快速对话"功能时，下方展开二级选择栏
3. 学科/年级/试卷类型点击切换（带记忆功能）
4. 输入框左侧显示蓝色前缀预览
5. 发送时自动组合前缀+用户输入

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

### 录音文件 .opus 类型识别（v0.214）

**现象**：眼镜录音（`.opus` 格式）同步后在相册中不显示

**原因**：`MediaSyncManager` 的 `addMediaFile()` 和 `detectMediaType()` 仅识别 `.wav`/`.mp3` 为 AUDIO，`.opus` 被兜底为 IMAGE

**修复**：两处类型检测都添加了 `.opus` → `MediaType.AUDIO`；已持久化的错误分类需清除应用数据或重新同步
