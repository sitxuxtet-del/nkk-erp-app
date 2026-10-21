package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_orders")
data class SaleOrderEntity(
    @PrimaryKey
    val orderId: String, // e.g. "INV-20261021-0001"
    val customerId: Long? = null,
    val customerName: String? = "Walk-in Customer",
    val cashierId: Long? = 1,
    val cashierName: String = "Admin / Cashier",
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val paymentMethod: String, // CASH, CARD, UPI_QR_WALLET, CUSTOMER_CREDIT, SPLIT
    val paidAmount: Double,
    val changeAmount: Double = 0.0,
    val status: String = "COMPLETED", // COMPLETED, HELD, REFUNDED
    val referenceOrTable: String = "", // e.g. "Table 4" or "Takeaway #12"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
