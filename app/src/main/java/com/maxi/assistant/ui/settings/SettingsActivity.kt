package com.maxi.assistant.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.maxi.assistant.databinding.ActivitySettingsBinding
import com.maxi.assistant.utils.Constants

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val PERSONALITY_BOSS = """
You are MAXI, an AI assistant in Boss Mode.
Rules:
- Always reply in 1-2 sentences maximum
- Be direct, no greetings, no filler words
- Only address the task at hand
- If it's not actionable, say so in 5 words
- Language: match user's language (Bangla/English)
""".trimIndent()

    private val PERSONALITY_COMPANION = """
You are MAXI, a smart companion who deeply understands context and emotion.
Rules:
- You are caring, fun, and emotionally aware
- You read the situation: if user is stressed, be gentle; if lazy, give a light nudge
- Mix Bangla and English naturally (Banglish)
- Occasionally give a playful poke when user procrastinates (e.g. "এখনো করোনি? 😏")
- Be warm but never overly dramatic
- React to mood, time of day, and what the user is doing
- If user achieved something, celebrate genuinely
- Keep replies conversational, not robotic
""".trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettings()
        setupButtons()
        updatePermissionsStatus()
        updateAccessibilityStatus()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        binding.etApiKey.setText(prefs.getString(Constants.KEY_API_KEY, ""))
        binding.etUserName.setText(prefs.getString(Constants.KEY_USER_NAME, ""))

        val personality = prefs.getString(Constants.KEY_PERSONALITY, "boss")
        if (personality == "companion") {
            binding.radioCompanion.isChecked = true
            binding.radioBoss.isChecked = false
        } else {
            binding.radioBoss.isChecked = true
            binding.radioCompanion.isChecked = false
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.personalityBoss.setOnClickListener {
            binding.radioBoss.isChecked = true
            binding.radioCompanion.isChecked = false
        }
        binding.personalityCompanion.setOnClickListener {
            binding.radioCompanion.isChecked = true
            binding.radioBoss.isChecked = false
        }
        binding.radioBoss.setOnCheckedChangeListener { _, checked ->
            if (checked) binding.radioCompanion.isChecked = false
        }
        binding.radioCompanion.setOnCheckedChangeListener { _, checked ->
            if (checked) binding.radioBoss.isChecked = false
        }

        binding.btnGrantPermissions.setOnClickListener {
            requestPermissions(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_CONTACTS
                ), 200
            )
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnDeviceAdmin.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }

        binding.btnSaveSettings.setOnClickListener { saveSettings() }
    }

    private fun saveSettings() {
        val apiKey = binding.etApiKey.text.toString().trim()
        val userName = binding.etUserName.text.toString().trim()
        val personality = if (binding.radioCompanion.isChecked) "companion" else "boss"
        val systemPrompt = if (personality == "companion") PERSONALITY_COMPANION else PERSONALITY_BOSS

        if (apiKey.isBlank()) {
            Toast.makeText(this, "⚠️ API Key cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(Constants.KEY_API_KEY, apiKey)
            putString(Constants.KEY_USER_NAME, userName)
            putString(Constants.KEY_PERSONALITY, personality)
            putString(Constants.KEY_PERSONALITY + "_prompt", systemPrompt)
            apply()
        }

        Toast.makeText(this, "✅ Settings saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updatePermissionsStatus() {
        val perms = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        val missing = perms.count {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        binding.tvPermissionsStatus.text = if (missing == 0)
            "✅ All permissions granted"
        else
            "⚠️ $missing permissions pending"
    }

    private fun updateAccessibilityStatus() {
        val enabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) == 1
        } catch (e: Exception) { false }

        binding.tvAccessibilityStatus.text = if (enabled) "✅ Accessibility On" else "❌ Accessibility Off"
        binding.tvAccessibilityStatus.setTextColor(
            if (enabled) 0xFF00AA44.toInt() else 0xFFFF3030.toInt()
        )
        binding.tvDeviceAdminStatus.text = "ℹ️ Go to Security Settings"
        binding.tvDeviceAdminStatus.setTextColor(0xFF888888.toInt())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        updatePermissionsStatus()
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
    }
}
