# Changelog

All notable changes to the Linkai星韵AI眼镜Android App project will be documented in this file.

## [1.1.0] - 2026-04-09

### Fixed - 关键崩溃修复

- **修复鸿蒙4.0兼容性崩溃**：`registerReceiver(receiver, filter, flags)` 三参数版本仅在 API 33+ 可用，鸿蒙基于 Android 10-12 会抛 NoSuchMethodError。将条件从 `>= O` 改为 `>= TIRAMISU`
- **修复 AlertDialog 主题崩溃**：`androidx.appcompat.app.AlertDialog` 需要 AppCompat 主题，改为 `android.app.AlertDialog`（系统原生）
- **修复 SDK 初始化链断裂**：`GlassesApplication.initBle()` 中 `initReceiver()` 抛异常导致后续回调注册和上下文设置全被跳过。拆分为独立步骤，每步独立 try-catch
- **修复前台服务被临时禁用**：恢复 `GlassesConnectionService.startService()` 调用
- **修复图标引用错误**：`BatteryStd`、`BatteryChargingFull`、`BatteryAlert`、`Bolt`、`Sync` 不在默认 Material Icons 集中，替换为可用图标
- **修复电量始终为0**：旧的 `addBatteryCallBack` + 反射方式无法获取电量，改为官方demo方式 `addOutDeviceListener` + `GlassesDeviceNotifyListener` + `syncBattery()`
- **修复 MediaViewerScreen 属性引用**：`fileSize` → `size`，`timestamp` → `createTime`
- **修复 ChatScreen `SwipeToDismissBox`**：API不可用，改用 `combinedClickable` 长按删除

### Added - 新功能

- **GlassesDeviceNotifyListener**：接收眼镜端推送的电量、按键、语音唤醒、OTA等通知
- **CrashLogHelper**：全局崩溃日志处理器，写入 `Android/data/com.glasses.app/files/crash_log.txt`
- **HomeViewModel 接入 MediaCaptureManager**：拍照、录像、录音、智能识图按钮真正调用SDK指令
- **前台服务异常保护**：GlassesConnectionService 各步骤添加独立异常捕获和日志

### Changed

- SDK 初始化流程重构：`initSDKCore()` → `registerBluetoothReceiver()` → `registerSDKCallback()` → `setmContext()`，各步独立
- 电量获取方式从反射改为官方demo标准方式
- 周期性电量监控从反射式 `getBatteryLevel()` 改为 `syncBattery()` + 监听器回调
- `README.md`：修正 targetSdk 为 34，修正权限管理为原生 ActivityCompat

### Documentation

- 更新 `集成状态报告.md`：所有4个页面已集成完成
- 更新 `README.md`：修正SDK版本、更新版本历史
- 更新 `CHANGELOG.md`：精简并反映实际修改

## [1.0.0] - 2026-03-17

### Added

- 初始项目架构搭建
- MVVM 架构（ViewModel + Repository + Room）
- Jetpack Compose + Material3 UI框架
- 4个主要页面：首页、AI对话、相册、我的
- 底部导航栏
- 蓝牙SDK封装（扫描、连接、断开）
- AI服务集成（ASR + LLM + TTS）
- 流式对话管理
- 媒体采集控制（拍照、录像、录音）
- 媒体同步管理
- 权限管理
- 前台服务和后台保活

### 🔴 已知问题

1. **录音文件下载** - 需要集成SDK的WiFi文件下载功能（当前使用临时空文件）
2. **静音检测** - SDK暂时没有提供实时音频电平接口
3. **API Key** - LinkAIClient.kt 中的 API_KEY 需要用户配置
4. **检查更新** - ProfileViewModel 的检查更新功能是模拟的

---

## 技术栈

- **UI**: Jetpack Compose + Material3
- **架构**: MVVM (ViewModel + StateFlow)
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **图片**: Coil
- **蓝牙SDK**: 青橙SDK (LIB_GLASSES_SDK-release.aar)
- **AI服务**: LinkAI API (ASR + LLM + TTS)

---

## 许可证

© 2026 LinkAI Team All Rights Reserved.
