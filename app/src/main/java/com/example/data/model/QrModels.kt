package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QrPayload(
    val orderNo: String? = null,
    val order_form_number: String? = null,
    val order_no: String? = null,
    val orderNumber: String? = null,
    val customer: String? = null,
    val customer_name: String? = null,
    val district: String? = null,
    val customer_district: String? = null,
    val deliveryType: String? = null,
    val delivery_type: String? = null,
    val salesPerson: String? = null,
    val sales_person_name: String? = null,
    val copy: String? = null,
    val copy_name: String? = null,
    val copy_type: String? = null,
    val copyType: String? = null,
    val totalUnits: Int? = null,
    val size: String? = null,
    val table_size: String? = null,
    val topColour: String? = null,
    val colour: String? = null,
    val frameColour: String? = null,
    val qty: Int? = null,
    val quantity: Int? = null,
    val legSize: String? = null,
    val legShape: String? = null,
    val legHeight: String? = null,
    val v: Int? = 1,
    val orderId: String? = null,
    val order_id: String? = null,
    val id: String? = null,
    val tableIndex: Int? = null,
    val table_index: Int? = null,
    val tableCount: Int? = null,
    val table_count: Int? = null,
    val deliveryDate: String? = null,
    val delivery_date: String? = null,
    val ts: String? = null,
    val vehicleNumber: String? = null
) {
    val resolvedOrderNo: String
        get() = (orderNo ?: order_form_number ?: order_no ?: orderNumber)
            ?.trim()
            ?.removePrefix("#")
            ?.trim()
            .orEmpty()

    val resolvedCopy: String?
        get() = (copy ?: copy_name ?: copy_type ?: copyType)?.trim()?.takeIf { it.isNotEmpty() }

    val resolvedOrderId: String
        get() = (orderId ?: order_id ?: id)?.trim().orEmpty()

    val resolvedCustomer: String
        get() = (customer ?: customer_name)?.trim().orEmpty()

    val resolvedDistrict: String
        get() = (district ?: customer_district)?.trim().orEmpty()

    val resolvedDeliveryType: String
        get() = (deliveryType ?: delivery_type)?.trim().orEmpty()

    val resolvedSalesPerson: String
        get() = (salesPerson ?: sales_person_name)?.trim().orEmpty()

    val resolvedSize: String
        get() = (size ?: table_size)?.trim().orEmpty()

    val resolvedTopColour: String
        get() = (topColour ?: colour)?.trim().orEmpty()

    val resolvedQty: Int
        get() = qty ?: quantity ?: 1

    val resolvedTotalUnits: Int
        get() = totalUnits ?: quantity ?: qty ?: 1

    val resolvedTableIndex: Int
        get() = tableIndex ?: table_index ?: 1

    val resolvedTableCount: Int
        get() = tableCount ?: table_count ?: resolvedTotalUnits

    val resolvedDeliveryDate: String
        get() = (deliveryDate ?: delivery_date)?.trim().orEmpty()

    fun toNormalized(normalizedCopy: String): QrPayload {
        return copy(
            orderNo = resolvedOrderNo,
            customer = resolvedCustomer,
            district = resolvedDistrict,
            deliveryType = resolvedDeliveryType,
            salesPerson = resolvedSalesPerson,
            copy = normalizedCopy,
            totalUnits = resolvedTotalUnits,
            size = resolvedSize,
            topColour = resolvedTopColour,
            qty = resolvedQty,
            orderId = resolvedOrderId,
            tableIndex = resolvedTableIndex,
            tableCount = resolvedTableCount,
            deliveryDate = resolvedDeliveryDate
        )
    }
}

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
