package com.maxi.assistant.ui.main

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maxi.assistant.R
import com.maxi.assistant.databinding.ActivityMainBinding
import com.maxi.assistant.ui.settings.SettingsActivity
import com.maxi.assistant.utils.Constants
import com.maxi.assistant.utils.LiveAudioManager
import com.maxi.assistant.websocket.GeminiWebSocketClient
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(val text: String, val isUser: Boolean, val time: String)

class ChatAdapter(private val messages: MutableList<ChatMessage> = mutableListOf()) :
    RecyclerView.Adapter<ChatAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvSenderName)
        val tvMsg: TextView = v.findViewById(R.id.tvMessage)
        val tvTime: TextView = v.findViewById(R.id.tvMsgTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (viewType == 0) R.layout.item_msg_maxi else R.layout.item_msg_user
        return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun getItemViewType(position: Int) = if (!messages[position].isUser) 0 else 1
    override fun getItemCount() = messages.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = messages[position]
        holder.tvName.text = if (msg.isUser) "YOU" else "MAXI"
        holder.tvMsg.text = msg.text
        holder.tvTime.text = msg.time
    }

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val TAG = "MAXI_MAIN"

    private lateinit var geminiClient: GeminiWebSocketClient
    private lateinit var audioManager: LiveAudioManager
    private var isLiveConnected = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isActive = false
    private var isSpeaking = false
    private var lastBotResponse = ""

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS
    )

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (scale > 0) binding.tvBattery.text = "${(level * 100 / scale.toFloat()).toInt()}%"
        }
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            binding.tvTime.text = timeFormat.format(Date())
            updateRam()
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = LiveAudioManager(this)
        setupChat()
        setupButtons()
        handler.post(clockRunnable)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        checkPermissions()
        createSpeechRecognizer()
    }

    // ── Speech Recognizer ─────────────────────────────────────────────────────

    private fun createSpeechRecognizer() {
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            addMessage("⚠️ Speech recognition device এ নেই।", false); return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { it.isNotBlank() } ?: ""
                if (text.isBlank()) { scheduleRestart(600); return }
                if (isEcho(text)) { Log.d(TAG, "Echo skip: $text"); scheduleRestart(800); return }

                addMessage(text, true)
                binding.tvStatus.text = "THINKING..."
                if (isLiveConnected) {
                    geminiClient.sendTextMessage(text)
                } else {
                    addMessage("⚠️ Gemini connected নেই। Reconnect করুন।", false)
                }
            }
            override fun onError(code: Int) {
                isListening = false
                if (isSpeaking) return
                when (code) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> scheduleRestart(500)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        handler.postDelayed({ createSpeechRecognizer(); scheduleRestart(900) }, 600)
                    }
                    SpeechRecognizer.ERROR_AUDIO,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        handler.postDelayed({ createSpeechRecognizer(); scheduleRestart(1200) }, 600)
                    }
                    else -> scheduleRestart(1000)
                }
            }
            override fun onReadyForSpeech(p: Bundle?) { binding.tvStatus.text = "LISTENING..." }
            override fun onBeginningOfSpeech() {}
            override fun onEndOfSpeech() { binding.tvStatus.text = "PROCESSING..." }
            override fun onPartialResults(b: Bundle?) {
                val partial = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) binding.tvStatus.text = "$partial..."
            }
            override fun onRmsChanged(f: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEvent(t: Int, b: Bundle?) {}
        })
    }

    private fun startListening() {
        if (isListening || isSpeaking || !isActive) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("bn-BD", "en-US", "hi-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            }
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "startListening: ${e.message}")
            isListening = false; scheduleRestart(1500)
        }
    }

    private var restartRunnable: Runnable? = null
    private fun scheduleRestart(delayMs: Long) {
        restartRunnable?.let { handler.removeCallbacks(it) }
        restartRunnable = Runnable {
            if (isActive && !isSpeaking && !isListening) startListening()
        }.also { handler.postDelayed(it, delayMs) }
    }

    private fun isEcho(text: String): Boolean {
        if (lastBotResponse.isEmpty()) return false
        val u = text.lowercase().trim()
        val b = lastBotResponse.lowercase().trim()
        if (u == b) return true
        if (b.length > 10 && b.contains(u)) return true
        val uW = u.split(" ").filter { it.length > 3 }.toSet()
        val bW = b.split(" ").filter { it.length > 3 }.toSet()
        return if (uW.isEmpty() || bW.isEmpty()) false
        else uW.intersect(bW).size.toDouble() / uW.size > 0.7
    }

    // ── Gemini Live ───────────────────────────────────────────────────────────

    private fun setupGeminiLive() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(Constants.KEY_API_KEY, "") ?: ""
        if (apiKey.isBlank()) {
            addMessage("⚠️ Settings → API Key দিন, তারপর Mic চাপুন।", false); return
        }
        val personality = prefs.getString(Constants.KEY_PERSONALITY + "_prompt", "") ?: ""
        val userName = prefs.getString(Constants.KEY_USER_NAME, "") ?: ""
        val systemPrompt = personality.ifBlank {
            "You are MAXI, a smart AI assistant. Reply in Bangla or English. Be helpful and concise."
        }

        binding.tvStatus.text = "CONNECTING..."

        geminiClient = GeminiWebSocketClient(
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            callback = object : GeminiWebSocketClient.LiveListener {

                override fun onConnected() {
                    isLiveConnected = true
                    handler.post {
                        val greet = if (userName.isNotBlank()) "হ্যালো $userName! আমি MAXI।" else "আমি MAXI, your AI companion।"
                        binding.tvStatus.text = "MAXI READY"
                        addMessage(greet, false)
                        // Greet পাঠাই যাতে MAXI voice এ বলে
                        geminiClient.sendTextMessage("Greet the user briefly in Bangla.")
                        if (isActive) scheduleRestart(800)
                    }
                }

                override fun onAudioReceived(data: ByteArray) {
                    // MAXI এর voice reply — speaker এ বাজাও
                    isSpeaking = true
                    handler.post { binding.tvStatus.text = "SPEAKING..." }
                    audioManager.playChunk(data)
                }

                override fun onTextReceived(text: String) {
                    if (text.isBlank()) return
                    lastBotResponse = text.lowercase()
                    handler.post { addMessage(text, false) }
                }

                override fun onTurnComplete() {
                    // MAXI বলা শেষ — আবার শুনতে শুরু করো
                    isSpeaking = false
                    audioManager.stop()
                    handler.post {
                        binding.tvStatus.text = "LISTENING..."
                        if (isActive) scheduleRestart(1200)
                    }
                }

                override fun onError(msg: String) {
                    isLiveConnected = false
                    Log.e(TAG, "Gemini error: $msg")
                    handler.post {
                        binding.tvStatus.text = "RECONNECTING..."
                        handler.postDelayed({
                            if (isActive) setupGeminiLive()
                        }, 5000)
                    }
                }
            }
        )
        geminiClient.start()
    }

    // ── UI Setup ──────────────────────────────────────────────────────────────

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).also { it.stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun setupButtons() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.micButton.setOnClickListener {
            if (!isActive) {
                // Mic চালু করো
                isActive = true
                if (!::geminiClient.isInitialized || !isLiveConnected) {
                    setupGeminiLive()
                } else {
                    binding.tvStatus.text = "LISTENING..."
                    scheduleRestart(300)
                }
            } else {
                // Mic বন্ধ করো
                isActive = false
                isListening = false
                isSpeaking = false
                restartRunnable?.let { handler.removeCallbacks(it) }
                try { speechRecognizer?.cancel() } catch (_: Exception) {}
                audioManager.stop()
                binding.tvStatus.text = "SYSTEM READY"
            }
        }

        // Long press = force reconnect
        binding.micButton.setOnLongClickListener {
            isLiveConnected = false
            if (::geminiClient.isInitialized) geminiClient.disconnect()
            addMessage("🔄 Reconnecting...", false)
            isActive = true
            setupGeminiLive()
            true
        }
    }

    fun addMessage(text: String, isUser: Boolean) {
        val time = timeFormat.format(Date())
        chatAdapter.addMessage(ChatMessage(text, isUser, time))
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun checkPermissions() {
        val missing = PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        else onPermissionsReady()
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        onPermissionsReady()
    }

    private fun onPermissionsReady() {
        binding.tvStatus.text = "SYSTEM READY"
        addMessage("আমি MAXI, your AI companion.", false)
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        if ((prefs.getString(Constants.KEY_API_KEY, "") ?: "").isBlank())
            addMessage("⚠️ Settings → API Key দিন, তারপর Mic চাপুন।", false)
        else
            addMessage("✅ নিচের Mic বাটন চাপুন কথা বলতে।", false)
    }

    override fun onResume() {
        super.onResume()
        // Settings থেকে ফিরলে যদি API key নতুন দেওয়া হয়
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(Constants.KEY_API_KEY, "") ?: ""
        if (apiKey.isNotBlank() && isActive && (!::geminiClient.isInitialized || !isLiveConnected)) {
            setupGeminiLive()
        }
    }

    private fun updateRam() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
        binding.tvRam.text = "${(mi.totalMem - mi.availMem) / (1024 * 1024)}MB"
    }

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
        restartRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacks(clockRunnable)
        try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Exception) {}
        if (::geminiClient.isInitialized) geminiClient.disconnect()
        audioManager.release()
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
    }
}
