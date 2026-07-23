#!/bin/bash
# Run the fat JAR
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"
JAR="build/libs/opencode-manager-1.0.0.jar"
if [ ! -f "$JAR" ]; then
    echo "请先运行 compile.sh 构建项目"
    exit 1
fi
exec java -jar "$JAR" "$@"
