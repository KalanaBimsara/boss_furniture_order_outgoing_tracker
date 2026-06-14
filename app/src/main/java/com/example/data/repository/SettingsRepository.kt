package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("factory_dispatch_prefs", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    fun getAppsScriptUrl(): String {
        return prefs.getString("apps_script_url", "") ?: ""
    }

    fun saveAppsScriptUrl(url: String) {
        prefs.edit().putString("apps_script_url", url.trim()).apply()
    }

    fun getScannerName(): String {
        return prefs.getString("scanner_name", "Packing Station 01") ?: "Packing Station 01"
    }

    fun saveScannerName(name: String) {
        prefs.edit().putString("scanner_name", name.trim()).apply()
    }

    fun getAuthorizedCopies(): String {
        return prefs.getString("authorized_copies", "") ?: ""
    }

    fun saveAuthorizedCopies(copies: String) {
        prefs.edit().putString("authorized_copies", copies.trim()).apply()
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ANDROID_DEVICE"
    }
}
