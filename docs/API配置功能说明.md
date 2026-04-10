# API配置功能说明

## 功能概述

在"我的"页面添加了API配置功能，允许用户配置四类能力：
1. **LinkAI语音API** - 用于ASR（语音识别）和TTS（语音合成）
2. **LinkAI对话API** - 用于LLM（大语言模型对话）
3. **阿里Qwen识图API** - 用于首页智能识图（DashScope视觉模型）
4. **OpenClaw API** - 预留接入OpenClaw（小龙虾）AI自动化代理引擎

## 实现内容

### 1. ApiKeyManager - API Key管理器

**文件**: `app/src/main/java/com/glasses/app/data/local/prefs/ApiKeyManager.kt`

**功能**:
- 使用SharedPreferences安全存储API Keys
- 单例模式，全局访问
- 支持 LinkAI 语音 / 对话、阿里Qwen 识图、OpenClaw 的配置管理

**API列表**:

| API类型 | 用途 | 配置项 |
|---------|------|--------|
| LinkAI语音API | ASR（语音识别）+ TTS（语音合成） | API Key |
| LinkAI对话API | LLM（大语言模型对话） | API Key |
| 阿里Qwen识图API | 智能识图（图片理解） | API Key + 模型 |
| OpenClaw API | AI自动化代理引擎（预留） | API Key + 应用ID |

**主要方法**:
```kotlin
// LinkAI语音API
fun saveLinkAIVoiceApiKey(apiKey: String)
fun getLinkAIVoiceApiKey(): String
fun hasLinkAIVoiceApiKey(): Boolean

// LinkAI对话API
fun saveLinkAIChatApiKey(apiKey: String)
fun getLinkAIChatApiKey(): String
fun hasLinkAIChatApiKey(): Boolean

// 阿里Qwen识图API
fun saveAliQwenVisionApiKey(apiKey: String)
fun getAliQwenVisionApiKey(): String
fun hasAliQwenVisionApiKey(): Boolean
fun saveAliQwenVisionModel(model: String)
fun getAliQwenVisionModel(): String

// OpenClaw API
fun saveOpenClawApiKey(apiKey: String)
fun getOpenClawApiKey(): String
fun saveOpenClawAppId(appId: String)
fun getOpenClawAppId(): String
fun hasOpenClawApiKey(): Boolean
fun hasOpenClawAppId(): Boolean

// 通用方法
fun clearAllApiKeys()
fun hasAllRequiredApiKeys(): Boolean
```

### 2. ProfileViewModel - 添加API配置功能

**文件**: `app/src/main/java/com/glasses/app/viewmodel/ProfileViewModel.kt`

**新增功能**:
- `showApiConfig()` - 显示API配置对话框
- `hideApiConfig()` - 隐藏API配置对话框
- `saveApiConfig()` - 保存API配置

**UI状态**:
```kotlin
data class ProfileUiState(
    ...
    val showApiConfigDialog: Boolean = false
)
```

### 3. ProfileScreen - API配置界面

**文件**: `app/src/main/java/com/glasses/app/ui/profile/ProfileScreen.kt`

**新增组件**:
- 菜单项："API配置"
- `ApiConfigDialog` - API配置对话框

**对话框功能**:
- 显示 LinkAI、阿里Qwen、OpenClaw 的配置输入框
- 自动加载已保存的API Keys
- 支持留空（不修改已保存的值）
- 保存后显示成功提示

## 使用方法

### 用户操作流程

1. 打开App，进入"我的"页面
2. 点击"API配置"菜单项
3. 弹出API配置对话框
4. 输入API Keys：
   - LinkAI语音API Key
   - LinkAI对话API Key
   - 阿里Qwen识图 API Key
   - 阿里Qwen识图模型
   - OpenClaw API Key（可选）
   - OpenClaw应用ID（可选）
5. 点击"保存"按钮
6. 显示"API配置已保存"提示

### 开发者集成

```kotlin
// 获取ApiKeyManager实例
val apiKeyManager = ApiKeyManager.getInstance(context)

// 获取API Keys
val voiceApiKey = apiKeyManager.getLinkAIVoiceApiKey()
val chatApiKey = apiKeyManager.getLinkAIChatApiKey()
val aliVisionApiKey = apiKeyManager.getAliQwenVisionApiKey()
val aliVisionModel = apiKeyManager.getAliQwenVisionModel()
val openclawApiKey = apiKeyManager.getOpenClawApiKey()
val openclawAppId = apiKeyManager.getOpenClawAppId()

// 检查是否已配置
if (apiKeyManager.hasAllRequiredApiKeys()) {
    // 所有必需的API Keys都已配置
}

// 智能识图前检查阿里视觉Key
if (apiKeyManager.hasAliQwenVisionApiKey()) {
    // 可以调用阿里Qwen视觉接口
}
```

## UI设计

### API配置对话框

**标题**: API配置 + 设置图标

**内容区**:
1. 说明文字（灰色背景）
   - "请输入您的API Key，留空表示不修改"

2. LinkAI语音API
   - 标题：蓝色
   - 输入框：语音API Key (ASR + TTS)

3. LinkAI对话API
   - 标题：蓝色
   - 输入框：对话API Key (LLM)

4. 阿里Qwen识图 API
   - 标题：橙色
   - 输入框：阿里API Key（DashScope）
   - 单选项：视觉模型
     - `qwen3.6-plus-2026-04-02`
     - `qwen3.5-flash`

5. OpenClaw API（预留）
   - 标题：绿色
   - 说明：OpenClaw是开源AI自动化代理引擎
   - 输入框1：OpenClaw API Key
   - 输入框2：OpenClaw应用ID

**按钮**:
- 取消（灰色文字按钮）
- 保存（蓝色按钮）

## OpenClaw背景

### 什么是OpenClaw？

OpenClaw（小龙虾）是2026年开年最火爆的开源AI自动化代理引擎，GitHub星标已突破28万。

**核心能力**:
- 理解自然语言指令
- 自主拆解任务
- 调用工具执行操作
- 多渠道交互（包括语音唤醒与对话）

**与传统AI的区别**:
- 传统AI：生成文本、提供建议，不执行操作
- OpenClaw：理解指令后直接执行实际操作

**应用场景**:
- 系统级操作（文件管理、脚本执行）
- 浏览器自动化（网页浏览、表单填写）
- 办公自动化（邮件处理、文档处理）
- 多渠道交互（IM接入、语音对话）

### 为什么预留OpenClaw接口？

1. **趋势**: OpenClaw社区已宣布开发智能眼镜版本
2. **能力**: OpenClaw的自主执行能力适合AR眼镜场景
3. **生态**: 700+社区技能插件可扩展眼镜功能
4. **未来**: 公司正在做OpenClaw专项优化

## 下一步工作

### 1. LinkAI语音/对话使用动态API Key

当前 LinkAI 语音和对话能力已使用 ApiKeyManager 动态读取 Key：

```kotlin
LinkAIClient.reloadApiKey()
```

### 2. 阿里Qwen智能识图链路

- 首页智能识图使用阿里 DashScope OpenAI 兼容接口
- 调用时将本地图片转为 `data:image/...;base64,...`
- 当前识图结果要求统一输出前缀：
  - `图片识别：`

### 3. 智能识图当前流程

1. 首页触发拍照
2. WiFi 同步最新图片到本地
3. 读取最新图片并转为 Base64 Data URL
4. 调用阿里Qwen视觉模型
5. 结果写入 AI 对话页

### 4. 添加API Key验证

- 在保存时验证API Key格式
- 在使用前检查API Key是否已配置
- 提供测试API Key连接的功能

### 5. OpenClaw集成（未来）

- 实现OpenClaw Lite轻量级执行引擎
- 接入OpenClaw技能生态
- 实现与云端OpenClaw服务的协同

## 安全考虑

1. **存储安全**: 使用SharedPreferences的MODE_PRIVATE模式
2. **传输安全**: API Key通过HTTPS传输
3. **显示安全**: 输入框可以设置为密码模式（可选）
4. **清除功能**: 提供清空所有API Keys的功能

## 测试建议

### 功能测试
1. ✅ 打开API配置对话框
2. ✅ 输入API Keys并保存
3. ✅ 验证保存成功提示
4. ✅ 重新打开对话框验证已保存的值
5. ✅ 留空输入框验证不修改已保存的值
6. ✅ 点击取消按钮验证不保存

### 集成测试
1. ⏳ 配置 LinkAI 语音/对话 Key 后调用 LinkAI API
2. ⏳ 配置阿里Qwen识图 Key 后执行首页智能识图
3. ⏳ 验证识图模型切换是否生效
4. ⏳ 验证未配置 Key 时的错误处理

### UI测试
1. ✅ 对话框布局正确
2. ✅ 输入框可以正常输入
3. ✅ 按钮点击响应正确
4. ✅ 提示信息显示正确

## 相关文件

- **ApiKeyManager**: `app/src/main/java/com/glasses/app/data/local/prefs/ApiKeyManager.kt`
- **ProfileViewModel**: `app/src/main/java/com/glasses/app/viewmodel/ProfileViewModel.kt`
- **ProfileScreen**: `app/src/main/java/com/glasses/app/ui/profile/ProfileScreen.kt`
- **AIServiceImpl**: `app/src/main/java/com/glasses/app/data/remote/api/AIServiceImpl.kt`
- **HomeViewModel**: `app/src/main/java/com/glasses/app/viewmodel/HomeViewModel.kt`
- **OpenClaw背景**: `docs/OpenClaw背景.md`

## 参考资料

- OpenClaw GitHub: https://github.com/openclaw/openclaw
- OpenClaw背景文档: `docs/OpenClaw背景.md`
- LinkAI API文档: `docs/linkai接口.md`
- 阿里 DashScope 视觉模型文档: https://www.alibabacloud.com/help/en/model-studio/vision

---

© 2026 Linkai Team. All Rights Reserved.
作者: 搏哥 (Linkai Team)
