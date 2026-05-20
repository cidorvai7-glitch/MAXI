package com.maxi.assistant.ai

object AIResponseManager {

    private val COMMAND_PREFIXES = listOf(
        "OPEN_APP", "CALL", "WHATSAPP_CALL", "WHATSAPP_MSG",
        "SMS", "YOUTUBE_PLAY", "SPOTIFY_PLAY",
        "FLASHLIGHT_ON", "FLASHLIGHT_OFF",
        "VOLUME_UP", "VOLUME_DOWN",
        "WIFI_ON", "WIFI_OFF",
        "BLUETOOTH_ON", "BLUETOOTH_OFF",
        "REBOOT", "POWER_OFF", "SCREENSHOT",
        "OPEN_WEBSITE", "PHONE_STATUS",
        "SILENT_MODE_ON", "SILENT_MODE_OFF",
        "SCREEN_BRIGHTNESS", "AIRPLANE_MODE"
    )

    fun clean(raw: String): String {
        return raw
            .replace(Regex("\\*+"), "")
            .replace(Regex("#+\\s*"), "")
            .trim()
    }

    fun extractCommand(text: String): String? {
        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (COMMAND_PREFIXES.any { trimmed.startsWith(it) }) {
                return trimmed
            }
        }
        return null
    }

    fun isCommandOnly(text: String): Boolean {
        return COMMAND_PREFIXES.any { text.trim().startsWith(it) }
    }
}
