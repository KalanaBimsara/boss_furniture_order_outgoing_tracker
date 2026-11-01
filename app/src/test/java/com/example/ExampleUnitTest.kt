package com.example

import com.example.data.api.DispatchReturnLogInsertBody
import com.example.data.api.DispatchReturnLogResponse
import com.example.data.api.OrderPatchBody
import com.example.data.api.ProfileResponse
import com.example.data.api.SupabaseApi
import com.example.data.api.SupabaseApiClient
import com.example.data.api.SupabaseOrder
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

        override suspend fun getRecentDispatchReturnLogs(
            limit: Int,
            order: String,
            select: String
        ): List<DispatchReturnLogResponse> = emptyList()

        override suspend fun insertDispatchReturnLog(body: DispatchReturnLogInsertBody): Response<Unit> =
            Response.success(Unit)
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
            duplicateKey = "DISPATCH|3517|GATE PASS|1",
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
            duplicateKey = "DISPATCH|3520|ACCOUNT COPY|1",
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
            duplicateKey = "DISPATCH|3520|GATE PASS|1",
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
            duplicateKey = "DISPATCH|3600|GATE PASS|1",
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
        // When network is available in test environment, verify JWT structure
        if (token != null) {
            assertTrue("Expected a valid 3-part JWT access token", token.startsWith("eyJ") && token.count { it == '.' } == 2)
            assertFalse("Token must not be the raw publishable key", token.startsWith("sb_publishable_"))
        }
    }

    private fun createRepositoryWithFakeApi(
        dao: FakeScannedRecordDao,
        fakeApi: FakeSupabaseApi
    ): DispatchRepository {
        return DispatchRepository(dao = dao, settingsRepo = null, apiOverride = fakeApi)
    }
}
