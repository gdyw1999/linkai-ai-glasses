# AR眼镜SDK开发分析设计文档

**项目**: 青橙无线眼镜App开发
**重点平台**: Android / 华为 (HarmonyOS)
**日期**: 2026-03-15
**状态**: 需求分析与方案设计

---

## 1. SDK概述

### 1.1 现有资源

| SDK | 平台 | 核心文件 | 开发语言 | 状态 |
|-----|------|----------|----------|------|
| 青橙无线眼镜SDK | Android | `LIB_GLASSES_SDK-release.aar` + `GlassesSDKSample/` | Kotlin | 主平台 ✓ |
| 青橙SDK示例 | Android | `GlassesSDKSample/` (已解压) | Kotlin | 参考代码 ✓ |
| QC SDK | iOS | `QCSDK.framework` + `JLAudioUnitKit.framework` | Objective-C | 暂不重点 |

> **注**: 用户明确iOS不是重点，聚焦Android和华为/HarmonyOS

### 1.2 核心功能 (Android SDK)

| 功能模块 | API |
|----------|-----|
| 设备扫描 | `BleScannerHelper.scanDevice()` |
| 设备连接 | `BleOperateManager.connectDirectly()` |
| 拍照控制 | `glassesControl(byteArrayOf(0x02, 0x01, 0x01))` |
| 录像控制 | `glassesControl(byteArrayOf(0x02, 0x01, 0x02/0x03))` |
| 录音控制 | `glassesControl(byteArrayOf(0x02, 0x01, 0x08/0x0c))` |
| 媒体同步 | `GlassesControl.importAlbum()` |
| 电量查询 | `syncBattery()` / 监听0x05 |
| 版本信息 | `syncDeviceInfo()` |
| 蓝牙OTA | `DfuHandle` |
| WiFi OTA | `GlassesControl` |
| 音量控制 | `getVolumeControl()` |
| 佩戴检测 | `wearCheck(true, isChecked, listener)` |
| 语音唤醒 | `aiVoiceWake(true, isChecked, listener)` |

---

## 2. 技术架构分析

### 2. Android SDK 详解

#### 2.1.1 核心类架构

```
LIB_GLASSES_SDK-release.aar
├── BleScannerHelper        # 设备扫描
├── BleOperateManager       # 蓝牙连接管理 (单例)
├── LargeDataHandler        # 数据指令处理、事件监听 (单例)
├── GlassesControl          # WiFi媒体传输 (单例)
└── DfuHandle               # 固件升级 (单例)
```

#### 2.1.2 示例项目结构 (GlassesSDKSample)

```
GlassesSDKSample/
├── app/
│   ├── src/main/
│   │   ├── java/com/sdk/glassessdksample/
│   │   │   ├── MainActivity.kt          # 主界面，功能演示
│   │   │   ├── MyApplication.kt         # SDK初始化
│   │   │   ├── DeviceBindActivity.kt    # 设备绑定
│   │   │   ├── BluetoothUtils.kt        # 蓝牙工具类
│   │   │   └── PermissionUtil.kt        # 权限工具
│   │   └── res/                         # 布局资源
│   └── build.gradle                     # 依赖配置
└── build.gradle.kts                     # 项目配置
```

#### 2.1.3 关键依赖

```kotlin
dependencies {
    implementation files('libs/LIB_GLASSES_SDK-release.aar')
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.getActivity:XXPermissions:20.0")  // 权限请求
    implementation("org.greenrobot:eventbus:3.2.0")              // 事件总线
    implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.4")
    implementation 'com.google.code.gson:gson:2.8.9'
    implementation 'com.squareup.okhttp3:okhttp:4.9.3'
}
```

**Android版本要求**: minSdk 24 (Android 7.0), targetSdk 35

**依赖**:
- `com.google.code.gson:gson:2.8.9`
- `com.squareup.okhttp3:okhttp:4.9.3`

**Android权限**:

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<!-- 蓝牙权限 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<!-- 存储权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<!-- WiFi权限 -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!-- 位置权限 (蓝牙扫描必需) -->
<!-- Android 13+ -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<!-- Android 12 及以下 -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

#### 2.1.2 华为/HarmonyOS 支持分析

| 考量点 | 说明 |
|--------|------|
| **HarmonyOS兼容** | Android SDK基于标准Android API开发，理论上可在HarmonyOS上运行（兼容Android） |
| **蓝牙API** | 标准BluetoothAdapter API，HarmonyOS兼容层支持 |
| **WiFi API** | 标准WiFi Manager API，需实测 |
| **后台服务** | HarmonyOS对后台限制更严格，需适配 |
| **推荐策略** | 1. 先基于Android SDK开发 2. 在华为设备上测试兼容性问题 3. 如有问题再考虑HarmonyOS特有适配 |

> **建议**: 先聚焦标准Android开发，SDK的AAR可直接用于华为手机（HarmonyOS兼容Android APK）

---

## 3. 设计方案 (Android 为主)

### 方案A: 原生Android开发

**思路**: 使用Kotlin + Jetpack开发原生Android App

**优点**:
- 完整发挥SDK功能
- 开发工具链成熟
- 华为/HarmonyOS兼容性最好

**预计工作量**: 4-6周

---

### 方案B: Flutter跨平台

**思路**: Flutter UI + Platform Channel调用原生SDK

**优点**:
- UI跨平台
- 开发效率高

**缺点**:
- 蓝牙/WiFi功能需Native Module封装

**预计工作量**: 5-6周

---

### 方案C: 混合开发 (WebView)

**思路**: 原生壳 + H5业务

**适用场景**: 业务逻辑主要在云端

**预计工作量**: 3-4周

---

## 4. 功能模块规划

### 4.1 核心功能 (MVP)

| 模块 | 功能点 | 优先级 |
|------|--------|--------|
| 设备连接 | 扫描、连接、断开、重连 | P0 |
| 拍照 | 触发拍照、预览 | P0 |
| 录像 | 开始/停止录像 | P0 |
| 媒体同步 | WiFi下载照片/视频/录音 | P0 |
| 电量显示 | 实时电量、充电状态 | P1 |
| 媒体管理 | 查看、删除本地文件 | P1 |
| 固件升级 | 蓝牙OTA / WiFi OTA | P2 |

### 4.2 扩展功能

| 模块 | 功能点 | 优先级 |
|------|--------|--------|
| 录音 | 开始/停止录音 | P2 |
| AI交互 | 语音对话、图片识别 | P2 |
| 设置 | 视频参数、音频参数、佩戴检测、语音唤醒 | P2 |
| 版本信息 | 硬件/固件版本查询 | P3 |

---

## 5. 下一步行动

请确认以下信息以便推进设计：

1. **方案选择**: 上述方案A(原生Android)/B(Flutter)/C(WebView)，您倾向哪个？
2. **核心功能**: 实际业务场景中最常用的功能是哪些？（确定MVP范围）
3. **时间要求**: 项目上线时间预期？
4. **AI需求**: 是否需要接入AI对话功能？
5. **华为适配**: 是否需要提前考虑HarmonyOS特有适配？

---

## 6. 参考文档

- [青橙无线眼镜SDK使用说明.md](./GLASSES_SDK_20260112_V1.1/青橙无线眼镜SDK使用说明.md)
- [Android示例项目](./GLASSES_SDK_20260112_V1.1/GlassesSDKSample/) - 可直接导入Android Studio
- [iOS_SDK_开发指南.md](./QCSDKDemo/iOS_SDK_开发指南.md) (参考)