package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ScannedRecord
import com.example.data.model.QrPayload
import com.example.data.repository.DispatchRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SyncStatusResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DispatchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = SettingsRepository(application)
    private val repository = DispatchRepository(db.scannedRecordDao, settingsRepo)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val qrAdapter = moshi.adapter(QrPayload::class.java)

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            forceSyncPending()
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    // Scanner state
    private val _scannedPayload = MutableStateFlow<QrPayload?>(null)
    val scannedPayload: StateFlow<QrPayload?> = _scannedPayload.asStateFlow()

    private val _rawPayload = MutableStateFlow<String?>(null)
    val rawPayload: StateFlow<String?> = _rawPayload.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sync progress
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Config settings
    private var _appsScriptUrl = MutableStateFlow(settingsRepo.getAppsScriptUrl())
    val appsScriptUrl: StateFlow<String> = _appsScriptUrl.asStateFlow()

    private var _scannerName = MutableStateFlow(settingsRepo.getScannerName())
    val scannerName: StateFlow<String> = _scannerName.asStateFlow()
    
    private var _authorizedCopies = MutableStateFlow(settingsRepo.getAuthorizedCopies())
    val authorizedCopies: StateFlow<String> = _authorizedCopies.asStateFlow()

    val deviceId = settingsRepo.getDeviceId()

    // Cooldown support (Accidental duplicate scanner scans)
    private var lastScannedTimestamp = 0L
    private val COOLDOWN_MS = 1000L

    // History state filtered by search
    val scannedHistory: StateFlow<List<ScannedRecord>> = repository.allRecords
        .combine(_searchQuery) { records, query ->
            if (query.isBlank()) {
                records
            } else {
                records.filter {
                    it.orderNo.contains(query, ignoreCase = true) ||
                    it.customer.contains(query, ignoreCase = true) ||
                    it.district.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun updateSettings(url: String, name: String, authorizedCopies: String) {
        settingsRepo.saveAppsScriptUrl(url)
        settingsRepo.saveScannerName(name)
        settingsRepo.saveAuthorizedCopies(authorizedCopies)
        _appsScriptUrl.value = url
        _scannerName.value = name
        _authorizedCopies.value = authorizedCopies
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Try to parse scanned QR content
    fun onQrScanned(rawContent: String, context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastScannedTimestamp < COOLDOWN_MS) {
            // Under cooldown interval, silently skip
            return
        }
        lastScannedTimestamp = now
        
        try {
            // Trim outer whitespaces and parse
            val trimmed = rawContent.trim()
            val payload = qrAdapter.fromJson(trimmed)
            if (payload == null || payload.orderNo.isNullOrEmpty()) {
                _scanError.value = "Invalid QR code."
                triggerErrorFeedback(context)
                clearErrorAfterDelay()
            } else {
                val duplicateKey = (payload.orderNo ?: "") + "-" + (payload.copy ?: "")
                val localDup = scannedHistory.value.find { it.duplicateKey == duplicateKey }
                
                // Authorization Check
                val requiredCopies = _authorizedCopies.value
                val itemCopy = payload.copy ?: ""
                
                if (requiredCopies.isNotBlank()) {
                    val allowedCopiesList = requiredCopies.split(",").map { it.trim().lowercase() }
                    if (!allowedCopiesList.contains(itemCopy.lowercase())) {
                        _scanError.value = "Unauthorized: Not allowed to scan $itemCopy"
                        triggerErrorFeedback(context)
                        clearErrorAfterDelay()
                        return
                    }
                }
                
                if (localDup != null) {
                    _scanError.value = "Item already scanned."
                    triggerDuplicateFeedback(context)
                    clearErrorAfterDelay()
                    return
                }

                _scanError.value = null
                _scannedPayload.value = payload
                _rawPayload.value = trimmed
                triggerFeedback(context)
                // Stop auto-submit and wait for user confirmation
            }
        } catch (e: Exception) {
            _scanError.value = "Invalid QR code."
            triggerErrorFeedback(context)
            clearErrorAfterDelay()
        }
    }
    
    private fun clearErrorAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            _scanError.value = null
        }
    }

    fun clearScannedData() {
        _scannedPayload.value = null
        _rawPayload.value = null
    }

    // Save scan outcome to Room DB and sync
    fun recordDispatch(context: Context, onSuccess: () -> Unit, onDuplicate: (String) -> Unit) {
        val payload = _scannedPayload.value ?: return
        val raw = _rawPayload.value ?: ""

        val duplicateKey = (payload.orderNo ?: "") + "-" + (payload.copy ?: "")

        val newRecord = ScannedRecord(
            scannerName = _scannerName.value,
            deviceId = deviceId,
            orderNo = payload.orderNo ?: "",
            orderId = payload.orderId ?: "",
            customer = payload.customer ?: "",
            district = payload.district ?: "",
            deliveryDate = payload.deliveryDate ?: "",
            deliveryType = payload.deliveryType ?: "",
            salesPerson = payload.salesPerson ?: "",
            totalUnits = payload.totalUnits ?: 1,
            copy = payload.copy ?: "",
            tableIndex = payload.tableIndex ?: 1,
            tableCount = payload.tableCount ?: payload.totalUnits ?: 1,
            size = payload.size ?: "",
            topColour = payload.topColour ?: "",
            frameColour = payload.frameColour ?: "",
            qty = payload.qty ?: 1,
            legSize = payload.legSize ?: "",
            legShape = payload.legShape ?: "",
            legHeight = payload.legHeight ?: "",
            qrTimestamp = payload.ts ?: "",
            duplicateKey = duplicateKey,
            rawPayload = raw,
            syncStatus = "PENDING"
        )
        
        triggerFeedback(context)
        clearScannedData()

        viewModelScope.launch {
            // Insert local first to secure offline support
            val id = repository.insertRecord(newRecord)
            val savedRecord = newRecord.copy(id = id)

            // Async background sync without blocking UI
            val syncResult = repository.syncRecord(savedRecord)

            when (syncResult) {
                is SyncStatusResult.Success -> {
                    onSuccess()
                }
                is SyncStatusResult.Duplicate -> {
                    onDuplicate(syncResult.message)
                }
                is SyncStatusResult.Failed -> {
                    onSuccess() // Still saved locally as FAILED, we treat it calmly as added
                }
                is SyncStatusResult.Offline -> {
                    onSuccess() // Offline pending cache saved successfully
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
    }

    fun triggerFeedback(context: Context) {
        // Loud success confirm tone
        try {
            val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            tg.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
            viewModelScope.launch {
                kotlinx.coroutines.delay(350)
                tg.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Tactile short vibration feedback
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerDuplicateFeedback(context: Context) {
        try {
            val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            tg.startTone(android.media.ToneGenerator.TONE_CDMA_NETWORK_BUSY, 800)
            viewModelScope.launch {
                kotlinx.coroutines.delay(850)
                tg.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 150)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerErrorFeedback(context: Context) {
        // Loud error tone
        try {
            val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            tg.startTone(android.media.ToneGenerator.TONE_SUP_ERROR, 1000)
            viewModelScope.launch {
                kotlinx.coroutines.delay(1050)
                tg.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(800)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun forceSyncPending() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Starting batch sync..."
            val summary = repository.syncPendingRecords()
            _isSyncing.value = false
            _syncMessage.value = summary.message
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
