package com.maxi.assistant.voice

import com.maxi.assistant.utils.AudioUtils
import com.maxi.assistant.websocket.GeminiWebSocketClient

/**
 * Bridges AudioRecorder chunks → GeminiWebSocketClient
 */
class AudioStreamSender(private val client: GeminiWebSocketClient) {

    fun send(pcm: ByteArray) {
        if (VoiceStateManager.isAiSpeaking()) return
        client.sendAudioChunk(pcm)
    }
}
