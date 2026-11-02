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
import com.example.data.api.SupabaseApiClient
import com.example.data.api.SupabaseOrderTable
import com.example.data.local.AppDatabase
import com.example.data.local.ScannedRecord
import com.example.data.model.QrPayload
import com.example.data.repository.DispatchRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SyncStatusResult
import com.example.data.repository.SyncSummary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
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

data class ToastNotice(
    val title: String,
    val subtitle: String,
    val isOffline: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

class DispatchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = SettingsRepository(application)
    private val repository = DispatchRepository(db.scannedRecordDao, settingsRepo)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val qrAdapter = moshi.adapter(QrPayload::class.java)

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _isOnline.value = true
            forceSyncPending()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            _isOnline.value = false
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

    private val _rejectTriggerCount = MutableStateFlow(0)
    val rejectTriggerCount: StateFlow<Int> = _rejectTriggerCount.asStateFlow()

    // Manual Confirmation & Note State for Confirmation Sheet
    private val _isManualConfirmation = MutableStateFlow(false)
    val isManualConfirmation: StateFlow<Boolean> = _isManualConfirmation.asStateFlow()

    private val _isFetchingManualOrder = MutableStateFlow(false)
    val isFetchingManualOrder: StateFlow<Boolean> = _isFetchingManualOrder.asStateFlow()

    private val _manualOrderFoundInDb = MutableStateFlow(true)
    val manualOrderFoundInDb: StateFlow<Boolean> = _manualOrderFoundInDb.asStateFlow()

    private val _manualOrderTables = MutableStateFlow<List<SupabaseOrderTable>>(emptyList())
    val manualOrderTables: StateFlow<List<SupabaseOrderTable>> = _manualOrderTables.asStateFlow()

    private val _confirmationNote = MutableStateFlow("")
    val confirmationNote: StateFlow<String> = _confirmationNote.asStateFlow()

    // Top Toast state (drops in from top for 2.6s on success or offline queue)
    private val _activeToast = MutableStateFlow<ToastNotice?>(null)
    val activeToast: StateFlow<ToastNotice?> = _activeToast.asStateFlow()
    private var toastJob: Job? = null

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
    private val _historyModeFilter = MutableStateFlow("ALL") // "ALL", "DISPATCH", "RETURN"
    val historyModeFilter: StateFlow<String> = _historyModeFilter.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sync progress & summary
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow<SyncSummary?>(null)
    val lastSyncSummary: StateFlow<SyncSummary?> = _lastSyncSummary.asStateFlow()

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

            val todayStr = DispatchRepository.formatSriLankaDateOnly()

            val filteredByToday = records.filter {
                DispatchRepository.formatSriLankaDateOnly(it.scannedAt) == todayStr
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
                        it.copy.contains(query, ignoreCase = true) ||
                        it.size.contains(query, ignoreCase = true) ||
                        it.notes.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun showTopToast(title: String, subtitle: String, isOffline: Boolean = false) {
        toastJob?.cancel()
        val notice = ToastNotice(title = title, subtitle = subtitle, isOffline = isOffline)
        _activeToast.value = notice
        toastJob = viewModelScope.launch {
            delay(2600)
            if (_activeToast.value?.id == notice.id) {
                _activeToast.value = null
            }
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _activeToast.value = null
    }

    fun dismissScanError() {
        _scanError.value = null
    }

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
        return when (DispatchRepository.resolveKnownDriverUuid(effective)) {
            DRIVER_UUID_RATHNAYAKE -> "GATE PASS assigns driver Rathnayake"
            DRIVER_UUID_LASANTHA -> "GATE PASS assigns driver Lasantha"
            else -> "GATE PASS assigns delivery · $effective"
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

    fun findLocalDuplicateError(
        mode: String,
        orderNo: String,
        copyName: String,
        tableIndex: Int = 1,
        size: String = "",
        topColour: String = "",
        frameColour: String = "",
        legSize: String = "",
        legHeight: String = "",
        legShape: String = ""
    ): String? {
        val normalizedOrder = DispatchRepository.normalizeOrderNo(orderNo)
        if (normalizedOrder.isEmpty()) return null
        val normalizedCopy = DispatchRepository.normalizeCopyType(copyName)
        val isReturn = mode.equals("RETURN", ignoreCase = true)

        val matchingRecords = allRecordsState.value.filter { record ->
            record.syncStatus != "FAILED" &&
                record.syncStatus != "DUPLICATE" &&
                DispatchRepository.hasSameTableFeatures(
                    record = record,
                    orderNo = normalizedOrder,
                    copyName = normalizedCopy,
                    size = size,
                    topColour = topColour,
                    frameColour = frameColour,
                    legSize = legSize,
                    legHeight = legHeight,
                    legShape = legShape,
                    tableIndex = tableIndex
                )
        }

        val featureSummary = DispatchRepository.formatTableFeatureDescriptor(
            size = size,
            topColour = topColour,
            frameColour = frameColour,
            legSize = legSize,
            legHeight = legHeight,
            legShape = legShape
        )
        val labelSuffix = if (featureSummary.isNotBlank()) " ($featureSummary)" else ""

        return if (!isReturn) {
            val alreadyDispatched = matchingRecords.firstOrNull {
                it.mode.equals("DISPATCH", ignoreCase = true) || it.mode.equals("RETURN", ignoreCase = true)
            }
            if (alreadyDispatched != null) {
                val timeStr = DispatchRepository.formatSriLankaDateTime(alreadyDispatched.scannedAt)
                "Duplicate Denied: $normalizedCopy for Order #$normalizedOrder$labelSuffix was already ${if (alreadyDispatched.mode.equals("RETURN", ignoreCase = true)) "returned" else "dispatched"}.\nFirst scan at $timeStr (SLST) · ${alreadyDispatched.scannerName.ifBlank { _scannerName.value }}"
            } else {
                null
            }
        } else {
            val alreadyReturned = matchingRecords.firstOrNull {
                it.mode.equals("RETURN", ignoreCase = true)
            }
            if (alreadyReturned != null) {
                val timeStr = DispatchRepository.formatSriLankaDateTime(alreadyReturned.scannedAt)
                "Duplicate Denied: $normalizedCopy for Order #$normalizedOrder$labelSuffix was already returned.\nReturned at $timeStr (SLST) · ${alreadyReturned.scannerName.ifBlank { _scannerName.value }}"
            } else {
                null
            }
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

        // Attempt 1: Standard JSON QrPayload (supports both camelCase and snake_case keys)
        try {
            val payload = qrAdapter.fromJson(trimmed)
            if (payload != null && payload.resolvedOrderNo.isNotEmpty()) {
                val normalizedCopy = DispatchRepository.normalizeCopyType(payload.resolvedCopy)
                handleValidPayload(payload.toNormalized(normalizedCopy), trimmed, isReturnMode, context)
                return
            }
        } catch (ignored: Exception) {
            // Not a JSON payload, try plaintext parsing below
        }

        // Attempt 2: Plaintext format or Order Number format (e.g., "ORD-12345" or "1024")
        if (trimmed.isNotBlank() && trimmed.length in 1..64 && !trimmed.startsWith("{")) {
            val rawOrderNum = if (trimmed.startsWith("ORDER:", ignoreCase = true)) {
                trimmed.substringAfter(":").trim()
            } else {
                trimmed
            }
            val orderNum = DispatchRepository.normalizeOrderNo(rawOrderNum)
            if (orderNum.isNotEmpty()) {
                val syntheticPayload = QrPayload(
                    orderNo = orderNum,
                    customer = "Factory Order",
                    copy = DispatchRepository.COPY_GATE_PASS,
                    qty = 1,
                    totalUnits = 1
                )
                handleValidPayload(syntheticPayload, trimmed, isReturnMode, context)
                return
            }
        }

        _scanError.value = "Invalid QR code format."
        _rejectTriggerCount.value += 1
        triggerErrorFeedback(context)
        clearErrorAfterDelay()
    }

    private fun handleValidPayload(
        payload: QrPayload,
        rawContent: String,
        isReturnMode: Boolean,
        context: Context
    ) {
        val normalizedCopy = DispatchRepository.normalizeCopyType(payload.resolvedCopy)
        val normalizedPayload = payload.toNormalized(normalizedCopy)
        val modeStr = if (isReturnMode) "RETURN" else "DISPATCH"
        val orderNo = normalizedPayload.resolvedOrderNo
        val tableIdx = normalizedPayload.resolvedTableIndex

        // Authorization check (only applies in dispatch mode if configured)
        if (!isReturnMode) {
            val requiredCopies = _authorizedCopies.value
            if (requiredCopies.isNotBlank()) {
                val allowedCopiesList = requiredCopies.split(",").map { it.trim().lowercase() }
                if (!allowedCopiesList.contains(normalizedCopy.lowercase())) {
                    _scanError.value = "Unauthorized: This station can't scan $normalizedCopy.\nAccepts ${requiredCopies.trim()}"
                    _rejectTriggerCount.value += 1
                    triggerErrorFeedback(context)
                    clearErrorAfterDelay()
                    return
                }
            }
        }

        // Strict local duplicate check using combined 7-feature key:
        // (orderNo, size, topColour, frameColour, legSize, legHeight, legShape) + copyName
        val localDupErr = findLocalDuplicateError(
            mode = modeStr,
            orderNo = orderNo,
            copyName = normalizedCopy,
            tableIndex = tableIdx,
            size = normalizedPayload.resolvedSize,
            topColour = normalizedPayload.resolvedTopColour,
            frameColour = normalizedPayload.resolvedFrameColour,
            legSize = normalizedPayload.resolvedLegSize,
            legHeight = normalizedPayload.resolvedLegHeight,
            legShape = normalizedPayload.resolvedLegShape
        )
        if (localDupErr != null) {
            _scanError.value = localDupErr
            _rejectTriggerCount.value += 1
            triggerDuplicateFeedback(context)
            clearErrorAfterDelay()
            return
        }

        _scanError.value = null
        _isManualConfirmation.value = false
        _isFetchingManualOrder.value = false
        _manualOrderFoundInDb.value = true
        _manualOrderTables.value = emptyList()
        _confirmationNote.value = ""
        _scannedPayload.value = normalizedPayload
        _rawPayload.value = rawContent
        triggerFeedback(context)
    }

    private fun clearErrorAfterDelay() {
        viewModelScope.launch {
            delay(4000)
            _scanError.value = null
        }
    }

    fun clearScannedData() {
        _scannedPayload.value = null
        _rawPayload.value = null
        _isManualConfirmation.value = false
        _isFetchingManualOrder.value = false
        _manualOrderFoundInDb.value = true
        _manualOrderTables.value = emptyList()
        _confirmationNote.value = ""
    }

    fun setConfirmationNote(note: String) {
        _confirmationNote.value = note
    }

    fun updateScannedPayloadDraft(updated: QrPayload) {
        _scannedPayload.value = updated
    }

    fun updateRecordNote(record: ScannedRecord, newNote: String, onSaved: (ScannedRecord) -> Unit = {}) {
        viewModelScope.launch {
            val updated = repository.updateRecordNote(record, newNote)
            onSaved(updated)
            showTopToast(
                title = "Note saved · #${record.orderNo}",
                subtitle = updated.notes.ifBlank { "Note cleared" },
                isOffline = false
            )
        }
    }

    // Process Manual Dispatch by typing Order Number:
    // Opens the confirmation popup, fetches order & table details from Supabase, and lets the user edit details & add a note before confirming.
    fun processManualDispatch(context: Context) {
        val orderNo = DispatchRepository.normalizeOrderNo(_manualDispatchOrderInput.value)
        if (orderNo.isBlank()) {
            _manualDispatchStatus.value = "Please enter an order number."
            triggerErrorFeedback(context)
            return
        }

        val copyType = DispatchRepository.COPY_GATE_PASS
        val vehicleValidationErr = validateVehicleReady()
        if (vehicleValidationErr != null) {
            _manualDispatchStatus.value = vehicleValidationErr
            triggerErrorFeedback(context)
            return
        }

        _scanError.value = null
        _manualDispatchStatus.value = null
        _isManualConfirmation.value = true
        _isFetchingManualOrder.value = true
        _manualOrderFoundInDb.value = false
        _manualOrderTables.value = emptyList()
        _confirmationNote.value = ""

        val initialDraft = QrPayload(
            orderNo = orderNo,
            orderId = orderNo,
            customer = "",
            district = "",
            copy = copyType,
            qty = 1,
            totalUnits = 1,
            tableIndex = 1,
            tableCount = 1
        )
        _scannedPayload.value = initialDraft
        _rawPayload.value = "MANUAL_DISPATCH:$orderNo:$copyType"

        viewModelScope.launch {
            val fetched = repository.fetchOrderDetailsForManualEntry(orderNo, copyType)
            if (_isManualConfirmation.value && _scannedPayload.value?.orderNo == orderNo) {
                _scannedPayload.value = fetched.payload
                _manualOrderTables.value = fetched.tables
                _manualOrderFoundInDb.value = fetched.foundInDatabase
            }
            _isFetchingManualOrder.value = false
        }
    }

    // Process Manual Return by typing Order Number:
    // Opens the confirmation popup, fetches order & table details from Supabase, and lets the user edit details & add a note before confirming.
    fun processManualReturn(context: Context) {
        val orderNo = DispatchRepository.normalizeOrderNo(_manualOrderInput.value)
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

        val copyType = DispatchRepository.COPY_GATE_PASS
        _scanError.value = null
        _manualReturnStatus.value = null
        _isManualConfirmation.value = true
        _isFetchingManualOrder.value = true
        _manualOrderFoundInDb.value = false
        _manualOrderTables.value = emptyList()
        _confirmationNote.value = ""

        val initialDraft = QrPayload(
            orderNo = orderNo,
            orderId = orderNo,
            customer = "",
            district = "",
            copy = copyType,
            qty = 1,
            totalUnits = 1,
            tableIndex = 1,
            tableCount = 1
        )
        _scannedPayload.value = initialDraft
        _rawPayload.value = "MANUAL_RETURN:$orderNo"

        viewModelScope.launch {
            val fetched = repository.fetchOrderDetailsForManualEntry(orderNo, copyType)
            if (_isManualConfirmation.value && _scannedPayload.value?.orderNo == orderNo) {
                _scannedPayload.value = fetched.payload
                _manualOrderTables.value = fetched.tables
                _manualOrderFoundInDb.value = fetched.foundInDatabase
            }
            _isFetchingManualOrder.value = false
        }
    }

    // Save scan outcome to Room DB and sync (handles both DISPATCH and RETURN modes, QR and Manual Confirmation)
    fun recordDispatch(
        context: Context,
        onSuccess: () -> Unit = {},
        onDuplicate: (String) -> Unit = {},
        overridePayload: QrPayload? = null,
        noteText: String? = null,
        restockOverride: Boolean? = null
    ) {
        val payload = overridePayload ?: _scannedPayload.value ?: return
        val raw = _rawPayload.value ?: ""
        val wasManual = _isManualConfirmation.value
        val finalNote = (noteText ?: _confirmationNote.value).trim()
        val mode = _currentMode.value
        val effectiveVehicle = getEffectiveVehicleNumber()
        val normalizedCopy = DispatchRepository.normalizeCopyType(payload.resolvedCopy)
        val isGatePass = DispatchRepository.isGatePassCopy(normalizedCopy)
        val isReturn = mode == OperationMode.RETURN
        val restockFlag = restockOverride ?: if (isReturn) _manualRestocked.value else false
        val modeStr = if (isReturn) "RETURN" else "DISPATCH"
        val orderNo = payload.resolvedOrderNo
        val tableIdx = payload.resolvedTableIndex
        val scannedSize = payload.resolvedSize
        val scannedTopColour = payload.resolvedTopColour
        val scannedFrameColour = payload.resolvedFrameColour
        val scannedLegSize = payload.resolvedLegSize
        val scannedLegHeight = payload.resolvedLegHeight
        val scannedLegShape = payload.resolvedLegShape
        val scannedQty = payload.resolvedQty
        val scannedTotalUnits = payload.resolvedTotalUnits

        val localDupErr = findLocalDuplicateError(
            mode = modeStr,
            orderNo = orderNo,
            copyName = normalizedCopy,
            tableIndex = tableIdx,
            size = scannedSize,
            topColour = scannedTopColour,
            frameColour = scannedFrameColour,
            legSize = scannedLegSize,
            legHeight = scannedLegHeight,
            legShape = scannedLegShape
        )
        if (localDupErr != null) {
            clearScannedData()
            _scanError.value = localDupErr
            _rejectTriggerCount.value += 1
            triggerDuplicateFeedback(context)
            clearErrorAfterDelay()
            onDuplicate(localDupErr)
            return
        }

        val duplicateKey = DispatchRepository.buildDuplicateKey(
            mode = modeStr,
            orderNo = orderNo,
            copyName = normalizedCopy,
            tableIndex = tableIdx,
            size = scannedSize,
            topColour = scannedTopColour,
            frameColour = scannedFrameColour,
            legSize = scannedLegSize,
            legHeight = scannedLegHeight,
            legShape = scannedLegShape
        )

        val newRecord = ScannedRecord(
            scannerName = _scannerName.value,
            deviceId = deviceId,
            mode = modeStr,
            restocked = if (isReturn) restockFlag else false,
            vehicleNumber = effectiveVehicle,
            notes = finalNote,
            orderNo = orderNo,
            orderId = payload.resolvedOrderId,
            customer = payload.resolvedCustomer,
            district = payload.resolvedDistrict,
            deliveryDate = payload.resolvedDeliveryDate,
            deliveryType = payload.resolvedDeliveryType,
            salesPerson = payload.resolvedSalesPerson,
            totalUnits = scannedTotalUnits,
            copy = normalizedCopy,
            tableIndex = tableIdx,
            tableCount = payload.resolvedTableCount,
            size = scannedSize,
            topColour = scannedTopColour,
            frameColour = scannedFrameColour,
            qty = scannedQty,
            legSize = scannedLegSize,
            legShape = scannedLegShape,
            legHeight = scannedLegHeight,
            qrTimestamp = payload.ts ?: "",
            duplicateKey = duplicateKey,
            rawPayload = raw,
            syncStatus = "PENDING"
        )

        if (wasManual) {
            if (isReturn) {
                _manualOrderInput.value = ""
            } else {
                _manualDispatchOrderInput.value = ""
            }
        }

        triggerFeedback(context)
        clearScannedData()

        viewModelScope.launch {
            val dbDup = repository.checkLocalDuplicate(
                mode = modeStr,
                orderNo = orderNo,
                copyName = normalizedCopy,
                tableIndex = tableIdx,
                size = scannedSize,
                topColour = scannedTopColour,
                frameColour = scannedFrameColour,
                legSize = scannedLegSize,
                legHeight = scannedLegHeight,
                legShape = scannedLegShape
            )
            if (dbDup != null) {
                _scanError.value = dbDup
                _rejectTriggerCount.value += 1
                triggerDuplicateFeedback(context)
                clearErrorAfterDelay()
                onDuplicate(dbDup)
                return@launch
            }

            val id = repository.insertRecord(newRecord)
            val savedRecord = newRecord.copy(id = id)

            val syncResult = repository.syncRecord(savedRecord)
            val sizeTag = if (scannedSize.isNotBlank()) " · $scannedSize" else ""

            when (syncResult) {
                is SyncStatusResult.Success -> {
                    val slTime = DispatchRepository.formatSriLankaDateTime(savedRecord.scannedAt)
                    val toastTitle = if (isReturn) "#$orderNo$sizeTag returned" else "#$orderNo$sizeTag dispatched"
                    val toastSub = when {
                        isReturn -> "$normalizedCopy · $effectiveVehicle · $slTime"
                        isGatePass -> "GATE PASS · ${effectiveVehicle.replace("(", "· ").replace(")", "")} · $slTime"
                        else -> "$normalizedCopy · recorded · $slTime"
                    }
                    showTopToast(title = toastTitle, subtitle = toastSub, isOffline = false)
                    onSuccess()
                }
                is SyncStatusResult.Duplicate -> {
                    _scanError.value = syncResult.message
                    _rejectTriggerCount.value += 1
                    triggerDuplicateFeedback(context)
                    clearErrorAfterDelay()
                    onDuplicate(syncResult.message)
                }
                is SyncStatusResult.Failed -> {
                    _scanError.value = syncResult.message
                    _rejectTriggerCount.value += 1
                    triggerErrorFeedback(context)
                    clearErrorAfterDelay()
                    onSuccess()
                }
                is SyncStatusResult.Offline -> {
                    val slTime = DispatchRepository.formatSriLankaDateTime(savedRecord.scannedAt)
                    showTopToast(
                        title = "Saved offline · #$orderNo$sizeTag",
                        subtitle = "$normalizedCopy · $slTime (SLST)",
                        isOffline = true
                    )
                    onSuccess()
                }
            }
        }
    }

    fun retrySingleRecord(record: ScannedRecord, context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncRecord(record)
            _isSyncing.value = false
            _lastSyncTime.value = DispatchRepository.formatSriLankaDateTime()
            when (result) {
                is SyncStatusResult.Success -> {
                    showTopToast(
                        title = "#${record.orderNo} synced",
                        subtitle = "${record.copy} · ${record.vehicleNumber} · ${DispatchRepository.formatSriLankaDateTime(record.scannedAt)}",
                        isOffline = false
                    )
                    triggerFeedback(context)
                }
                is SyncStatusResult.Duplicate -> {
                    _syncMessage.value = result.message
                    triggerDuplicateFeedback(context)
                }
                is SyncStatusResult.Failed -> {
                    _syncMessage.value = result.message
                    triggerErrorFeedback(context)
                }
                is SyncStatusResult.Offline -> {
                    _syncMessage.value = result.message
                }
            }
        }
    }

    fun deleteSingleRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
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
            _lastSyncTime.value = DispatchRepository.formatSriLankaDateTime()
            _lastSyncSummary.value = summary
            _syncMessage.value = summary.message
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun clearTodayLogAndSync(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val todayStr = DispatchRepository.formatSriLankaDateOnly()
                val currentRecords = allRecordsState.value
                val recordsToDelete = currentRecords.filter {
                    DispatchRepository.formatSriLankaDateOnly(it.scannedAt) == todayStr
                }

                recordsToDelete.forEach { record ->
                    repository.deleteRecord(record.id)
                }

                val api = SupabaseApiClient.getApiService(_supabaseUrl.value, _supabaseKey.value)
                val recentLogs = try {
                    api.getRecentDispatchReturnLogs(limit = 100)
                } catch (_: Exception) {
                    emptyList()
                }
                val recentOrders = try {
                    api.getRecentOrders(limit = 100)
                } catch (_: Exception) {
                    emptyList()
                }
                val ordersById = recentOrders.associateBy { it.id }

                val todayLogs = recentLogs.filter { log ->
                    val createdDate = log.created_at?.take(10).orEmpty()
                    createdDate == todayStr || createdDate.isEmpty()
                }

                if (todayLogs.isNotEmpty()) {
                    todayLogs.forEach { log ->
                        val matchedOrder = ordersById[log.order_id]
                        val orderNo = log.order_form_number?.ifBlank { null }
                            ?: matchedOrder?.order_form_number
                            ?: log.order_id?.take(8).orEmpty()
                        val normalizedCopy = DispatchRepository.normalizeCopyType(log.copy_type)
                        val modeStr = if (log.operation_mode.equals("RETURN", ignoreCase = true)) "RETURN" else "DISPATCH"
                        val parsedNotes = DispatchRepository.parseNotesKeyValues(log.notes)
                        val tableSize = log.table_size?.ifBlank { null } ?: parsedNotes["size"].orEmpty()
                        val topCol = log.colour?.ifBlank { null } ?: parsedNotes["topColour"].orEmpty()
                        val frameCol = parsedNotes["frameColour"].orEmpty()
                        val legSz = parsedNotes["legSize"].orEmpty()
                        val legHt = parsedNotes["legHeight"].orEmpty()
                        val legShp = parsedNotes["legShape"].orEmpty()
                        val qtyVal = log.quantity ?: parsedNotes["qty"]?.toIntOrNull() ?: 1

                        val newRecord = ScannedRecord(
                            scannerName = log.scanner_name?.ifBlank { _scannerName.value } ?: _scannerName.value,
                            deviceId = log.device_id?.ifBlank { deviceId } ?: deviceId,
                            mode = modeStr,
                            restocked = log.restocked ?: (modeStr == "RETURN"),
                            vehicleNumber = log.vehicle_number.orEmpty(),
                            notes = DispatchRepository.extractUserNote(log.notes),
                            orderNo = orderNo,
                            orderId = log.order_id.orEmpty(),
                            customer = log.customer_name?.ifBlank { matchedOrder?.customer_name.orEmpty() }.orEmpty(),
                            district = log.customer_district?.ifBlank { matchedOrder?.customer_district.orEmpty() }.orEmpty(),
                            deliveryDate = matchedOrder?.delivery_date.orEmpty(),
                            deliveryType = matchedOrder?.delivery_type.orEmpty(),
                            salesPerson = matchedOrder?.sales_person_name.orEmpty(),
                            totalUnits = matchedOrder?.quantity ?: qtyVal,
                            copy = normalizedCopy,
                            tableIndex = 1,
                            tableCount = matchedOrder?.quantity ?: qtyVal,
                            size = tableSize,
                            topColour = topCol,
                            frameColour = frameCol,
                            qty = qtyVal,
                            legSize = legSz,
                            legShape = legShp,
                            legHeight = legHt,
                            qrTimestamp = log.created_at.orEmpty(),
                            duplicateKey = DispatchRepository.buildDuplicateKey(
                                mode = modeStr,
                                orderNo = orderNo,
                                copyName = normalizedCopy,
                                tableIndex = 1,
                                size = tableSize,
                                topColour = topCol,
                                frameColour = frameCol,
                                legSize = legSz,
                                legHeight = legHt,
                                legShape = legShp
                            ),
                            rawPayload = "",
                            syncStatus = "SYNCED",
                            scannedAt = System.currentTimeMillis()
                        )
                        repository.insertRecord(newRecord)
                    }
                } else {
                    val recentTransports = api.getRecentTransports(limit = 100)
                    val todayTransports = recentTransports.filter { tr ->
                        val loadedDate = tr.loaded_at?.take(10).orEmpty()
                        loadedDate == todayStr || loadedDate.isEmpty()
                    }
                    todayTransports.forEach { tr ->
                        val matchedOrder = ordersById[tr.order_id]
                        val orderNo = matchedOrder?.order_form_number ?: tr.order_id?.take(8).orEmpty()
                        val normalizedCopy = DispatchRepository.normalizeCopyType(tr.copy_name)
                        val isReturn = tr.transport_status?.trim().equals(DispatchRepository.TRANSPORT_STATUS_RETURNED, ignoreCase = true)
                        val modeStr = if (isReturn) "RETURN" else "DISPATCH"

                        val newRecord = ScannedRecord(
                            scannerName = _scannerName.value,
                            deviceId = deviceId,
                            mode = modeStr,
                            restocked = isReturn,
                            vehicleNumber = tr.transport_mode.orEmpty(),
                            notes = "",
                            orderNo = orderNo,
                            orderId = tr.order_id.orEmpty(),
                            customer = matchedOrder?.customer_name.orEmpty(),
                            district = matchedOrder?.customer_district.orEmpty(),
                            deliveryDate = matchedOrder?.delivery_date.orEmpty(),
                            deliveryType = matchedOrder?.delivery_type.orEmpty(),
                            salesPerson = matchedOrder?.sales_person_name.orEmpty(),
                            totalUnits = matchedOrder?.quantity ?: 1,
                            copy = normalizedCopy,
                            tableIndex = 1,
                            tableCount = matchedOrder?.quantity ?: 1,
                            size = matchedOrder?.table_size.orEmpty(),
                            topColour = matchedOrder?.colour.orEmpty(),
                            frameColour = "",
                            qty = 1,
                            qrTimestamp = tr.loaded_at.orEmpty(),
                            duplicateKey = DispatchRepository.buildDuplicateKey(modeStr, orderNo, normalizedCopy, 1),
                            rawPayload = "",
                            syncStatus = "SYNCED",
                            scannedAt = System.currentTimeMillis()
                        )
                        repository.insertRecord(newRecord)
                    }
                }
                _lastSyncTime.value = DispatchRepository.formatSriLankaDateTime()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Unknown error")
            }
        }
    }
}
