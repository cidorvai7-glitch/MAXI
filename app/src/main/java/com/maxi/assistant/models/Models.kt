package com.maxi.assistant.models

data class VoiceCommand(
    val rawText: String,
    val commandType: CommandType,
    val parameters: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f
)

enum class CommandType {
    OPEN_APP,
    SEND_MESSAGE,
    MAKE_CALL,
    SEARCH,
    SET_ALARM,
    PLAY_MUSIC,
    NAVIGATE,
    SYSTEM_CONTROL,
    TAKE_PHOTO,
    READ_NOTIFICATION,
    AUTOMATION,
    CONVERSATION,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    VOLUME_UP,
    VOLUME_DOWN,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    WIFI_ON,
    WIFI_OFF,
    BLUETOOTH_ON,
    BLUETOOTH_OFF,
    SCREEN_LOCK,
    TAKE_SCREENSHOT,
    SILENT_MODE_ON,
    SILENT_MODE_OFF,
    VIBRATE_MODE,
    DO_NOT_DISTURB,
    CALL,
    WHATSAPP_CALL,
    WHATSAPP_MSG,
    YOUTUBE_PLAY,
    SPOTIFY_PLAY,
    REBOOT,
    POWER_OFF,
    SCREENSHOT,
    UNKNOWN
}

data class AIResponse(
    val text: String,
    val action: CommandType? = null,
    val parameters: Map<String, String> = emptyMap(),
    val isSuccess: Boolean = true
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false
)
