package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.example.BuildConfig

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("factory_dispatch_prefs", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    companion object {
        const val DEFAULT_SUPABASE_URL = "https://baxnczbtheknzpoerpfn.supabase.co"
        const val DEFAULT_SUPABASE_KEY = "sb_publishable_4H_QqrQ4jWxfZ39KVoaWaQ_NX8U8gIs"
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

    fun getSupabaseUrl(): String {
        val saved = prefs.getString("supabase_url", "") ?: ""
        return if (saved.isNotBlank() && !saved.contains("your-project.supabase.co") && !saved.contains("placeholder.supabase.co")) {
            saved
        } else {
            val buildCfg = BuildConfig.SUPABASE_URL
            if (buildCfg.isNotBlank() && !buildCfg.contains("your-project.supabase.co")) buildCfg else DEFAULT_SUPABASE_URL
        }
    }

    fun saveSupabaseUrl(url: String) {
        prefs.edit().putString("supabase_url", url.trim().ifBlank { DEFAULT_SUPABASE_URL }).apply()
    }

    fun getSupabaseKey(): String {
        val saved = prefs.getString("supabase_key", "") ?: ""
        return if (saved.isNotBlank() && !saved.contains("your-supabase")) {
            saved
        } else {
            val buildCfg = BuildConfig.SUPABASE_KEY
            if (buildCfg.isNotBlank() && !buildCfg.contains("your-supabase")) buildCfg else DEFAULT_SUPABASE_KEY
        }
    }

    fun saveSupabaseKey(key: String) {
        prefs.edit().putString("supabase_key", key.trim().ifBlank { DEFAULT_SUPABASE_KEY }).apply()
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ANDROID_DEVICE"
    }

    fun getSelectedVehicleOption(): String {
        return prefs.getString("selected_vehicle_option", "DAC-4912 (Rathnayake)") ?: "DAC-4912 (Rathnayake)"
    }

    fun saveSelectedVehicleOption(option: String) {
        prefs.edit().putString("selected_vehicle_option", option.trim()).apply()
    }

    fun getPickMeVehicleNumber(): String {
        return prefs.getString("pick_me_vehicle_number", "") ?: ""
    }

    fun savePickMeVehicleNumber(number: String) {
        prefs.edit().putString("pick_me_vehicle_number", number.trim()).apply()
    }

    fun getCustomVehicleNumber(): String {
        return prefs.getString("custom_vehicle_number", "") ?: ""
    }

    fun saveCustomVehicleNumber(number: String) {
        prefs.edit().putString("custom_vehicle_number", number.trim()).apply()
    }

    fun getSavedCustomVehicles(): List<String> {
        val raw = prefs.getString("saved_custom_vehicles", "") ?: ""
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun addSavedCustomVehicle(vehicle: String) {
        val trimmed = vehicle.trim()
        if (trimmed.isEmpty()) return
        val current = getSavedCustomVehicles().toMutableList()
        if (!current.any { it.equals(trimmed, ignoreCase = true) }) {
            current.add(trimmed)
            prefs.edit().putString("saved_custom_vehicles", current.joinToString("|")).apply()
        }
    }
}
