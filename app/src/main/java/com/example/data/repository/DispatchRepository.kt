package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.local.ScannedRecord
import com.example.data.local.ScannedRecordDao
import com.example.data.model.PostBody
import com.example.data.model.QrPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DispatchRepository(
    private val dao: ScannedRecordDao,
    private val settingsRepo: SettingsRepository
) {
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

    // Try to sync a single saved record directly to Google Apps Script
    suspend fun syncRecord(record: ScannedRecord): SyncStatusResult = withContext(Dispatchers.IO) {
        val scriptUrl = settingsRepo.getAppsScriptUrl()
        if (scriptUrl.isEmpty()) {
            val err = "Google Apps Script URL is empty"
            record.syncStatus = "PENDING"
            record.errorMessage = err
            dao.updateRecord(record)
            return@withContext SyncStatusResult.Failed(err)
        }

        val body = PostBody(
            scannerName = record.scannerName,
            deviceId = record.deviceId,
            payload = QrPayload(
                v = 1,
                orderNo = record.orderNo,
                orderId = record.orderId,
                customer = record.customer,
                district = record.district,
                deliveryDate = record.deliveryDate,
                deliveryType = record.deliveryType,
                salesPerson = record.salesPerson,
                totalUnits = record.totalUnits,
                copy = record.copy,
                tableIndex = record.tableIndex,
                tableCount = record.tableCount,
                size = record.size,
                topColour = record.topColour,
                frameColour = record.frameColour,
                qty = record.qty,
                legSize = record.legSize,
                legShape = record.legShape,
                legHeight = record.legHeight,
                ts = record.qrTimestamp
            ),
            rawPayload = record.rawPayload
        )

        try {
            val response = ApiClient.apiService.syncData(scriptUrl, body)
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if (responseBody.success) {
                        record.syncStatus = "SYNCED"
                        record.errorMessage = null
                        dao.updateRecord(record)
                        SyncStatusResult.Success
                    } else if (responseBody.duplicate == true) {
                        record.syncStatus = "DUPLICATE"
                        record.errorMessage = responseBody.message ?: "This item was already scanned"
                        dao.updateRecord(record)
                        SyncStatusResult.Duplicate(record.errorMessage ?: "Duplicate")
                    } else {
                        record.syncStatus = "FAILED"
                        record.errorMessage = responseBody.message ?: "Server rejected scan"
                        dao.updateRecord(record)
                        SyncStatusResult.Failed(record.errorMessage ?: "Failed")
                    }
                } else {
                    val err = "Empty server response"
                    record.syncStatus = "FAILED"
                    record.errorMessage = err
                    dao.updateRecord(record)
                    SyncStatusResult.Failed(err)
                }
            } else {
                val err = "HTTP Error ${response.code()}"
                record.syncStatus = "FAILED"
                record.errorMessage = err
                dao.updateRecord(record)
                SyncStatusResult.Failed(err)
            }
        } catch (e: Exception) {
            // If offline, keep as PENDING
            record.syncStatus = "PENDING"
            record.errorMessage = "Offline: ${e.localizedMessage ?: "Network issue"}"
            dao.updateRecord(record)
            SyncStatusResult.Offline(record.errorMessage ?: "Network issue")
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
            message = "Batch Sync complete. Synced: $synced, Duplicates: $duplicates, Failures: $failures."
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
