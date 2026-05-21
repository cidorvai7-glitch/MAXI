package com.maxi.assistant.automation

import android.content.Context
import android.content.Intent
import android.net.Uri

class CallAutomation(private val context: Context) {

    fun makeCall(number: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
