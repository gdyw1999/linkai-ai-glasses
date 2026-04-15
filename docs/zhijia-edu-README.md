# 智教未来 - AI教育平台

**智教未来** - 
让AI助力教育，一句话生成专业课件、


**GitHub 仓库**: [https://github.com/gdyw1999/zhijiao-edu](https://github.com/gdyw1999/zhijiao-edu)

## 🚀 快速开始

### 环境要求

- **Node.js** >= 18.0.0 (前端开发)
- **npm** >= 9.0.0
- **Python** >= 3.8 (后端开发)
- **Git**

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/gdyw1999/zhijiao-edu.git
   cd zhijiao-edu
   ```

2. **创建环境变量文件**
   ```bash
   # 复制配置模板
   cp .env.example .env

   # 编辑 .env 文件，配置你的端口和API密钥
   ```

3. **安装依赖并启动**
   ```bash
   # 使用启动脚本自动安装依赖并启动
   python start_dev.py
   ```

   或者手动安装：
   ```bash
   # 后端依赖 (Python)
   cd backend
   python -m venv venv
   .\venv\Scripts\pip.exe install -r requirements.txt

   # 前端依赖 (Node.js)
   cd ../frontend
   npm install
   npm run dev
   ```

4. **访问应用**
   - 前端页面: http://localhost:3000
   - 后端 API: http://localhost:8000
   - API 健康检查: http://localhost:8000/api/health
   - API 文档: http://localhost:8000/docs

## 📁 项目结构

```
zhijiao-edu/
├── .env                      # 环境变量配置（需创建）
├── .env.example              # 环境变量模板
├── start_dev.py              # 跨平台启动脚本（Python）
├── README.md                 # 项目说明
├── CHANGELOG.md              # 更新日志
│
├── backend/                  # 后端服务 (Python/FastAPI)
│   ├── app/
│   │   ├── api/             # API路由
│   │   │   ├── __init__.py
│   │   │   └── routes/
│   │   │       ├── categories.py
│   │   │       ├── chat.py
│   │   │       ├── generations.py
│   │   │       └── health.py
│   │   ├── core/            # 核心模块
│   │   │   ├── config.py    # 配置管理
│   │   │   └── logger.py    # 日志系统
│   │   ├── services/        # 业务服务
│   │   │   └── linkai.py    # LinkAI集成
│   │   ├── __init__.py
│   │   └── main.py          # 应用入口
│   ├── requirements.txt     # Python依赖
│   └── venv/                # 虚拟环境
│
└── frontend/                 # 前端应用 (Next.js)
    ├── src/app/
    │   ├── components/        # React组件
    │   │   ├── Sidebar.tsx
    │   │   ├── Header.tsx
    │   │   ├── ChatInput.tsx
    │   │   ├── CategoryTabs.tsx
    │   │   ├── UGCGrid.tsx
    │   │   └── UGCCard.tsx
    │   ├── page.tsx          # 主页
    │   ├── layout.tsx        # 布局
    │   └── globals.css       # 全局样式
    ├── public/               # 静态资源
    ├── package.json
    └── next.config.ts
```

## ⚙️ 配置说明

### 环境变量配置

编辑 `.env` 文件配置以下选项：

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `BACKEND_PORT` | 后端服务端口 | 8000 |
| `FRONTEND_PORT` | 前端开发服务器端口 | 3000 |
| `DATABASE_URL` | PostgreSQL 数据库连接字符串 | - |
| `REDIS_URL` | Redis 连接字符串 | - |
| `LINKAI_API_BASE` | LinkAI API 地址 | - |
| `LINKAI_API_KEY` | LinkAI API 密钥 | - |
| `JWT_SECRET` | JWT 签名密钥 | - |

### LinkAI 四大功能工作流配置

在 `.env` 文件中配置四个工作流编码：

```env
# AI教案·大单元 - 用于生成完整教学设计
LINKAI_WORKFLOW_LESSON=lesson_plan_workflow

# AI命题 - 用于生成考试题目
LINKAI_WORKFLOW_QUESTION=question_gen_workflow

# AI组题 - 用于组合完整试卷
LINKAI_WORKFLOW_EXAM=exam_gen_workflow

# AI互动课件/教学动画 - 用于生成教学动画脚本
LINKAI_WORKFLOW_ANIMATION=animation_workflow
```

### 启动脚本配置

启动脚本 `start_dev.py` 支持以下特性：

- **自动检测 Python 和 Node.js 环境**
- **自动创建虚拟环境并安装依赖**
- **自动检测端口占用**
- **从 .env 文件读取端口配置**

运行：
```bash
python start_dev.py
python start_dev.py --help   # 查看帮助
```

## 🛠️ 开发指南

### 后端开发 (Python/FastAPI)

```bash
cd backend

# 激活虚拟环境（Windows）
.\venv\Scripts\activate

# 激活虚拟环境（Linux/macOS）
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 启动开发服务器
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**API 列表：**
- `GET /api/health` - 健康检查
- `GET /api/categories` - 获取分类列表
- `GET /api/generations/hot` - 获取热门UGC
- `POST /api/chat/generate` - 创建AI生成任务
- `GET /api/chat/status/{taskId}` - 查询任务状态

**API 文档：**
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

### 前端开发 (Next.js)

```bash
cd frontend
npm run dev   # 启动 Next.js 开发服务器
```

**技术栈：**
- Next.js 14
- React 18
- TypeScript
- Tailwind CSS
- Lucide Icons

## 📊 技术栈总览

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Next.js | 14.x |
| 前端语言 | TypeScript | 5.x |
| 前端样式 | Tailwind CSS | 3.x |
| 后端框架 | FastAPI | 0.109.x |
| 后端语言 | Python | 3.8+ |
| ASGI 服务器 | Uvicorn | 0.27.x |
| 数据验证 | Pydantic | 2.6.x |
| HTTP 客户端 | httpx | 0.26.x |
| 前端运行环境 | Node.js | >= 18.0.0 |

## 📝 更新日志

详见 [CHANGELOG.md](./CHANGELOG.md)

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT

---

