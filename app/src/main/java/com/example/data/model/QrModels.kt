package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QrPayload(
    val orderNo: String? = null,
    val customer: String? = null,
    val district: String? = null,
    val deliveryType: String? = null,
    val salesPerson: String? = null,
    val copy: String? = null,
    val totalUnits: Int? = null,
    val size: String? = null,
    val topColour: String? = null,
    val frameColour: String? = null,
    val qty: Int? = null,
    val legSize: String? = null,
    val legShape: String? = null,
    val legHeight: String? = null,
    val v: Int? = 1,
    val orderId: String? = null,
    val tableIndex: Int? = null,
    val tableCount: Int? = null,
    val deliveryDate: String? = null,
    val ts: String? = null,
    val vehicleNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class PostBody(
    val action: String? = "APPEND",
    val scannerName: String,
    val deviceId: String,
    val vehicleNumber: String? = null,
    val payload: QrPayload,
    val rawPayload: String
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val success: Boolean,
    val duplicate: Boolean? = null,
    val message: String?,
    val scanId: String? = null,
    val records: List<RemoteRecord>? = null
)

@JsonClass(generateAdapter = true)
data class RemoteRecord(
    val id: String?,
    val timestamp: String?, // QR ts or current time
    val orderNo: String?,
    val orderId: String?,
    val customer: String?,
    val district: String?,
    val deliveryDate: String?,
    val deliveryType: String?,
    val salesPerson: String?,
    val copy: String?,
    val tableIndex: String?,
    val tableCount: String?,
    val size: String?,
    val topColour: String?,
    val frameColour: String?,
    val qty: String?,
    val legSize: String?,
    val legShape: String?,
    val legHeight: String?,
    val duplicateKey: String?,
    val vehicleNumber: String? = null
)
