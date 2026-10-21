package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_order_items",
    indices = [Index(value = ["orderId"]), Index(value = ["productId"])]
)
data class SaleOrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: String,
    val productId: Long,
    val productName: String,
    val barcode: String = "",
    val unitPrice: Double,
    val costPrice: Double = 0.0,
    val quantity: Double,
    val unit: String = "pcs",
    val discountPercent: Double = 0.0,
    val itemTotal: Double
)
