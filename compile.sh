#!/bin/bash
# Build fat JAR with Gradle
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"
gradle clean jar
echo "编译成功！fat JAR: $(ls -t build/libs/*.jar 2>/dev/null | head -1)"
