# Changelog

All notable changes to the Linkai星韵AI眼镜Android App project will be documented in this file.

## [1.3.0] - 2026-04-11

### Changed - 智能识图链路调整

- **智能识图切换到阿里 DashScope Qwen**
  - 首页“智能识图”不再走 LinkAI 图像识别链路，改为调用阿里 DashScope OpenAI 兼容接口
  - 本地图片读取后转为 `data:image/...;base64,...`，直接发送给视觉模型，无需额外图片上传服务
  - 当前支持模型：
    - `qwen3.6-plus-2026-04-02`
    - `qwen3.5-flash`

- **识图流程完善**
  - Home 页流程调整为：拍照 -> 同步最新图片 -> 阿里 Qwen 识图 -> 结果写入 AI 对话页
  - 识图结果统一使用提示词前缀：`图片识别：`
  - 新增识图结果中转仓库，确保从首页触发后可在 AI 对话页查看识别结果

### Added - 配置能力扩展

- **API配置新增阿里Qwen识图配置**
  - 新增 DashScope API Key 配置项
  - 新增视觉模型选择项
  - 保留 LinkAI 语音 / 对话 Key 配置，用于原有语音对话能力

### Documentation

- 更新 `README.md`
- 更新 `docs/API配置功能说明.md`
- 更新 `docs/集成状态报告.md`

## [1.2.0] - 2026-04-09

### Added - 新功能

- **API Key 动态配置**
  - `ApiKeyManager` 支持语音Key（ASR/TTS）和对话Key（LLM）分开管理
  - `LinkAIClient` 每次HTTP请求前自动从 ApiKeyManager 重新读取最新 Key，保存后立即生效无需重启
  - 未配置 Key 时给出明确引导提示（"请先在「我的」→「API配置」中设置..."）
  - 支持通过「我的」页面 UI 配置 API Key

- **日志系统（Task 23）**
  - 新增 `AppLogger` 工具类，支持 DEBUG / INFO / WARN / ERROR 四级日志
  - 日志写入文件 `app_log.txt`，超过 10MB 自动清理旧日志
  - 在 `GlassesApplication` 中初始化
  - ProfileScreen「查看日志」对话框支持两个 Tab：
    - 崩溃日志 - 显示 `crash_log.txt`
    - 运行日志 - 显示 `app_log.txt`
  - 两个 Tab 均支持复制和清空操作

- **HomeScreen 媒体采集实时状态**
  - 录像/录音时实时监听 `MediaCaptureState`，更新录制时长显示
  - 录制中显示计时器卡片

## [1.1.0] - 2026-04-09

### Fixed - 关键崩溃修复

- **修复鸿蒙4.0兼容性崩溃**：`registerReceiver(receiver, filter, flags)` 三参数版本仅在 API 33+ 可用，将条件从 `>= O` 改为 `>= TIRAMISU`
- **修复 AlertDialog 主题崩溃**：改为 `android.app.AlertDialog`（系统原生）
- **修复 SDK 初始化链断裂**：拆分为独立步骤，每步独立 try-catch
- **修复前台服务被临时禁用**：恢复 `GlassesConnectionService.startService()` 调用
- **修复图标引用错误**：替换为默认 Material Icons 集中可用的图标
- **修复电量始终为0**：改为官方demo方式 `addOutDeviceListener` + `GlassesDeviceNotifyListener` + `syncBattery()`
- **修复 MediaViewerScreen 属性引用**：`fileSize` → `size`，`timestamp` → `createTime`
- **修复 ChatScreen `SwipeToDismissBox`**：改用 `combinedClickable` 长按删除

### Added

- **GlassesDeviceNotifyListener**：接收眼镜端电量、按键、语音唤醒、OTA等通知
- **CrashLogHelper**：全局崩溃日志处理器
- **HomeViewModel 接入 MediaCaptureManager**：拍照、录像、录音、智能识图按钮调用SDK指令
- **前台服务异常保护**：各步骤独立异常捕获和日志

### Changed

- SDK 初始化流程重构为独立步骤
- 电量获取方式从反射改为官方demo标准方式
- 周期性电量监控改为 `syncBattery()` + 监听器回调

### Documentation

- 更新 `集成状态报告.md`、`README.md`、`CHANGELOG.md`

## [1.0.0] - 2026-03-17

### Added

- 初始项目架构搭建（MVVM + Jetpack Compose + Material3）
- 4个主要页面：首页、AI对话、相册、我的
- 蓝牙SDK封装、AI服务集成、流式对话管理
- 媒体采集控制、媒体同步管理
- 权限管理、前台服务、后台保活

### 已知问题

1. **录音文件下载** - 需要集成SDK的WiFi文件下载功能
2. **静音检测** - SDK暂无实时音频电平接口
3. **检查更新** - ProfileViewModel 检查更新功能是模拟的

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
