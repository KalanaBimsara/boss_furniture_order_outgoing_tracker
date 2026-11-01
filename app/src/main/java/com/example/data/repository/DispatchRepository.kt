package com.example.data.repository

import com.example.data.api.DispatchReturnLogInsertBody
import com.example.data.api.OrderPatchBody
import com.example.data.api.SupabaseApi
import com.example.data.api.SupabaseApiClient
import com.example.data.api.SupabaseOrder
import com.example.data.api.TransportInsertBody
import com.example.data.api.TransportPatchBody
import com.example.data.local.ScannedRecord
import com.example.data.local.ScannedRecordDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

open class DispatchRepository(
    private val dao: ScannedRecordDao,
    private val settingsRepo: SettingsRepository? = null,
    private val apiOverride: SupabaseApi? = null
) {
    companion object {
        const val DRIVER_UUID_RATHNAYAKE = "de7a7b6c-92db-4fe7-82d6-2f2cda9e8a56"
        const val DRIVER_UUID_LASANTHA = "0c940655-4963-4036-ae66-4a889f9d368f"

        const val COPY_ACCOUNT = "ACCOUNT COPY"
        const val COPY_GATE_PASS = "GATE PASS"
        const val COPY_TRANSPORT = "TRANSPORT COPY"
        const val COPY_PRODUCTION = "PRODUCTION COPY"

        const val TRANSPORT_STATUS_DISPATCHED = "dispatched"
        const val TRANSPORT_STATUS_RETURNED = "returned"

        val VALID_COPIES = listOf(
            COPY_GATE_PASS,
            COPY_ACCOUNT,
            COPY_TRANSPORT,
            COPY_PRODUCTION
        )

        val SRI_LANKA_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Colombo")

        fun formatSriLankaIsoTimestamp(epochMillis: Long = System.currentTimeMillis()): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                sdf.timeZone = SRI_LANKA_TIME_ZONE
                sdf.format(Date(epochMillis))
            } catch (_: Exception) {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+05:30'", Locale.US)
                sdf.timeZone = SRI_LANKA_TIME_ZONE
                sdf.format(Date(epochMillis))
            }
        }

        fun formatSriLankaDateTime(epochMillis: Long = System.currentTimeMillis()): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd · HH:mm:ss", Locale.US)
            sdf.timeZone = SRI_LANKA_TIME_ZONE
            return sdf.format(Date(epochMillis))
        }

        fun formatSriLankaDateOnly(epochMillis: Long = System.currentTimeMillis()): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = SRI_LANKA_TIME_ZONE
            return sdf.format(Date(epochMillis))
        }

        fun formatSriLankaTimeOnly(epochMillis: Long = System.currentTimeMillis()): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            sdf.timeZone = SRI_LANKA_TIME_ZONE
            return sdf.format(Date(epochMillis))
        }

        fun normalizeOrderNo(rawOrderNo: String?): String {
            return rawOrderNo
                ?.trim()
                ?.removePrefix("#")
                ?.trim()
                .orEmpty()
        }

        fun normalizeCopyType(rawCopy: String?): String {
            val cleaned = rawCopy?.trim().orEmpty().uppercase()
            return when {
                cleaned.contains("GATE") || cleaned.contains("PASS") -> COPY_GATE_PASS
                cleaned.contains("ACCOUNT") -> COPY_ACCOUNT
                cleaned.contains("TRANSPORT") -> COPY_TRANSPORT
                cleaned.contains("PRODUCTION") -> COPY_PRODUCTION
                cleaned.isNotEmpty() -> cleaned
                else -> COPY_GATE_PASS
            }
        }

        fun isGatePassCopy(rawCopy: String?): Boolean {
            return normalizeCopyType(rawCopy) == COPY_GATE_PASS
        }

        fun buildDuplicateKey(
            mode: String,
            orderNo: String,
            copyName: String,
            tableIndex: Int = 1
        ): String {
            val normalizedMode = mode.trim().uppercase()
            val normalizedOrder = normalizeOrderNo(orderNo).uppercase()
            val normalizedCopy = normalizeCopyType(copyName)
            return "$normalizedMode|$normalizedOrder|$normalizedCopy|$tableIndex"
        }

        fun resolveKnownDriverUuid(vehicleOption: String): String? {
            val normalized = vehicleOption.trim()
            return when {
                normalized.contains("DAC-4912", ignoreCase = true) ||
                    normalized.contains("Rathnayake", ignoreCase = true) -> DRIVER_UUID_RATHNAYAKE
                normalized.contains("DAE-4217", ignoreCase = true) ||
                    normalized.contains("Lasantha", ignoreCase = true) -> DRIVER_UUID_LASANTHA
                else -> null
            }
        }
    }

    val allRecords: Flow<List<ScannedRecord>> = dao.getAllRecords()

    suspend fun insertRecord(record: ScannedRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun updateRecord(record: ScannedRecord) {
        dao.updateRecord(record)
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteRecordById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllRecords()
    }

    /**
     * Checks local SQLite history for duplicate Dispatch or Return of the same (orderNo, copy_name, tableIndex).
     * Every record can be dispatched once and returned once.
     */
    suspend fun checkLocalDuplicate(
        mode: String,
        orderNo: String,
        copyName: String,
        tableIndex: Int = 1,
        excludeId: Long = 0L
    ): String? {
        val normalizedOrder = normalizeOrderNo(orderNo)
        if (normalizedOrder.isEmpty()) return null
        val normalizedCopy = normalizeCopyType(copyName)
        val isReturn = mode.equals("RETURN", ignoreCase = true)

        val existingForOrder = dao.getRecordsByOrderNo(normalizedOrder).filter { record ->
            record.id != excludeId &&
                record.syncStatus != "FAILED" &&
                record.syncStatus != "DUPLICATE" &&
                normalizeCopyType(record.copy) == normalizedCopy &&
                record.tableIndex == tableIndex
        }

        return if (!isReturn) {
            val alreadyDispatched = existingForOrder.firstOrNull {
                it.mode.equals("DISPATCH", ignoreCase = true) || it.mode.equals("RETURN", ignoreCase = true)
            }
            if (alreadyDispatched != null) {
                "Duplicate Denied: $normalizedCopy for Order #$normalizedOrder was already ${if (alreadyDispatched.mode.equals("RETURN", ignoreCase = true)) "returned" else "dispatched"}."
            } else {
                null
            }
        } else {
            val alreadyReturned = existingForOrder.firstOrNull {
                it.mode.equals("RETURN", ignoreCase = true)
            }
            if (alreadyReturned != null) {
                "Duplicate Denied: $normalizedCopy for Order #$normalizedOrder was already returned."
            } else {
                null
            }
        }
    }

    private fun getIsoTimestamp(epochMillis: Long = System.currentTimeMillis()): String {
        return formatSriLankaIsoTimestamp(epochMillis)
    }

    /**
     * Assigns the order in `public.orders` to the resolved driver (`delivery_person_id`) and sets
     * `status = "assigned"` and `delivery_status = "assigned"`.
     * Strictly verifies that Supabase returned at least 1 updated row in the response body.
     * Never writes to `note` or any other order columns.
     */
    private suspend fun assignOrderToDriverInSupabase(
        api: SupabaseApi,
        resolvedOrderId: String,
        cleanOrderNo: String,
        deliveryPersonId: String?,
        isoTime: String
    ): String? {
        val patchBody = OrderPatchBody(
            delivery_person_id = deliveryPersonId,
            status = "assigned",
            delivery_status = "assigned",
            updated_at = isoTime
        )

        var patchResponse = api.updateOrderStatusById("eq.$resolvedOrderId", patchBody)
        if (!patchResponse.isSuccessful || patchResponse.body().isNullOrEmpty()) {
            val fallbackResponse = api.updateOrderStatus("eq.$cleanOrderNo", patchBody)
            if (fallbackResponse.isSuccessful && !fallbackResponse.body().isNullOrEmpty()) {
                patchResponse = fallbackResponse
            }
        }

        if (!patchResponse.isSuccessful) {
            return "Supabase Order Update HTTP ${patchResponse.code()}: ${patchResponse.message()}"
        }

        val updatedOrders = patchResponse.body()
        if (updatedOrders.isNullOrEmpty()) {
            return "Order #$cleanOrderNo could not be updated to assigned in public.orders (0 rows affected)."
        }

        val updated = updatedOrders.first()
        if (!updated.status.equals("assigned", ignoreCase = true)) {
            return "Order #$cleanOrderNo status remained '${updated.status}' instead of 'assigned'."
        }

        return null
    }

    /**
     * Syncs a ScannedRecord (Dispatch or Return) to the central Supabase Factory Database:
     * 1. Looks up the order in `public.orders` by `order_form_number` (or `id`) to resolve its UUID (`id`).
     * 2. Enforces strict duplicate prevention against `public.transport` using `(order_id, copy_name)` and `transport_status`:
     *    - Every `(order_id, copy_name)` can be dispatched once (`transport_status = "dispatched"`) and returned (`transport_status = "returned"`).
     *    - Scanning the same copy in DISPATCH mode when a row already exists in `public.transport` is rejected as a Duplicate
     *      (unless `public.orders` was previously left in `pending` state, in which case it self-heals `public.orders` first).
     *    - Scanning the same copy in RETURN mode when `transport_status` is already `"returned"` is rejected as a Duplicate.
     * 3. In DISPATCH mode:
     *    - ONLY when the scanned copy is `GATE PASS`:
     *      - Assigns the order to the selected driver (`delivery_person_id`) and sets `status = "assigned"`, `delivery_status = "assigned"` in `public.orders`.
     *      - Strictly verifies that `public.orders` returned the updated row before recording transport.
     *      - Never writes to `note` or any other order columns in `public.orders`.
     *    - Records `copy_name` (`ACCOUNT COPY`, `GATE PASS`, `TRANSPORT COPY`, `PRODUCTION COPY`) and `transport_status = "dispatched"` in `public.transport`.
     * 4. In RETURN mode:
     *    - Updates (or inserts) the row in `public.transport` with `copy_name` and `transport_status = "returned"`.
     */
    open suspend fun syncRecord(record: ScannedRecord): SyncStatusResult = withContext(Dispatchers.IO) {
        val supabaseUrl = settingsRepo?.getSupabaseUrl() ?: SettingsRepository.DEFAULT_SUPABASE_URL
        val supabaseKey = settingsRepo?.getSupabaseKey() ?: SettingsRepository.DEFAULT_SUPABASE_KEY

        try {
            val api = apiOverride ?: SupabaseApiClient.getApiService(supabaseUrl, supabaseKey)
            val isoTime = getIsoTimestamp(record.scannedAt)
            val cleanOrderNo = normalizeOrderNo(record.orderNo)

            // 1. Fetch order from `public.orders` by order_form_number (or id)
            var matchedOrder: SupabaseOrder? = null
            if (cleanOrderNo.isNotEmpty()) {
                val foundByOrderNo = api.getOrderByOrderNumber("eq.$cleanOrderNo")
                if (foundByOrderNo.isNotEmpty()) {
                    matchedOrder = foundByOrderNo.first()
                }
            }
            if (matchedOrder == null && record.orderId.length == 36 && record.orderId.contains("-")) {
                val foundById = api.getOrderById("eq.${record.orderId}")
                if (foundById.isNotEmpty()) {
                    matchedOrder = foundById.first()
                }
            }
            if (matchedOrder == null && cleanOrderNo.length == 36 && cleanOrderNo.contains("-")) {
                val foundByOrderNoAsUuid = api.getOrderById("eq.$cleanOrderNo")
                if (foundByOrderNoAsUuid.isNotEmpty()) {
                    matchedOrder = foundByOrderNoAsUuid.first()
                }
            }

            val effectiveVehicle = record.vehicleNumber.trim().ifBlank {
                settingsRepo?.getSelectedVehicleOption() ?: "DAC-4912 (Rathnayake)"
            }
            val normalizedCopy = normalizeCopyType(record.copy)

            // Enrich local record with real Supabase order metadata if available
            val enrichedRecord = if (matchedOrder != null) {
                record.copy(
                    orderNo = matchedOrder.order_form_number?.ifBlank { cleanOrderNo } ?: cleanOrderNo,
                    copy = normalizedCopy,
                    orderId = matchedOrder.id ?: record.orderId,
                    customer = matchedOrder.customer_name?.ifBlank { record.customer } ?: record.customer,
                    district = matchedOrder.customer_district?.ifBlank { record.district } ?: record.district,
                    deliveryDate = matchedOrder.delivery_date?.ifBlank { record.deliveryDate } ?: record.deliveryDate,
                    deliveryType = matchedOrder.delivery_type?.ifBlank { record.deliveryType } ?: record.deliveryType,
                    salesPerson = matchedOrder.sales_person_name?.ifBlank { record.salesPerson } ?: record.salesPerson,
                    size = matchedOrder.table_size?.ifBlank { record.size } ?: record.size,
                    topColour = matchedOrder.colour?.ifBlank { record.topColour } ?: record.topColour,
                    qty = matchedOrder.quantity ?: record.qty,
                    totalUnits = matchedOrder.quantity ?: record.totalUnits,
                    notes = ""
                )
            } else {
                record.copy(orderNo = cleanOrderNo, copy = normalizedCopy, notes = "")
            }

            // 2. Resolve driver UUID from selected vehicle or `public.profiles`
            var deliveryPersonId: String? = resolveKnownDriverUuid(effectiveVehicle)
            if (deliveryPersonId == null && !effectiveVehicle.startsWith("Pick Me", ignoreCase = true)) {
                val candidateName = if (effectiveVehicle.contains("(") && effectiveVehicle.contains(")")) {
                    effectiveVehicle.substringAfter("(").substringBefore(")").trim()
                } else {
                    effectiveVehicle.trim()
                }
                if (candidateName.isNotEmpty()) {
                    try {
                        val profiles = api.getProfileByName("eq.$candidateName")
                        if (profiles.isNotEmpty()) {
                            deliveryPersonId = profiles[0].id
                        }
                    } catch (_: Exception) {}
                }
            }

            val resolvedOrderId: String? = matchedOrder?.id
            val isReturnMode = enrichedRecord.mode.equals("RETURN", ignoreCase = true)
            val isGatePass = isGatePassCopy(normalizedCopy)

            if (resolvedOrderId.isNullOrBlank()) {
                val err = "Order #${enrichedRecord.orderNo} not found in factory database."
                enrichedRecord.syncStatus = "FAILED"
                enrichedRecord.errorMessage = err
                dao.updateRecord(enrichedRecord)
                return@withContext SyncStatusResult.Failed(err)
            }

            // 3. Query `public.transport` for existing records of this order to enforce strict duplicate prevention
            val existingTransports = api.getTransportByOrderId("eq.$resolvedOrderId")
            val matchingTransport = existingTransports.firstOrNull { row ->
                row.copy_name?.trim().equals(normalizedCopy, ignoreCase = true)
            } ?: existingTransports.firstOrNull { row ->
                row.copy_name.isNullOrBlank() && isGatePass
            }

            if (!isReturnMode) {
                // DISPATCH MODE: Every record (order_id + copy_name) can only be dispatched ONCE.
                // Self-healing check: If a transport record exists for GATE PASS from an earlier unauthenticated scan
                // where `public.orders` was left in `pending` or without `delivery_person_id`, repair `public.orders` first!
                if (matchingTransport != null) {
                    val orderNeedsAssignmentRepair = isGatePass && (
                        !matchedOrder.status.equals("assigned", ignoreCase = true) ||
                            !matchedOrder.delivery_status.equals("assigned", ignoreCase = true) ||
                            (deliveryPersonId != null && matchedOrder.delivery_person_id != deliveryPersonId)
                        )

                    if (orderNeedsAssignmentRepair) {
                        val patchErr = assignOrderToDriverInSupabase(
                            api = api,
                            resolvedOrderId = resolvedOrderId,
                            cleanOrderNo = enrichedRecord.orderNo,
                            deliveryPersonId = deliveryPersonId,
                            isoTime = isoTime
                        )
                        if (patchErr != null) {
                            enrichedRecord.syncStatus = "FAILED"
                            enrichedRecord.errorMessage = patchErr
                            dao.updateRecord(enrichedRecord)
                            return@withContext SyncStatusResult.Failed(patchErr)
                        }
                        if (!matchingTransport.id.isNullOrBlank()) {
                            try {
                                api.updateTransportById(
                                    idFilter = "eq.${matchingTransport.id}",
                                    body = TransportPatchBody(
                                        transport_mode = effectiveVehicle,
                                        loaded_at = isoTime,
                                        loaded_by = deliveryPersonId,
                                        copy_name = normalizedCopy,
                                        transport_status = TRANSPORT_STATUS_DISPATCHED
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                        enrichedRecord.syncStatus = "SYNCED"
                        enrichedRecord.errorMessage = null
                        dao.updateRecord(enrichedRecord)
                        return@withContext SyncStatusResult.Success
                    }

                    val existingStatus = matchingTransport.transport_status?.trim()?.ifBlank { TRANSPORT_STATUS_DISPATCHED }
                        ?: TRANSPORT_STATUS_DISPATCHED
                    val dupMsg = "Duplicate Denied: $normalizedCopy for Order #${enrichedRecord.orderNo} was already $existingStatus."
                    enrichedRecord.syncStatus = "DUPLICATE"
                    enrichedRecord.errorMessage = dupMsg
                    dao.updateRecord(enrichedRecord)
                    return@withContext SyncStatusResult.Duplicate(dupMsg)
                }

                // 4a. ONLY GATE PASS QR code assigns the order to the driver and marks it "assigned" in `public.orders`.
                //     Never write to `note` or any other column in `public.orders`.
                if (isGatePass) {
                    val patchErr = assignOrderToDriverInSupabase(
                        api = api,
                        resolvedOrderId = resolvedOrderId,
                        cleanOrderNo = enrichedRecord.orderNo,
                        deliveryPersonId = deliveryPersonId,
                        isoTime = isoTime
                    )
                    if (patchErr != null) {
                        enrichedRecord.syncStatus = "FAILED"
                        enrichedRecord.errorMessage = patchErr
                        dao.updateRecord(enrichedRecord)
                        return@withContext SyncStatusResult.Failed(patchErr)
                    }
                }

                // 4b. Record copy_name and transport_status = "dispatched" in `public.transport`
                val insertResp = api.insertTransport(
                    TransportInsertBody(
                        order_id = resolvedOrderId,
                        transport_mode = effectiveVehicle,
                        loaded_at = isoTime,
                        loaded_by = deliveryPersonId,
                        copy_name = normalizedCopy,
                        transport_status = TRANSPORT_STATUS_DISPATCHED
                    )
                )
                if (!insertResp.isSuccessful) {
                    val err = "Supabase Transport HTTP ${insertResp.code()}: ${insertResp.message()}"
                    enrichedRecord.syncStatus = "FAILED"
                    enrichedRecord.errorMessage = err
                    dao.updateRecord(enrichedRecord)
                    return@withContext SyncStatusResult.Failed(err)
                }
            } else {
                // RETURN MODE: Every record (order_id + copy_name) can be returned ONCE
                if (matchingTransport != null &&
                    matchingTransport.transport_status?.trim().equals(TRANSPORT_STATUS_RETURNED, ignoreCase = true)
                ) {
                    val dupMsg = "Duplicate Denied: $normalizedCopy for Order #${enrichedRecord.orderNo} was already returned."
                    enrichedRecord.syncStatus = "DUPLICATE"
                    enrichedRecord.errorMessage = dupMsg
                    dao.updateRecord(enrichedRecord)
                    return@withContext SyncStatusResult.Duplicate(dupMsg)
                }

                if (matchingTransport != null && !matchingTransport.id.isNullOrBlank()) {
                    val updateResp = api.updateTransportById(
                        idFilter = "eq.${matchingTransport.id}",
                        body = TransportPatchBody(
                            transport_mode = effectiveVehicle,
                            loaded_at = isoTime,
                            loaded_by = deliveryPersonId,
                            copy_name = normalizedCopy,
                            transport_status = TRANSPORT_STATUS_RETURNED
                        )
                    )
                    if (!updateResp.isSuccessful) {
                        val err = "Supabase Transport HTTP ${updateResp.code()}: ${updateResp.message()}"
                        enrichedRecord.syncStatus = "FAILED"
                        enrichedRecord.errorMessage = err
                        dao.updateRecord(enrichedRecord)
                        return@withContext SyncStatusResult.Failed(err)
                    }
                } else {
                    val insertResp = api.insertTransport(
                        TransportInsertBody(
                            order_id = resolvedOrderId,
                            transport_mode = effectiveVehicle,
                            loaded_at = isoTime,
                            loaded_by = deliveryPersonId,
                            copy_name = normalizedCopy,
                            transport_status = TRANSPORT_STATUS_RETURNED
                        )
                    )
                    if (!insertResp.isSuccessful) {
                        val err = "Supabase Transport HTTP ${insertResp.code()}: ${insertResp.message()}"
                        enrichedRecord.syncStatus = "FAILED"
                        enrichedRecord.errorMessage = err
                        dao.updateRecord(enrichedRecord)
                        return@withContext SyncStatusResult.Failed(err)
                    }
                }
            }

            // Optional audit log insert if `public.dispatch_return_logs` exists
            try {
                api.insertDispatchReturnLog(
                    DispatchReturnLogInsertBody(
                        order_id = resolvedOrderId,
                        order_form_number = enrichedRecord.orderNo,
                        operation_mode = enrichedRecord.mode,
                        vehicle_number = effectiveVehicle,
                        scanner_name = enrichedRecord.scannerName,
                        device_id = enrichedRecord.deviceId,
                        restocked = enrichedRecord.restocked,
                        copy_type = normalizedCopy,
                        customer_name = enrichedRecord.customer,
                        customer_district = enrichedRecord.district,
                        table_size = enrichedRecord.size,
                        colour = enrichedRecord.topColour,
                        quantity = enrichedRecord.qty,
                        notes = null
                    )
                )
            } catch (_: Exception) {
                // Ignore if optional dispatch_return_logs table is not present
            }

            enrichedRecord.syncStatus = "SYNCED"
            enrichedRecord.errorMessage = null
            dao.updateRecord(enrichedRecord)
            return@withContext SyncStatusResult.Success
        } catch (e: Exception) {
            val err = "Offline: ${e.localizedMessage ?: "Network error"}"
            record.syncStatus = "PENDING"
            record.errorMessage = err
            dao.updateRecord(record)
            return@withContext SyncStatusResult.Offline(err)
        }
    }

    // Sync all pending records in background
    suspend fun syncPendingRecords(): SyncSummary = withContext(Dispatchers.IO) {
        val pendingList = dao.getPendingRecords()
        if (pendingList.isEmpty()) {
            return@withContext SyncSummary(0, 0, 0, 0, "No pending items to sync.")
        }

        var synced = 0
        var duplicates = 0
        var failures = 0

        for (record in pendingList) {
            val result = syncRecord(record)
            when (result) {
                is SyncStatusResult.Success -> synced++
                is SyncStatusResult.Duplicate -> duplicates++
                else -> failures++
            }
        }

        SyncSummary(
            total = pendingList.size,
            synced = synced,
            duplicates = duplicates,
            failures = failures,
            message = "Supabase Batch Sync complete. Synced: $synced, Duplicates: $duplicates, Pending/Failed: $failures."
        )
    }
}

sealed class SyncStatusResult {
    object Success : SyncStatusResult()
    data class Duplicate(val message: String) : SyncStatusResult()
    data class Failed(val message: String) : SyncStatusResult()
    data class Offline(val message: String) : SyncStatusResult()
}

data class SyncSummary(
    val total: Int,
    val synced: Int,
    val duplicates: Int,
    val failures: Int,
    val message: String
)
