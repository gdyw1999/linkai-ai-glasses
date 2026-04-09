@echo off
REM 青橙眼镜App构建脚本 (Windows)

REM 设置JAVA_HOME
set "JAVA_HOME=E:\Program Files\Android\Android Studio\jbr"

echo =========================================
echo 青橙AI眼镜Android App 构建脚本
echo =========================================
echo.
echo JAVA_HOME: %JAVA_HOME%
echo.

REM 检查gradlew.bat是否存在
if not exist "gradlew.bat" (
    echo 错误: gradlew.bat文件不存在
    exit /b 1
)

echo 步骤1: 清理构建缓存...
call gradlew.bat clean

if %errorlevel% neq 0 (
    echo 错误: 清理失败
    exit /b 1
)

echo.
echo 步骤2: 构建Debug APK...
call gradlew.bat assembleDebug

if %errorlevel% neq 0 (
    echo 错误: 构建失败
    exit /b 1
)

echo.
echo =========================================
echo 构建成功!
echo =========================================
echo.
echo APK位置: app\build\outputs\apk\debug\app-debug.apk
echo.
echo 安装命令:
echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.
echo 启动命令:
echo   adb shell am start -n com.glasses.app/.MainActivity
echo.
pause
