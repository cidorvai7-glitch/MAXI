package com.maxi.assistant.ai

import android.content.Context
import com.maxi.assistant.automation.AutomationManager
import com.maxi.assistant.automation.CallAutomation
import com.maxi.assistant.apps.AppLauncher
import com.maxi.assistant.models.CommandType
import com.maxi.assistant.models.VoiceCommand
import com.maxi.assistant.system.SystemController

class DynamicDecisionEngine(private val context: Context) {

    private val systemController = SystemController(context)
    private val appLauncher = AppLauncher(context)
    private val callAutomation = CallAutomation(context)
    private val automationManager = AutomationManager(context)

    fun execute(command: VoiceCommand): Boolean {
        val args = command.parameters
        val raw = command.rawText
        return when (command.commandType) {
            CommandType.OPEN_APP -> appLauncher.openApp(args["app"] ?: "")
            CommandType.CALL -> callAutomation.makeCall(args["number"] ?: "")
            CommandType.WHATSAPP_CALL -> callAutomation.makeCall(args["number"] ?: "")
            CommandType.FLASHLIGHT_ON -> systemController.execute("FLASHLIGHT_ON")
            CommandType.FLASHLIGHT_OFF -> systemController.execute("FLASHLIGHT_OFF")
            CommandType.VOLUME_UP -> systemController.execute("VOLUME_UP")
            CommandType.VOLUME_DOWN -> systemController.execute("VOLUME_DOWN")
            CommandType.WIFI_ON -> systemController.execute("WIFI_ON")
            CommandType.WIFI_OFF -> systemController.execute("WIFI_OFF")
            CommandType.BLUETOOTH_ON -> systemController.execute("BLUETOOTH_ON")
            CommandType.BLUETOOTH_OFF -> systemController.execute("BLUETOOTH_OFF")
            CommandType.REBOOT -> systemController.execute("REBOOT")
            CommandType.OPEN_WEBSITE -> openWebsite(args["url"] ?: "")
            else -> false
        }
    }

    private fun openWebsite(url: String): Boolean = false
}
