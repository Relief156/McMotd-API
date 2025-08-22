import com.alibaba.fastjson.JSONObject
import java.io.File
import java.net.URL
import java.util.Base64

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("请提供服务器地址作为参数，例如：kotlin base64ToPng.kt mcsy.net")
        return
    }
    
    val serverAddress = args[0]
    val outputFileName = "${serverAddress}.png"
    
    try {
        // 从/raw端点获取JSON数据
        val url = URL("http://localhost:8082/raw/${serverAddress}")
        val jsonResponse = url.readText()
        
        // 解析JSON数据
        val jsonObject = JSONObject.parseObject(jsonResponse)
        
        // 提取base64编码的图像数据
        val base64Image = jsonObject.getString("favicon")
        
        // 去除可能的data URL前缀
        val imageData = if (base64Image.startsWith("data:image/png;base64,")) {
            base64Image.substring("data:image/png;base64,".length)
        } else {
            base64Image
        }
        
        // 解码base64数据
        val imageBytes = Base64.getDecoder().decode(imageData)
        
        // 保存为PNG文件
        File(outputFileName).writeBytes(imageBytes)
        
        println("成功将base64图像转换为PNG文件：${outputFileName}")
        println("文件大小：${imageBytes.size} 字节")
    } catch (e: Exception) {
        println("转换失败：${e.message}")
        e.printStackTrace()
    }
}