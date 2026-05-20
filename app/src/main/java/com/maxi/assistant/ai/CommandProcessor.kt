package com.maxi.assistant.ai

import com.maxi.assistant.models.CommandType

object CommandProcessor {
    fun process(input: String): String {
        val cmd = IntentAnalyzer.analyze(input)
        return when (cmd.type) {
            CommandType.OPEN_APP -> "OPEN_APP ${cmd.args["app"] ?: ""}"
            CommandType.CALL -> "CALL ${cmd.args["name"] ?: ""}"
            CommandType.WHATSAPP_CALL -> "WHATSAPP_CALL ${cmd.args["name"] ?: ""}"
            CommandType.WHATSAPP_MSG -> "WHATSAPP_MSG ${cmd.args["name"] ?: ""} ${cmd.args["message"] ?: ""}"
            CommandType.YOUTUBE_PLAY -> "YOUTUBE_PLAY ${cmd.args["query"] ?: ""}"
            CommandType.SPOTIFY_PLAY -> "SPOTIFY_PLAY ${cmd.args["query"] ?: ""}"
            CommandType.VOLUME_UP -> "VOLUME_UP"
            CommandType.VOLUME_DOWN -> "VOLUME_DOWN"
            CommandType.FLASHLIGHT_ON -> "FLASHLIGHT_ON"
            CommandType.FLASHLIGHT_OFF -> "FLASHLIGHT_OFF"
            CommandType.WIFI_ON -> "WIFI_ON"
            CommandType.WIFI_OFF -> "WIFI_OFF"
            CommandType.BLUETOOTH_ON -> "BLUETOOTH_ON"
            CommandType.BLUETOOTH_OFF -> "BLUETOOTH_OFF"
            CommandType.REBOOT -> "REBOOT"
            CommandType.POWER_OFF -> "POWER_OFF"
            CommandType.SCREENSHOT -> "SCREENSHOT"
            CommandType.OPEN_WEBSITE -> "OPEN_WEBSITE ${cmd.args["url"] ?: ""}"
            CommandType.PHONE_STATUS -> "PHONE_STATUS"
            CommandType.SILENT_MODE_ON -> "SILENT_MODE_ON"
            CommandType.SILENT_MODE_OFF -> "SILENT_MODE_OFF"
            CommandType.UNKNOWN -> {
                val action = cmd.args["action"] ?: ""
                if (action.isNotBlank()) "$action ${cmd.args["value"] ?: ""}".trim()
                else input
            }
            else -> input
        }
    }
}
