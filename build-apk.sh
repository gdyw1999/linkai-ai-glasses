#!/bin/bash

# 青橙眼镜App构建脚本

echo "========================================="
echo "青橙AI眼镜Android App 构建脚本"
echo "========================================="
echo ""

# 检查gradlew是否存在
if [ ! -f "./gradlew" ]; then
    echo "错误: gradlew文件不存在"
    exit 1
fi

# 赋予执行权限
chmod +x ./gradlew

echo "步骤1: 清理构建缓存..."
./gradlew clean

if [ $? -ne 0 ]; then
    echo "错误: 清理失败"
    exit 1
fi

echo ""
echo "步骤2: 构建Debug APK..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "错误: 构建失败"
    exit 1
fi

echo ""
echo "========================================="
echo "构建成功!"
echo "========================================="
echo ""
echo "APK位置: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "安装命令:"
echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "启动命令:"
echo "  adb shell am start -n com.glasses.app/.MainActivity"
echo ""
