package com.maxi.assistant.utils

object Constants {
    // Gemini
    const val GEMINI_MODEL = "models/gemini-2.5-flash-native-audio-preview-12-2025"
    const val GEMINI_WS_BASE = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    const val GEMINI_VOICE = "Aoede"

    // Prefs
    const val PREFS_NAME = "maxi_prefs"
    const val KEY_API_KEY = "api_key"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_NICK = "user_nick"
    const val DEFAULT_USER_NICK = "জান"
    const val KEY_PERSONALITY = "personality"
    const val KEY_VOICE_TYPE = "voice_type"
    const val KEY_LIVE_MODE = "live_mode"
    const val KEY_PRIME_NAME = "prime_name"
    const val KEY_PRIME_NUMBER = "prime_number"
    const val KEY_CALL_ANNOUNCE = "call_announce"
    const val KEY_WAKE_WORD = "wake_word"
    const val DEFAULT_WAKE_WORD = "MAXI"
    const val KEY_SILENT_MODE = "silent_mode"

    // Notifications
    const val NOTIF_CHANNEL_OVERLAY = "maxi_overlay_channel"
    const val NOTIF_CHANNEL_VOICE = "maxi_voice_channel"
    const val NOTIF_ID_OVERLAY = 1001
    const val NOTIF_ID_VOICE = 1002

    // Audio
    const val SAMPLE_RATE_IN = 16000
    const val SAMPLE_RATE_OUT = 24000
    const val VAD_SILENCE_MS = 600L

    // Actions
    const val ACTION_SHOW_OVERLAY = "SHOW_OVERLAY"
    const val ACTION_HIDE_OVERLAY = "HIDE_OVERLAY"
    const val ACTION_TOGGLE_OVERLAY = "TOGGLE_OVERLAY"
    const val ACTION_START_LISTENING = "START_LISTENING"
    const val ACTION_STOP_LISTENING = "STOP_LISTENING"
    const val ACTION_WAKE_WORD_DETECTED = "WAKE_WORD_DETECTED"
    const val ACTION_SILENT_MODE = "SILENT_MODE"

    // Voice States
    const val STATE_IDLE = "IDLE"
    const val STATE_LISTENING = "LISTENING"
    const val STATE_THINKING = "THINKING"
    const val STATE_SPEAKING = "SPEAKING"

    // Phone Status thresholds
    const val TEMP_WARNING_C = 40.0f
    const val RAM_WARNING_PERCENT = 85
}
