package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QrPayload(
    val orderNo: String?,
    val customer: String?,
    val district: String?,
    val deliveryType: String?,
    val salesPerson: String?,
    val copy: String?,
    val totalUnits: Int?,
    val size: String?,
    val topColour: String?,
    val frameColour: String?,
    val qty: Int?,
    val legSize: String?,
    val legShape: String?,
    val legHeight: String?,
    val v: Int? = 1,
    val orderId: String? = null,
    val tableIndex: Int? = null,
    val tableCount: Int? = null,
    val deliveryDate: String? = null,
    val ts: String? = null
)

@JsonClass(generateAdapter = true)
data class PostBody(
    val scannerName: String,
    val deviceId: String,
    val payload: QrPayload,
    val rawPayload: String
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val success: Boolean,
    val duplicate: Boolean? = null,
    val message: String?,
    val scanId: String? = null
)
