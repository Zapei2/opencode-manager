# OpenCode Session Manager

浏览和管理本机 [OpenCode](https://github.com/anomalyco/opencode) 会话的图形界面工具。

## 功能

- 浏览所有本地 OpenCode 会话
- 按目录树组织会话，精确过滤
- 重命名、复制、粘贴、移动、归档、删除会话
- 深色/浅色主题切换（Arc / Arc Dark Orange）
- 键盘快捷键全操作
- SQLite 数据库，增量备份

## 系统要求

- **Java 21+** 运行时（运行 fat JAR）
- **或** 无需 Java → 使用 jpackage 原生安装包

## 快速开始

### 方式一：原生安装包

从 [Releases](https://github.com/Zapei2/opencode-manager/releases) 下载对应系统的安装包：

- **Linux**: `OpenCodeManager_1.0.0-1_amd64.deb`
- **Windows**: `OpenCodeManager-1.0.0.exe`

安装后直接从应用菜单启动。

### 方式二：fat JAR

```bash
# 前提：安装 JDK 21+ 和 Maven
mvn clean package -DskipTests
java -jar target/opencode-manager-1.0.0.jar
```

## 构建

```bash
# fat JAR
mvn clean package -DskipTests

# Linux .deb
jpackage \
  --input target \
  --name "OpenCodeManager" \
  --main-jar "opencode-manager-1.0.0.jar" \
  --main-class opencode.manager.Main \
  --type deb \
  --app-version "1.0.0" \
  --vendor "Zapei2" \
  --linux-shortcut \
  --dest dist
```

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

## 数据库

默认连接 `~/.local/share/opencode/opencode.db`（OpenCode 的 SQLite 数据库）。

## 许可证

[MIT](LICENSE)
