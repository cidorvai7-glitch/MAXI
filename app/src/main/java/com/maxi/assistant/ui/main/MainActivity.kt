package com.maxi.assistant.ui.main

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maxi.assistant.R
import com.maxi.assistant.databinding.ActivityMainBinding
import com.maxi.assistant.ui.settings.SettingsActivity
import com.maxi.assistant.utils.Constants
import com.maxi.assistant.websocket.GeminiWebSocketClient
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(val text: String, val isUser: Boolean, val time: String)

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
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
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var geminiClient: GeminiWebSocketClient? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordThread: Thread? = null
    @Volatile private var isListening = false
    @Volatile private var isSpeaking = false

    private val SAMPLE_RATE_IN = 16000
    private val SAMPLE_RATE_OUT = 24000
    private val CHUNK_SIZE = 1024

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS
    )
    private val PERMISSION_REQUEST_CODE = 101

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = (level * 100 / scale.toFloat()).toInt()
            binding.tvBattery.text = "$pct%"
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

        setupChat()
        setupButtons()
        startClock()
        registerBatteryReceiver()
        checkAndRequestPermissions()
        initAudioTrack()
    }

    private fun initAudioTrack() {
        val bufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_OUT,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE_OUT,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize * 4,
            AudioTrack.MODE_STREAM
        )
        audioTrack?.play()
    }

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
            val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val apiKey = prefs.getString(Constants.KEY_API_KEY, "") ?: ""

            if (apiKey.isBlank()) {
                addMessage("⚠️ API Key নেই। Settings → Gemini API Key দিন।", false)
                return@setOnClickListener
            }

            if (isListening) {
                stopListening()
            } else {
                startConversation(apiKey, prefs)
            }
        }

        binding.micButton.setOnLongClickListener {
            stopListening()
            geminiClient?.disconnect()
            geminiClient = null
            addMessage("🔄 Reconnecting...", false)
            binding.tvStatus.text = "RECONNECTING..."
            val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val apiKey = prefs.getString(Constants.KEY_API_KEY, "") ?: ""
            if (apiKey.isNotBlank()) {
                handler.postDelayed({ startConversation(apiKey, prefs) }, 1000)
            }
            true
        }
    }

    private fun startConversation(apiKey: String, prefs: android.content.SharedPreferences) {
        val personality = prefs.getString(Constants.KEY_PERSONALITY + "_prompt", "") ?: ""
        val userName = prefs.getString(Constants.KEY_USER_NAME, "") ?: ""

        val systemPrompt = if (personality.isNotBlank()) personality else
            "You are MAXI, a smart AI companion. Reply in Bangla or English based on user's language. Be helpful and friendly."

        binding.tvStatus.text = "CONNECTING..."
        addMessage("🔗 Connecting to MAXI...", false)

        geminiClient = GeminiWebSocketClient(
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            onConnected = {
                handler.post {
                    binding.tvStatus.text = "LISTENING..."
                    val greet = if (userName.isNotBlank()) "Hello $userName! আমি MAXI। বলুন।" else "আমি MAXI। বলুন।"
                    addMessage(greet, false)
                    startMic()
                }
            },
            onAudioReceived = { pcm ->
                isSpeaking = true
                audioTrack?.write(pcm, 0, pcm.size)
                handler.postDelayed({ isSpeaking = false }, 500)
            },
            onTextReceived = { text ->
                handler.post {
                    if (text.isNotBlank()) addMessage(text, false)
                }
            },
            onTurnComplete = {
                handler.post { binding.tvStatus.text = "LISTENING..." }
            },
            onError = { err ->
                handler.post {
                    addMessage("❌ Error: $err", false)
                    binding.tvStatus.text = "ERROR"
                    stopListening()
                }
            }
        )
        geminiClient?.connect()
    }

    private fun startMic() {
        if (isListening) return
        val bufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_IN,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE_IN,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize * 2
        )
        audioRecord?.startRecording()
        isListening = true

        recordThread = Thread {
            val buffer = ByteArray(CHUNK_SIZE)
            while (isListening && !Thread.interrupted()) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0 && !isSpeaking) {
                    geminiClient?.sendAudioChunk(buffer.copyOf(read))
                }
            }
        }.also {
            it.name = "MAXI_Mic"
            it.isDaemon = true
            it.start()
        }
    }

    private fun stopListening() {
        isListening = false
        recordThread?.interrupt()
        recordThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {}
        handler.post { binding.tvStatus.text = "SYSTEM READY" }
    }

    private fun checkAndRequestPermissions() {
        val missing = PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            onPermissionsReady()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val denied = results.any { it != PackageManager.PERMISSION_GRANTED }
            if (!denied) onPermissionsReady()
            else addMessage("⚠️ কিছু permission দেওয়া হয়নি। MAXI পুরোপুরি কাজ নাও করতে পারে।", false)
        }
    }

    private fun onPermissionsReady() {
        binding.tvStatus.text = "SYSTEM READY"
        addMessage("আমি MAXI, your AI companion.", false)
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(Constants.KEY_API_KEY, "") ?: ""
        if (apiKey.isBlank()) {
            addMessage("⚠️ Settings → API Key দিন, তারপর Mic চাপুন।", false)
        } else {
            addMessage("✅ Mic বাটন চাপুন কথা বলতে।", false)
        }
    }

    fun addMessage(text: String, isUser: Boolean) {
        val time = timeFormat.format(Date())
        messages.add(ChatMessage(text, isUser, time))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun startClock() { handler.post(clockRunnable) }

    private fun updateRam() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val usedMb = (mi.totalMem - mi.availMem) / (1024 * 1024)
        binding.tvRam.text = "${usedMb}MB"
    }

    private fun registerBatteryReceiver() {
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onResume() {
        super.onResume()
        // Re-check if API key was just set
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(Constants.KEY_API_KEY, "") ?: ""
        if (apiKey.isNotBlank() && geminiClient == null) {
            binding.tvStatus.text = "SYSTEM READY — Mic চাপুন"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        geminiClient?.disconnect()
        audioTrack?.stop()
        audioTrack?.release()
        handler.removeCallbacks(clockRunnable)
        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
    }
}
