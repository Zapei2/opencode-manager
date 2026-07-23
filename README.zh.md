# OpenCode Manager

浏览和管理本机 [OpenCode](https://github.com/anomalyco/opencode) 会话的桌面工具。

> 🇬🇧 [English](README.md)

---

## 简介

OpenCode Manager 是一个基于 Tauri 的跨平台桌面应用，直接读取 OpenCode 的 SQLite 数据库，无需任何配置即可使用。

## 功能

- 📂 **目录树浏览** — 按文件系统路径组织会话，支持展开/折叠
- 🔍 **搜索过滤** — 实时搜索标题和目录路径
- 📋 **会话管理** — 重命名、复制、粘贴、移动、归档、删除
- 🎯 **多选操作** — Ctrl+点击切换选中，Shift+点击范围选中，鼠标拖拽批量选择
- 🌓 **深色/浅色主题** — 一键切换，自动持久化
- 🌐 **中英文界面** — 内置双语支持，设置中切换
- ⌨️ **键盘快捷键** — 全套快捷键操作，效率拉满
- 📦 **零依赖** — 无需安装 Java 或其他运行时

## 截图

*(等待添加)*

## 快捷键

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

## 下载

从 [Releases](https://github.com/Zapei2/opencode-manager/releases) 下载对应系统的安装包：

| 平台 | 格式 | 说明 |
|------|------|------|
| 🐧 Linux | `.deb` | Debian/Ubuntu 双击安装 |
| 🪟 Windows | `.exe` | NSIS 安装包，双击安装 |

> 两个平台均**无需额外安装运行环境**，开箱即用。

## 开发

```bash
# 克隆
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

### Linux 系统依赖

```bash
sudo apt install libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev
```

## 技术栈

| 层 | 技术 |
|----|------|
| 桌面框架 | [Tauri v2](https://v2.tauri.app) |
| 后端语言 | Rust 2021 |
| 数据库 | SQLite via rusqlite |
| 前端 | HTML + CSS + JavaScript |
| 构建工具 | Vite 6 |

## 许可证

[MIT](LICENSE)
