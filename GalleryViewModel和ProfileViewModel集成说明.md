# GalleryViewModel和ProfileViewModel集成说明

## 概述

本文档说明GalleryViewModel和ProfileViewModel与后端模块的集成情况。

---

## GalleryViewModel集成

### 集成的后端模块

#### 1. MediaSyncManager
- **功能**: 管理眼镜媒体文件的同步
- **集成内容**:
  - `initSync(albumDirPath)` - 初始化媒体同步，设置相册目录和WiFi下载监听器
  - `startSync()` - 开始同步媒体文件
  - `stopSync()` - 停止同步
  - `mediaFiles: StateFlow<List<MediaFile>>` - 监听媒体文件列表
  - `syncProgress: StateFlow<SyncProgress>` - 监听同步进度
  - `deleteMediaFile(mediaFile)` - 删除单个媒体文件
  - `clearAllMediaFiles()` - 清空所有媒体文件

#### 2. LocalMediaManager
- **功能**: 管理本地媒体文件和缩略图
- **集成内容**:
  - `generateImageThumbnail(imagePath)` - 生成图片缩略图
  - `generateVideoThumbnail(videoPath)` - 生成视频缩略图
  - `getMediaDuration(mediaPath)` - 获取媒体时长
  - `getMediaFileSize(mediaPath)` - 获取文件大小

### 完整的媒体同步流程

```
1. 初始化 (initSync)
   ↓
2. 用户点击"同步"按钮 (startSync)
   ↓
3. SDK扫描眼镜中的媒体文件
   ↓
4. WiFi下载文件到本地
   ↓
5. 实时更新同步进度 (syncProgress Flow)
   ↓
6. 文件下载完成后添加到媒体列表 (mediaFiles Flow)
   ↓
7. UI自动更新显示新的媒体文件
```

### 主要功能

#### 1. 媒体同步
- 点击"同步"按钮开始同步
- 实时显示同步进度（百分比）
- 显示当前同步的文件名和索引
- 同步完成后自动更新媒体列表

#### 2. 媒体类型筛选
- 全部：显示所有媒体文件
- 图片：只显示图片文件（.jpg, .png）
- 视频：只显示视频文件（.mp4, .mov）
- 音频：只显示音频文件（.wav, .mp3）

#### 3. 媒体管理
- 查看媒体文件
- 删除单个媒体文件
- 清空所有媒体文件
- 生成缩略图（图片和视频）

### 数据流

```
MediaSyncManager.mediaFiles (StateFlow)
         ↓
GalleryViewModel._uiState.mediaFiles
         ↓
GalleryScreen UI更新
```

```
MediaSyncManager.syncProgress (StateFlow)
         ↓
GalleryViewModel._uiState (isSyncing, syncProgress, syncMessage)
         ↓
GalleryScreen 进度条更新
```

### 相册目录

- 路径: `{ExternalFilesDir}/GlassesAlbum/`
- 自动创建目录
- 媒体文件保存在此目录

---

## ProfileViewModel集成

### 集成的后端模块

#### 1. GlassesSDKManager
- **功能**: 管理眼镜SDK连接和设备信息
- **集成内容**:
  - `connectionState: StateFlow<ConnectionState>` - 监听连接状态
  - `batteryLevel: StateFlow<Int>` - 监听电量
  - `disconnect()` - 断开连接
  - `getCurrentDevice()` - 获取当前连接的设备信息

### 主要功能

#### 1. 设备状态显示
- 连接状态：已连接 / 未连接
- 设备名称：显示当前连接的设备名称
- 电量显示：显示设备电量百分比

#### 2. 断开连接
- 点击"断开连接"按钮
- 调用SDK的disconnect()方法
- 连接状态自动更新为"未连接"

#### 3. 应用信息
- 应用版本号：从PackageInfo读取真实版本号
- SDK版本号：显示SDK版本（1.1）

### 数据流

```
GlassesSDKManager.connectionState (StateFlow)
         ↓
ProfileViewModel.updateConnectionState()
         ↓
ProfileViewModel._uiState (deviceConnected, deviceName)
         ↓
ProfileScreen 设备状态卡片更新
```

```
GlassesSDKManager.batteryLevel (StateFlow)
         ↓
ProfileViewModel._uiState.batteryLevel
         ↓
ProfileScreen 电量显示更新
```

### 连接状态

- **DISCONNECTED**: 未连接
- **CONNECTING**: 连接中...
- **CONNECTED**: 已连接（显示设备名称和电量）

---

## UI更新

### GalleryScreen更新
- 使用真实的`MediaFile`类型（来自`com.glasses.app.data.local.media`）
- 使用`GalleryMediaType`枚举（UI层）映射到`MediaType`枚举（数据层）
- 媒体网格项根据`MediaType`枚举显示不同图标
- 视频和音频显示时长标签

### ProfileScreen更新
- 设备信息卡片新增电量显示
- 电量只在已连接且电量>0时显示
- 格式：`电量: XX%`

---

## 测试建议

### GalleryViewModel测试
1. 连接眼镜设备
2. 在眼镜上拍摄照片、录制视频、录制音频
3. 在App相册页点击"同步"按钮
4. 观察同步进度是否正常更新
5. 同步完成后检查媒体列表是否显示新文件
6. 测试媒体类型筛选功能
7. 测试删除媒体文件功能

### ProfileViewModel测试
1. 连接眼镜设备
2. 在"我的"页面查看设备状态卡片
3. 确认显示"已连接"、设备名称、电量
4. 点击"断开连接"按钮
5. 确认状态更新为"未连接"
6. 重新连接设备，确认状态恢复

---

## 已知限制

### GalleryViewModel
1. **WiFi文件下载** - 依赖SDK的WiFi功能，需要眼镜和手机在同一WiFi网络
2. **缩略图生成** - 目前只在调用`generateThumbnail()`时生成，未自动生成
3. **大文件同步** - 大视频文件同步可能较慢，需要优化进度显示

### ProfileViewModel
1. **电量更新频率** - 依赖SDK的电量查询频率（30秒）
2. **断开连接** - 只断开App与SDK的连接，不影响系统蓝牙配对

---

## 下一步工作

1. **HomeViewModel媒体采集集成** - 集成MediaCaptureManager实现拍照、录像、录音、智能识图功能
2. **会话管理UI** - 实现ChatScreen的会话切换、历史记录功能
3. **媒体查看器** - 实现GalleryScreen的全屏查看、视频播放、音频播放功能
4. **自动缩略图生成** - 媒体同步完成后自动生成缩略图
5. **错误处理优化** - 完善网络错误、文件错误的提示和处理

---

## 文件清单

### 修改的文件
- `app/src/main/java/com/glasses/app/viewmodel/GalleryViewModel.kt` - 完全重写，集成后端
- `app/src/main/java/com/glasses/app/viewmodel/ProfileViewModel.kt` - 完全重写，集成后端
- `app/src/main/java/com/glasses/app/ui/gallery/GalleryScreen.kt` - 更新导入和类型
- `app/src/main/java/com/glasses/app/ui/profile/ProfileScreen.kt` - 新增电量显示
- `CHANGELOG.md` - 更新集成记录

### 依赖的后端模块
- `app/src/main/java/com/glasses/app/data/remote/sdk/MediaSyncManager.kt`
- `app/src/main/java/com/glasses/app/data/local/media/LocalMediaManager.kt`
- `app/src/main/java/com/glasses/app/data/local/media/MediaFile.kt`
- `app/src/main/java/com/glasses/app/data/remote/sdk/GlassesSDKManager.kt`

---

© 2026 Linkai Team. All Rights Reserved.
