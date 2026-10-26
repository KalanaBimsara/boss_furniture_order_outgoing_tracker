package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_records")
data class ScannedRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scannedAt: Long = System.currentTimeMillis(),
    val scannerName: String,
    val deviceId: String,
    
    // Operation mode: "DISPATCH" or "RETURN"
    val mode: String = "DISPATCH",
    val restocked: Boolean = false,
    val vehicleNumber: String = "",
    val notes: String = "",

    // QR payload fields
    val orderNo: String,
    val orderId: String = "",
    val customer: String = "",
    val district: String = "",
    val deliveryDate: String = "",
    val deliveryType: String = "",
    val salesPerson: String = "",
    val totalUnits: Int = 1,
    val copy: String = "",
    val tableIndex: Int = 0,
    val tableCount: Int = 0,
    val size: String = "",
    val topColour: String = "",
    val frameColour: String = "",
    val qty: Int = 1,
    val legSize: String = "",
    val legShape: String = "",
    val legHeight: String = "",
    val qrTimestamp: String = "",
    
    // Generated keys
    val duplicateKey: String = "",
    val rawPayload: String = "",
    
    // Sync status: "PENDING", "SYNCED", "DUPLICATE", "FAILED"
    var syncStatus: String = "PENDING",
    var errorMessage: String? = null
)
