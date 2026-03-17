# 青橙AI眼镜 Android App

青橙AI眼镜Android App是一款为青橙无线AR眼镜开发的配套手机控制应用。

## 项目信息

- **包名**: com.glasses.app
- **最低SDK**: Android 7.0 (API 24)
- **目标SDK**: Android 14 (API 35)
- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose

## 技术栈

- **UI**: Jetpack Compose + Material3
- **架构**: MVVM (ViewModel + Repository)
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **图片加载**: Coil
- **权限管理**: XXPermissions
- **事件总线**: EventBus
- **后台任务**: WorkManager
- **蓝牙SDK**: 青橙SDK (LIB_GLASSES_SDK-release.aar)

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
├── manager/                     # 管理器层
└── util/                        # 工具类
    ├── PermissionUtil.kt        # 权限管理（复用官方demo）
    ├── BluetoothUtils.java      # 蓝牙工具（复用官方demo）
    └── ActivityExt.kt           # Activity扩展（复用官方demo）
```

## 核心功能

### MVP阶段（当前）

1. **设备连接管理**
   - 蓝牙设备扫描和连接
   - 连接状态监控
   - 电量查询和显示

2. **媒体采集控制**
   - 拍照、录像、录音控制
   - 智能识图功能

3. **AI语音对话**
   - 语音识别（ASR）
   - 对话生成（LLM）
   - 语音合成（TTS）
   - 会话管理

4. **媒体同步管理**
   - WiFi文件传输
   - 相册浏览和管理

## 开发环境

- **Android Studio**: Hedgehog | 2023.1.1 或更高版本
- **Kotlin**: 1.9.22
- **Gradle**: 8.2.2
- **JDK**: 17

## 构建项目

1. 克隆项目到本地
2. 使用Android Studio打开项目
3. 等待Gradle同步完成
4. 连接Android设备或启动模拟器
5. 点击运行按钮

## 依赖库

详见 `app/build.gradle.kts`

主要依赖：
- Jetpack Compose BOM 2024.02.00
- Room 2.6.1
- Retrofit 2.9.0
- OkHttp 4.12.0
- Coil 2.5.0
- XXPermissions 20.0
- EventBus 3.3.1
- WorkManager 2.9.0

## 参考文档

- [需求文档](.kiro/specs/glasses-app-mvp/requirements.md)
- [设计文档](.kiro/specs/glasses-app-mvp/design.md)
- [任务列表](.kiro/specs/glasses-app-mvp/tasks.md)
- [青橙SDK使用说明](src/GLASSES_SDK_20260112_V1.1/青橙无线眼镜SDK使用说明.md)

## 版本历史

- **v1.0.0** (2026-03-16): 项目初始化，基础架构搭建

## 许可证

Copyright © 2026 青橙AI眼镜团队
