package com.maxi.assistant.ui.main

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
            val apiKey = prefs.getString(Constants.KEY_API_KEY, "")
            if (apiKey.isNullOrBlank()) {
                addMessage("⚠️ API Key required. Please go to Settings → Enter Gemini API Key.", false)
                binding.tvStatus.text = "API KEY MISSING"
            } else {
                addMessage("🎤 Listening...", false)
                binding.tvStatus.text = "LISTENING..."
            }
        }

        binding.micButton.setOnLongClickListener {
            addMessage("Reconnecting to MAXI...", false)
            binding.tvStatus.text = "RECONNECTING..."
            true
        }
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
            else {
                addMessage("⚠️ Some permissions were denied. MAXI may not work fully.", false)
            }
        }
    }

    private fun onPermissionsReady() {
        binding.tvStatus.text = "SYSTEM READY"
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(Constants.KEY_API_KEY, "")
        val userName = prefs.getString(Constants.KEY_USER_NAME, "") ?: ""

        if (apiKey.isNullOrBlank()) {
            addMessage("⚠️ API Key required. Please go to Settings → Enter Gemini API Key.", false)
        }
        if (!isAccessibilityEnabled()) {
            addMessage("⚠️ Enable Accessibility Service for app control. Settings → Accessibility.", false)
        }
        if (apiKey.isNullOrBlank() && isAccessibilityEnabled()) {
            val greeting = if (userName.isNotBlank()) "Hello $userName! I'm MAXI." else "I'm MAXI, your AI companion."
            addMessage(greeting, false)
        }
    }

    fun addMessage(text: String, isUser: Boolean) {
        val time = timeFormat.format(Date())
        messages.add(ChatMessage(text, isUser, time))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            val enabled = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            enabled == 1
        } catch (e: Exception) { false }
    }

    private fun startClock() {
        handler.post(clockRunnable)
    }

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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
    }
}
