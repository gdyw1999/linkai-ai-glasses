# 替换App图标脚本
# 使用方法：
# 1. 确保logo01.png在项目根目录
# 2. 在PowerShell中运行此脚本：.\replace_app_icon.ps1

Write-Host "开始替换App图标..." -ForegroundColor Green

# 检查logo01.png是否存在
if (-not (Test-Path "logo01.png")) {
    Write-Host "错误：找不到logo01.png文件！" -ForegroundColor Red
    Write-Host "请确保logo01.png在项目根目录" -ForegroundColor Yellow
    exit 1
}

# 定义图标尺寸和目标目录
$iconSizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

Write-Host "需要安装ImageMagick来调整图片大小" -ForegroundColor Yellow
Write-Host "请访问：https://imagemagick.org/script/download.php#windows" -ForegroundColor Yellow
Write-Host ""
Write-Host "或者使用在线工具：" -ForegroundColor Cyan
Write-Host "1. 访问 https://icon.kitchen/" -ForegroundColor Cyan
Write-Host "2. 上传 logo01.png" -ForegroundColor Cyan
Write-Host "3. 下载生成的图标包" -ForegroundColor Cyan
Write-Host "4. 解压到 app/src/main/res/ 目录" -ForegroundColor Cyan
Write-Host ""

# 检查ImageMagick是否安装
$magickInstalled = Get-Command magick -ErrorAction SilentlyContinue

if ($magickInstalled) {
    Write-Host "检测到ImageMagick，开始生成图标..." -ForegroundColor Green
    
    foreach ($dir in $iconSizes.Keys) {
        $size = $iconSizes[$dir]
        $targetDir = "app\src\main\res\$dir"
        
        # 创建目录（如果不存在）
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }
        
        # 生成ic_launcher.png
        $targetFile = "$targetDir\ic_launcher.png"
        Write-Host "生成 $targetFile (${size}x${size})" -ForegroundColor Cyan
        magick convert logo01.png -resize ${size}x${size} $targetFile
        
        # 生成ic_launcher_round.png
        $targetFileRound = "$targetDir\ic_launcher_round.png"
        Write-Host "生成 $targetFileRound (${size}x${size})" -ForegroundColor Cyan
        magick convert logo01.png -resize ${size}x${size} $targetFileRound
    }
    
    Write-Host ""
    Write-Host "图标替换完成！" -ForegroundColor Green
    Write-Host "请在IntelliJ IDEA中执行：" -ForegroundColor Yellow
    Write-Host "1. Build -> Clean Project" -ForegroundColor Yellow
    Write-Host "2. Build -> Rebuild Project" -ForegroundColor Yellow
    Write-Host "3. 运行App查看新图标" -ForegroundColor Yellow
} else {
    Write-Host "未检测到ImageMagick" -ForegroundColor Yellow
    Write-Host "请使用在线工具生成图标" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
