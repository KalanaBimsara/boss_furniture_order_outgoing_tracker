package com.example

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ScannedRecord
import com.example.ui.screens.AppNavigator
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DispatchViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class UiRedesignScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var viewModel: DispatchViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dao = AppDatabase.getDatabase(app).scannedRecordDao
        val now = System.currentTimeMillis()
        runBlocking {
            dao.clearAllRecords()
            dao.insertRecord(
                ScannedRecord(
                    scannedAt = now - 60_000, scannerName = "Station 01", deviceId = "dev",
                    mode = "DISPATCH", orderNo = "1024", customer = "Nimal Perera", district = "Colombo",
                    copy = "GATE PASS", tableIndex = 1, tableCount = 2, size = "6x3",
                    vehicleNumber = "DAC-4912 (Rathnayake)", syncStatus = "SYNCED"
                )
            )
            dao.insertRecord(
                ScannedRecord(
                    scannedAt = now - 30_000, scannerName = "Station 01", deviceId = "dev",
                    mode = "RETURN", restocked = true, orderNo = "ORD-8821", customer = "Kamal Silva",
                    district = "Kandy", copy = "TRANSPORT COPY", vehicleNumber = "Pick Me (CAA-8910)",
                    syncStatus = "PENDING"
                )
            )
            dao.insertRecord(
                ScannedRecord(
                    scannedAt = now, scannerName = "Station 01", deviceId = "dev",
                    mode = "DISPATCH", orderNo = "1031", customer = "Sunil Traders", district = "Galle",
                    copy = "ACCOUNT COPY", syncStatus = "DUPLICATE", errorMessage = "Duplicate scan blocked"
                )
            )
        }
        viewModel = DispatchViewModel(app)
    }

    private fun setApp() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigator(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun capture(name: String) {
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/redesign/$name.png")
    }

    @Test
    fun scanner_dispatch() {
        setApp()
        capture("01_scanner_dispatch")
    }

    @Test
    fun scanner_return() {
        setApp()
        composeTestRule.onNodeWithTag("mode_switch_return").performClick()
        capture("02_scanner_return")
    }

    @Test
    fun pending() {
        setApp()
        composeTestRule.onNodeWithTag("nav_item_pending").performClick()
        capture("03_pending")
    }

    @Test
    fun history() {
        setApp()
        composeTestRule.onNodeWithTag("nav_item_history").performClick()
        capture("04_history")
    }

    @Test
    fun settings_lock() {
        setApp()
        composeTestRule.onNodeWithTag("nav_item_settings").performClick()
        capture("05_settings_lock")
    }

    @Test
    @Config(qualifiers = "+night")
    fun scanner_dispatch_dark() {
        setApp()
        capture("06_scanner_dispatch_dark")
    }

    @Test
    @Config(qualifiers = "+night")
    fun history_dark() {
        setApp()
        composeTestRule.onNodeWithTag("nav_item_history").performClick()
        capture("07_history_dark")
    }
}
