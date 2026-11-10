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
        assertTrue(options.contains("DAE-4717 (Lasantha)"))
        assertTrue(options.contains("Pick Me"))
        assertTrue(options.contains("Custom Vehicle"))

        // Select DAE-4717 (Lasantha)
        viewModel.setSelectedVehicleOption("DAE-4717 (Lasantha)")
        assertEquals("DAE-4717 (Lasantha)", viewModel.getEffectiveVehicleNumber())
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
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // Switch to Return mode and verify vehicle remains available
        viewModel.setMode(OperationMode.RETURN)
        assertEquals(OperationMode.RETURN, viewModel.currentMode.value)
        assertEquals("DAC-4912 (Rathnayake)", viewModel.getEffectiveVehicleNumber())

        // Verify Supabase preconfigured credentials
        assertEquals("https://hlemlubraovotrsdkgka.supabase.co", viewModel.supabaseUrl.value)
        assertEquals("sb_publishable_pYesGxabujz8z3PWiJjD9A_myGuSLGA", viewModel.supabaseKey.value)
    }

    @Test
    fun `vehicle selection maps DAC-4912 and DAE-4717 to exact Supabase profile UUIDs without showing UUID in UI summary`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DispatchViewModel(app)

        // 1. DAC-4912 (Rathnayake) -> 574e0364-858b-43e8-bf5f-5f09fc7e30cf
        viewModel.setSelectedVehicleOption("DAC-4912 (Rathnayake)")
        assertEquals(
            "574e0364-858b-43e8-bf5f-5f09fc7e30cf",
            viewModel.getAssignedDriverUuid()
        )
        assertEquals("GATE PASS assigns driver Rathnayake", viewModel.getAssignedDeliverySummary())
        org.junit.Assert.assertFalse(viewModel.getAssignedDeliverySummary().contains("574e0364"))

        // 2. DAE-4717 (Lasantha) -> 149a7d44-326a-48fe-b970-8ac196352779
        viewModel.setSelectedVehicleOption("DAE-4717 (Lasantha)")
        assertEquals(
            "149a7d44-326a-48fe-b970-8ac196352779",
            viewModel.getAssignedDriverUuid()
        )
        assertEquals("GATE PASS assigns driver Lasantha", viewModel.getAssignedDeliverySummary())
        org.junit.Assert.assertFalse(viewModel.getAssignedDeliverySummary().contains("149a7d44"))

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
            com.example.data.repository.DispatchRepository.buildDuplicateKey("dispatch", "#1024", "Gate Pass", 1)
        )
        assertEquals(
            "RETURN|1024|GATE PASS|1",
            com.example.data.repository.DispatchRepository.buildDuplicateKey("return", "#1024", "Gate Pass", 1)
        )
        assertEquals(
            "DISPATCH|3535|30x60|teak|black|1.5x1.5|30|o shape|GATE PASS",
            com.example.data.repository.DispatchRepository.buildDuplicateKey(
                mode = "dispatch",
                orderNo = "3535",
                copyName = "GATE PASS",
                tableIndex = 1,
                size = "30x60",
                topColour = "teak",
                frameColour = "black",
                legSize = "1.5x1.5",
                legHeight = "30",
                legShape = "O Shape"
            )
        )
    }

    @Test
    fun `QR scan parses Order 3535 payload with all 7 table features and reports exact size and qty`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DispatchViewModel(app)
        viewModel.setMode(OperationMode.DISPATCH)
        viewModel.setSelectedVehicleOption("DAE-4717 (Lasantha)")

        val qr3535 = """{"orderNo":"3535","customer":"kalana ","district":"Kandy","deliveryType":"non-courier","salesPerson":"Niyana Furniture","copy":"GATE PASS","totalUnits":3,"size":"30x60","topColour":"teak","frameColour":"black","qty":1,"legSize":"1.5x1.5","legShape":"O Shape","legHeight":"30"}"""
        viewModel.onQrScanned(qr3535, app)

        val scanned = viewModel.scannedPayload.value
        org.junit.Assert.assertNotNull(scanned)
        assertEquals("3535", scanned?.orderNo)
        assertEquals("GATE PASS", scanned?.copy)
        assertEquals("kalana", scanned?.customer)
        assertEquals("Kandy", scanned?.district)
        assertEquals("30x60", scanned?.size)
        assertEquals("teak", scanned?.topColour)
        assertEquals("black", scanned?.frameColour)
        assertEquals("1.5x1.5", scanned?.legSize)
        assertEquals("O Shape", scanned?.legShape)
        assertEquals("30", scanned?.legHeight)
        assertEquals(1, scanned?.qty)
        assertEquals(3, scanned?.totalUnits)
    }

    @Test
    fun `manual order entry opens confirmation sheet with editable details and note support`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DispatchViewModel(app)
        viewModel.setMode(OperationMode.DISPATCH)
        viewModel.setSelectedVehicleOption("DAC-4912 (Rathnayake)")

        viewModel.setManualDispatchOrderInput("4058")
        viewModel.processManualDispatch(app)

        // Manual entry immediately opens the confirmation sheet in manual edit mode
        assertTrue(viewModel.isManualConfirmation.value)
        org.junit.Assert.assertNotNull(viewModel.scannedPayload.value)
        assertEquals("4058", viewModel.scannedPayload.value?.orderNo)

        // User can edit order details and set a confirmation note before confirming
        val editedPayload = viewModel.scannedPayload.value!!.copy(
            customer = "Sunil Furniture",
            district = "Gampaha",
            size = "24x60",
            topColour = "teak",
            frameColour = "black",
            legSize = "1.5x1.5",
            legHeight = "30",
            legShape = "O Shape",
            qty = 1
        )
        viewModel.updateScannedPayloadDraft(editedPayload)
        viewModel.setConfirmationNote("Handle glass top with care")

        assertEquals("Sunil Furniture", viewModel.scannedPayload.value?.customer)
        assertEquals("24x60", viewModel.scannedPayload.value?.size)
        assertEquals("Handle glass top with care", viewModel.confirmationNote.value)
        viewModel.clearScannedData()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }
}

