# 构建和测试指南

## ✅ 测试UI已创建

我已经为你创建了一个简单的测试界面，可以测试任务1-7的所有功能：

### 测试UI功能
- ✅ 蓝牙设备扫描和连接
- ✅ 连接状态显示
- ✅ 电量显示
- ✅ 设备列表显示
- ✅ 录音测试按钮
- ✅ 实时日志输出

### 新增文件
- `app/src/main/java/com/glasses/app/ui/test/TestScreen.kt` - 测试界面UI
- `app/src/main/java/com/glasses/app/viewmodel/TestViewModel.kt` - 测试界面ViewModel
- `build-apk.sh` - Linux/Mac构建脚本
- `build-apk.bat` - Windows构建脚本

## 前置条件

### 1. 安装Android Studio
你提到正在下载 **Android Studio Panda 2 | 2025.3.2**，安装完成后：

1. 打开Android Studio
2. 选择 "Open an Existing Project"（打开现有项目）
3. 选择当前项目目录

### 2. 等待Gradle同步
首次打开项目时，Android Studio会自动同步Gradle依赖，这可能需要几分钟时间。

## 当前项目状态

### ⚠️ 重要提示
目前项目**缺少UI界面**，只有底层功能模块。在构建APK之前，我们需要：

1. 创建一个简单的测试UI
2. 修复可能的编译错误
3. 配置签名密钥

## 快速构建APK

### 方法1: 使用构建脚本（推荐）

#### Windows系统
```bash
# 双击运行或在命令行执行
build-apk.bat
```

#### Linux/Mac系统
```bash
# 赋予执行权限并运行
chmod +x build-apk.sh
./build-apk.sh
```

### 方法2: 使用Gradle命令

### 方法2: 使用Gradle命令

#### 在Android Studio中构建

```bash
# 菜单: Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

#### 使用命令行（Windows bash）

```bash
# 清理构建
./gradlew clean

# 构建Debug APK
./gradlew assembleDebug

# 查看构建输出
ls -la app/build/outputs/apk/debug/
```

生成的APK位置：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 安装APK

### 方法1: 使用Android Studio
1. 连接Android设备或启动模拟器
2. 点击工具栏的 "Run" 按钮（绿色三角形）
3. 选择目标设备
4. 应用会自动安装并启动

### 方法2: 使用ADB命令
```bash
# 确保设备已连接
adb devices

# 安装APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 如果已安装，使用-r参数覆盖安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.glasses.app/.MainActivity
```

### 方法3: 直接在设备上安装
1. 将APK文件传输到手机
2. 在手机上打开文件管理器
3. 点击APK文件安装
4. 允许"未知来源"安装（如果需要）

## 配置Android虚拟机（AVD）

### 创建虚拟设备
1. 在Android Studio中打开 "Device Manager"
2. 点击 "Create Device"
3. 选择设备类型（推荐: Pixel 5）
4. 选择系统镜像（推荐: Android 13 或 14）
5. 配置AVD设置
6. 点击 "Finish"

### 虚拟机要求
- **最低**: Android 7.0 (API 24)
- **推荐**: Android 13 (API 33) 或更高
- **内存**: 至少2GB RAM
- **存储**: 至少4GB

### 启动虚拟机
```bash
# 列出可用的AVD
emulator -list-avds

# 启动指定的AVD
emulator -avd Pixel_5_API_33
```

## 当前项目的限制

### ⚠️ 无法完整测试的功能

由于缺少UI，以下功能暂时无法测试：

1. **蓝牙扫描和连接**: 需要UI显示设备列表和连接按钮
2. **录音功能**: 需要UI触发录音开始/停止
3. **唤醒功能**: 需要眼镜设备的按键或语音触发
4. **电量显示**: 需要UI显示电量信息

### ✅ 可以验证的内容

1. **应用启动**: 验证应用能否正常启动
2. **SDK初始化**: 通过logcat查看SDK初始化日志
3. **权限请求**: 验证权限请求对话框是否显示
4. **崩溃测试**: 验证应用是否稳定运行

## 推荐的测试流程

### 第一步: 创建测试UI
让我帮你创建一个简单的测试界面，这样可以：
- 可视化测试蓝牙连接
- 手动触发录音
- 查看连接状态和电量
- 显示日志信息

### 第二步: 修复编译错误
检查并修复可能的编译错误：
- 缺少的依赖
- 包名错误
- SDK版本不匹配

### 第三步: 构建和安装
使用上述方法构建APK并安装到设备

### 第四步: 功能测试
按照测试用例逐一验证功能

## 需要的测试设备

### 选项1: 真实Android设备（推荐）
- Android 7.0或更高版本
- 支持蓝牙BLE
- 开启开发者选项和USB调试

### 选项2: Android虚拟机
- 可以测试基本功能
- ⚠️ 蓝牙功能可能受限
- 无法连接真实的眼镜设备

### 选项3: 青橙眼镜设备
- 用于完整功能测试
- 需要配对和连接

## 查看日志

### 使用Android Studio
1. 打开 "Logcat" 窗口（底部工具栏）
2. 选择设备和应用进程
3. 过滤日志：输入 "GlassesSDK" 或 "com.glasses.app"

### 使用ADB命令
```bash
# 查看所有日志
adb logcat

# 过滤应用日志
adb logcat | grep "com.glasses.app"

# 过滤特定标签
adb logcat | grep "GlassesSDKManager"

# 清除日志
adb logcat -c

# 保存日志到文件
adb logcat > app_log.txt
```

## 常见问题

### Q1: Gradle同步失败
**解决方案**:
1. 检查网络连接
2. 使用国内镜像源
3. 清理Gradle缓存: `./gradlew clean`

### Q2: SDK版本不匹配
**解决方案**:
1. 打开 SDK Manager
2. 安装 Android SDK Platform 35
3. 安装 Android SDK Build-Tools

### Q3: 应用安装失败
**解决方案**:
1. 卸载旧版本: `adb uninstall com.glasses.app`
2. 重新安装: `adb install -r app-debug.apk`

### Q4: 权限被拒绝
**解决方案**:
1. 在设备设置中手动授予权限
2. 或使用ADB: `adb shell pm grant com.glasses.app android.permission.BLUETOOTH_SCAN`

## 下一步行动

### 建议顺序：

1. **等待Android Studio安装完成**
2. **打开项目并等待Gradle同步**
3. **让我创建测试UI**（约30分钟）
4. **修复编译错误**（如果有）
5. **构建Debug APK**
6. **安装到设备或虚拟机**
7. **进行功能测试**

## 需要我做什么？

请告诉我你想要：

**选项A**: 我现在创建一个简单的测试UI，包含蓝牙扫描、连接、录音等功能的测试界面

**选项B**: 先尝试构建当前项目，看看有什么编译错误，然后再决定

**选项C**: 创建一个更完整的UI，但需要更多时间

你的选择是？
