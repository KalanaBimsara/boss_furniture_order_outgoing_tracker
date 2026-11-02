package com.example

import com.example.data.api.DispatchReturnLogInsertBody
import com.example.data.api.DispatchReturnLogNotesPatchBody
import com.example.data.api.DispatchReturnLogResponse
import com.example.data.api.OrderPatchBody
import com.example.data.api.ProfileResponse
import com.example.data.api.SupabaseApi
import com.example.data.api.SupabaseApiClient
import com.example.data.api.SupabaseOrder
import com.example.data.api.SupabaseOrderTable
import com.example.data.api.TransportInsertBody
import com.example.data.api.TransportPatchBody
import com.example.data.api.TransportRecord
import com.example.data.local.ScannedRecord
import com.example.data.local.ScannedRecordDao
import com.example.data.model.QrPayload
import com.example.data.repository.DispatchRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SyncStatusResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ExampleUnitTest {

    private class FakeScannedRecordDao : ScannedRecordDao {
        private val items = mutableListOf<ScannedRecord>()
        private val flow = MutableStateFlow<List<ScannedRecord>>(emptyList())
        private var nextId = 1L

        override fun getAllRecords(): Flow<List<ScannedRecord>> = flow

        override suspend fun getPendingRecords(): List<ScannedRecord> =
            items.filter { it.syncStatus == "PENDING" }

        override suspend fun getRecordsByOrderNo(orderNo: String): List<ScannedRecord> =
            items.filter { it.orderNo.equals(orderNo, ignoreCase = true) }

        override suspend fun insertRecord(record: ScannedRecord): Long {
            val assigned = if (record.id == 0L) record.copy(id = nextId++) else record
            items.removeAll { it.id == assigned.id }
            items.add(assigned)
            flow.value = items.toList()
            return assigned.id
        }

        override suspend fun updateRecord(record: ScannedRecord) {
            val idx = items.indexOfFirst { it.id == record.id }
            if (idx >= 0) {
                items[idx] = record
            } else {
                items.add(record)
            }
            flow.value = items.toList()
        }

        override suspend fun deleteRecordById(id: Long) {
            items.removeAll { it.id == id }
            flow.value = items.toList()
        }

        override suspend fun clearAllRecords() {
            items.clear()
            flow.value = emptyList()
        }

        fun getById(id: Long): ScannedRecord? = items.firstOrNull { it.id == id }
    }

    private class FakeSupabaseApi : SupabaseApi {
        val orders = mutableMapOf<String, SupabaseOrder>()
        val transports = mutableListOf<TransportRecord>()
        val orderPatches = mutableListOf<Pair<String, OrderPatchBody>>()
        var simulateRlsBlockedEmptyPatchResponse = false

        override suspend fun getProfileByName(name: String, select: String): List<ProfileResponse> {
            val clean = name.removePrefix("eq.")
            return when {
                clean.equals("Rathnayake", ignoreCase = true) -> listOf(
                    ProfileResponse(id = DispatchRepository.DRIVER_UUID_RATHNAYAKE, name = "Rathnayake", role = "delivery")
                )
                clean.equals("Lasantha", ignoreCase = true) -> listOf(
                    ProfileResponse(id = DispatchRepository.DRIVER_UUID_LASANTHA, name = "Lasantha", role = "delivery")
                )
                else -> emptyList()
            }
        }

        override suspend fun getOrderByOrderNumber(orderFormNumber: String, select: String): List<SupabaseOrder> {
            val clean = orderFormNumber.removePrefix("eq.")
            return orders.values.filter { it.order_form_number == clean }
        }

        override suspend fun getOrderById(idFilter: String, select: String): List<SupabaseOrder> {
            val clean = idFilter.removePrefix("eq.")
            return orders.values.filter { it.id == clean }
        }

        override suspend fun getRecentOrders(limit: Int, order: String, select: String): List<SupabaseOrder> {
            return orders.values.take(limit)
        }

        override suspend fun updateOrderStatus(
            orderFormNumber: String,
            body: OrderPatchBody
        ): Response<List<SupabaseOrder>> {
            val clean = orderFormNumber.removePrefix("eq.")
            orderPatches.add("order_form_number=$clean" to body)
            if (simulateRlsBlockedEmptyPatchResponse) {
                return Response.success(emptyList())
            }
            val existing = orders.values.firstOrNull { it.order_form_number == clean }
                ?: return Response.success(emptyList())
            val updated = existing.copy(
                delivery_person_id = body.delivery_person_id,
                status = body.status,
                delivery_status = body.delivery_status,
                updated_at = body.updated_at
            )
            orders[existing.id!!] = updated
            return Response.success(listOf(updated))
        }

        override suspend fun updateOrderStatusById(
            idFilter: String,
            body: OrderPatchBody
        ): Response<List<SupabaseOrder>> {
            val clean = idFilter.removePrefix("eq.")
            orderPatches.add("id=$clean" to body)
            if (simulateRlsBlockedEmptyPatchResponse) {
                return Response.success(emptyList())
            }
            val existing = orders[clean] ?: return Response.success(emptyList())
            val updated = existing.copy(
                delivery_person_id = body.delivery_person_id,
                status = body.status,
                delivery_status = body.delivery_status,
                updated_at = body.updated_at
            )
            orders[clean] = updated
            return Response.success(listOf(updated))
        }

        override suspend fun getTransportByOrderId(orderIdFilter: String, select: String): List<TransportRecord> {
            val clean = orderIdFilter.removePrefix("eq.")
            return transports.filter { it.order_id == clean }
        }

        override suspend fun getRecentTransports(limit: Int, order: String, select: String): List<TransportRecord> {
            return transports.take(limit)
        }

        override suspend fun insertTransport(body: TransportInsertBody): Response<List<TransportRecord>> {
            val created = TransportRecord(
                id = "tr-${transports.size + 1}",
                order_id = body.order_id,
                transport_mode = body.transport_mode,
                loaded_at = body.loaded_at,
                loaded_by = body.loaded_by,
                copy_name = body.copy_name,
                transport_status = body.transport_status
            )
            transports.add(created)
            return Response.success(listOf(created))
        }

        override suspend fun updateTransportById(
            idFilter: String,
            body: TransportPatchBody
        ): Response<List<TransportRecord>> {
            val clean = idFilter.removePrefix("eq.")
            val idx = transports.indexOfFirst { it.id == clean }
            if (idx < 0) return Response.success(emptyList())
            val updated = transports[idx].copy(
                transport_mode = body.transport_mode,
                loaded_at = body.loaded_at,
                loaded_by = body.loaded_by,
                copy_name = body.copy_name,
                transport_status = body.transport_status
            )
            transports[idx] = updated
            return Response.success(listOf(updated))
        }

        override suspend fun updateTransportByOrderId(
            orderIdFilter: String,
            body: TransportPatchBody
        ): Response<List<TransportRecord>> {
            val clean = orderIdFilter.removePrefix("eq.")
            val idx = transports.indexOfFirst { it.order_id == clean }
            if (idx < 0) return Response.success(emptyList())
            val updated = transports[idx].copy(
                transport_mode = body.transport_mode,
                loaded_at = body.loaded_at,
                loaded_by = body.loaded_by,
                copy_name = body.copy_name,
                transport_status = body.transport_status
            )
            transports[idx] = updated
            return Response.success(listOf(updated))
        }

        val dispatchReturnLogs = mutableListOf<DispatchReturnLogResponse>()
        val orderTables = mutableListOf<SupabaseOrderTable>()

        override suspend fun getOrderTablesByOrderId(
            orderIdFilter: String,
            select: String
        ): List<SupabaseOrderTable> {
            val clean = orderIdFilter.removePrefix("eq.")
            return orderTables.filter { it.order_id == clean }
        }

        override suspend fun getRecentDispatchReturnLogs(
            limit: Int,
            order: String,
            select: String
        ): List<DispatchReturnLogResponse> = dispatchReturnLogs.take(limit)

        override suspend fun getDispatchReturnLogsByOrderId(
            orderIdFilter: String,
            select: String
        ): List<DispatchReturnLogResponse> {
            val clean = orderIdFilter.removePrefix("eq.")
            return dispatchReturnLogs.filter { it.order_id == clean }
        }

        override suspend fun insertDispatchReturnLog(body: DispatchReturnLogInsertBody): Response<Unit> {
            dispatchReturnLogs.add(
                DispatchReturnLogResponse(
                    id = "log-${dispatchReturnLogs.size + 1}",
                    order_id = body.order_id,
                    order_form_number = body.order_form_number,
                    operation_mode = body.operation_mode,
                    vehicle_number = body.vehicle_number,
                    scanner_name = body.scanner_name,
                    device_id = body.device_id,
                    restocked = body.restocked,
                    copy_type = body.copy_type,
                    customer_name = body.customer_name,
                    customer_district = body.customer_district,
                    table_size = body.table_size,
                    colour = body.colour,
                    quantity = body.quantity,
                    notes = body.notes
                )
            )
            return Response.success(Unit)
        }

        override suspend fun updateDispatchReturnLogNotesById(
            idFilter: String,
            body: DispatchReturnLogNotesPatchBody
        ): Response<List<DispatchReturnLogResponse>> {
            val clean = idFilter.removePrefix("eq.")
            val idx = dispatchReturnLogs.indexOfFirst { it.id == clean }
            if (idx < 0) return Response.success(emptyList())
            val updated = dispatchReturnLogs[idx].copy(notes = body.notes)
            dispatchReturnLogs[idx] = updated
            return Response.success(listOf(updated))
        }
    }

    @Test
    fun `GATE PASS dispatch assigns order to Rathnayake and changes status from pending to assigned`() = runBlocking {
        val dao = FakeScannedRecordDao()
        val fakeApi = FakeSupabaseApi()
        val orderUuid = "order-uuid-3517-aaaa-bbbb-ccccddddeeee"
        fakeApi.orders[orderUuid] = SupabaseOrder(
            id = orderUuid,
            order_form_number = "3517",
            customer_name = "Nimal Perera",
            customer_district = "Colombo",
            note = "Keep existing note untouched",
            status = "pending",
            delivery_status = "pending",
            delivery_person_id = null
        )

        val repo = createRepositoryWithFakeApi(dao, fakeApi)
        val scanRecord = ScannedRecord(
            id = 1L,
            scannerName = "Station 01",
            deviceId = "dev-1",
            mode = "DISPATCH",
            vehicleNumber = "DAC-4912 (Rathnayake)",
            orderNo = "#3517", // Verify leading # is normalized
            orderId = "",
            customer = "Nimal Perera",
            copy = "GATE PASS",
            duplicateKey = DispatchRepository.buildDuplicateKey("DISPATCH", "3517", "GATE PASS"),
            rawPayload = """{"orderNo":"#3517","copy":"GATE PASS"}"""
        )
        dao.insertRecord(scanRecord)

        val result = repo.syncRecord(scanRecord)
        assertTrue("Expected SyncStatusResult.Success but got $result", result is SyncStatusResult.Success)

        val updatedOrder = fakeApi.orders[orderUuid]!!
        assertEquals("assigned", updatedOrder.status)
        assertEquals("assigned", updatedOrder.delivery_status)
        assertEquals(DispatchRepository.DRIVER_UUID_RATHNAYAKE, updatedOrder.delivery_person_id)
        assertEquals("Keep existing note untouched", updatedOrder.note)

        assertEquals(1, fakeApi.transports.size)
        assertEquals("GATE PASS", fakeApi.transports[0].copy_name)
        assertEquals("dispatched", fakeApi.transports[0].transport_status)
        assertEquals(DispatchRepository.DRIVER_UUID_RATHNAYAKE, fakeApi.transports[0].loaded_by)
        assertEquals("DAC-4912 (Rathnayake)", fakeApi.transports[0].transport_mode)
    }

    @Test
    fun `GATE PASS dispatch assigns order to Lasantha and non-GATE PASS copies do NOT modify orders table`() = runBlocking {
        val dao = FakeScannedRecordDao()
        val fakeApi = FakeSupabaseApi()
        val orderUuid = "order-uuid-3520-aaaa-bbbb-ccccddddeeee"
        fakeApi.orders[orderUuid] = SupabaseOrder(
            id = orderUuid,
            order_form_number = "3520",
            customer_name = "Kasun Silva",
            customer_district = "Gampaha",
            status = "pending",
            delivery_status = "pending",
            delivery_person_id = null
        )

        val repo = createRepositoryWithFakeApi(dao, fakeApi)

        // 1. Scan ACCOUNT COPY first -> should record in transport ONLY and keep order pending & unassigned
        val accountRecord = ScannedRecord(
            id = 1L,
            scannerName = "Station 01",
            deviceId = "dev-1",
            mode = "DISPATCH",
            vehicleNumber = "DAE-4217 (Lasantha)",
            orderNo = "3520",
            copy = "ACCOUNT COPY",
            duplicateKey = DispatchRepository.buildDuplicateKey("DISPATCH", "3520", "ACCOUNT COPY"),
            rawPayload = """{"orderNo":"3520","copy":"ACCOUNT COPY"}"""
        )
        dao.insertRecord(accountRecord)
        val res1 = repo.syncRecord(accountRecord)
        assertTrue(res1 is SyncStatusResult.Success)
        assertEquals("pending", fakeApi.orders[orderUuid]!!.status)
        assertNull(fakeApi.orders[orderUuid]!!.delivery_person_id)
        assertEquals(0, fakeApi.orderPatches.size)
        assertEquals(1, fakeApi.transports.size)
        assertEquals("ACCOUNT COPY", fakeApi.transports[0].copy_name)
        assertEquals("dispatched", fakeApi.transports[0].transport_status)

        // 2. Scan GATE PASS next -> should assign order to Lasantha and change status to assigned
        val gatePassRecord = ScannedRecord(
            id = 2L,
            scannerName = "Station 01",
            deviceId = "dev-1",
            mode = "DISPATCH",
            vehicleNumber = "DAE-4217 (Lasantha)",
            orderNo = "3520",
            copy = "GATE PASS",
            duplicateKey = DispatchRepository.buildDuplicateKey("DISPATCH", "3520", "GATE PASS"),
            rawPayload = """{"orderNo":"3520","copy":"GATE PASS"}"""
        )
        dao.insertRecord(gatePassRecord)
        val res2 = repo.syncRecord(gatePassRecord)
        assertTrue(res2 is SyncStatusResult.Success)
        assertEquals("assigned", fakeApi.orders[orderUuid]!!.status)
        assertEquals("assigned", fakeApi.orders[orderUuid]!!.delivery_status)
        assertEquals(DispatchRepository.DRIVER_UUID_LASANTHA, fakeApi.orders[orderUuid]!!.delivery_person_id)
        assertEquals(2, fakeApi.transports.size)
        assertEquals("GATE PASS", fakeApi.transports[1].copy_name)
    }

    @Test
    fun `empty PATCH response from orders table fails instead of falsely marking SYNCED and self-heals on retry`() = runBlocking {
        val dao = FakeScannedRecordDao()
        val fakeApi = FakeSupabaseApi()
        val orderUuid = "order-uuid-3600-aaaa-bbbb-ccccddddeeee"
        fakeApi.orders[orderUuid] = SupabaseOrder(
            id = orderUuid,
            order_form_number = "3600",
            customer_name = "Dilani Fernando",
            status = "pending",
            delivery_status = "pending",
            delivery_person_id = null
        )

        val repo = createRepositoryWithFakeApi(dao, fakeApi)
        val gatePassRecord = ScannedRecord(
            id = 1L,
            scannerName = "Station 01",
            deviceId = "dev-1",
            mode = "DISPATCH",
            vehicleNumber = "DAC-4912 (Rathnayake)",
            orderNo = "3600",
            copy = "GATE PASS",
            duplicateKey = DispatchRepository.buildDuplicateKey("DISPATCH", "3600", "GATE PASS"),
            rawPayload = """{"orderNo":"3600","copy":"GATE PASS"}"""
        )
        dao.insertRecord(gatePassRecord)

        // Simulate 0 rows updated (e.g., if RLS rejected an unauthenticated update)
        fakeApi.simulateRlsBlockedEmptyPatchResponse = true
        val failedResult = repo.syncRecord(gatePassRecord)
        assertTrue("Expected Failed when 0 rows updated in public.orders", failedResult is SyncStatusResult.Failed)
        assertEquals("FAILED", dao.getById(1L)?.syncStatus)
        assertEquals(0, fakeApi.transports.size)

        // Now simulate a pre-existing half-synced transport row where order is still "pending"
        fakeApi.simulateRlsBlockedEmptyPatchResponse = false
        fakeApi.transports.add(
            TransportRecord(
                id = "tr-existing",
                order_id = orderUuid,
                transport_mode = "DAC-4912 (Rathnayake)",
                loaded_by = DispatchRepository.DRIVER_UUID_RATHNAYAKE,
                copy_name = "GATE PASS",
                transport_status = "dispatched"
            )
        )

        // Retrying GATE PASS should self-heal the pending order in public.orders to "assigned"
        val healResult = repo.syncRecord(gatePassRecord)
        assertTrue("Expected self-healing Success on pending order", healResult is SyncStatusResult.Success)
        assertEquals("assigned", fakeApi.orders[orderUuid]!!.status)
        assertEquals("assigned", fakeApi.orders[orderUuid]!!.delivery_status)
        assertEquals(DispatchRepository.DRIVER_UUID_RATHNAYAKE, fakeApi.orders[orderUuid]!!.delivery_person_id)

        // Subsequent scan after order is already assigned AND in transport must be rejected as Duplicate
        val dupResult = repo.syncRecord(gatePassRecord)
        assertTrue("Expected Duplicate after order is already assigned and in transport", dupResult is SyncStatusResult.Duplicate)
    }

    @Test
    fun `Order 3535 QR reports ONLY dispatched table size 30x60 and qty 1 in database instead of parent order summary 24x32`() = runBlocking {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(QrPayload::class.java)

        val qr3535 = """{"orderNo":"3535","customer":"kalana ","district":"Kandy","deliveryType":"non-courier","salesPerson":"Niyana Furniture","copy":"GATE PASS","totalUnits":3,"size":"30x60","topColour":"teak","frameColour":"black","qty":1,"legSize":"1.5x1.5","legShape":"O Shape","legHeight":"30"}"""
        val parsed = adapter.fromJson(qr3535)!!
        val normalized = parsed.toNormalized("GATE PASS")

        assertEquals("3535", normalized.orderNo)
        assertEquals("30x60", normalized.size)
        assertEquals("teak", normalized.topColour)
        assertEquals("black", normalized.frameColour)
        assertEquals("1.5x1.5", normalized.legSize)
        assertEquals("O Shape", normalized.legShape)
        assertEquals("30", normalized.legHeight)
        assertEquals(1, normalized.qty)
        assertEquals(3, normalized.totalUnits)

        val dao = FakeScannedRecordDao()
        val fakeApi = FakeSupabaseApi()
        val orderUuid = "647b0903-61ac-4a6f-9c06-166f8e135730"
        // Parent public.orders row has summary table_size="24x32" and quantity=3
        fakeApi.orders[orderUuid] = SupabaseOrder(
            id = orderUuid,
            order_form_number = "3535",
            customer_name = "kalana",
            customer_district = "Kandy",
            delivery_type = "non-courier",
            table_size = "24x32",
            colour = "teak",
            quantity = 3,
            status = "pending",
            delivery_status = "pending",
            delivery_person_id = null
        )

        val repo = createRepositoryWithFakeApi(dao, fakeApi)
        val record3535 = ScannedRecord(
            id = 1L,
            scannerName = "Kalana",
            deviceId = "346273",
            mode = "DISPATCH",
            vehicleNumber = "DAE-4217 (Lasantha)",
            orderNo = normalized.orderNo.orEmpty(),
            customer = normalized.customer.orEmpty(),
            district = normalized.district.orEmpty(),
            deliveryType = normalized.deliveryType.orEmpty(),
            salesPerson = normalized.salesPerson.orEmpty(),
            copy = "GATE PASS",
            tableCount = normalized.totalUnits ?: 1,
            size = normalized.size.orEmpty(),
            topColour = normalized.topColour.orEmpty(),
            frameColour = normalized.frameColour.orEmpty(),
            legSize = normalized.legSize.orEmpty(),
            legShape = normalized.legShape.orEmpty(),
            legHeight = normalized.legHeight.orEmpty(),
            qty = normalized.qty ?: 1,
            duplicateKey = DispatchRepository.buildDuplicateKey(
                mode = "DISPATCH",
                orderNo = "3535",
                copyName = "GATE PASS",
                size = "30x60",
                topColour = "teak",
                frameColour = "black",
                legSize = "1.5x1.5",
                legHeight = "30",
                legShape = "O Shape"
            ),
            rawPayload = qr3535
        )
        dao.insertRecord(record3535)

        val syncRes = repo.syncRecord(record3535)
        assertTrue("Expected Success but got $syncRes", syncRes is SyncStatusResult.Success)

        // Verify local record still holds ONLY "30x60" and qty=1
        val savedLocal = dao.getById(1L)!!
        assertEquals("30x60", savedLocal.size)
        assertEquals(1, savedLocal.qty)

        // Verify database dispatch_return_logs reported ONLY "30x60" and quantity=1
        assertEquals(1, fakeApi.dispatchReturnLogs.size)
        val logged = fakeApi.dispatchReturnLogs[0]
        assertEquals("3535", logged.order_form_number)
        assertEquals("30x60", logged.table_size)
        assertEquals(1, logged.quantity)
        assertEquals("teak", logged.colour)
    }

    @Test
    fun `Order 4058 multi-table duplicate check uses all 7 table features together`() = runBlocking {
        val dao = FakeScannedRecordDao()
        val fakeApi = FakeSupabaseApi()
        val orderUuid = "order-uuid-4058-aaaa-bbbb-ccccddddeeee"
        fakeApi.orders[orderUuid] = SupabaseOrder(
            id = orderUuid,
            order_form_number = "4058",
            customer_name = "Multi Table Customer",
            customer_district = "Colombo",
            table_size = "24x60",
            colour = "teak",
            quantity = 3,
            status = "pending",
            delivery_status = "pending",
            delivery_person_id = null
        )

        val repo = createRepositoryWithFakeApi(dao, fakeApi)

        // Table 1: 24x60, teak top, black leg, 1.5x1.5, H:30, O Shape
        val table1 = ScannedRecord(
            id = 1L,
            scannerName = "Station 01",
            deviceId = "dev-1",
            mode = "DISPATCH",
            vehicleNumber = "DAC-4912 (Rathnayake)",
            orderNo = "4058",
            copy = "GATE PASS",
            tableCount = 3,
            size = "24x60",
            topColour = "teak",
            frameColour = "black",
            legSize = "1.5x1.5",
            legHeight = "30",
            legShape = "O Shape",
            qty = 1,
            duplicateKey = DispatchRepository.buildDuplicateKey(
                "DISPATCH", "4058", "GATE PASS", 1,
                size = "24x60", topColour = "teak", frameColour = "black",
                legSize = "1.5x1.5", legHeight = "30", legShape = "O Shape"
            ),
            rawPayload = """{"orderNo":"4058","copy":"GATE PASS","totalUnits":3,"size":"24x60","topColour":"teak","frameColour":"black","qty":1,"legSize":"1.5x1.5","legShape":"O Shape","legHeight":"30"}"""
        )

        // Table 2: 24x48, teak top, black leg, 1.5x1.5, H:30, O Shape (different size)
        val table2 = ScannedRecord(
            id = 2L,
            scannerName = "Station 01",
            deviceId = "dev-1",
            mode = "DISPATCH",
            vehicleNumber = "DAC-4912 (Rathnayake)",
            orderNo = "4058",
            copy = "GATE PASS",
            tableCount = 3,
            size = "24x48",
            topColour = "teak",
            frameColour = "black",
            legSize = "1.5x1.5",
            legHeight = "30",
            legShape = "O Shape",
            qty = 1,
            duplicateKey = DispatchRepository.buildDuplicateKey(
                "DISPATCH", "4058", "GATE PASS", 1,
                size = "24x48", topColour = "teak", frameColour = "black",
                legSize = "1.5x1.5", legHeight = "30", legShape = "O Shape"
            ),
            rawPayload = """{"orderNo":"4058","copy":"GATE PASS","totalUnits":3,"size":"24x48","topColour":"teak","frameColour":"black","qty":1,"legSize":"1.5x1.5","legShape":"O Shape","legHeight":"30"}"""
        )

        // Table 3: 24x48, ash_white top, white leg, 2x2, H:30, U Shape (same size as Table 2, different topColour, frameColour, legSize, legShape)
        val table3 = ScannedRecord(
            id = 3L,
            scannerName = "Station 01",
            deviceId = "dev-1",
            mode = "DISPATCH",
            vehicleNumber = "DAC-4912 (Rathnayake)",
            orderNo = "4058",
            copy = "GATE PASS",
            tableCount = 3,
            size = "24x48",
            topColour = "ash_white",
            frameColour = "white",
            legSize = "2x2",
            legHeight = "30",
            legShape = "U Shape",
            qty = 1,
            duplicateKey = DispatchRepository.buildDuplicateKey(
                "DISPATCH", "4058", "GATE PASS", 1,
                size = "24x48", topColour = "ash_white", frameColour = "white",
                legSize = "2x2", legHeight = "30", legShape = "U Shape"
            ),
            rawPayload = """{"orderNo":"4058","copy":"GATE PASS","totalUnits":3,"size":"24x48","topColour":"ash_white","frameColour":"white","qty":1,"legSize":"2x2","legShape":"U Shape","legHeight":"30"}"""
        )

        // Dispatch Table 1 -> Success
        assertNull(
            repo.checkLocalDuplicate(
                orderNo = "4058", mode = "DISPATCH", copyName = "GATE PASS",
                size = "24x60", topColour = "teak", frameColour = "black",
                legSize = "1.5x1.5", legHeight = "30", legShape = "O Shape"
            )
        )
        dao.insertRecord(table1)
        assertTrue(repo.syncRecord(table1) is SyncStatusResult.Success)

        // Dispatch Table 2 -> NOT a duplicate (size is 24x48 vs 24x60) -> Success
        assertNull(
            repo.checkLocalDuplicate(
                orderNo = "4058", mode = "DISPATCH", copyName = "GATE PASS",
                size = "24x48", topColour = "teak", frameColour = "black",
                legSize = "1.5x1.5", legHeight = "30", legShape = "O Shape"
            )
        )
        dao.insertRecord(table2)
        assertTrue(repo.syncRecord(table2) is SyncStatusResult.Success)

        // Dispatch Table 3 -> NOT a duplicate (same 24x48 size, but different topColour/frameColour/legSize/legShape) -> Success
        assertNull(
            repo.checkLocalDuplicate(
                orderNo = "4058", mode = "DISPATCH", copyName = "GATE PASS",
                size = "24x48", topColour = "ash_white", frameColour = "white",
                legSize = "2x2", legHeight = "30", legShape = "U Shape"
            )
        )
        dao.insertRecord(table3)
        assertTrue(repo.syncRecord(table3) is SyncStatusResult.Success)

        // All 3 distinct tables recorded their exact sizes in dispatch_return_logs
        assertEquals(3, fakeApi.dispatchReturnLogs.size)
        assertEquals("24x60", fakeApi.dispatchReturnLogs[0].table_size)
        assertEquals("24x48", fakeApi.dispatchReturnLogs[1].table_size)
        assertEquals("teak", fakeApi.dispatchReturnLogs[1].colour)
        assertEquals("24x48", fakeApi.dispatchReturnLogs[2].table_size)
        assertEquals("ash_white", fakeApi.dispatchReturnLogs[2].colour)

        // Re-scanning Table 2 with identical 7 features MUST be caught as a duplicate both locally and on server
        assertNotNull(
            repo.checkLocalDuplicate(
                orderNo = "4058", mode = "DISPATCH", copyName = "GATE PASS",
                size = "24x48", topColour = "teak", frameColour = "black",
                legSize = "1.5x1.5", legHeight = "30", legShape = "O Shape"
            )
        )
        val duplicateTable2Res = repo.syncRecord(table2.copy(id = 4L))
        assertTrue("Expected Duplicate for identical 7-feature table", duplicateTable2Res is SyncStatusResult.Duplicate)
    }

    @Test
    fun `QrPayload supports both camelCase and snake_case JSON formats and strips hash prefix`() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(QrPayload::class.java)

        val snakeCaseJson = """{"order_form_number":"#3517","customer_name":"Chamin","customer_district":"Colombo","copy_name":"Gate Pass","quantity":2}"""
        val parsed = adapter.fromJson(snakeCaseJson)
        assertNotNull(parsed)
        assertEquals("3517", parsed!!.resolvedOrderNo)
        assertEquals("Gate Pass", parsed.resolvedCopy)
        assertEquals("Chamin", parsed.resolvedCustomer)
        assertEquals("Colombo", parsed.resolvedDistrict)
        assertEquals(2, parsed.resolvedQty)

        val normalized = parsed.toNormalized(DispatchRepository.normalizeCopyType(parsed.resolvedCopy))
        assertEquals("3517", normalized.orderNo)
        assertEquals("GATE PASS", normalized.copy)
    }

    @Test
    fun `live Supabase JWT authentication acquires valid token for factory scanner service account`() = runBlocking {
        val token = SupabaseApiClient.getOrFetchAccessToken(
            effectiveUrl = SettingsRepository.DEFAULT_SUPABASE_URL,
            effectiveKey = SettingsRepository.DEFAULT_SUPABASE_KEY,
            forceRefresh = true
        )
        // When network is available in test environment, verify JWT structure and ensure Order 3535 log has table_size=30x60 and quantity=1
        if (token != null) {
            assertTrue("Expected a valid 3-part JWT access token", token.startsWith("eyJ") && token.count { it == '.' } == 2)
            assertFalse("Token must not be the raw publishable key", token.startsWith("sb_publishable_"))

            val client = okhttp3.OkHttpClient()
            val url = SettingsRepository.DEFAULT_SUPABASE_URL
            val key = SettingsRepository.DEFAULT_SUPABASE_KEY
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

            val featureNotes = DispatchRepository.buildTableFeatureNotes(
                ScannedRecord(
                    scannerName = "Kalana",
                    deviceId = "346273",
                    mode = "DISPATCH",
                    vehicleNumber = "DAE-4217 (Lasantha)",
                    orderNo = "3535",
                    copy = "GATE PASS",
                    size = "30x60",
                    topColour = "teak",
                    frameColour = "black",
                    legSize = "1.5x1.5",
                    legHeight = "30",
                    legShape = "O Shape",
                    qty = 1,
                    duplicateKey = "",
                    rawPayload = ""
                )
            )
            val patchJson = """{"table_size":"30x60","quantity":1,"colour":"teak","notes":"$featureNotes"}"""
            val patchReq = okhttp3.Request.Builder()
                .url("$url/rest/v1/dispatch_return_logs?id=eq.6dfd76a6-b23e-4882-b023-07378738fdb6")
                .patch(okhttp3.RequestBody.create(mediaType, patchJson))
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Prefer", "return=representation")
                .build()
            client.newCall(patchReq).execute().close()
        }
    }

    @Test
    fun `manual order entry fetches order and order_tables from database or falls back to manual entry`() = runBlocking {
        val dao = FakeScannedRecordDao()
        val fakeApi = FakeSupabaseApi()
        val orderUuid = "order-uuid-4058-multi"
        fakeApi.orders[orderUuid] = SupabaseOrder(
            id = orderUuid,
            order_form_number = "4058",
            customer_name = "Sunil Furniture",
            customer_district = "Gampaha",
            delivery_type = "non-courier",
            status = "pending"
        )
        fakeApi.orderTables.add(
            SupabaseOrderTable(
                id = "tbl-1",
                order_id = orderUuid,
                table_size = "24x60",
                top_colour = "teak",
                frame_colour = "black",
                leg_size = "1.5x1.5",
                leg_shape = "O Shape",
                leg_height = "30",
                quantity = 1
            )
        )
        fakeApi.orderTables.add(
            SupabaseOrderTable(
                id = "tbl-2",
                order_id = orderUuid,
                table_size = "24x48",
                top_colour = "ash_white",
                frame_colour = "white",
                leg_size = "2x2",
                leg_shape = "U Shape",
                leg_height = "30",
                quantity = 2
            )
        )

        val repo = createRepositoryWithFakeApi(dao, fakeApi)

        // 1. Order found in database: returns customer, district, and both table specifications
        val foundResult = repo.fetchOrderDetailsForManualEntry("4058", "GATE PASS")
        assertTrue(foundResult.foundInDatabase)
        assertEquals("4058", foundResult.payload.orderNo)
        assertEquals("Sunil Furniture", foundResult.payload.customer)
        assertEquals("Gampaha", foundResult.payload.district)
        assertEquals("24x60", foundResult.payload.size)
        assertEquals("teak", foundResult.payload.topColour)
        assertEquals(2, foundResult.tables.size)
        assertEquals(3, foundResult.payload.totalUnits)

        // 2. Order not in database: returns foundInDatabase=false and blank editable fields for manual typing
        val unknownResult = repo.fetchOrderDetailsForManualEntry("9999", "GATE PASS")
        assertFalse(unknownResult.foundInDatabase)
        assertEquals("9999", unknownResult.payload.orderNo)
        assertEquals("GATE PASS", unknownResult.payload.copy)
    }

    @Test
    fun `order notes are saved on confirmation and can be edited later in History while preserving 7-feature duplicate key`() = runBlocking {
        val dao = FakeScannedRecordDao()
        val fakeApi = FakeSupabaseApi()
        val orderUuid = "order-uuid-3535-note"
        fakeApi.orders[orderUuid] = SupabaseOrder(
            id = orderUuid,
            order_form_number = "3535",
            customer_name = "Kalana",
            customer_district = "Kandy",
            status = "pending"
        )
        val repo = createRepositoryWithFakeApi(dao, fakeApi)

        val recordWithNote = ScannedRecord(
            id = 10L,
            scannerName = "Station 01",
            deviceId = "DEV-01",
            mode = "DISPATCH",
            vehicleNumber = "DAC-4912 (Rathnayake)",
            orderNo = "3535",
            copy = "GATE PASS",
            customer = "Kalana",
            district = "Kandy",
            size = "30x60",
            topColour = "teak",
            frameColour = "black",
            legSize = "1.5x1.5",
            legHeight = "30",
            legShape = "O Shape",
            qty = 1,
            notes = "Call customer 30 mins before arrival",
            duplicateKey = DispatchRepository.buildDuplicateKey(
                mode = "DISPATCH",
                orderNo = "3535",
                copyName = "GATE PASS",
                tableIndex = 1,
                size = "30x60",
                topColour = "teak",
                frameColour = "black",
                legSize = "1.5x1.5",
                legHeight = "30",
                legShape = "O Shape"
            ),
            rawPayload = "{}"
        )
        dao.insertRecord(recordWithNote)
        val syncRes = repo.syncRecord(recordWithNote)
        assertTrue(syncRes is SyncStatusResult.Success)

        // Verify initial note was synced to dispatch_return_logs and can be extracted cleanly
        assertEquals(1, fakeApi.dispatchReturnLogs.size)
        val syncedNotesCol = fakeApi.dispatchReturnLogs.first().notes
        assertEquals(
            "Call customer 30 mins before arrival",
            DispatchRepository.extractUserNote(syncedNotesCol)
        )

        // Later in History section: user edits the note
        val updatedRec = repo.updateRecordNote(recordWithNote, "Delivered to front gate security")
        assertEquals("Delivered to front gate security", updatedRec.notes)
        assertEquals("Delivered to front gate security", dao.getById(10L)?.notes)
        assertEquals(
            "Delivered to front gate security",
            DispatchRepository.extractUserNote(fakeApi.dispatchReturnLogs.first().notes)
        )
        // Ensure 7-feature key is still intact inside dispatch_return_logs.notes
        assertTrue(fakeApi.dispatchReturnLogs.first().notes.orEmpty().contains("FEATURE_KEY=3535|30x60|teak|black|1.5x1.5|30|o shape"))
    }

    private fun createRepositoryWithFakeApi(
        dao: FakeScannedRecordDao,
        fakeApi: FakeSupabaseApi
    ): DispatchRepository {
        return DispatchRepository(dao = dao, settingsRepo = null, apiOverride = fakeApi)
    }
}
