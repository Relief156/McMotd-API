param(
    [string]$serverAddress = "mcsy.net"
)

if (-not $serverAddress) {
    echo "请提供服务器地址作为参数，例如：.\base64ToPng_fixed.ps1 mcsy.net"
    exit 1
}

$outputFileName = "${serverAddress}.png"

# 从/raw端点获取JSON数据
try {
    $jsonResponse = Invoke-WebRequest -Uri "http://localhost:8082/raw/${serverAddress}" -UseBasicParsing
    $jsonData = $jsonResponse.Content | ConvertFrom-Json
    
    # 提取base64编码的图像数据
    $base64Image = $jsonData.favicon
    
    # 去除可能的data URL前缀
    if ($base64Image -like "data:image/png;base64,*") {
        $imageData = $base64Image -replace "data:image/png;base64,", ""
    } else {
        $imageData = $base64Image
    }
    
    # 解码base64数据并保存为PNG文件
    $bytes = [System.Convert]::FromBase64String($imageData)
    [System.IO.File]::WriteAllBytes($outputFileName, $bytes)
    
    # 检查文件是否存在并显示信息
    if (Test-Path $outputFileName) {
        $fileInfo = Get-Item $outputFileName
        echo "成功将base64图像转换为PNG文件：$outputFileName"
        echo "文件大小：$($fileInfo.Length) 字节"
    } else {
        echo "错误：文件未创建"
        exit 1
    }
} catch {
    echo "转换失败：$($_.Exception.Message)"
    exit 1
}