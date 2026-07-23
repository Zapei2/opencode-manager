#!/bin/bash
# Build fat JAR + jpackage native package
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== 1. Build fat JAR ==="
mvn clean package -DskipTests

JAR="target/opencode-manager-1.0.0.jar"
echo "=== 2. jpackage (Linux) ==="
jpackage \
  --input target \
  --name "OpenCodeManager" \
  --main-jar "opencode-manager-1.0.0.jar" \
  --main-class opencode.manager.Main \
  --type deb \
  --app-version "1.0.0" \
  --vendor "Zapei2" \
  --description "OpenCode Session Manager - 浏览和管理本地 OpenCode 会话" \
  --linux-menu-group "Utility" \
  --linux-shortcut \
  --dest dist

echo "=== 完成 ==="
echo "fat JAR: $JAR"
ls -lh dist/ 2>/dev/null || true
