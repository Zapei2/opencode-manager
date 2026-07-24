# OpenCode Manager

OpenCode 的桌面伴侣工具，让会话管理变得直观高效。

> **前置要求**：使用前请先安装 [OpenCode](https://opencode.ai)。本工具读取 OpenCode 的本地数据库，并通过 `opencode -s <id>` 恢复会话。

> 🇬🇧 [English](README.md)

---

## 为什么需要这个？

OpenCode 自带的 TUI 在编码场景下很好用，但**无法可视化地浏览、整理或批量管理**会话。随着时间推移，会话列表会变成一长串没有区分度的文字——清理旧项目、归档已完成的工作、找到上周的某次对话，都变得很困难。

OpenCode Manager 提供一个纯正的桌面界面，让你可以：

- **一目了然** — 会话以表格呈现，按目录树组织，支持排序
- **批量操作** — 选择多个会话（点击、Shift+点击、或拖拽），一键删除/归档/移动/重命名
- **快速查找** — 实时搜索标题或目录路径
- **放心清理** — 归档旧会话而不是直接丢失，删除前自动备份数据库

它直接读取本机 OpenCode 的 SQLite 数据库。无需服务器、无需同步、无需配置。

## 前置要求

请先安装 [OpenCode](https://opencode.ai)：

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
| 前端 | HTML/CSS/JS + Vite |

## 许可证

MIT
