# Linkai星韵AI眼镜 Android App

> **Linkai星韵AI眼镜** - LinkAI星系列首款AR智能眼镜配套应用

## 产品介绍

**Linkai星韵AI眼镜**是LinkAI星系列的首款AR智能眼镜产品，通过AI语音交互实现智能问答、信息查询等功能。本应用是眼镜的配套手机控制端。

### 产品特点

- 🌟 **AI语音助手** - 基于LinkAI大模型的智能对话
- 📸 **拍照录像** - 一键捕捉精彩瞬间
- 🎤 **录音功能** - 语音备忘录
- 🔄 **WiFi同步** - 快速同步媒体文件
- 🔋 **电量管理** - 实时监控眼镜电量

---

## 项目信息

| 项目 | 说明 |
|------|------|
| **产品名称** | Linkai星韵AI眼镜 |
| **产品系列** | LinkAI星系列 |
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
| **AI服务** | LinkAI API (ASR + LLM + TTS) |

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
| **媒体采集** | 拍照、录像、录音、智能识图 |
| **AI对话** | 语音识别、对话生成、语音合成、会话管理 |
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
| v1.0.0 | 2026-03-17 | 初次提交，基础架构搭建 |
| v1.1.0 | 2026-04-09 | 修复鸿蒙兼容性崩溃、集成全部后端模块、接入SDK媒体控制 |
| v1.2.0 | 2026-04-09 | API Key动态配置、日志系统（AppLogger）、媒体采集实时状态 |

---

## 产品系列

> **LinkAI星系列** - 专注于AI智能穿戴设备的创新产品线

---

## 许可证

Copyright © 2026 LinkAI

---

## 联系方式

- GitHub: [@gdyw1999](https://github.com/gdyw1999)
- Email: gdyw1999@163.com
