# OpenCode Manager

浏览和管理本机 [OpenCode](https://github.com/anomalyco/opencode) 会话的桌面工具。

## 技术栈

- **Tauri v2** — 原生 WebView 桌面框架
- **Rust** — 后端数据库访问
- **Vanilla JS** — 前端 UI

## 系统要求

运行时无需额外依赖（打包了 WebView，Windows 使用系统 WebView2，Linux 使用 WebKitGTK）。

## 下载

从 [Releases](https://github.com/Zapei2/opencode-manager/releases) 获取：

- **Linux**: `.deb` 安装包
- **Windows**: `.msi` 安装包

## 开发

```bash
npm install
npm run tauri dev     # 开发模式
npx tauri build       # 构建发布包
```

## 许可证

[MIT](LICENSE)
