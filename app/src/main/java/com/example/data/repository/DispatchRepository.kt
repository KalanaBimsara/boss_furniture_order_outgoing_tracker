package com.example.data.repository

import com.example.data.api.DispatchReturnLogInsertBody
import com.example.data.api.DispatchReturnLogNotesPatchBody
import com.example.data.api.DispatchReturnLogResponse
import com.example.data.api.OrderPatchBody
import com.example.data.api.SupabaseApi
import com.example.data.api.SupabaseApiClient
import com.example.data.api.SupabaseOrder
import com.example.data.api.SupabaseOrderTable
import com.example.data.api.TransportInsertBody
import com.example.data.api.TransportPatchBody
import com.example.data.local.ScannedRecord
import com.example.data.local.ScannedRecordDao
import com.example.data.model.QrPayload
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
        const val DRIVER_UUID_RATHNAYAKE = "574e0364-858b-43e8-bf5f-5f09fc7e30cf"
        const val DRIVER_UUID_LASANTHA = "149a7d44-326a-48fe-b970-8ac196352779"

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

        fun normalizeFeatureToken(value: String?): String {
            return value
                ?.trim()
                ?.lowercase(Locale.US)
                ?.replace(Regex("\\s+"), " ")
                .orEmpty()
        }

        /**
         * Builds the combined 7-feature uniqueness key for a table within an order:
         * (orderNo, size, topColour, frameColour, legSize, legHeight, legShape)
         */
        fun buildTableFeatureSignature(
            orderNo: String,
            size: String = "",
            topColour: String = "",
            frameColour: String = "",
            legSize: String = "",
            legHeight: String = "",
            legShape: String = ""
        ): String {
            val normOrder = normalizeOrderNo(orderNo).uppercase(Locale.US)
            val normSize = normalizeFeatureToken(size)
            val normTop = normalizeFeatureToken(topColour)
            val normFrame = normalizeFeatureToken(frameColour)
            val normLegSize = normalizeFeatureToken(legSize)
            val normLegHeight = normalizeFeatureToken(legHeight)
            val normLegShape = normalizeFeatureToken(legShape)
            return "$normOrder|$normSize|$normTop|$normFrame|$normLegSize|$normLegHeight|$normLegShape"
        }

        fun hasAnyTableFeatures(
            size: String = "",
            topColour: String = "",
            frameColour: String = "",
            legSize: String = "",
            legHeight: String = "",
            legShape: String = ""
        ): Boolean {
            return size.isNotBlank() ||
                topColour.isNotBlank() ||
                frameColour.isNotBlank() ||
                legSize.isNotBlank() ||
                legHeight.isNotBlank() ||
                legShape.isNotBlank()
        }

        fun formatTableFeatureDescriptor(
            size: String,
            topColour: String = "",
            frameColour: String = "",
            legSize: String = "",
            legHeight: String = "",
            legShape: String = ""
        ): String {
            return listOfNotNull(
                size.trim().ifBlank { null },
                topColour.trim().ifBlank { null }?.let { "top:$it" },
                frameColour.trim().ifBlank { null }?.let { "leg:$it" },
                legSize.trim().ifBlank { null },
                legHeight.trim().ifBlank { null }?.let { "H:$it" },
                legShape.trim().ifBlank { null }
            ).joinToString(" · ")
        }

        fun buildTableFeatureNotes(record: ScannedRecord): String {
            val sig = buildTableFeatureSignature(
                orderNo = record.orderNo,
                size = record.size,
                topColour = record.topColour,
                frameColour = record.frameColour,
                legSize = record.legSize,
                legHeight = record.legHeight,
                legShape = record.legShape
            )
            val base = "FEATURE_KEY=$sig;size=${record.size.trim()};topColour=${record.topColour.trim()};frameColour=${record.frameColour.trim()};legSize=${record.legSize.trim()};legHeight=${record.legHeight.trim()};legShape=${record.legShape.trim()};qty=${record.qty}"
            val cleanUserNote = record.notes.trim().replace(";", ",").replace("=", ":")
            return if (cleanUserNote.isNotEmpty()) {
                "$base;userNote=$cleanUserNote"
            } else {
                base
            }
        }

        fun parseNotesKeyValues(notes: String?): Map<String, String> {
            if (notes.isNullOrBlank()) return emptyMap()
            return notes.split(";")
                .mapNotNull { entry ->
                    val idx = entry.indexOf('=')
                    if (idx > 0) {
                        entry.substring(0, idx).trim() to entry.substring(idx + 1).trim()
                    } else null
                }
                .toMap()
        }

        fun extractUserNote(notes: String?): String {
            if (notes.isNullOrBlank()) return ""
            val parsed = parseNotesKeyValues(notes)
            if (parsed.containsKey("userNote")) {
                return parsed["userNote"].orEmpty()
            }
            return if (!notes.contains("FEATURE_KEY=")) notes.trim() else ""
        }

        fun matchesRemoteLogTableFeatures(
            log: DispatchReturnLogResponse,
            record: ScannedRecord,
            normalizedCopy: String
        ): Boolean {
            val logCopy = normalizeCopyType(log.copy_type)
            if (logCopy != normalizedCopy) return false

            val targetSig = buildTableFeatureSignature(
                orderNo = record.orderNo,
                size = record.size,
                topColour = record.topColour,
                frameColour = record.frameColour,
                legSize = record.legSize,
                legHeight = record.legHeight,
                legShape = record.legShape
            )

            val parsedNotes = parseNotesKeyValues(log.notes)
            val savedFeatureKey = parsedNotes["FEATURE_KEY"]
            if (!savedFeatureKey.isNullOrBlank()) {
                return savedFeatureKey.equals(targetSig, ignoreCase = true)
            }

            // Fallback for older logs without FEATURE_KEY in notes:
            val logSize = normalizeFeatureToken(log.table_size)
            val recSize = normalizeFeatureToken(record.size)
            if (logSize != recSize) return false

            val logColour = normalizeFeatureToken(log.colour)
            val recTopColour = normalizeFeatureToken(record.topColour)
            if (logColour.isNotEmpty() && recTopColour.isNotEmpty() && logColour != recTopColour) {
                return false
            }

            return true
        }

        fun buildDuplicateKey(
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
        ): String {
            val normalizedMode = mode.trim().uppercase(Locale.US)
            val normalizedOrder = normalizeOrderNo(orderNo).uppercase(Locale.US)
            val normalizedCopy = normalizeCopyType(copyName)
            return if (hasAnyTableFeatures(size, topColour, frameColour, legSize, legHeight, legShape)) {
                val sig = buildTableFeatureSignature(
                    orderNo = normalizedOrder,
                    size = size,
                    topColour = topColour,
                    frameColour = frameColour,
                    legSize = legSize,
                    legHeight = legHeight,
                    legShape = legShape
                )
                "$normalizedMode|$sig|$normalizedCopy"
            } else {
                "$normalizedMode|$normalizedOrder|$normalizedCopy|$tableIndex"
            }
        }

        fun hasSameTableFeatures(
            record: ScannedRecord,
            orderNo: String,
            copyName: String,
            size: String = "",
            topColour: String = "",
            frameColour: String = "",
            legSize: String = "",
            legHeight: String = "",
            legShape: String = "",
            tableIndex: Int = 1
        ): Boolean {
            if (!normalizeOrderNo(record.orderNo).equals(normalizeOrderNo(orderNo), ignoreCase = true)) {
                return false
            }
            if (normalizeCopyType(record.copy) != normalizeCopyType(copyName)) {
                return false
            }
            val incomingHasFeatures = hasAnyTableFeatures(size, topColour, frameColour, legSize, legHeight, legShape)
            val existingHasFeatures = hasAnyTableFeatures(
                record.size,
                record.topColour,
                record.frameColour,
                record.legSize,
                record.legHeight,
                record.legShape
            )

            return if (incomingHasFeatures || existingHasFeatures) {
                val sigA = buildTableFeatureSignature(
                    orderNo = record.orderNo,
                    size = record.size,
                    topColour = record.topColour,
                    frameColour = record.frameColour,
                    legSize = record.legSize,
                    legHeight = record.legHeight,
                    legShape = record.legShape
                )
                val sigB = buildTableFeatureSignature(
                    orderNo = orderNo,
                    size = size,
                    topColour = topColour,
                    frameColour = frameColour,
                    legSize = legSize,
                    legHeight = legHeight,
                    legShape = legShape
                )
                sigA == sigB
            } else {
                record.tableIndex == tableIndex
            }
        }

        fun resolveKnownDriverUuid(vehicleOption: String): String? {
            val normalized = vehicleOption.trim()
            return when {
                normalized.contains("DAC-4912", ignoreCase = true) ||
                    normalized.contains("DAC 4912", ignoreCase = true) ||
                    normalized.contains("Rathnayake", ignoreCase = true) -> DRIVER_UUID_RATHNAYAKE
                normalized.contains("DAE-4717", ignoreCase = true) ||
                    normalized.contains("DAE 4717", ignoreCase = true) ||
                    normalized.contains("DAE-4217", ignoreCase = true) ||
                    normalized.contains("DAE 4217", ignoreCase = true) ||
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
     * Updates the user note for a ScannedRecord locally in Room and syncs it to `public.dispatch_return_logs` in Supabase.
     * Never writes to `public.orders.note`.
     */
    suspend fun updateRecordNote(record: ScannedRecord, newNote: String): ScannedRecord = withContext(Dispatchers.IO) {
        val cleanedNote = newNote.trim()
        val updatedRecord = record.copy(notes = cleanedNote)
        dao.updateRecord(updatedRecord)

        val supabaseUrl = settingsRepo?.getSupabaseUrl() ?: SettingsRepository.DEFAULT_SUPABASE_URL
        val supabaseKey = settingsRepo?.getSupabaseKey() ?: SettingsRepository.DEFAULT_SUPABASE_KEY
        try {
            val api = apiOverride ?: SupabaseApiClient.getApiService(supabaseUrl, supabaseKey)
            val resolvedOrderId = updatedRecord.orderId.takeIf { it.isNotBlank() }
            val existingLogs = if (!resolvedOrderId.isNullOrBlank()) {
                api.getDispatchReturnLogsByOrderId("eq.$resolvedOrderId")
            } else {
                api.getRecentDispatchReturnLogs(limit = 50)
            }
            val normalizedCopy = normalizeCopyType(updatedRecord.copy)
            val targetLog = existingLogs.firstOrNull { log ->
                log.operation_mode.equals(updatedRecord.mode, ignoreCase = true) &&
                    matchesRemoteLogTableFeatures(log, updatedRecord, normalizedCopy)
            } ?: existingLogs.firstOrNull { log ->
                log.order_form_number.equals(updatedRecord.orderNo, ignoreCase = true) &&
                    log.operation_mode.equals(updatedRecord.mode, ignoreCase = true)
            }

            if (targetLog?.id != null) {
                api.updateDispatchReturnLogNotesById(
                    idFilter = "eq.${targetLog.id}",
                    body = DispatchReturnLogNotesPatchBody(notes = buildTableFeatureNotes(updatedRecord))
                )
            }
        } catch (_: Exception) {
            // Local Room update already succeeded
        }
        updatedRecord
    }

    /**
     * Fetches order details and any multi-table specifications (`public.order_tables`) from Supabase
     * for Manual Order Entry so the user can review and edit details in the confirmation popup before
     * marking the order as Dispatched or Returned.
     */
    suspend fun fetchOrderDetailsForManualEntry(
        rawOrderNo: String,
        defaultCopy: String = COPY_GATE_PASS
    ): ManualOrderFetchResult = withContext(Dispatchers.IO) {
        val cleanOrderNo = normalizeOrderNo(rawOrderNo)
        val normalizedCopy = normalizeCopyType(defaultCopy)
        val supabaseUrl = settingsRepo?.getSupabaseUrl() ?: SettingsRepository.DEFAULT_SUPABASE_URL
        val supabaseKey = settingsRepo?.getSupabaseKey() ?: SettingsRepository.DEFAULT_SUPABASE_KEY

        try {
            val api = apiOverride ?: SupabaseApiClient.getApiService(supabaseUrl, supabaseKey)
            var matchedOrder: SupabaseOrder? = null
            if (cleanOrderNo.isNotEmpty()) {
                val found = api.getOrderByOrderNumber("eq.$cleanOrderNo")
                if (found.isNotEmpty()) {
                    matchedOrder = found.first()
                }
            }
            if (matchedOrder == null && cleanOrderNo.length == 36 && cleanOrderNo.contains("-")) {
                val foundById = api.getOrderById("eq.$cleanOrderNo")
                if (foundById.isNotEmpty()) {
                    matchedOrder = foundById.first()
                }
            }

            if (matchedOrder != null) {
                val orderId = matchedOrder.id.orEmpty()
                val orderTables = if (orderId.isNotBlank()) {
                    try {
                        api.getOrderTablesByOrderId("eq.$orderId")
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                val firstTable = orderTables.firstOrNull()
                val totalUnits = if (orderTables.isNotEmpty()) {
                    orderTables.sumOf { (it.quantity ?: 1).coerceAtLeast(1) }
                } else {
                    (matchedOrder.quantity ?: 1).coerceAtLeast(1)
                }

                val payload = QrPayload(
                    orderNo = matchedOrder.order_form_number?.ifBlank { cleanOrderNo } ?: cleanOrderNo,
                    orderId = orderId.ifBlank { cleanOrderNo },
                    customer = matchedOrder.customer_name?.ifBlank { "Factory Order" } ?: "Factory Order",
                    district = matchedOrder.customer_district.orEmpty(),
                    deliveryDate = matchedOrder.delivery_date.orEmpty(),
                    deliveryType = matchedOrder.delivery_type.orEmpty(),
                    salesPerson = matchedOrder.sales_person_name.orEmpty(),
                    totalUnits = totalUnits,
                    copy = normalizedCopy,
                    tableIndex = 1,
                    tableCount = orderTables.size.coerceAtLeast(1),
                    size = firstTable?.table_size?.ifBlank { matchedOrder.table_size.orEmpty() }
                        ?: matchedOrder.table_size.orEmpty(),
                    topColour = firstTable?.top_colour?.ifBlank { matchedOrder.colour.orEmpty() }
                        ?: matchedOrder.colour.orEmpty(),
                    frameColour = firstTable?.frame_colour.orEmpty(),
                    qty = firstTable?.quantity ?: matchedOrder.quantity ?: 1,
                    legSize = firstTable?.leg_size.orEmpty(),
                    legShape = firstTable?.leg_shape.orEmpty(),
                    legHeight = firstTable?.leg_height.orEmpty()
                )
                return@withContext ManualOrderFetchResult(
                    payload = payload,
                    tables = orderTables,
                    foundInDatabase = true
                )
            }
        } catch (_: Exception) {
            // Fall back to local cache or blank editable fields below
        }

        val localMatch = try {
            dao.getRecordsByOrderNo(cleanOrderNo).firstOrNull()
        } catch (_: Exception) {
            null
        }

        if (localMatch != null) {
            val payload = QrPayload(
                orderNo = cleanOrderNo,
                orderId = localMatch.orderId.ifBlank { cleanOrderNo },
                customer = localMatch.customer.ifBlank { "Factory Order" },
                district = localMatch.district,
                deliveryDate = localMatch.deliveryDate,
                deliveryType = localMatch.deliveryType,
                salesPerson = localMatch.salesPerson,
                totalUnits = localMatch.totalUnits.coerceAtLeast(1),
                copy = normalizedCopy,
                tableIndex = 1,
                tableCount = localMatch.tableCount.coerceAtLeast(1),
                size = localMatch.size,
                topColour = localMatch.topColour,
                frameColour = localMatch.frameColour,
                qty = localMatch.qty.coerceAtLeast(1),
                legSize = localMatch.legSize,
                legShape = localMatch.legShape,
                legHeight = localMatch.legHeight
            )
            return@withContext ManualOrderFetchResult(
                payload = payload,
                tables = emptyList(),
                foundInDatabase = true
            )
        }

        ManualOrderFetchResult(
            payload = QrPayload(
                orderNo = cleanOrderNo,
                orderId = cleanOrderNo,
                customer = "",
                district = "",
                copy = normalizedCopy,
                qty = 1,
                totalUnits = 1,
                tableIndex = 1,
                tableCount = 1
            ),
            tables = emptyList(),
            foundInDatabase = false
        )
    }

    /**
     * Checks local SQLite history for duplicate Dispatch or Return using the combined 7-feature key:
     * (orderNo, size, topColour, frameColour, legSize, legHeight, legShape) + copyName.
     * Every distinct table specification can be dispatched once and returned once.
     */
    suspend fun checkLocalDuplicate(
        mode: String,
        orderNo: String,
        copyName: String,
        tableIndex: Int = 1,
        excludeId: Long = 0L,
        size: String = "",
        topColour: String = "",
        frameColour: String = "",
        legSize: String = "",
        legHeight: String = "",
        legShape: String = ""
    ): String? {
        val normalizedOrder = normalizeOrderNo(orderNo)
        if (normalizedOrder.isEmpty()) return null
        val normalizedCopy = normalizeCopyType(copyName)
        val isReturn = mode.equals("RETURN", ignoreCase = true)

        val existingForOrder = dao.getRecordsByOrderNo(normalizedOrder).filter { record ->
            record.id != excludeId &&
                record.syncStatus != "FAILED" &&
                record.syncStatus != "DUPLICATE" &&
                hasSameTableFeatures(
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

        val featureSummary = formatTableFeatureDescriptor(
            size = size,
            topColour = topColour,
            frameColour = frameColour,
            legSize = legSize,
            legHeight = legHeight,
            legShape = legShape
        )
        val labelSuffix = if (featureSummary.isNotBlank()) " ($featureSummary)" else ""

        return if (!isReturn) {
            val alreadyDispatched = existingForOrder.firstOrNull {
                it.mode.equals("DISPATCH", ignoreCase = true) || it.mode.equals("RETURN", ignoreCase = true)
            }
            if (alreadyDispatched != null) {
                "Duplicate Denied: $normalizedCopy for Order #$normalizedOrder$labelSuffix was already ${if (alreadyDispatched.mode.equals("RETURN", ignoreCase = true)) "returned" else "dispatched"}."
            } else {
                null
            }
        } else {
            val alreadyReturned = existingForOrder.firstOrNull {
                it.mode.equals("RETURN", ignoreCase = true)
            }
            if (alreadyReturned != null) {
                "Duplicate Denied: $normalizedCopy for Order #$normalizedOrder$labelSuffix was already returned."
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
        isoTime: String,
        driverNameHint: String? = null
    ): String? {
        val patchBody = OrderPatchBody(
            delivery_person_id = deliveryPersonId,
            status = "assigned",
            delivery_status = "assigned",
            updated_at = isoTime
        )

        var patchResponse = api.updateOrderStatusById("eq.$resolvedOrderId", patchBody)
        if (!patchResponse.isSuccessful && patchResponse.code() == 409 && deliveryPersonId != null) {
            // Foreign key constraint on delivery_person_id: try resolving driver UUID from public.profiles by name,
            // or fall back to marking the order assigned without delivery_person_id so sync never fails with 409.
            var fallbackDriverId: String? = null
            if (!driverNameHint.isNullOrBlank()) {
                try {
                    val profiles = api.getProfileByName("ilike.*${driverNameHint.trim()}*")
                    if (profiles.isNotEmpty() && profiles[0].id != deliveryPersonId) {
                        fallbackDriverId = profiles[0].id
                    }
                } catch (_: Exception) {}
            }
            val retryBody = patchBody.copy(delivery_person_id = fallbackDriverId)
            val retryResp = api.updateOrderStatusById("eq.$resolvedOrderId", retryBody)
            if (retryResp.isSuccessful) {
                patchResponse = retryResp
            } else if (fallbackDriverId != null) {
                val nullDriverResp = api.updateOrderStatusById("eq.$resolvedOrderId", patchBody.copy(delivery_person_id = null))
                if (nullDriverResp.isSuccessful) {
                    patchResponse = nullDriverResp
                }
            }
        }

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
     * 2. Preserves the exact scanned QR table size (`size`), quantity (`qty`), and table features
     *    (`topColour`, `frameColour`, `legSize`, `legHeight`, `legShape`) without overwriting them
     *    with parent order summary fields.
     * 3. Enforces duplicate prevention using the combined 7-feature key:
     *    `(orderNo, size, topColour, frameColour, legSize, legHeight, legShape)` + `copy_name`:
     *    - When the scan contains table features, checks `public.dispatch_return_logs` for an existing scan
     *      matching all 7 table features so multi-table orders (e.g., #3535, #4058) can dispatch each distinct table.
     *    - When the scan is a manual order entry without table features, checks `public.transport` by `(order_id, copy_name)`.
     * 4. In DISPATCH mode:
     *    - ONLY when the scanned copy is `GATE PASS`:
     *      - Assigns the order to the selected driver (`delivery_person_id`) and sets `status = "assigned"`, `delivery_status = "assigned"` in `public.orders`.
     *      - Never writes to `note` or any other order columns in `public.orders`.
     *    - Records `copy_name` and `transport_status = "dispatched"` in `public.transport`.
     *    - Records ONLY the dispatched table's size (`table_size`), `quantity`, and feature key in `public.dispatch_return_logs`.
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

            val rawEffectiveVehicle = record.vehicleNumber.trim().ifBlank {
                settingsRepo?.getSelectedVehicleOption() ?: "DAC-4912 (Rathnayake)"
            }
            val effectiveVehicle = if (
                rawEffectiveVehicle.contains("DAE-4217", ignoreCase = true) ||
                rawEffectiveVehicle.contains("DAE 4217", ignoreCase = true)
            ) {
                "DAE-4717 (Lasantha)"
            } else {
                rawEffectiveVehicle
            }
            val normalizedCopy = normalizeCopyType(record.copy)
            val isManualOrderOnly = record.rawPayload.startsWith("MANUAL_") && record.size.isBlank()

            // IMPORTANT: Always preserve the scanned QR's own table size, qty, and table features!
            // Only fall back to matchedOrder.table_size / colour / quantity if the scan was a manual entry with blank size.
            val enrichedRecord = if (matchedOrder != null) {
                val preservedSize = record.size.ifBlank { matchedOrder.table_size.orEmpty() }
                val preservedTopColour = record.topColour.ifBlank { matchedOrder.colour.orEmpty() }
                val preservedQty = if (isManualOrderOnly) {
                    matchedOrder.quantity ?: record.qty.coerceAtLeast(1)
                } else {
                    record.qty.coerceAtLeast(1)
                }
                val preservedTotalUnits = if (record.totalUnits > 1) {
                    record.totalUnits
                } else {
                    matchedOrder.quantity ?: record.totalUnits.coerceAtLeast(1)
                }
                val resolvedOrderNum = matchedOrder.order_form_number?.ifBlank { cleanOrderNo } ?: cleanOrderNo

                record.copy(
                    orderNo = resolvedOrderNum,
                    copy = normalizedCopy,
                    vehicleNumber = effectiveVehicle,
                    orderId = matchedOrder.id ?: record.orderId,
                    customer = record.customer.takeIf {
                        it.isNotBlank() &&
                            !it.equals("Factory Order", ignoreCase = true) &&
                            !it.equals("Factory Dispatch Order", ignoreCase = true) &&
                            !it.equals("Return Item", ignoreCase = true)
                    } ?: matchedOrder.customer_name?.ifBlank { record.customer } ?: record.customer,
                    district = record.district.ifBlank { matchedOrder.customer_district.orEmpty() },
                    deliveryDate = record.deliveryDate.ifBlank { matchedOrder.delivery_date.orEmpty() },
                    deliveryType = record.deliveryType.ifBlank { matchedOrder.delivery_type.orEmpty() },
                    salesPerson = record.salesPerson.ifBlank { matchedOrder.sales_person_name.orEmpty() },
                    size = preservedSize,
                    topColour = preservedTopColour,
                    frameColour = record.frameColour,
                    qty = preservedQty,
                    totalUnits = preservedTotalUnits,
                    legSize = record.legSize,
                    legShape = record.legShape,
                    legHeight = record.legHeight,
                    duplicateKey = buildDuplicateKey(
                        mode = record.mode,
                        orderNo = resolvedOrderNum,
                        copyName = normalizedCopy,
                        tableIndex = record.tableIndex,
                        size = preservedSize,
                        topColour = preservedTopColour,
                        frameColour = record.frameColour,
                        legSize = record.legSize,
                        legHeight = record.legHeight,
                        legShape = record.legShape
                    ),
                    notes = record.notes.trim()
                )
            } else {
                record.copy(orderNo = cleanOrderNo, copy = normalizedCopy, vehicleNumber = effectiveVehicle, notes = record.notes.trim())
            }

            // 2. Resolve driver UUID from known company vehicles first (`574e0364-...` for DAC-4912 Rathnayake,
            //    `149a7d44-...` for DAE-4717 Lasantha), falling back to `public.profiles` for custom drivers
            val candidateDriverName = if (effectiveVehicle.contains("(") && effectiveVehicle.contains(")")) {
                effectiveVehicle.substringAfter("(").substringBefore(")").trim()
            } else {
                effectiveVehicle.trim()
            }
            var deliveryPersonId: String? = resolveKnownDriverUuid(effectiveVehicle)
            if (deliveryPersonId == null && !effectiveVehicle.startsWith("Pick Me", ignoreCase = true)) {
                if (candidateDriverName.isNotEmpty()) {
                    try {
                        val profiles = api.getProfileByName("eq.$candidateDriverName")
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

            // 3. Query `public.transport` and `public.dispatch_return_logs` for existing records of this order
            val existingTransports = api.getTransportByOrderId("eq.$resolvedOrderId")
            val matchingTransport = existingTransports.firstOrNull { row ->
                row.copy_name?.trim().equals(normalizedCopy, ignoreCase = true)
            } ?: existingTransports.firstOrNull { row ->
                row.copy_name.isNullOrBlank() && isGatePass
            }

            val hasScannedTableFeatures = !isManualOrderOnly && hasAnyTableFeatures(
                size = record.size,
                topColour = record.topColour,
                frameColour = record.frameColour,
                legSize = record.legSize,
                legHeight = record.legHeight,
                legShape = record.legShape
            )

            val existingOrderLogs: List<DispatchReturnLogResponse> = if (hasScannedTableFeatures) {
                try {
                    api.getDispatchReturnLogsByOrderId("eq.$resolvedOrderId")
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val matchingTableLogs = if (hasScannedTableFeatures) {
                existingOrderLogs.filter { log ->
                    matchesRemoteLogTableFeatures(log, enrichedRecord, normalizedCopy)
                }
            } else {
                emptyList()
            }

            val featureDesc = formatTableFeatureDescriptor(
                size = enrichedRecord.size,
                topColour = enrichedRecord.topColour,
                frameColour = enrichedRecord.frameColour,
                legSize = enrichedRecord.legSize,
                legHeight = enrichedRecord.legHeight,
                legShape = enrichedRecord.legShape
            )
            val descSuffix = if (featureDesc.isNotBlank()) " ($featureDesc)" else ""

            if (!isReturnMode) {
                // DISPATCH MODE:
                // When scanning a QR with table features, check if THIS exact table specification
                // (orderNo + size + topColour + frameColour + legSize + legHeight + legShape + copy)
                // was already dispatched in `public.dispatch_return_logs`.
                val alreadyDispatchedForTable = if (hasScannedTableFeatures) {
                    matchingTableLogs.firstOrNull {
                        it.operation_mode.equals("DISPATCH", ignoreCase = true) ||
                            it.operation_mode.equals("RETURN", ignoreCase = true)
                    } != null
                } else {
                    matchingTransport != null
                }

                if (alreadyDispatchedForTable) {
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
                            isoTime = isoTime,
                            driverNameHint = candidateDriverName
                        )
                        if (patchErr != null) {
                            enrichedRecord.syncStatus = "FAILED"
                            enrichedRecord.errorMessage = patchErr
                            dao.updateRecord(enrichedRecord)
                            return@withContext SyncStatusResult.Failed(patchErr)
                        }
                        if (matchingTransport != null && !matchingTransport.id.isNullOrBlank()) {
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

                    val existingStatus = matchingTransport?.transport_status?.trim()?.ifBlank { TRANSPORT_STATUS_DISPATCHED }
                        ?: TRANSPORT_STATUS_DISPATCHED
                    val dupMsg = "Duplicate Denied: $normalizedCopy for Order #${enrichedRecord.orderNo}$descSuffix was already $existingStatus."
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
                        isoTime = isoTime,
                        driverNameHint = candidateDriverName
                    )
                    if (patchErr != null) {
                        enrichedRecord.syncStatus = "FAILED"
                        enrichedRecord.errorMessage = patchErr
                        dao.updateRecord(enrichedRecord)
                        return@withContext SyncStatusResult.Failed(patchErr)
                    }
                }

                // 4b. Record or update `copy_name` and `transport_status = "dispatched"` in `public.transport`
                if (matchingTransport != null && !matchingTransport.id.isNullOrBlank()) {
                    val patchBody = TransportPatchBody(
                        transport_mode = effectiveVehicle,
                        loaded_at = isoTime,
                        loaded_by = deliveryPersonId,
                        copy_name = normalizedCopy,
                        transport_status = TRANSPORT_STATUS_DISPATCHED
                    )
                    var updateResp = api.updateTransportById(
                        idFilter = "eq.${matchingTransport.id}",
                        body = patchBody
                    )
                    if (!updateResp.isSuccessful && updateResp.code() == 409 && deliveryPersonId != null) {
                        updateResp = api.updateTransportById(
                            idFilter = "eq.${matchingTransport.id}",
                            body = patchBody.copy(loaded_by = null)
                        )
                    }
                    if (!updateResp.isSuccessful) {
                        val err = "Supabase Transport HTTP ${updateResp.code()}: ${updateResp.message()}"
                        enrichedRecord.syncStatus = "FAILED"
                        enrichedRecord.errorMessage = err
                        dao.updateRecord(enrichedRecord)
                        return@withContext SyncStatusResult.Failed(err)
                    }
                } else {
                    val insertBody = TransportInsertBody(
                        order_id = resolvedOrderId,
                        transport_mode = effectiveVehicle,
                        loaded_at = isoTime,
                        loaded_by = deliveryPersonId,
                        copy_name = normalizedCopy,
                        transport_status = TRANSPORT_STATUS_DISPATCHED
                    )
                    var insertResp = api.insertTransport(insertBody)
                    if (!insertResp.isSuccessful && insertResp.code() == 409 && deliveryPersonId != null) {
                        insertResp = api.insertTransport(insertBody.copy(loaded_by = null))
                    }
                    if (!insertResp.isSuccessful) {
                        val err = "Supabase Transport HTTP ${insertResp.code()}: ${insertResp.message()}"
                        enrichedRecord.syncStatus = "FAILED"
                        enrichedRecord.errorMessage = err
                        dao.updateRecord(enrichedRecord)
                        return@withContext SyncStatusResult.Failed(err)
                    }
                }
            } else {
                // RETURN MODE: Every table specification (or order+copy for manual entry) can be returned ONCE
                val alreadyReturnedForTable = if (hasScannedTableFeatures) {
                    matchingTableLogs.any { it.operation_mode.equals("RETURN", ignoreCase = true) }
                } else {
                    matchingTransport != null &&
                        matchingTransport.transport_status?.trim().equals(TRANSPORT_STATUS_RETURNED, ignoreCase = true)
                }

                if (alreadyReturnedForTable) {
                    val dupMsg = "Duplicate Denied: $normalizedCopy for Order #${enrichedRecord.orderNo}$descSuffix was already returned."
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

            // 5. Record ONLY the dispatched table's size (`enrichedRecord.size`), quantity (`enrichedRecord.qty`),
            //    and full 7-feature signature in `public.dispatch_return_logs`.
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
                        notes = buildTableFeatureNotes(enrichedRecord)
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

data class ManualOrderFetchResult(
    val payload: QrPayload,
    val tables: List<SupabaseOrderTable> = emptyList(),
    val foundInDatabase: Boolean = false
)
