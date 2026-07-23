@echo off
REM Build fat JAR + jpackage native installer (Windows)
cd /d "%~dp0"

echo === 1. Build fat JAR ===
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo === 2. jpackage (Windows) ===
jpackage ^
  --input target ^
  --name "OpenCodeManager" ^
  --main-jar "opencode-manager-1.0.0.jar" ^
  --main-class opencode.manager.Main ^
  --type exe ^
  --app-version "1.0.0" ^
  --vendor "Zapei2" ^
  --description "OpenCode Session Manager" ^
  --win-shortcut ^
  --dest dist

echo === 完成 ===
dir dist\
