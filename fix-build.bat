@echo off
REM 修复构建问题脚本

echo =========================================
echo 清理Gradle缓存并重新构建
echo =========================================
echo.

echo 步骤1: 停止Gradle守护进程...
call gradlew.bat --stop

echo.
echo 步骤2: 清理构建缓存...
call gradlew.bat clean

echo.
echo 步骤3: 清理Gradle缓存...
rmdir /s /q .gradle 2>nul
rmdir /s /q app\build 2>nul

echo.
echo 步骤4: 重新构建...
call gradlew.bat assembleDebug --stacktrace

if %errorlevel% neq 0 (
    echo.
    echo =========================================
    echo 构建失败，请查看上面的错误信息
    echo =========================================
    pause
    exit /b 1
)

echo.
echo =========================================
echo 构建成功!
echo =========================================
echo.
echo APK位置: app\build\outputs\apk\debug\app-debug.apk
echo.
pause
