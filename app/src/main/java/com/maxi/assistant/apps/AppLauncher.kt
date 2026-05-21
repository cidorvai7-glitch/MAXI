package com.maxi.assistant.apps

import android.content.Context
import android.content.Intent

class AppLauncher(private val context: Context) {

    fun openApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
