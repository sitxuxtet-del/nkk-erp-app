package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val name: String,
    val category: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Double,
    val minStockAlert: Double = 5.0,
    val unit: String = "pcs", // pcs, kg, ltr, pack, box, dose
    val isOnlineShopVisible: Boolean = true,
    val taxRatePercent: Double = 0.0,
    val expiryDate: String? = null, // e.g. "2027-12-31" for pharmacy/groceries
    val batchNumber: String? = null,
    val description: String = "",
    val businessSector: String = "RETAIL"
)
