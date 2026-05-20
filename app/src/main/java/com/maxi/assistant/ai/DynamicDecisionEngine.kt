package com.maxi.assistant.ai

import android.content.Context
import android.util.Log
import com.maxi.assistant.automation.AutomationManager
import com.maxi.assistant.automation.CallAutomation
import com.maxi.assistant.models.CommandType
import com.maxi.assistant.models.VoiceCommand
import com.maxi.assistant.system.SystemController

object DynamicDecisionEngine {
    private const val TAG = "MAXI_DECISION"

    fun execute(context: Context, cmd: VoiceCommand): Boolean {
        Log.d(TAG, "Executing: ${cmd.type} | args: ${cmd.args}")
        val sysCtrl = SystemController(context)

        return when (cmd.type) {
            CommandType.OPEN_APP -> {
                val app = cmd.args["app"] ?: return false
                val accessService = com.maxi.assistant.service.AccessibilityHelperService.instance
                if (accessService != null) {
                    AutomationManager.executeTask("OPEN_APP $app")
                } else {
                    sysCtrl.openApp(app)
                }
                true
            }
            CommandType.CALL -> {
                val name = cmd.args["name"] ?: return false
                CallAutomation.makePhoneCall(context, name)
                true
            }
            CommandType.WHATSAPP_CALL -> {
                val name = cmd.args["name"] ?: return false
                CallAutomation.makeWhatsAppCall(context, name)
                true
            }
            CommandType.WHATSAPP_MSG -> {
                val name = cmd.args["name"] ?: return false
                val msg = cmd.args["message"] ?: ""
                CallAutomation.sendWhatsAppMessage(context, name, msg)
                true
            }
            CommandType.SMS -> {
                val name = cmd.args["name"] ?: return false
                val msg = cmd.args["message"] ?: ""
                CallAutomation.sendSMS(context, name, msg)
                true
            }
            CommandType.YOUTUBE_PLAY -> {
                val query = cmd.args["query"] ?: return false
                sysCtrl.openWebsite("https://www.youtube.com/results?search_query=${query.replace(" ", "+")}")
                true
            }
            CommandType.SPOTIFY_PLAY -> {
                val query = cmd.args["query"] ?: return false
                sysCtrl.openApp("spotify")
                true
            }
            CommandType.FLASHLIGHT_ON -> { sysCtrl.setFlashlight(true); true }
            CommandType.FLASHLIGHT_OFF -> { sysCtrl.setFlashlight(false); true }
            CommandType.VOLUME_UP -> { sysCtrl.volumeUp(); true }
            CommandType.VOLUME_DOWN -> { sysCtrl.volumeDown(); true }
            CommandType.UNKNOWN -> handleSpecial(context, cmd, sysCtrl)
            CommandType.CONVERSATION -> false
            else -> false
        }
    }

    private fun handleSpecial(context: Context, cmd: VoiceCommand, sysCtrl: SystemController): Boolean {
        val action = cmd.args["action"] ?: cmd.raw.split(" ").firstOrNull() ?: return false
        val value = cmd.args["value"] ?: ""

        return when (action.uppercase()) {
            "WIFI_ON" -> { sysCtrl.setWifi(true); true }
            "WIFI_OFF" -> { sysCtrl.setWifi(false); true }
            "BLUETOOTH_ON" -> { sysCtrl.setBluetooth(true); true }
            "BLUETOOTH_OFF" -> { sysCtrl.setBluetooth(false); true }
            "REBOOT" -> { sysCtrl.reboot(context); true }
            "POWER_OFF" -> { sysCtrl.powerOff(context); true }
            "SCREENSHOT" -> { sysCtrl.takeScreenshot(); true }
            "OPEN_WEBSITE" -> {
                val url = cmd.args["url"] ?: value
                if (url.isNotBlank()) { sysCtrl.openWebsite(url); true } else false
            }
            "PHONE_STATUS" -> {
                val status = sysCtrl.getPhoneStatus()
                Log.d(TAG, "Status: $status")
                true
            }
            "SILENT_MODE_ON" -> { sysCtrl.setSilentMode(true); true }
            "SILENT_MODE_OFF" -> { sysCtrl.setSilentMode(false); true }
            "SCREEN_BRIGHTNESS" -> {
                val level = value.toIntOrNull() ?: 50
                sysCtrl.setBrightness(level)
                true
            }
            "AIRPLANE_MODE_ON", "AIRPLANE_MODE_OFF" -> {
                sysCtrl.openAirplaneSettings()
                true
            }
            else -> {
                val service = com.maxi.assistant.service.AccessibilityHelperService.instance
                if (service != null) {
                    AutomationManager.executeTask(cmd.raw)
                } else false
            }
        }
    }
}
