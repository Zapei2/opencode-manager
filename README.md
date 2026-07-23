# OpenCode Manager

> 浏览和管理本机 [OpenCode](https://github.com/anomalyco/opencode) 会话的桌面工具。
> A desktop tool to browse and manage local OpenCode sessions.

---

## 中文

### 简介

OpenCode Manager 是一个基于 Tauri 的跨平台桌面应用，用于浏览、搜索和管理本机的 OpenCode 会话。它直接读取 OpenCode 的 SQLite 数据库，无需额外配置。

### 功能

- 📂 **目录树浏览** — 按文件系统路径组织会话，支持展开/折叠
- 🔍 **搜索过滤** — 实时搜索标题和目录路径
- 📋 **会话管理** — 重命名、复制、粘贴、移动、归档、删除
- 🎯 **多选操作** — Ctrl+点击切换选中，Shift+点击范围选中，鼠标拖拽批量选择
- 🌓 **深色/浅色主题** — 支持切换并持久化
- 🌐 **中英文界面** — 内置双语支持，一键切换
- ⌨️ **键盘快捷键** — 全套快捷键操作
- 📦 **零依赖** — 内置 JRE 无需安装 Java

### 快捷键

| 快捷键 | 操作 |
|--------|------|
| Ctrl+A | 全选 |
| Ctrl+Shift+C | 复制会话 |
| Ctrl+Shift+V | 粘贴会话 |
| Ctrl+R | 重命名 |
| Ctrl+D | 删除 |
| Ctrl+E | 归档/取消归档 |
| Ctrl+M | 移动 |
| F5 | 刷新 |
| Ctrl+F | 搜索 |

### 下载

从 [Releases](https://github.com/Zapei2/opencode-manager/releases) 下载对应系统的安装包：

- **Linux**: `.deb` 安装包（Debian/Ubuntu）
- **Windows**: `.exe` 安装包（NSIS）

### 开发

```bash
# 克隆仓库
git clone https://github.com/Zapei2/opencode-manager.git
cd opencode-manager

# 安装前端依赖
npm install

# 开发模式（热重载）
npm run tauri dev

# 生产构建
npx tauri build --bundles deb   # Linux
npx tauri build --bundles nsis  # Windows
```

#### 系统依赖（Linux）

```bash
sudo apt install libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev
```

### 技术栈

| 层 | 技术 |
|----|------|
| 桌面框架 | [Tauri v2](https://v2.tauri.app) |
| 后端语言 | Rust 2021 |
| 数据库 | SQLite via rusqlite |
| 前端 | HTML + CSS + JavaScript (Vanilla) |
| 构建工具 | Vite 6 |

### 许可证

[MIT](LICENSE)

---

## English

### Introduction

OpenCode Manager is a cross-platform desktop application built with Tauri for browsing, searching, and managing local OpenCode sessions. It reads OpenCode's SQLite database directly with zero configuration.

### Features

- 📂 **Directory Tree** — Sessions organized by filesystem path, with collapsible folders
- 🔍 **Search** — Real-time filtering by title and directory path
- 📋 **Session Management** — Rename, copy, paste, move, archive, delete
- 🎯 **Multi-Select** — Ctrl+click to toggle, Shift+click for range, drag to select
- 🌓 **Dark/Light Theme** — Switchable and persistent
- 🌐 **i18n** — Built-in Chinese and English support
- ⌨️ **Keyboard Shortcuts** — Full shortcut coverage
- 📦 **Zero Runtime** — No JRE or JDK required

### Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+A | Select All |
| Ctrl+Shift+C | Copy Session |
| Ctrl+Shift+V | Paste Session |
| Ctrl+R | Rename |
| Ctrl+D | Delete |
| Ctrl+E | Archive/Unarchive |
| Ctrl+M | Move |
| F5 | Refresh |
| Ctrl+F | Search |

### Download

Get the latest package from [Releases](https://github.com/Zapei2/opencode-manager/releases):

- **Linux**: `.deb` package (Debian/Ubuntu)
- **Windows**: `.exe` installer (NSIS)

### Development

```bash
# Clone
git clone https://github.com/Zapei2/opencode-manager.git
cd opencode-manager

# Install frontend deps
npm install

# Dev mode (hot reload)
npm run tauri dev

# Production build
npx tauri build --bundles deb   # Linux
npx tauri build --bundles nsis  # Windows
```

#### Linux System Dependencies

```bash
sudo apt install libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Desktop Framework | [Tauri v2](https://v2.tauri.app) |
| Backend | Rust 2021 |
| Database | SQLite via rusqlite |
| Frontend | HTML + CSS + JavaScript (Vanilla) |
| Build Tool | Vite 6 |

### License

[MIT](LICENSE)
