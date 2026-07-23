#!/bin/bash
# OpenCode 会话管理器 - 编译脚本
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LIBS="lib/sqlite-jdbc-3.45.3.0.jar:lib/flatlaf-3.4.1.jar:lib/flatlaf-intellij-themes-3.4.1.jar"
SLF4J_API="/usr/lib/slf4j-api-2.0.16.jar"
SLF4J_IMPL="/usr/lib/slf4j-simple-2.0.16.jar"

if [ ! -f "$SLF4J_API" ]; then
    SLF4J_API=$(find /home/zapei2/.gradle -name "slf4j-api-*.jar" 2>/dev/null | head -1)
    SLF4J_IMPL=$(find /home/zapei2/.gradle -name "slf4j-simple-*.jar" 2>/dev/null | head -1)
fi

CP="$LIBS"
[ -f "$SLF4J_API" ] && CP="$CP:$SLF4J_API"
[ -f "$SLF4J_IMPL" ] && CP="$CP:$SLF4J_IMPL"

OUT="out"
mkdir -p "$OUT"

echo "Classpath: $CP"
javac -d "$OUT" -cp "$CP" \
    src/opencode/manager/db/ProjectRecord.java \
    src/opencode/manager/db/SessionRecord.java \
    src/opencode/manager/db/Database.java \
    src/opencode/manager/ui/SessionTableModel.java \
    src/opencode/manager/ui/Theme.java \
    src/opencode/manager/ui/Settings.java \
    src/opencode/manager/ui/SettingsDialog.java \
    src/opencode/manager/ui/MainWindow.java \
    src/opencode/manager/Main.java

echo "编译成功！输出目录: $OUT"
