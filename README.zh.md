# OpenCode Manager

OpenCode 的桌面伴侣工具 - 可视化管理会话**并在内嵌终端中直接启动**，解决 OpenCode 官方桌面端无法同步 TUI 会话历史的问题。

> **前置要求**：使用前请先安装 [OpenCode](https://opencode.ai/zh)。

> 🇬🇧 [English](README.md)

---

## 为什么需要这个？

OpenCode 自带的 TUI 在编码场景下很好用，但会话管理存在三个痛点：

- **无可视化浏览** - 会话列表是扁平的，没有目录树和搜索
- **无批量操作** - 不能批量删除、归档、移动会话
- **桌面端与 TUI 会话不同步** - 官方桌面应用和 TUI 使用不同的会话数据库，TUI 里创建的会话在桌面端看不到，反之亦然

OpenCode Manager 一次性解决这三个问题：

### 会话管理器
- **一目了然** - 会话以表格呈现，按目录树组织，支持排序
- **批量操作** - 选择多个会话（点击、Shift+点击、或拖拽），一键删除/归档/移动/重命名
- **快速查找** - 实时搜索标题或目录路径
- **放心清理** - 归档旧会话而不是直接丢失，删除前自动备份数据库

### 会话启动器
- **内嵌终端** - 双击任意会话即可在应用窗口内通过 `opencode -s <id>` 直接启动，无需调用外部终端
- **标签页界面** - 可同时打开多个会话标签页，一键切换
- **主题感知** - 终端跟随应用的深色/浅色主题，包括 TUI 配色方案自动检测
- **非破坏性导航** - 可随时返回会话列表而不关闭正在运行的终端

### 数据库同步修复
OpenCode 根据安装通道使用不同的数据库文件（如 `opencode.db` 和 `opencode-master.db`）。OpenCode Manager 自动检测正确的数据库，并在内嵌终端中设置 `OPENCODE_DISABLE_CHANNEL_DB=true`，确保从应用启动的会话使用**与应用相同的数据库** -- 不再出现 "Session not found" 错误。

## 前置要求

请先安装 [OpenCode](https://opencode.ai/zh)：

```bash
# macOS / Linux
pipx install opencode

# 或用 pip
pip install opencode
```

安装后运行 `opencode --help` 确认。

## 下载

| 平台 | 格式 |
|------|------|
| 🐧 Linux | `.deb`（Debian/Ubuntu） |
| 🪟 Windows | `.exe` 安装包 |

从 [Releases](https://github.com/Zapei2/opencode-manager/releases) 获取最新版本。

## 从源码构建

```bash
git clone https://github.com/Zapei2/opencode-manager.git
cd opencode-manager
npm install
npm run tauri dev              # 开发模式
npx tauri build --bundles deb   # Linux 打包
npx tauri build --bundles nsis  # Windows 打包
```

Linux 系统依赖：

```bash
sudo apt install libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev
```

## 技术栈

| 层 | 技术 |
|----|------|
| 桌面框架 | [Tauri v2](https://v2.tauri.app) |
| 后端 | Rust |
| 数据库 | SQLite (rusqlite) |
| 终端 | [xterm.js](https://xtermjs.org) + [portable-pty](https://github.com/wez/portable-pty) |
| 前端 | HTML/CSS/JS + Vite |

## 许可证

MIT
