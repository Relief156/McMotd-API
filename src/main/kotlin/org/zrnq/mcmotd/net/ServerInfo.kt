package org.zrnq.mcmotd.net

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import org.zrnq.mcmotd.*

class ServerInfo(private val actualAddress: String,
                 response : String,
                 private val latency : Int) {
    /**服务器图标*/
    val favicon : String?
    /**服务器描述*/
    private val description : String
    /**服务器版本号*/
    private val version : String
    /**在线人数*/
    var onlinePlayerCount : Int?
    /**服务器宣称的最大人数*/
    private var maxPlayerCount : Int?
    /**服务器提供的部分在线玩家列表*/
    private var samplePlayerList : String?
    
    /**获取服务器描述*/
    fun getDescription() = description
    
    /**获取服务器版本*/
    fun getVersion() = version
    
    /**获取最大玩家数*/
    fun getMaxPlayerCount() = maxPlayerCount
    
    /**获取玩家列表样本*/
    fun getSamplePlayerList() = samplePlayerList
    
    /**获取延迟*/
    fun getLatency() = latency
    /**本次查询用户所提供的原始服务器地址*/
    private lateinit var originalAddress : String
    /**服务器的显示地址*/
    private val serverAddress : String
        get() = if(configStorage.showTrueAddress) actualAddress else originalAddress
    
    /**获取服务器地址*/
    fun getDisplayAddress() = serverAddress

    init {
        val json = JSON.parseObject(response)
        favicon = json.getString("favicon")
        description = json.getString("description")
        version = json.getJSONObject("version").getString("name")
        val playerJson = json.getJSONObject("players")

        onlinePlayerCount = playerJson?.getIntValue("online")
        maxPlayerCount = playerJson?.getIntValue("max")
        samplePlayerList = playerJson?.getJSONArray("sample")?.toPlayerListString(10)
    }

    fun setOriginalAddress(address : String) : ServerInfo {
        originalAddress = address
        return this
    }

    private fun getDescriptionHTML(): String
        = if(description.startsWith("{")) jsonStringToHTML(JSON.parseObject(description))
            else jsonStringToHTML(JSON.parseObject("{\"text\":\"$description\"}"))

    private fun getPingHTML(): String {
        val bars = when(latency) {
            -1 -> "red" to 0
            in 0 until 100 -> "green" to 5
            in 100 until 300 -> "green" to 4
            in 300 until 500 -> "green" to 3
            in 500 until 1000 -> "yellow" to 2
            else -> "red" to 1
        }.let { colorMap[it.first] to it.second }
        if(latency < 0) return "失败 [<span style='color:${bars.first};text-shadow: gray 2px 2px;'>×</span>]"
        return "${latency}ms " +
               "[<span style='color:${bars.first}; font-weight:bold;'>${"|".repeat(bars.second)}</span>" +
               "<span style='color:${colorMap["gray"]}; font-weight:bold;'>${"|".repeat(5 - bars.second)}</span>]"
    }

    private fun getPlayerDescriptionHTML(): String {
        if(onlinePlayerCount == null) return "服务器未提供在线玩家信息"
        val playerCount = StringBuilder("在线人数: $onlinePlayerCount")
        if(configStorage.showPeakPlayers && dataStorage.peakPlayers.contains(originalAddress)) {
            playerCount.append("(${dataStorage.peakPlayers[originalAddress]})")
        }
        playerCount.append("/$maxPlayerCount　")
        if(!configStorage.showPlayerList) return playerCount.toString()
        playerCount.append("玩家列表: ")
        if(samplePlayerList == null) return playerCount.append("没有信息").toString()
        return playerCount.append(jsonStringToHTML(JSON.parseObject("{\"text\":\"$samplePlayerList\"}"))).toString()
    }


    fun toHTMLString(): String {
        val sb = StringBuilder("<!DOCTYPE html><html><head></head><body><div>")
        sb.append(getDescriptionHTML())
            .append("</div>")
            .append("<div style='color:white;margin-top: 10px;'>访问地址: $serverAddress　Ping: ")
            .append(getPingHTML())
            .append("</div>")
        if(configStorage.showServerVersion) {
            sb.append("<div style='color:white;'>")
                .append(version.limitLength(50))
                .append("</div>")
        }
        sb.append("<div style='color:white;'>")
            .append(getPlayerDescriptionHTML())
            .append("</div>")
            .append("</body></html>")
        return sb.toString()
    }

    fun merge(queryInfo: QueryServerInfo) {
        if(onlinePlayerCount == null) {
            onlinePlayerCount = queryInfo.serverProperties["numplayers"]?.toInt()
        }
        if(maxPlayerCount == null) {
            maxPlayerCount = queryInfo.serverProperties["maxplayers"]?.toInt()
        }
        if(samplePlayerList == null || samplePlayerList == emptyPlayerListMsg) {
            samplePlayerList = if(queryInfo.playerList.isEmpty()) emptyPlayerListMsg
            else queryInfo.playerList.joinToString(", ", limit = 10)
        }
    }

    companion object {
        fun JSONArray?.toPlayerListString(limit: Int) : String? {
            return if(this == null) null
            else if(isEmpty()) emptyPlayerListMsg
            else joinToString(", ", limit = limit) { (it as JSONObject).getString("name") }
        }

        const val emptyPlayerListMsg = "空"
    }
}

class QueryServerInfo(val serverProperties: Map<String, String>, val playerList: List<String>)