# OpenCode Manager

A desktop companion for [OpenCode](https://github.com/anomalyco/opencode) that makes session management visual and efficient.

> **Prerequisite**: [OpenCode](https://github.com/anomalyco/opencode) must be installed on your system. This tool reads OpenCode's local database and uses `opencode -s <id>` to resume sessions.

> 🇨🇳 [中文版](README.zh.md)

---

## Why This?

OpenCode's built-in TUI is great for coding but offers no way to **visually browse, organize, or batch-manage** sessions. Over time, your session list grows into a long, undifferentiated wall of text — making it hard to clean up old projects, archive finished work, or find that one conversation from last week.

OpenCode Manager gives you a proper desktop interface to:

- **See everything at a glance** — sessions in a sortable table, organized by directory tree
- **Bulk delete, archive, move, or rename** — select multiple sessions at once (click, shift-click, or drag)
- **Find anything fast** — real-time search by title or directory path
- **Clean up with confidence** — archive old sessions instead of losing them; backup before deletion

It reads your local OpenCode database directly. No server, no sync, no configuration.

## Prerequisites

[OpenCode](https://github.com/anomalyco/opencode) is required. Install it:

```bash
# macOS / Linux (via pipx)
pipx install opencode

# Or via pip
pip install opencode
```

After installation, run `opencode --help` to verify.

## Download

| Platform | Format |
|----------|--------|
| Linux | `.deb` (Debian/Ubuntu) |
| Windows | `.exe` installer |

Grab the latest from [Releases](https://github.com/Zapei2/opencode-manager/releases).

## Build from Source

```bash
git clone https://github.com/Zapei2/opencode-manager.git
cd opencode-manager
npm install
npm run tauri dev          # development
npx tauri build --bundles deb   # Linux package
npx tauri build --bundles nsis  # Windows installer
```

Linux system dependencies:

```bash
sudo apt install libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Desktop | [Tauri v2](https://v2.tauri.app) |
| Backend | Rust |
| Database | SQLite (rusqlite) |
| Frontend | HTML/CSS/JS + Vite |

## License

MIT
