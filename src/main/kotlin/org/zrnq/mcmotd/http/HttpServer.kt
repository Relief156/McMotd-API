package org.zrnq.mcmotd.http

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zrnq.mcmotd.*
import org.zrnq.mcmotd.output.APIOutputHandler
import org.zrnq.mcmotd.ImageUtil.appendPlayerHistory
import org.zrnq.mcmotd.ImageUtil.drawErrorMessage
import org.zrnq.mcmotd.net.ServerInfo
import org.zrnq.mcmotd.net.parseAddressCached
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object HttpServer {

    private var httpServer : ApplicationEngine? = null

    fun configureHttpServer() {
        if(configStorage.httpServerPort == 0) return
        genericLogger.info("Starting embedded http server on http://localhost:${configStorage.httpServerPort}")
        httpServer = embeddedServer(Netty, configStorage.httpServerPort, module = Application::mcmotdHttpServer).start(false)
    }

    fun stopHttpServer() {
        if(httpServer != null)
            httpServer!!.stop()
    }
}
fun Application.mcmotdHttpServer() {
    routing {
        configureRouting()
    }
}

suspend fun PipelineContext<*, ApplicationCall>.respondImage(image : BufferedImage)
        = call.respondBytes(ContentType.Image.PNG, HttpStatusCode.OK) {
    ByteArrayOutputStream().also { stream ->
        ImageIO.write(image, "png", stream)
    }.toByteArray()
}

suspend fun PipelineContext<*, ApplicationCall>.respondErrorImage(msg : String)
        = respondImage(BufferedImage(1000, 200, BufferedImage.TYPE_INT_RGB).also {
    it.createGraphics().drawErrorMessage(msg, 0, 0, 1000, 200)
})

fun Route.configureRouting() {
    // 原始API，需要配置文件中的服务器名
    route("/info") {
        get("{server?}") {
            if(!RateLimiter.pass(call.request.origin.remoteAddress))
                return@get call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
            val servername = call.parameters["server"] ?: return@get respondErrorImage("未指定服务器名")
            val target = configStorage.httpServerMapping[servername]
                ?: return@get respondErrorImage("指定的服务器名没有在配置文件中定义")
            var error : String? = null
            var image : BufferedImage? = null
            val address = target.parseAddressCached()
            if(address == null) {
                genericLogger.error("Http服务器中配置的服务器地址无效:$target")
                return@get
            }
            withContext(Dispatchers.IO) {
                pingInternal(address, APIOutputHandler({ error = it }, { image = renderBasicInfoImage(it).appendPlayerHistory(target) }))
            }
            if(image == null) {
                genericLogger.error("Http请求失败:$error")
                return@get respondErrorImage("服务器信息获取失败")
            }
            return@get respondImage(image!!)
        }
    }

    // 返回服务器信息的原始JSON数据
    route("/raw") {
        get("{address?}") {
            if(!RateLimiter.pass(call.request.origin.remoteAddress))
                return@get call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
            val address = call.parameters["address"] ?: return@get call.respondText(
                "未指定服务器地址", 
                status = HttpStatusCode.BadRequest
            )
            
            var error : String? = null
            var serverInfo : ServerInfo? = null
            val parsedAddress = address.parseAddressCached()
            if(parsedAddress == null) {
                genericLogger.error("无效的服务器地址:$address")
                return@get call.respondText(
                    "无效的服务器地址", 
                    status = HttpStatusCode.BadRequest
                )
            }
            
            withContext(Dispatchers.IO) {
                pingInternal(parsedAddress, APIOutputHandler(
                    { error = it }, 
                    { serverInfo = it.setOriginalAddress(address) }
                ))
            }
            
            if(serverInfo == null) {
                genericLogger.error("请求失败:$error")
                return@get call.respondText(
                    "服务器信息获取失败: $error", 
                    status = HttpStatusCode.InternalServerError
                )
            }
            
            // 构建返回的JSON数据
            val result = mutableMapOf<String, Any?>(
                "online" to true,
                "motd" to serverInfo!!.getDescription(),
                "players" to mutableMapOf(
                    "max" to serverInfo!!.getMaxPlayerCount(),
                    "online" to serverInfo!!.onlinePlayerCount,
                    "list" to serverInfo!!.getSamplePlayerList()
                ),
                "version" to serverInfo!!.getVersion(),
                "favicon" to serverInfo!!.favicon
            )
            
            // 转换为JSON字符串并返回
            val jsonResult = com.alibaba.fastjson.JSON.toJSONString(result)
            call.respondText(jsonResult, ContentType.Application.Json)
        }
    }

    // 返回64x64 PNG图像的API端点
    route("/icon") {
        get("{address?}") {
            if(!RateLimiter.pass(call.request.origin.remoteAddress))
                return@get call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
            val address = call.parameters["address"] ?: return@get call.respondText(
                "未指定服务器地址", 
                status = HttpStatusCode.BadRequest
            )
            
            var error : String? = null
            var serverInfo : ServerInfo? = null
            val parsedAddress = address.parseAddressCached()
            if(parsedAddress == null) {
                genericLogger.error("无效的服务器地址:$address")
                return@get call.respondText(
                    "无效的服务器地址", 
                    status = HttpStatusCode.BadRequest
                )
            }
            
            withContext(Dispatchers.IO) {
                pingInternal(parsedAddress, APIOutputHandler(
                    { error = it }, 
                    { serverInfo = it.setOriginalAddress(address) }
                ))
            }
            
            if(serverInfo == null) {
                genericLogger.error("请求失败:$error")
                return@get call.respondText(
                    "服务器信息获取失败: $error", 
                    status = HttpStatusCode.InternalServerError
                )
            }
            
            try {
                // 提取favicon数据并解码为BufferedImage
                val faviconBase64 = serverInfo!!.favicon
                if (faviconBase64 != null && faviconBase64.isNotEmpty()) {
                    // 去除可能的data URL前缀
                    val imageData = if (faviconBase64.startsWith("data:image/png;base64,")) {
                        faviconBase64.substring("data:image/png;base64,".length)
                    } else {
                        faviconBase64
                    }
                    
                    // 解码base64数据
                    val imageBytes = java.util.Base64.getDecoder().decode(imageData)
                    
                    // 转换为BufferedImage
                    val inputStream = java.io.ByteArrayInputStream(imageBytes)
                    val image = ImageIO.read(inputStream)
                    
                    // 确保图像大小为64x64
                    val resizedImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
                    val g = resizedImage.createGraphics()
                    g.drawImage(image, 0, 0, 64, 64, null)
                    g.dispose()
                    
                    // 直接返回PNG图像
                    return@get respondImage(resizedImage)
                } else {
                    // 如果没有favicon，返回错误图像
                    return@get respondErrorImage("服务器没有提供图标")
                }
            } catch (e: Exception) {
                genericLogger.error("图像处理失败", e)
                return@get respondErrorImage("图像转换失败: ${e.message}")
            }
        }
    }

    // 直接地址查询路由
    route("/infos") {
        get("{address?}") {
            if(!RateLimiter.pass(call.request.origin.remoteAddress))
                return@get call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
            val address = call.parameters["address"] ?: return@get respondErrorImage("未指定服务器地址")
            
            var error : String? = null
            var image : BufferedImage? = null
            val parsedAddress = address.parseAddressCached()
            if(parsedAddress == null) {
                genericLogger.error("无效的服务器地址:$address")
                return@get respondErrorImage("无效的服务器地址")
            }
            
            withContext(Dispatchers.IO) {
                pingInternal(parsedAddress, APIOutputHandler(
                    { error = it }, 
                    { image = renderBasicInfoImage(it.setOriginalAddress(address)).appendPlayerHistory(address) }
                ))
            }
            
            if(image == null) {
                genericLogger.error("请求失败:$error")
                return@get respondErrorImage("服务器信息获取失败")
            }
            
            return@get respondImage(image!!)
        }
    }
}