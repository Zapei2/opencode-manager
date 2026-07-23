# OpenCode Manager

A desktop tool to browse and manage local [OpenCode](https://github.com/anomalyco/opencode) sessions.

> 🇨🇳 [中文版](README.zh.md)

---

## Introduction

OpenCode Manager is a cross-platform desktop application built with Tauri. It reads OpenCode's SQLite database directly with zero configuration.

## Features

- 📂 **Directory Tree** — Sessions organized by filesystem path, with collapsible folders
- 🔍 **Search** — Real-time filtering by title and directory path
- 📋 **Session Management** — Rename, copy, paste, move, archive, delete
- 🎯 **Multi-Select** — Ctrl+click to toggle, Shift+click for range, drag to select
- 🌓 **Dark/Light Theme** — Switchable and persistent
- 🌐 **i18n** — Built-in Chinese and English support
- ⌨️ **Keyboard Shortcuts** — Full shortcut coverage
- 📦 **Zero Runtime** — No JRE or JDK required

## Screenshots

*(Add screenshots here)*

## Shortcuts

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

## Download

Get the latest package from [Releases](https://github.com/Zapei2/opencode-manager/releases):

- **Linux**: `.deb` package (Debian/Ubuntu)
- **Windows**: `.exe` installer (NSIS)

## Development

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

### Linux System Dependencies

```bash
sudo apt install libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Desktop Framework | [Tauri v2](https://v2.tauri.app) |
| Backend | Rust 2021 |
| Database | SQLite via rusqlite |
| Frontend | HTML + CSS + JavaScript (Vanilla) |
| Build Tool | Vite 6 |

## License

[MIT](LICENSE)
