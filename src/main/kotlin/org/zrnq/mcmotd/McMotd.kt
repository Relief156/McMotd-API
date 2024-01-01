package org.zrnq.mcmotd

import com.alibaba.fastjson.parser.ParserConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import org.zrnq.mclient.MClientOptions
import org.zrnq.mclient.output.APIOutputHandler
import org.zrnq.mclient.pingInternal
import java.util.*

lateinit var miraiLogger : MiraiLogger

object McMotd : KotlinPlugin(
    JvmPluginDescription(
        id = "org.zrnq.mcmotd",
        name = "Minecraft MOTD Fetcher",
        version = "1.1.16",
    ) {
        author("ZRnQ")
        info("""以图片的形式获取指定Minecraft服务器的基本信息""")
    }
) {
    override fun onEnable() {
        miraiLogger = logger
        logger.info { "McMotd is loading" }
        ParserConfig.getGlobalInstance().isSafeMode = true
        PluginConfig.reload()
        PluginData.reload()
        QueryCommand.register()
        BindCommand.register()
        DelCommand.register()
        RecordCommand.register()
        HttpServerCommand.register()
        MClientOptions.loadPluginConfig()
        startRecord()
        configureHttpServer()
    }

    override fun onDisable() {
        logger.info { "McMotd is unloading" }
        QueryCommand.unregister()
        BindCommand.unregister()
        DelCommand.unregister()
        RecordCommand.unregister()
        HttpServerCommand.unregister()
        stopRecord()
        stopHttpServer()
    }

    private lateinit var timer : Timer

    private fun startRecord() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                PluginConfig.recordOnlinePlayer.forEach { address ->
                    pingInternal(address, APIOutputHandler(miraiLogger,
                        { miraiLogger.warning("Periodic check failed for $address: $it") },
                        { if(it.onlinePlayerCount == null) miraiLogger.warning("Periodic check: target server ($address) does not supply online player count")
                        else PluginData.recordHistory(address, it.onlinePlayerCount) })
                    )
                }
            }
        }, PluginConfig.recordInterval.toLong() * 1000, PluginConfig.recordInterval.toLong() * 1000)
    }

    private fun stopRecord() {
        timer.cancel()
    }

    private var httpServer : ApplicationEngine? = null

    private fun configureHttpServer() {
        if(PluginConfig.httpServerPort == 0) return
        logger.info("Starting embedded http server on http://localhost:${PluginConfig.httpServerPort}")
        httpServer = embeddedServer(Netty, PluginConfig.httpServerPort, module = Application::mcmotdHttpServer).start(false)
    }

    private fun stopHttpServer() {
        if(httpServer != null)
            httpServer!!.stop()
    }
}
