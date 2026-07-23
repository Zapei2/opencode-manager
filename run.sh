#!/bin/bash
# OpenCode 会话管理器 - 运行脚本
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LIB="lib/sqlite-jdbc-3.45.3.0.jar"
SLF4J_API="/usr/lib/slf4j-api-2.0.16.jar"
SLF4J_IMPL="/usr/lib/slf4j-simple-2.0.16.jar"

if [ ! -f "$SLF4J_API" ]; then
    SLF4J_API=$(find /home/zapei2/.gradle -name "slf4j-api-*.jar" 2>/dev/null | head -1)
    SLF4J_IMPL=$(find /home/zapei2/.gradle -name "slf4j-simple-*.jar" 2>/dev/null | head -1)
fi

CP="out:$LIB"
[ -f "$SLF4J_API" ] && CP="$CP:$SLF4J_API"
[ -f "$SLF4J_IMPL" ] && CP="$CP:$SLF4J_IMPL"

exec java -cp "$CP" opencode.manager.Main "$@"
