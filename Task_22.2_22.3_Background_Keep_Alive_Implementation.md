# Task 22.2 & 22.3: 华为设备保活引导和电池优化请求实现报告

## 实现概述

成功实现了华为设备保活引导和电池优化请求功能，用于确保应用在后台持续运行，保持语音唤醒功能可用。

## 实现内容

### 1. HuaweiProtectedAppsHelper (Task 22.2)

**文件位置**: `app/src/main/java/com/glasses/app/util/HuaweiProtectedAppsHelper.kt`

**功能**:
- 检测是否为华为/荣耀设备
- 显示保活引导对话框
- 引导用户将应用加入"受保护应用"列表
- 自动打开华为设备的应用启动管理页面

**核心方法**:

```kotlin
object HuaweiProtectedAppsHelper {
    // 检测华为/荣耀设备
    fun isHuaweiDevice(): Boolean
    
    // 显示保活引导对话框
    fun showProtectedAppsGuide(context: Context)
    
    // 打开华为设备的受保护应用设置页面
    private fun openProtectedAppsSettings(context: Context)
}
```

**设备检测逻辑**:
- 检查 `Build.MANUFACTURER` 是否为 "HUAWEI" 或 "HONOR"
- 不区分大小写

**引导流程**:
1. 显示对话框，说明需要将应用加入"受保护应用"列表
2. 提供详细的设置步骤说明
3. 点击"去设置"按钮，自动跳转到华为的应用启动管理页面
4. 如果无法打开特定页面，则打开应用详情页作为备选

### 2. BatteryOptimizationHelper (Task 22.3)

**文件位置**: `app/src/main/java/com/glasses/app/util/BatteryOptimizationHelper.kt`

**功能**:
- 检查是否已忽略电池优化
- 请求忽略电池优化权限
- 显示厂商特定的保活引导
- 支持多个主流厂商的保活设置指引

**核心方法**:

```kotlin
object BatteryOptimizationHelper {
    // 检查是否已忽略电池优化
    fun isIgnoringBatteryOptimizations(context: Context): Boolean
    
    // 请求忽略电池优化
    fun requestIgnoreBatteryOptimization(activity: Activity)
    
    // 显示厂商特定的保活引导
    fun showManufacturerSpecificGuide(context: Context)
    
    // 打开电池优化设置页面
    private fun openBatteryOptimizationSettings(activity: Activity)
}
```

**支持的厂商**:
- 小米/Redmi
- OPPO
- vivo
- 三星
- OnePlus
- Realme
- 其他厂商（通用指引）

**电池优化请求流程**:
1. 检查 Android 版本（需要 Android 6.0+）
2. 检查是否已忽略电池优化
3. 如果未忽略，显示对话框说明原因
4. 点击"去设置"按钮，跳转到电池优化设置页面
5. 用户手动关闭电池优化

**厂商特定引导**:
- 根据 `Build.MANUFACTURER` 识别设备厂商
- 显示该厂商的详细保活设置步骤
- 包括电池优化、自启动、后台运行等设置

### 3. HomeViewModel 集成

**文件位置**: `app/src/main/java/com/glasses/app/viewmodel/HomeViewModel.kt`

**新增方法**:

```kotlin
/**
 * 请求后台保活权限
 * 包括电池优化和华为设备保活引导
 */
fun requestBackgroundPermissions() {
    // 如果是华为设备，显示保活引导
    if (HuaweiProtectedAppsHelper.isHuaweiDevice()) {
        HuaweiProtectedAppsHelper.showProtectedAppsGuide(context)
    }
    
    // 请求忽略电池优化
    if (context is android.app.Activity) {
        BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
    }
}

/**
 * 显示厂商特定的保活引导
 */
fun showManufacturerGuide() {
    BatteryOptimizationHelper.showManufacturerSpecificGuide(context)
}
```

### 4. HomeScreen UI 集成

**文件位置**: `app/src/main/java/com/glasses/app/ui/home/HomeScreen.kt`

**自动触发逻辑**:
- 使用 `LaunchedEffect` 监听设备连接状态
- 当设备首次连接时，自动调用 `requestBackgroundPermissions()`
- 使用 `hasRequestedBatteryOptimization` 标志避免重复请求

```kotlin
// 当设备首次连接时，请求后台保活权限
LaunchedEffect(uiState.isConnected) {
    if (uiState.isConnected && !hasRequestedBatteryOptimization) {
        hasRequestedBatteryOptimization = true
        viewModel.requestBackgroundPermissions()
    }
}
```

### 5. 依赖更新

**文件位置**: `app/build.gradle.kts`

**新增依赖**:
```kotlin
// AppCompat - 用于 AlertDialog
implementation("androidx.appcompat:appcompat:1.6.1")

// Robolectric - 用于单元测试
testImplementation("org.robolectric:robolectric:4.11.1")
```

### 6. 单元测试

**文件位置**:
- `app/src/test/java/com/glasses/app/util/HuaweiProtectedAppsHelperTest.kt`
- `app/src/test/java/com/glasses/app/util/BatteryOptimizationHelperTest.kt`

**测试内容**:
- 华为设备检测功能
- 电池优化状态检查
- Android 版本兼容性测试

## 验证需求

✅ **需求 10.6**: 后台保活

**验证点**:
- 华为/荣耀设备能够正确识别
- 保活引导对话框正确显示
- 能够跳转到华为的应用启动管理页面
- 电池优化请求对话框正确显示
- 能够跳转到电池优化设置页面
- 支持多个主流厂商的保活引导
- 设备首次连接时自动请求保活权限

## 技术要点

### 1. 设备厂商识别

使用 `Build.MANUFACTURER` 识别设备厂商：
```kotlin
val manufacturer = Build.MANUFACTURER.lowercase()
when {
    manufacturer.contains("huawei") || manufacturer.contains("honor") -> // 华为/荣耀
    manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> // 小米
    manufacturer.contains("oppo") -> // OPPO
    // ...
}
```

### 2. Intent 跳转

使用 `ComponentName` 跳转到特定的系统设置页面：
```kotlin
val intent = Intent().apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
    component = ComponentName(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
    )
}
context.startActivity(intent)
```

### 3. 电池优化检查

使用 `PowerManager` 检查电池优化状态：
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
```

### 4. 异常处理

使用 try-catch 处理 Intent 跳转失败的情况：
```kotlin
try {
    // 尝试打开特定页面
    context.startActivity(specificIntent)
} catch (e: Exception) {
    try {
        // 打开应用详情页作为备选
        context.startActivity(fallbackIntent)
    } catch (e: Exception) {
        // 打开通用设置页作为最后备选
        context.startActivity(settingsIntent)
    }
}
```

## 用户体验

### 1. 华为设备用户

1. 连接眼镜设备后，自动弹出保活引导对话框
2. 对话框显示详细的设置步骤
3. 点击"去设置"按钮，自动跳转到应用启动管理页面
4. 用户按照提示完成设置

### 2. 其他厂商设备用户

1. 连接眼镜设备后，自动弹出电池优化请求对话框
2. 点击"去设置"按钮，跳转到电池优化设置页面
3. 用户手动关闭电池优化
4. 可以通过"我的"页面查看厂商特定的保活引导

### 3. 对话框设计

- 标题清晰：明确说明是"后台保活设置"或"电池优化设置"
- 内容详细：提供具体的设置步骤
- 操作简单：提供"去设置"和"取消"两个按钮
- 自动跳转：点击"去设置"后自动打开相应的系统设置页面

## 测试建议

### 1. 华为设备测试

- [ ] 在华为手机上测试设备识别
- [ ] 验证保活引导对话框显示
- [ ] 验证能否跳转到应用启动管理页面
- [ ] 完成保活设置后，验证应用在后台是否被杀死

### 2. 其他厂商设备测试

- [ ] 在小米手机上测试电池优化请求
- [ ] 在OPPO手机上测试电池优化请求
- [ ] 在vivo手机上测试电池优化请求
- [ ] 在三星手机上测试电池优化请求
- [ ] 验证厂商特定引导的准确性

### 3. Android 版本测试

- [ ] Android 6.0 (API 23) - 电池优化功能首次引入
- [ ] Android 8.0 (API 26) - 后台服务限制
- [ ] Android 10 (API 29) - 后台位置访问限制
- [ ] Android 12 (API 31) - 精确闹钟权限
- [ ] Android 14 (API 34) - 最新版本

### 4. 边界情况测试

- [ ] 用户拒绝跳转到设置页面
- [ ] 系统设置页面不存在（旧版本系统）
- [ ] 用户已经关闭电池优化
- [ ] 用户多次触发保活请求
- [ ] 应用在后台时触发保活请求

## 已知限制

1. **厂商差异**: 不同厂商的系统设置页面路径可能不同，部分设备可能无法直接跳转到特定页面
2. **系统版本**: 旧版本系统可能没有某些设置页面
3. **用户操作**: 需要用户手动完成设置，无法自动完成
4. **权限限制**: Android 不允许应用自动关闭电池优化，必须由用户手动操作

## 后续优化建议

1. **持久化设置**: 记录用户是否已经完成保活设置，避免重复提示
2. **设置检查**: 定期检查保活设置是否仍然有效
3. **更多厂商**: 添加更多厂商的保活引导
4. **视频教程**: 提供视频教程链接，帮助用户更好地理解设置步骤
5. **设置验证**: 完成设置后，验证保活是否生效

## 相关文件

### 新增文件
- `app/src/main/java/com/glasses/app/util/HuaweiProtectedAppsHelper.kt`
- `app/src/main/java/com/glasses/app/util/BatteryOptimizationHelper.kt`
- `app/src/test/java/com/glasses/app/util/HuaweiProtectedAppsHelperTest.kt`
- `app/src/test/java/com/glasses/app/util/BatteryOptimizationHelperTest.kt`

### 修改文件
- `app/src/main/java/com/glasses/app/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/glasses/app/ui/home/HomeScreen.kt`
- `app/build.gradle.kts`

## 设计参考

- 设计文档: `.kiro/specs/glasses-app-mvp/design.md` (电池优化章节)
- 需求文档: `.kiro/specs/glasses-app-mvp/requirements.md` (需求 10)
- 任务文档: `.kiro/specs/glasses-app-mvp/tasks.md` (任务 22.2, 22.3)

## 总结

成功实现了华为设备保活引导和电池优化请求功能，为应用的后台保活提供了完整的解决方案。通过自动识别设备厂商并提供相应的保活引导，大大提高了用户体验和应用的后台存活率。
