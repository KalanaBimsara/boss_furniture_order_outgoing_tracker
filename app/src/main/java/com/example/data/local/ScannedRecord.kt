package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_records")
data class ScannedRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scannedAt: Long = System.currentTimeMillis(),
    val scannerName: String,
    val deviceId: String,
    
    // QR payload fields
    val orderNo: String,
    val orderId: String,
    val customer: String,
    val district: String,
    val deliveryDate: String,
    val deliveryType: String,
    val salesPerson: String,
    val totalUnits: Int,
    val copy: String,
    val tableIndex: Int,
    val tableCount: Int,
    val size: String,
    val topColour: String,
    val frameColour: String,
    val qty: Int,
    val legSize: String,
    val legShape: String,
    val legHeight: String,
    val qrTimestamp: String,
    
    // Generated keys
    val duplicateKey: String,
    val rawPayload: String,
    
    // Sync status: "PENDING", "SYNCED", "DUPLICATE", "FAILED"
    var syncStatus: String,
    var errorMessage: String? = null
)
