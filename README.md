# 智教未来 (Linkai Future) Android App

> **智教未来** - AI教育平台移动端，配合 Linkai星韵AI眼镜 增强体验

## 产品介绍

**智教未来**是 AI 教育平台的移动端应用，支持 AI 对话、智能识图、教学场景快捷操作等功能。连接 **Linkai星韵AI眼镜** 后可解锁语音唤醒、眼镜拍照识图、WiFi相册同步等增强能力。

App 可独立使用，无需眼镜即可进行 AI 对话。配合 Web 端（[zhijiao-edu](https://github.com/gdyw1999/zhijiao-edu)）形成完整的教育平台体验。

### 产品特点

- AI智能对话 — 基于LinkAI超级AI助理的流式对话，支持思考过程展示
- 教学场景快捷入口 — 教学游戏、作文批改、作业解析、AI命题、AI组题
- 智能识图 — 拍照/上传图片 → 千问识别 → 超级AI助理分析
- 语音唤醒 — 配合眼镜说"嘿塞恩"自动进入对话
- 星韵AI眼镜 — 蓝牙连接、拍照录像、WiFi相册同步、电量管理

---

## 项目信息

| 项目 | 说明 |
|------|------|
| **App名称** | 智教未来 (Linkai Future) |
| **关联平台** | [zhijiao-edu](https://github.com/gdyw1999/zhijiao-edu) (Web端) |
| **配套硬件** | Linkai星韵AI眼镜（可选） |
| **包名** | com.glasses.app |
| **最低SDK** | Android 7.0 (API 24) |
| **目标SDK** | Android 14 (API 34) |
| **开发语言** | Kotlin |
| **UI框架** | Jetpack Compose |

---

## 技术栈

| 分类 | 技术 |
|------|------|
| **UI** | Jetpack Compose + Material3 |
| **架构** | MVVM (ViewModel + Repository) |
| **数据库** | Room |
| **网络** | Retrofit + OkHttp |
| **图片加载** | Coil |
| **权限管理** | 原生ActivityCompat |
| **蓝牙SDK** | 青橙SDK (LIB_GLASSES_SDK-release.aar) |
| **AI服务** | LinkAI API（语音对话） + 阿里 DashScope Qwen（智能识图） |

---

## 项目结构

```
app/src/main/java/com/glasses/app/
├── ui/                          # UI层
│   ├── home/                    # 首页模块
│   ├── chat/                    # AI对话模块
│   ├── gallery/                 # 相册模块
│   ├── profile/                 # 我的页模块
│   └── theme/                   # Compose主题
├── viewmodel/                   # ViewModel层
├── data/                        # 数据层
│   ├── repository/              # 数据仓库
│   ├── local/                   # 本地数据源
│   │   ├── db/                  # Room数据库
│   │   └── prefs/               # SharedPreferences
│   └── remote/                  # 远程数据源
│       ├── sdk/                 # 青橙SDK封装
│       └── api/                 # LinkAI API
├── domain/                      # 领域层
│   ├── model/                   # 数据模型
│   └── usecase/                 # 业务用例
├── service/                     # 服务层
│   ├── wakeup/                  # 唤醒服务（语音/按键）
│   └── GlassesConnectionService # 蓝牙连接服务
├── manager/                     # 管理器层
│   └── RecordingManager         # 录音管理
└── util/                        # 工具类
```

---

## 核心功能

### MVP阶段（当前）

| 模块 | 功能 |
|------|------|
| **设备连接** | 蓝牙扫描、连接、断开、电量查询 |
| **媒体采集** | 拍照、录像、录音、智能识图（拍照后自动识别） |
| **AI对话** | 语音识别、对话生成、语音合成、会话管理、识图结果展示 |
| **语音唤醒** | 眼镜端检测唤醒词"嘿塞恩"，APP自动跳转对话页并开始录音 |
| **媒体管理** | WiFi同步、相册浏览 |

---

## 开发环境

- **Android Studio**: Hedgehog | 2023.1.1 或更高版本
- **Kotlin**: 1.9.22
- **Gradle**: 8.2.2
- **JDK**: 17

---

## 快速开始

### 构建项目

```bash
# 1. 克隆项目
git clone https://github.com/gdyw1999/linkai-ai-glasses.git
cd linkai-ai-glasses

# 2. 使用Android Studio打开项目

# 3. 等待Gradle同步完成

# 4. 连接Android设备或启动模拟器

# 5. 点击运行按钮
```

### 构建APK

```bash
# Windows
./build-apk.bat

# Linux/Mac
./build-apk.sh
```

---

## 依赖库

详见 `app/build.gradle.kts`

主要依赖：
- Jetpack Compose BOM 2024.02.00
- Room 2.6.1
- Retrofit 2.9.0
- OkHttp 4.12.0
- Coil 2.5.0
- XXPermissions 20.0（已改用原生ActivityCompat）

---

## AI能力说明

### 语音对话

- 使用 LinkAI 语音 API：ASR（语音识别）+ TTS（语音合成）
- 使用 LinkAI 对话 API：LLM 流式对话

### 语音唤醒

- 眼镜端本地检测唤醒词"嘿塞恩"，通过 SDK 回调通知 APP
- APP 收到唤醒事件后自动跳转到新对话页面并开始录音
- 支持后台唤醒：APP 在后台时唤醒能拉起前台
- 完整流程：唤醒词 → 跳转对话页 → 自动录音 → ASR → 超级AI助理回复

### 智能识图

- 首页点击“智能识图”后，流程为：拍照 -> WiFi同步最新图片 -> 本地图片转 Base64 Data URL -> 调用阿里 DashScope Qwen 视觉模型 -> 结果写入 AI 对话页
- 当前支持模型：
  - `qwen3.6-plus-2026-04-02`
  - `qwen3.5-flash`
- 当前识图提示词固定为：
  - `识别图片内容，输出开头使用：“图片识别：xxxxxxx”`

### API配置

在”我的 -> API配置”中支持以下配置项：

- LinkAI语音 API Key
- LinkAI对话 API Key
- **LinkAI App Code**（可选，用于指定 LinkAI 后台工作流）
- 阿里Qwen识图 API Key（DashScope）
- 阿里Qwen识图模型
- OpenClaw API Key / 应用ID（预留）

### 文本输入对话

AI 对话页底部支持：
- **文本输入框** — 手动打字发送消息，直接调用 LinkAI LLM 流式对话
- **语音按钮** — 按住录音，经 ASR → LLM → TTS 完整流程
- 两种方式均支持通过 App Code 指定 LinkAI 工作流

---

## 参考文档

| 文档 | 路径 |
|------|------|
| 设计文档 | `docs/superpowers/specs/2026-03-16-glasses-app-design.md` |
| MVP定义 | `04-MVP核心功能定义.md` |
| SDK分析 | `01-SDK分析设计.md` |
| LinkAI接口 | `docs/linkai接口.md` |
| 青橙SDK说明 | `src/GLASSES_SDK_20260112_V1.1/青橙无线眼镜SDK使用说明.md` |
- 需求文档: `.kiro/specs/glasses-app-mvp/requirements.md`
- 设计文档: `.kiro/specs/glasses-app-mvp/design.md`
- 官方SDK demo: `src/GLASSES_SDK_20260112_V1.1/GlassesSDKSample/`
- LinkAI API文档: `docs/linkai接口.md`

## 注意事项

1. **复用优先**: 标注"复用官方demo"的任务,直接从官方SDK demo复制代码
2. **Checkpoint**: 在关键节点设置Checkpoint,确保功能正常后再继续
3. **官方demo路径**: `src/GLASSES_SDK_20260112_V1.1/GlassesSDKSample/`


## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.219 | 2026-05-12 | 唤醒词"嘿塞恩"接入对话流程：眼镜端检测唤醒词后APP自动跳转对话页并开始录音 |
| v0.216 | 2026-04-16 | HTML/Markdown内容渲染页面、输入栏按钮优化、网络超时180秒、录音WAV转换 |
| v0.215 | 2026-04-15 | 导航重组：对话列表作为首页，AI对话从Tab移除，新增搜索/重命名/删除会话 |
| v0.214 | 2026-04-12 | ChatScreen功能栏与教学场景快捷输入（6功能+学科/年级/试卷类型选择） |
| v0.212 | 2026-04-11 | 相册同步稳定性修复、媒体文件持久化与本地扫描补充 |
| v0.211 | 2026-04-11 | AI对话页文本输入、LinkAI App Code工作流配置支持 |
| v1.3.0 | 2026-04-11 | 智能识图切换为阿里Qwen Base64方案，支持模型选择与结果写入AI对话页 |
| v1.2.0 | 2026-04-09 | API Key动态配置、日志系统（AppLogger）、媒体采集实时状态 |
| v1.1.0 | 2026-04-09 | 修复鸿蒙兼容性崩溃、集成全部后端模块、接入SDK媒体控制 |
| v1.0.0 | 2026-03-17 | 初次提交，基础架构搭建 |

---

## 产品系列

> **智教未来** - AI教育平台（Web + App + 星韵AI眼镜）

---

## 许可证

Copyright © 2026 LinkAI

---

## 联系方式

- GitHub: [@gdyw1999](https://github.com/gdyw1999)
- Email: gdyw1999@163.com
