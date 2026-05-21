package com.maxi.assistant.automation

import android.content.Context

class AutomationManager(private val context: Context) {

    fun execute(action: String, params: Map<String, String> = emptyMap()): Boolean {
        return false
    }
}
