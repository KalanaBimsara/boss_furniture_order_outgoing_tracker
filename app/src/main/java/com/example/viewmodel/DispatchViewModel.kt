package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClient
import com.example.data.api.SupabaseApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.ScannedRecord
import com.example.data.model.PostBody
import com.example.data.model.QrPayload
import com.example.data.repository.DispatchRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SyncStatusResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OperationMode {
    DISPATCH,
    RETURN
}

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
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val VEHICLE_DAC_4912 = "DAC-4912 (Rathnayake)"
        const val VEHICLE_DAE_4217 = "DAE-4217 (Lasantha)"
        const val VEHICLE_PICK_ME = "Pick Me"
        const val VEHICLE_CUSTOM_OPTION = "Custom Vehicle"

        const val DRIVER_UUID_RATHNAYAKE = DispatchRepository.DRIVER_UUID_RATHNAYAKE
        const val DRIVER_UUID_LASANTHA = DispatchRepository.DRIVER_UUID_LASANTHA

        val DEFAULT_VEHICLES = listOf(
            VEHICLE_DAC_4912,
            VEHICLE_DAE_4217,
            VEHICLE_PICK_ME
        )
    }

    // Active Operational Mode (DISPATCH vs RETURN)
    private val _currentMode = MutableStateFlow(OperationMode.DISPATCH)
    val currentMode: StateFlow<OperationMode> = _currentMode.asStateFlow()

    // Vehicle Selection State (shared across both Dispatch & Return modes)
    private val _selectedVehicleOption = MutableStateFlow(settingsRepo.getSelectedVehicleOption())
    val selectedVehicleOption: StateFlow<String> = _selectedVehicleOption.asStateFlow()

    private val _pickMeVehicleNumber = MutableStateFlow(settingsRepo.getPickMeVehicleNumber())
    val pickMeVehicleNumber: StateFlow<String> = _pickMeVehicleNumber.asStateFlow()

    private val _customVehicleNumber = MutableStateFlow(settingsRepo.getCustomVehicleNumber())
    val customVehicleNumber: StateFlow<String> = _customVehicleNumber.asStateFlow()

    private val _savedCustomVehicles = MutableStateFlow(settingsRepo.getSavedCustomVehicles())
    val savedCustomVehicles: StateFlow<List<String>> = _savedCustomVehicles.asStateFlow()

    // Scanner state
    private val _scannedPayload = MutableStateFlow<QrPayload?>(null)
    val scannedPayload: StateFlow<QrPayload?> = _scannedPayload.asStateFlow()

    private val _rawPayload = MutableStateFlow<String?>(null)
    val rawPayload: StateFlow<String?> = _rawPayload.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // Manual Dispatch Input State
    private val _manualDispatchOrderInput = MutableStateFlow("")
    val manualDispatchOrderInput: StateFlow<String> = _manualDispatchOrderInput.asStateFlow()

    private val _manualDispatchCopyType = MutableStateFlow(DispatchRepository.COPY_GATE_PASS)
    val manualDispatchCopyType: StateFlow<String> = _manualDispatchCopyType.asStateFlow()

    private val _manualDispatchStatus = MutableStateFlow<String?>(null)
    val manualDispatchStatus: StateFlow<String?> = _manualDispatchStatus.asStateFlow()

    private val _isProcessingDispatch = MutableStateFlow(false)
    val isProcessingDispatch: StateFlow<Boolean> = _isProcessingDispatch.asStateFlow()

    // Manual Return Input State
    private val _manualOrderInput = MutableStateFlow("")
    val manualOrderInput: StateFlow<String> = _manualOrderInput.asStateFlow()

    private val _manualRestocked = MutableStateFlow(true)
    val manualRestocked: StateFlow<Boolean> = _manualRestocked.asStateFlow()

    private val _manualReturnStatus = MutableStateFlow<String?>(null)
    val manualReturnStatus: StateFlow<String?> = _manualReturnStatus.asStateFlow()

    private val _isProcessingReturn = MutableStateFlow(false)
    val isProcessingReturn: StateFlow<Boolean> = _isProcessingReturn.asStateFlow()

    // History filter by mode
    private val _historyModeFilter = MutableStateFlow<String>("ALL") // "ALL", "DISPATCH", "RETURN"
    val historyModeFilter: StateFlow<String> = _historyModeFilter.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sync progress
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Config settings (preconfigured to Supabase Factory Database)
    private var _scannerName = MutableStateFlow(settingsRepo.getScannerName())
    val scannerName: StateFlow<String> = _scannerName.asStateFlow()
    
    private var _authorizedCopies = MutableStateFlow(settingsRepo.getAuthorizedCopies())
    val authorizedCopies: StateFlow<String> = _authorizedCopies.asStateFlow()

    private var _supabaseUrl = MutableStateFlow(settingsRepo.getSupabaseUrl())
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private var _supabaseKey = MutableStateFlow(settingsRepo.getSupabaseKey())
    val supabaseKey: StateFlow<String> = _supabaseKey.asStateFlow()

    private val _testConnectionStatus = MutableStateFlow<String?>(null)
    val testConnectionStatus: StateFlow<String?> = _testConnectionStatus.asStateFlow()

    val deviceId = settingsRepo.getDeviceId()

    // Cooldown support
    private var lastScannedTimestamp = 0L
    private val COOLDOWN_MS = 1000L

    // Unfiltered local records for strict duplicate prevention
    val allRecordsState: StateFlow<List<ScannedRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Filtered Scanned History
    val scannedHistory: StateFlow<List<ScannedRecord>> = repository.allRecords
        .combine(_searchQuery) { records, query -> records to query }
        .combine(_scannerName) { (records, query), scanner -> records to query to scanner }
        .combine(_historyModeFilter) { pair, modeFilter ->
            val records = pair.first.first
            val query = pair.first.second
            val scanner = pair.second

            val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = todayFormat.format(Date())
            
            val filteredByToday = records.filter {
                todayFormat.format(Date(it.scannedAt)) == todayStr
            }

            val modeFiltered = if (modeFilter == "ALL") {
                filteredByToday
            } else {
                filteredByToday.filter { it.mode.equals(modeFilter, ignoreCase = true) }
            }

            if (query.isBlank()) {
                modeFiltered
            } else {
                modeFiltered.filter {
                    it.orderNo.contains(query, ignoreCase = true) ||
                    it.customer.contains(query, ignoreCase = true) ||
                    it.district.contains(query, ignoreCase = true) ||
                    it.vehicleNumber.contains(query, ignoreCase = true) ||
                    it.copy.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setMode(mode: OperationMode) {
        _currentMode.value = mode
        clearScannedData()
        _scanError.value = null
    }

    fun setSelectedVehicleOption(option: String) {
        _selectedVehicleOption.value = option
        settingsRepo.saveSelectedVehicleOption(option)
    }

    fun setPickMeVehicleNumber(number: String) {
        _pickMeVehicleNumber.value = number
        settingsRepo.savePickMeVehicleNumber(number)
    }

    fun setCustomVehicleNumber(number: String) {
        _customVehicleNumber.value = number
        settingsRepo.saveCustomVehicleNumber(number)
    }

    fun addAndSelectCustomVehicle(customNumber: String) {
        val trimmed = customNumber.trim()
        if (trimmed.isBlank()) return
        settingsRepo.addSavedCustomVehicle(trimmed)
        _savedCustomVehicles.value = settingsRepo.getSavedCustomVehicles()
        _selectedVehicleOption.value = trimmed
        settingsRepo.saveSelectedVehicleOption(trimmed)
        _customVehicleNumber.value = ""
        settingsRepo.saveCustomVehicleNumber("")
    }

    fun getAvailableVehicleOptions(): List<String> {
        val customList = _savedCustomVehicles.value.filter { custom ->
            DEFAULT_VEHICLES.none { it.equals(custom, ignoreCase = true) } &&
                !custom.equals(VEHICLE_CUSTOM_OPTION, ignoreCase = true)
        }
        return DEFAULT_VEHICLES + customList + listOf(VEHICLE_CUSTOM_OPTION)
    }

    fun getEffectiveVehicleNumber(): String {
        return when (val option = _selectedVehicleOption.value) {
            VEHICLE_PICK_ME -> {
                val pickMeNum = _pickMeVehicleNumber.value.trim()
                if (pickMeNum.isNotBlank()) "Pick Me ($pickMeNum)" else VEHICLE_PICK_ME
            }
            VEHICLE_CUSTOM_OPTION -> {
                val customNum = _customVehicleNumber.value.trim()
                if (customNum.isNotBlank()) customNum else VEHICLE_CUSTOM_OPTION
            }
            else -> option
        }
    }

    fun getAssignedDriverUuid(): String? {
        return DispatchRepository.resolveKnownDriverUuid(getEffectiveVehicleNumber())
    }

    fun getAssignedDeliverySummary(): String {
        val effective = getEffectiveVehicleNumber()
        return when (val uuid = DispatchRepository.resolveKnownDriverUuid(effective)) {
            DRIVER_UUID_RATHNAYAKE -> "GATE PASS assigns Driver: Rathnayake • UUID: $uuid"
            DRIVER_UUID_LASANTHA -> "GATE PASS assigns Driver: Lasantha • UUID: $uuid"
            else -> "GATE PASS assigns Delivery: $effective"
        }
    }

    fun validateVehicleReady(): String? {
        return when (_selectedVehicleOption.value) {
            VEHICLE_PICK_ME -> {
                if (_pickMeVehicleNumber.value.isBlank()) {
                    "Please enter the Pick Me vehicle number."
                } else null
            }
            VEHICLE_CUSTOM_OPTION -> {
                if (_customVehicleNumber.value.isBlank()) {
                    "Please enter the custom vehicle number."
                } else null
            }
            else -> null
        }
    }

    fun setHistoryModeFilter(filter: String) {
        _historyModeFilter.value = filter
    }

    fun setManualDispatchOrderInput(input: String) {
        _manualDispatchOrderInput.value = input
    }

    fun setManualDispatchCopyType(copyType: String) {
        _manualDispatchCopyType.value = DispatchRepository.normalizeCopyType(copyType)
    }

    fun clearManualDispatchStatus() {
        _manualDispatchStatus.value = null
    }

    fun setManualOrderInput(input: String) {
        _manualOrderInput.value = input
    }

    fun setManualRestocked(restocked: Boolean) {
        _manualRestocked.value = restocked
    }

    fun clearManualReturnStatus() {
        _manualReturnStatus.value = null
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun updateSettings(
        name: String,
        authorizedCopies: String,
        supabaseUrl: String = _supabaseUrl.value,
        supabaseKey: String = _supabaseKey.value
    ) {
        settingsRepo.saveScannerName(name)
        settingsRepo.saveAuthorizedCopies(authorizedCopies)
        settingsRepo.saveSupabaseUrl(supabaseUrl)
        settingsRepo.saveSupabaseKey(supabaseKey)

        _scannerName.value = name
        _authorizedCopies.value = authorizedCopies
        _supabaseUrl.value = settingsRepo.getSupabaseUrl()
        _supabaseKey.value = settingsRepo.getSupabaseKey()
    }

    fun testSupabaseConnection() {
        viewModelScope.launch {
            _testConnectionStatus.value = "Testing connection to Supabase factory database..."
            try {
                val api = SupabaseApiClient.getApiService(_supabaseUrl.value, _supabaseKey.value)
                val response = api.getRecentOrders(limit = 5)
                _testConnectionStatus.value = "Success: Connected to Supabase! (${response.size} recent factory orders verified)"
            } catch (e: Exception) {
                _testConnectionStatus.value = "Connection failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
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
            return
        }
        lastScannedTimestamp = now
        
        val trimmed = rawContent.trim()
        val isReturnMode = _currentMode.value == OperationMode.RETURN

        // Attempt 1: Standard JSON QrPayload
        try {
            val payload = qrAdapter.fromJson(trimmed)
            if (payload != null && !payload.orderNo.isNullOrEmpty()) {
                val normalizedCopy = DispatchRepository.normalizeCopyType(payload.copy)
                handleValidPayload(payload.copy(copy = normalizedCopy), trimmed, isReturnMode, context)
                return
            }
        } catch (ignored: Exception) {
            // Not a JSON payload, try plaintext parsing below
        }

        // Attempt 2: Plaintext format or Order Number format (e.g., "ORD-12345" or "1024")
        if (trimmed.isNotBlank() && trimmed.length in 3..64) {
            val orderNum = if (trimmed.startsWith("ORDER:", ignoreCase = true)) {
                trimmed.substringAfter(":").trim()
            } else {
                trimmed
            }
            val syntheticPayload = QrPayload(
                orderNo = orderNum,
                customer = "Factory Order",
                copy = if (isReturnMode) DispatchRepository.COPY_GATE_PASS else _manualDispatchCopyType.value,
                qty = 1,
                totalUnits = 1
            )
            handleValidPayload(syntheticPayload, trimmed, isReturnMode, context)
            return
        }

        _scanError.value = "Invalid QR code format."
        triggerErrorFeedback(context)
        clearErrorAfterDelay()
    }

    private fun handleValidPayload(
        payload: QrPayload,
        rawContent: String,
        isReturnMode: Boolean,
        context: Context
    ) {
        val normalizedCopy = DispatchRepository.normalizeCopyType(payload.copy)
        val normalizedPayload = payload.copy(copy = normalizedCopy)

        val duplicateKey = listOf(
            normalizedPayload.orderNo, normalizedPayload.tableIndex, normalizedPayload.copy, normalizedPayload.size, normalizedPayload.topColour, 
            normalizedPayload.frameColour, normalizedPayload.legSize, normalizedPayload.legShape, normalizedPayload.legHeight
        ).joinToString("-") { it?.toString() ?: "" }.trim()
        
        // Authorization check (only applies in dispatch mode if configured)
        if (!isReturnMode) {
            val requiredCopies = _authorizedCopies.value
            if (requiredCopies.isNotBlank()) {
                val allowedCopiesList = requiredCopies.split(",").map { it.trim().lowercase() }
                if (!allowedCopiesList.contains(normalizedCopy.lowercase())) {
                    _scanError.value = "Unauthorized: Not allowed to scan $normalizedCopy"
                    triggerErrorFeedback(context)
                    clearErrorAfterDelay()
                    return
                }
            }

            val localDup = scannedHistory.value.find { 
                it.duplicateKey == duplicateKey && it.mode == "DISPATCH" 
            }
            if (localDup != null) {
                _scanError.value = "Item ($normalizedCopy) already scanned."
                triggerDuplicateFeedback(context)
                clearErrorAfterDelay()
                return
            }
        }

        _scanError.value = null
        _scannedPayload.value = normalizedPayload
        _rawPayload.value = rawContent
        triggerFeedback(context)
    }
    
    private fun clearErrorAfterDelay() {
        viewModelScope.launch {
            delay(2500)
            _scanError.value = null
        }
    }

    fun clearScannedData() {
        _scannedPayload.value = null
        _rawPayload.value = null
    }

    // Process Manual Dispatch by typing Order Number
    fun processManualDispatch(context: Context) {
        val orderNo = _manualDispatchOrderInput.value.trim()
        if (orderNo.isBlank()) {
            _manualDispatchStatus.value = "Please enter an order number."
            triggerErrorFeedback(context)
            return
        }

        val copyType = DispatchRepository.normalizeCopyType(_manualDispatchCopyType.value)
        val isGatePass = DispatchRepository.isGatePassCopy(copyType)

        if (isGatePass) {
            val vehicleValidationErr = validateVehicleReady()
            if (vehicleValidationErr != null) {
                _manualDispatchStatus.value = vehicleValidationErr
                triggerErrorFeedback(context)
                return
            }
        }

        val effectiveVehicle = getEffectiveVehicleNumber()
        _isProcessingDispatch.value = true
        _manualDispatchStatus.value = if (isGatePass) {
            "Assigning Order #$orderNo (GATE PASS) to $effectiveVehicle..."
        } else {
            "Recording $copyType scan for Order #$orderNo..."
        }

        val dispatchRecord = ScannedRecord(
            scannerName = _scannerName.value,
            deviceId = deviceId,
            mode = "DISPATCH",
            restocked = false,
            vehicleNumber = if (isGatePass) effectiveVehicle else "",
            notes = "",
            orderNo = orderNo,
            orderId = orderNo,
            customer = "Factory Dispatch Order",
            copy = copyType,
            duplicateKey = "DISPATCH-$orderNo-$copyType-${System.currentTimeMillis()}",
            rawPayload = "MANUAL_DISPATCH:$orderNo:$copyType",
            syncStatus = "PENDING"
        )

        viewModelScope.launch {
            val id = repository.insertRecord(dispatchRecord)
            val savedRecord = dispatchRecord.copy(id = id)

            val syncResult = repository.syncRecord(savedRecord)
            _isProcessingDispatch.value = false

            when (syncResult) {
                is SyncStatusResult.Success -> {
                    _manualDispatchStatus.value = if (isGatePass) {
                        "Order #$orderNo (GATE PASS) assigned to $effectiveVehicle!"
                    } else {
                        "Order #$orderNo ($copyType) recorded in database."
                    }
                    _manualDispatchOrderInput.value = ""
                    triggerFeedback(context)
                }
                is SyncStatusResult.Offline -> {
                    _manualDispatchStatus.value = if (isGatePass) {
                        "Saved locally (Offline). Order #$orderNo (GATE PASS) queued for $effectiveVehicle."
                    } else {
                        "Saved locally (Offline). Order #$orderNo ($copyType) queued."
                    }
                    _manualDispatchOrderInput.value = ""
                    triggerFeedback(context)
                }
                is SyncStatusResult.Duplicate -> {
                    _manualDispatchStatus.value = "Notice: ${syncResult.message}"
                    triggerDuplicateFeedback(context)
                }
                is SyncStatusResult.Failed -> {
                    _manualDispatchStatus.value = "Saved locally with alert: ${syncResult.message}"
                    triggerFeedback(context)
                }
            }
        }
    }

    // Process Manual Return by typing Order Number
    fun processManualReturn(context: Context) {
        val orderNo = _manualOrderInput.value.trim()
        if (orderNo.isBlank()) {
            _manualReturnStatus.value = "Please enter an order number."
            triggerErrorFeedback(context)
            return
        }

        val vehicleValidationErr = validateVehicleReady()
        if (vehicleValidationErr != null) {
            _manualReturnStatus.value = vehicleValidationErr
            triggerErrorFeedback(context)
            return
        }

        val effectiveVehicle = getEffectiveVehicleNumber()
        val restocked = _manualRestocked.value
        _isProcessingReturn.value = true
        _manualReturnStatus.value = "Processing return for Order #$orderNo via $effectiveVehicle..."

        val returnRecord = ScannedRecord(
            scannerName = _scannerName.value,
            deviceId = deviceId,
            mode = "RETURN",
            restocked = restocked,
            vehicleNumber = effectiveVehicle,
            notes = "",
            orderNo = orderNo,
            orderId = orderNo,
            customer = "Return Item",
            copy = DispatchRepository.COPY_GATE_PASS,
            duplicateKey = "RETURN-$orderNo-${System.currentTimeMillis()}",
            rawPayload = "MANUAL_RETURN:$orderNo",
            syncStatus = "PENDING"
        )

        viewModelScope.launch {
            val id = repository.insertRecord(returnRecord)
            val savedRecord = returnRecord.copy(id = id)

            val syncResult = repository.syncRecord(savedRecord)
            _isProcessingReturn.value = false

            when (syncResult) {
                is SyncStatusResult.Success -> {
                    _manualReturnStatus.value = "Order #$orderNo recorded as RETURNED & RESTOCKED ($effectiveVehicle)!"
                    _manualOrderInput.value = ""
                    triggerFeedback(context)
                }
                is SyncStatusResult.Offline -> {
                    _manualReturnStatus.value = "Saved locally (Offline). Order #$orderNo marked returned & restocked ($effectiveVehicle)."
                    _manualOrderInput.value = ""
                    triggerFeedback(context)
                }
                is SyncStatusResult.Duplicate -> {
                    _manualReturnStatus.value = "Notice: ${syncResult.message}"
                    triggerDuplicateFeedback(context)
                }
                is SyncStatusResult.Failed -> {
                    _manualReturnStatus.value = "Saved locally with alert: ${syncResult.message}"
                    triggerFeedback(context)
                }
            }
        }
    }

    // Save scan outcome to Room DB and sync (handles both DISPATCH and RETURN modes)
    fun recordDispatch(context: Context, onSuccess: () -> Unit, onDuplicate: (String) -> Unit) {
        val payload = _scannedPayload.value ?: return
        val raw = _rawPayload.value ?: ""
        val mode = _currentMode.value
        val effectiveVehicle = getEffectiveVehicleNumber()
        val normalizedCopy = DispatchRepository.normalizeCopyType(payload.copy)
        val isGatePass = DispatchRepository.isGatePassCopy(normalizedCopy)

        val duplicateKey = listOf(
            payload.orderNo, payload.tableIndex, normalizedCopy, payload.size, payload.topColour, 
            payload.frameColour, payload.legSize, payload.legShape, payload.legHeight
        ).joinToString("-") { it?.toString() ?: "" }.trim()

        val isReturn = mode == OperationMode.RETURN

        val newRecord = ScannedRecord(
            scannerName = _scannerName.value,
            deviceId = deviceId,
            mode = if (isReturn) "RETURN" else "DISPATCH",
            restocked = isReturn,
            vehicleNumber = if (isGatePass || isReturn) effectiveVehicle else "",
            notes = "",
            orderNo = payload.orderNo ?: "",
            orderId = payload.orderId ?: "",
            customer = payload.customer ?: "",
            district = payload.district ?: "",
            deliveryDate = payload.deliveryDate ?: "",
            deliveryType = payload.deliveryType ?: "",
            salesPerson = payload.salesPerson ?: "",
            totalUnits = payload.totalUnits ?: 1,
            copy = normalizedCopy,
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
            duplicateKey = if (isReturn) "RETURN-$duplicateKey-${System.currentTimeMillis()}" else duplicateKey,
            rawPayload = raw,
            syncStatus = "PENDING"
        )
        
        triggerFeedback(context)
        clearScannedData()

        viewModelScope.launch {
            val id = repository.insertRecord(newRecord)
            val savedRecord = newRecord.copy(id = id)

            val syncResult = repository.syncRecord(savedRecord)

            when (syncResult) {
                is SyncStatusResult.Success -> {
                    onSuccess()
                }
                is SyncStatusResult.Duplicate -> {
                    onDuplicate(syncResult.message)
                }
                is SyncStatusResult.Failed -> {
                    onSuccess()
                }
                is SyncStatusResult.Offline -> {
                    onSuccess()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
    }

    fun triggerFeedback(context: Context) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
            viewModelScope.launch {
                delay(350)
                tg.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_CDMA_NETWORK_BUSY, 800)
            viewModelScope.launch {
                delay(850)
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
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_SUP_ERROR, 1000)
            viewModelScope.launch {
                delay(1050)
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
            _syncMessage.value = "Syncing pending records with Supabase factory database..."
            val summary = repository.syncPendingRecords()
            _isSyncing.value = false
            _syncMessage.value = summary.message
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun clearTodayLogAndSync(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = todayFormat.format(Date())
                val currentRecords = scannedHistory.value
                val recordsToDelete = currentRecords.filter {
                    todayFormat.format(Date(it.scannedAt)) == todayStr
                }

                recordsToDelete.forEach { record ->
                    repository.deleteRecord(record.id)
                }

                val api = SupabaseApiClient.getApiService(_supabaseUrl.value, _supabaseKey.value)
                val remoteLogs = api.getRecentDispatchReturnLogs(limit = 100)
                val todayLogs = remoteLogs.filter { log ->
                    val createdDate = log.created_at?.take(10).orEmpty()
                    createdDate == todayStr || createdDate.isEmpty()
                }

                todayLogs.forEach { log ->
                    val normalizedCopy = DispatchRepository.normalizeCopyType(log.copy_type)
                    val isReturn = log.operation_mode.equals("RETURN", ignoreCase = true)

                    val newRecord = ScannedRecord(
                        scannerName = log.scanner_name?.ifBlank { _scannerName.value } ?: _scannerName.value,
                        deviceId = log.device_id?.ifBlank { deviceId } ?: deviceId,
                        mode = if (isReturn) "RETURN" else "DISPATCH",
                        restocked = log.restocked ?: isReturn,
                        vehicleNumber = log.vehicle_number.orEmpty(),
                        notes = "",
                        orderNo = log.order_form_number.orEmpty(),
                        orderId = log.order_id.orEmpty(),
                        customer = log.customer_name.orEmpty(),
                        district = log.customer_district.orEmpty(),
                        deliveryDate = "",
                        deliveryType = "",
                        salesPerson = "",
                        totalUnits = log.quantity ?: 1,
                        copy = normalizedCopy,
                        tableIndex = 1,
                        tableCount = log.quantity ?: 1,
                        size = log.table_size.orEmpty(),
                        topColour = log.colour.orEmpty(),
                        frameColour = "",
                        qty = log.quantity ?: 1,
                        qrTimestamp = log.created_at.orEmpty(),
                        duplicateKey = "${log.order_form_number}-$normalizedCopy-${log.id}",
                        rawPayload = "",
                        syncStatus = "SYNCED",
                        scannedAt = System.currentTimeMillis()
                    )
                    repository.insertRecord(newRecord)
                }
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Unknown error")
            }
        }
    }
}
