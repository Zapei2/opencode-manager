# OpenCode Manager

A desktop companion for [OpenCode](https://opencode.ai) — visually manage sessions **and launch them in an embedded terminal**, solving the problem where OpenCode's official desktop app can't sync TUI session history.

> **Prerequisite**: [OpenCode](https://opencode.ai) must be installed on your system.

> 🇨🇳 [中文](README.zh.md)

---

## Why This?

OpenCode's built-in TUI is great for coding, but session management is limited:

- **No visual browser** — sessions are a flat list with no directory tree or search
- **No batch operations** — can't bulk delete, archive, or move sessions
- **Desktop app can't sync TUI history** — the official desktop app and TUI use separate session databases, so sessions created in the TUI don't appear in the desktop app and vice versa

OpenCode Manager solves all three:

### Session Manager
- **See everything at a glance** — sortable table organized by directory tree
- **Bulk delete, archive, move, or rename** — select multiple sessions at once
- **Find anything fast** — real-time search by title or directory path
- **Clean up with confidence** — archive old sessions; backup before deletion

### Session Launcher
- **Embedded terminal** — double-click any session to launch it directly inside the app window via `opencode -s <id>`, no external terminal needed
- **Tabbed interface** — open multiple sessions in tabs, switch between them instantly
- **Theme-aware** — terminal follows the app's dark/light theme, including TUI color scheme detection
- **Non-destructive navigation** — switch back to the session list without closing running terminals

### Database Sync Fix
OpenCode uses different database files depending on the installation channel (e.g., `opencode.db` vs `opencode-master.db`). OpenCode Manager automatically detects the correct database and sets `OPENCODE_DISABLE_CHANNEL_DB=true` in the embedded terminal, ensuring that sessions launched from the app use the **same database** the app reads from — no more "Session not found" errors.

## Prerequisites

[OpenCode](https://opencode.ai) is required. Install it:

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
| Terminal | [xterm.js](https://xtermjs.org) + [portable-pty](https://github.com/wez/portable-pty) |
| Frontend | HTML/CSS/JS + Vite |

## License

MIT
