package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.OperationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Boss Furniture QR Scanner", appName)
    }

    @Test
    fun `vehicle options include predefined vehicles Pick Me and Custom Vehicle`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DispatchViewModel(app)

        val options = viewModel.getAvailableVehicleOptions()
        assertTrue(options.contains("DAC-4912 (Rathnayake)"))
        assertTrue(options.contains("DAE-4217 (Lasantha)"))
        assertTrue(options.contains("Pick Me"))
        assertTrue(options.contains("Custom Vehicle"))

        // Select DAE-4217 (Lasantha)
        viewModel.setSelectedVehicleOption("DAE-4217 (Lasantha)")
        assertEquals("DAE-4217 (Lasantha)", viewModel.getEffectiveVehicleNumber())
        assertNull(viewModel.validateVehicleReady())

        // Select Pick Me and type vehicle number
        viewModel.setSelectedVehicleOption("Pick Me")
        viewModel.setPickMeVehicleNumber("")
        assertEquals("Please enter the Pick Me vehicle number.", viewModel.validateVehicleReady())

        viewModel.setPickMeVehicleNumber("CAA-8910")
        assertNull(viewModel.validateVehicleReady())
        assertEquals("Pick Me (CAA-8910)", viewModel.getEffectiveVehicleNumber())

        // Add Custom Vehicle
        viewModel.addAndSelectCustomVehicle("WP-DAG-5544 (Nimal)")
        assertEquals("WP-DAG-5544 (Nimal)", viewModel.selectedVehicleOption.value)
        assertEquals("WP-DAG-5544 (Nimal)", viewModel.getEffectiveVehicleNumber())
        assertTrue(viewModel.getAvailableVehicleOptions().contains("WP-DAG-5544 (Nimal)"))
    }

    @Test
    fun `vehicle selection persists across Dispatch and Return modes and manual dispatch works`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DispatchViewModel(app)

        viewModel.setSelectedVehicleOption("DAC-4912 (Rathnayake)")
        viewModel.setMode(OperationMode.DISPATCH)
        assertEquals(OperationMode.DISPATCH, viewModel.currentMode.value)
        assertEquals("DAC-4912 (Rathnayake)", viewModel.getEffectiveVehicleNumber())

        viewModel.setManualDispatchOrderInput("ORD-9001")
        assertEquals("ORD-9001", viewModel.manualDispatchOrderInput.value)
        viewModel.processManualDispatch(app)

        // Switch to Return mode and verify vehicle remains available
        viewModel.setMode(OperationMode.RETURN)
        assertEquals(OperationMode.RETURN, viewModel.currentMode.value)
        assertEquals("DAC-4912 (Rathnayake)", viewModel.getEffectiveVehicleNumber())

        // Verify Supabase preconfigured credentials
        assertEquals("https://baxnczbtheknzpoerpfn.supabase.co", viewModel.supabaseUrl.value)
        assertEquals("sb_publishable_4H_QqrQ4jWxfZ39KVoaWaQ_NX8U8gIs", viewModel.supabaseKey.value)
    }

    @Test
    fun `vehicle selection maps DAC-4912 and DAE-4217 to exact Supabase profile UUIDs without showing UUID in UI summary`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DispatchViewModel(app)

        // 1. DAC-4912 (Rathnayake) -> de7a7b6c-92db-4fe7-82d6-2f2cda9e8a56
        viewModel.setSelectedVehicleOption("DAC-4912 (Rathnayake)")
        assertEquals(
            "de7a7b6c-92db-4fe7-82d6-2f2cda9e8a56",
            viewModel.getAssignedDriverUuid()
        )
        assertEquals("GATE PASS assigns driver Rathnayake", viewModel.getAssignedDeliverySummary())
        org.junit.Assert.assertFalse(viewModel.getAssignedDeliverySummary().contains("de7a7b6c"))

        // 2. DAE-4217 (Lasantha) -> 0c940655-4963-4036-ae66-4a889f9d368f
        viewModel.setSelectedVehicleOption("DAE-4217 (Lasantha)")
        assertEquals(
            "0c940655-4963-4036-ae66-4a889f9d368f",
            viewModel.getAssignedDriverUuid()
        )
        assertEquals("GATE PASS assigns driver Lasantha", viewModel.getAssignedDeliverySummary())
        org.junit.Assert.assertFalse(viewModel.getAssignedDeliverySummary().contains("0c940655"))

        // 3. Pick Me -> delivery option assigned to Pick Me (CAA-8910)
        viewModel.setSelectedVehicleOption("Pick Me")
        viewModel.setPickMeVehicleNumber("CAA-8910")
        assertNull(viewModel.getAssignedDriverUuid())
        assertEquals("Pick Me (CAA-8910)", viewModel.getEffectiveVehicleNumber())
        assertTrue(viewModel.getAssignedDeliverySummary().contains("Pick Me (CAA-8910)"))

        // 4. Custom Vehicle -> delivery option assigned to custom vehicle
        viewModel.addAndSelectCustomVehicle("WP-DAG-5544")
        assertNull(viewModel.getAssignedDriverUuid())
        assertEquals("WP-DAG-5544", viewModel.getEffectiveVehicleNumber())
        assertTrue(viewModel.getAssignedDeliverySummary().contains("WP-DAG-5544"))

        // 5. Verify Sri Lanka (Asia/Colombo, UTC+05:30) timestamp formatting
        // Epoch 0L is 1970-01-01 00:00:00 UTC -> 1970-01-01 05:30:00 in Sri Lanka
        assertEquals(
            "1970-01-01 · 05:30:00",
            com.example.data.repository.DispatchRepository.formatSriLankaDateTime(0L)
        )
        assertTrue(
            com.example.data.repository.DispatchRepository.formatSriLankaIsoTimestamp(0L)
                .startsWith("1970-01-01T05:30:00+05:30")
        )
    }

    @Test
    fun `only GATE PASS assigns order to driver and all four copy types are normalized`() {
        assertEquals("GATE PASS", com.example.data.repository.DispatchRepository.normalizeCopyType("Gate Pass"))
        assertEquals("ACCOUNT COPY", com.example.data.repository.DispatchRepository.normalizeCopyType("Account Copy"))
        assertEquals("TRANSPORT COPY", com.example.data.repository.DispatchRepository.normalizeCopyType("Transport Copy"))
        assertEquals("PRODUCTION COPY", com.example.data.repository.DispatchRepository.normalizeCopyType("Production Copy"))

        assertTrue(com.example.data.repository.DispatchRepository.isGatePassCopy("GATE PASS"))
        org.junit.Assert.assertFalse(com.example.data.repository.DispatchRepository.isGatePassCopy("ACCOUNT COPY"))
        org.junit.Assert.assertFalse(com.example.data.repository.DispatchRepository.isGatePassCopy("TRANSPORT COPY"))
        org.junit.Assert.assertFalse(com.example.data.repository.DispatchRepository.isGatePassCopy("PRODUCTION COPY"))

        // Verify OrderPatchBody does not have a note property
        val fieldNames = com.example.data.api.OrderPatchBody::class.java.declaredFields.map { it.name }
        org.junit.Assert.assertFalse(fieldNames.contains("note"))
        org.junit.Assert.assertFalse(fieldNames.contains("delivery_type"))
        assertTrue(fieldNames.contains("delivery_person_id"))
        assertTrue(fieldNames.contains("status"))
        assertTrue(fieldNames.contains("delivery_status"))

        // Verify TransportInsertBody and TransportPatchBody include copy_name and transport_status
        val transportInsertFields = com.example.data.api.TransportInsertBody::class.java.declaredFields.map { it.name }
        assertTrue(transportInsertFields.contains("copy_name"))
        assertTrue(transportInsertFields.contains("transport_status"))

        val transportPatchFields = com.example.data.api.TransportPatchBody::class.java.declaredFields.map { it.name }
        assertTrue(transportPatchFields.contains("copy_name"))
        assertTrue(transportPatchFields.contains("transport_status"))

        assertEquals("dispatched", com.example.data.repository.DispatchRepository.TRANSPORT_STATUS_DISPATCHED)
        assertEquals("returned", com.example.data.repository.DispatchRepository.TRANSPORT_STATUS_RETURNED)
        assertEquals(
            "DISPATCH|1024|GATE PASS|1",
            com.example.data.repository.DispatchRepository.buildDuplicateKey("dispatch", "1024", "Gate Pass", 1)
        )
        assertEquals(
            "RETURN|1024|GATE PASS|1",
            com.example.data.repository.DispatchRepository.buildDuplicateKey("return", "1024", "Gate Pass", 1)
        )
    }
}

