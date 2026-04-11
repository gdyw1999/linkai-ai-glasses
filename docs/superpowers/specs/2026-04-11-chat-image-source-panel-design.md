# ChatScreen 附件扩展面板设计规格

## 1. 概述与目标

在 AI 对话页面（ChatScreen）底部输入栏右侧新增 **"+" 展开按钮**，点击后显示附件扩展面板，提供 4 个图片来源入口（手机相册、眼镜相册、手机拍照、眼镜拍照），用户选择图片后自动调用 Qwen 分析，结果写入对话。

**参考原型**：豆包 App 聊天详情页附件扩展态（点击"+"展开附件面板）

---

## 2. UI 规格

### 2.1 标准态（默认）

```
┌─────────────────────────────────────────────┐
│  ChatTopBar（顶部栏，标题"AI对话"）           │
├─────────────────────────────────────────────┤
│                                             │
│            消息列表（LazyColumn）             │
│                                             │
├─────────────────────────────────────────────┤
│  [StatusBar 状态提示]（有消息时显示）          │
├─────────────────────────────────────────────┤
│  ○ ○ ○  （拖拽手柄，三个小圆点）              │
├─────────────────────────────────────────────┤
│  [  输入框...                        ] [🎤] [+] │
│  ChatControlBar + 右侧 + 按钮                 │
└─────────────────────────────────────────────┘
```

- **拖拽手柄**：3 个小圆点（#BDBDBD），宽度 40dp，高度 4dp，圆角 2dp
- **右侧"+"按钮**：`Icons.Default.Add`，点击后展开面板
- 输入框 + 语音按钮逻辑不变

### 2.2 扩展态（面板展开）

```
┌─────────────────────────────────────────────┐
│  ChatTopBar                                 │
├─────────────────────────────────────────────┤
│            消息列表（不可滚动/禁用）           │
├─────────────────────────────────────────────┤
│  [StatusBar]                                │
├─────────────────────────────────────────────┤
│  ═══  （拖拽手柄）              [× 关闭]    │
├─────────────────────────────────────────────┤
│  ┌─────────────────────────────────────┐   │
│  │  核心功能区（2×2 网格）               │   │
│  │  ┌─────────┐  ┌─────────┐         │   │
│  │  │ 📷 手机  │  │ 🖼️ 眼镜  │         │   │
│  │  │  相册    │  │  相册    │         │   │
│  │  └─────────┘  └─────────┘         │   │
│  │  ┌─────────┐  ┌─────────┐         │   │
│  │  │ 📸 手机  │  │ 🔭 眼镜  │         │   │
│  │  │  拍照    │  │  拍照    │         │   │
│  │  └─────────┘  └─────────┘         │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  最近图片网格（横向 4列，最近 N 张）           │
│  ┌───┐┌───┐┌───┐┌───┐                     │
│  │img││img││img││img│                     │
│  └───┘└───┘└───┘└───┘                     │
├─────────────────────────────────────────────┤
│  [  输入框...                        ] [🎤] [+] │
└─────────────────────────────────────────────┘
```

### 2.3 面板规格

**容器**：
- 高度：约 380dp（含最近图片区）
- 背景：白色 `#FFFFFF`，顶部圆角 16dp
- 阴影：`elevation = 8dp`
- 从底部向上弹出动画（300ms，Material 默认）

**拖拽手柄**：灰色圆点，宽度 40dp，可下拉关闭面板（velocity threshold 500dp/s）

**关闭按钮**：右上角 `Icons.Default.Close`，`#757575`，点击收起面板

**功能区**：
- 2×2 网格，每个选项：80dp × 80dp
- 图标：32dp，颜色 `#424242`
- 文字：12sp，颜色 `#424242`，居中
- 选中/点击背景：`#E3F2FD`，圆角 12dp

**四个入口**：

| 图标 | 标签 | 颜色 | 说明 |
|------|------|------|------|
| `Icons.Default.Photo` | 手机相册 | `#757575` | 系统图片选择器 |
| `Icons.Default.Image` | 眼镜相册 | `#757575` | 跳转 GalleryScreen 选择模式 |
| `Icons.Default.CameraAlt` | 手机拍照 | `#757575` | 系统相机拍摄 |
| `Icons.Default.Cameras` | 眼镜拍照 | `#757575` | 触发眼镜拍照+同步 |

**最近图片网格**：
- 横向滚动，4 列
- 每格：正方形 80dp，圆角 8dp
- 点击直接触发 Qwen 分析（不展开面板）
- 从 MediaSyncManager 的 mediaFiles 取最新 8 张图片（IMAGE 类型）

**面板收起动画**：300ms，向下滑出

---

## 3. 功能规格

### 3.1 交互流程总览

```
用户点击 "+"
    ↓
面板展开（最近图片加载）
    ↓
用户选择入口
    ├── 手机相册 → 系统图片选择器 → 返回 URI → 复制到缓存目录
    ├── 眼镜相册 → GalleryScreen（选择模式）→ 选中 MediaFile
    ├── 手机拍照 → 系统相机 → 返回 URI → 复制到缓存目录
    └── 眼镜拍照 → 触发眼镜拍照 → 等待3秒 → 同步 → 获取最新图片路径
            ↓
    调用 Qwen 分析（路径）
            ↓
    结果写入对话：
    用户消息：「📷 分析图片: filename.jpg」
    AI  回复：「[Qwen 分析结果文字]」
            ↓
    面板自动收起
```

### 3.2 手机相册

- 使用 `ActivityResultContracts.PickVisualMedia`（API 33+）
- 不支持时 fallback `ActivityResultContracts.GetContent`
- 返回 URI → 复制到 app 缓存目录（因为 Qwen 需要 File 路径）
- 临时文件用完后可删除

### 3.3 眼镜相册（GalleryScreen 选择模式）

- 新增参数 `onMediaSelected: ((MediaFile) → Unit)?` — 为 null 时为浏览模式，非 null 时为选择模式
- 浏览模式（`GalleryScreen`）：点击图片打开 Viewer（原有逻辑）
- 选择模式：点击图片不打开 Viewer，直接回调 `onMediaSelected`，面板关闭
- 选择模式顶部显示"选择图片"标题和"取消"按钮

### 3.4 手机拍照

- 使用 `ActivityResultContracts.TakePicture`
- 输出到 app 缓存目录，得到 File 路径
- 临时文件用完后可删除

### 3.5 眼镜拍照

- 调用 `MediaCaptureManager.takePhoto()`
- `delay(3000)` 等眼镜端保存
- 调用 `MediaSyncManager.startSync()` 同步
- 轮询 `MediaSyncManager.mediaFiles` 等新图片出现（超时 20s）
- 获取最新图片路径，交给 Qwen 分析
- 分析完成后自动同步最新图片（因为眼镜刚拍完，下次打开 Gallery 会显示）

### 3.6 Qwen 分析

- 复用 `AIServiceImpl.recognizeImage(imagePath)`
- 分析完成后写入对话：
  ```
  用户消息：📷 分析图片: {filename}
  AI 回复：{Qwen 分析结果}
  ```
- 同时持久化到 Room 数据库
- 若分析失败，写入错误提示消息

### 3.7 最近图片

- 从 `MediaSyncManager.mediaFiles` 取最新的 8 张 IMAGE 类型
- 每次面板展开时刷新
- 点击后直接触发 Qwen 分析（不经过选择器）

---

## 4. 组件清单

| 组件 | 文件 | 说明 |
|------|------|------|
| `ChatScreen` | `ChatScreen.kt` | 新增 BottomSheet 状态管理，改造 ChatControlBar |
| `ChatControlBar` | `ChatScreen.kt` | 新增 `+` 按钮参数，拖拽手柄内置 |
| `ImageSourceBottomSheet` | `ImageSourceBottomSheet.kt` | 面板组件：拖拽手柄 + 功能网格 + 最近图片 |
| `GalleryScreen` | `GalleryScreen.kt` | 新增选择模式 `onMediaSelected` |
| `ChatViewModel` | `ChatViewModel.kt` | 新增 `recognizeImageAndSend(imagePath, filename)` |

---

## 5. 状态管理

```kotlin
// ChatScreen 新增状态
var showImageSourceSheet by remember { mutableStateOf(false) }
var recentImages by remember { mutableStateOf<List<MediaFile>>(emptyList()) }

// 面板展开/收起
fun openImageSourceSheet() { showImageSourceSheet = true }
fun closeImageSourceSheet() { showImageSourceSheet = false }

// 选择图片来源
fun onSourceSelected(source: ImageSource) {
    // source = ALBUM_PHONE | ALBUM_GLASSES | CAMERA_PHONE | CAMERA_GLASSES
    // 触发对应逻辑，分析完成后 closeImageSourceSheet()
}
```

---

## 6. 错误处理

| 场景 | 处理 |
|------|------|
| 手机相册无可选图片 | Toast "请先拍摄或保存图片到相册" |
| 眼镜未连接时选"眼镜相册/拍照" | Toast "请先连接眼镜" |
| 眼镜拍照超时（20s） | Toast "拍照同步超时，请重试" |
| Qwen 分析失败 | 写入消息「图片分析失败，请重试」 |
| 无最近图片 | 最近图片区不显示 |

---

## 7. 非功能性

- **性能**：最近图片使用 Coil 缩略图，不卡 UI
- **兼容性**：手机相册/拍照在 API < 33 fallback 到 GetContent
- **动画**：面板展开 300ms，Android 默认 Material 曲线
