# 快速修复构建问题

## 问题原因
Android Gradle Plugin 8.2.2 + Gradle 8.7 + 本地AAR文件会导致依赖解析冲突。

## 解决方案

### 方案1: 暂时移除青橙SDK（推荐，快速验证）

1. 注释掉青橙SDK依赖：

编辑 `app/build.gradle.kts`，找到这两行并注释掉：
```kotlin
// implementation(files("libs/LIB_GLASSES_SDK-release.aar"))
// implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
```

2. 注释掉使用SDK的代码：

编辑以下文件，暂时注释掉SDK相关代码：
- `app/src/main/java/com/glasses/app/data/remote/sdk/GlassesSDKManager.kt`
- `app/src/main/java/com/glasses/app/manager/RecordingManager.kt`
- `app/src/main/java/com/glasses/app/service/wakeup/*.kt`

3. 重新构建：
```powershell
./gradlew.bat clean
./gradlew.bat assembleDebug
```

### 方案2: 降级Android Gradle Plugin

编辑根目录的 `build.gradle.kts`：
```kotlin
plugins {
    id("com.android.application") version "8.1.4" apply false  // 从8.2.2降级到8.1.4
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
```

然后运行：
```powershell
./gradlew.bat clean
./gradlew.bat assembleDebug
```

### 方案3: 使用Android Studio构建（最简单）

1. 打开Android Studio
2. File -> Open -> 选择项目目录
3. 等待Gradle同步完成
4. 点击 Build -> Build Bundle(s) / APK(s) -> Build APK(s)

Android Studio会自动处理很多兼容性问题。

## 当前状态

我已经做了以下修改：
1. ✅ 降级Gradle到8.7版本
2. ✅ 添加了依赖解析策略
3. ✅ 配置了gradle.properties

## 下一步

建议：
1. 先使用方案1，暂时移除SDK，验证项目能否构建
2. 如果成功，再逐步添加SDK功能
3. 或者等Android Studio安装完成，直接在IDE中构建

## 测试命令

```powershell
# 清理
./gradlew.bat clean

# 构建
./gradlew.bat assembleDebug --stacktrace

# 如果成功，APK位置：
# app\build\outputs\apk\debug\app-debug.apk
```
