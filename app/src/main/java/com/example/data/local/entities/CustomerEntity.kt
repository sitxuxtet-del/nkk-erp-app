package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val outstandingBalance: Double = 0.0, // positive means customer owes money
    val creditLimit: Double = 500.0,
    val loyaltyPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_transactions")
data class CustomerTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val orderId: String? = null,
    val type: String, // "CREDIT_SALE" (customer owes more), "PAYMENT_RECEIVED" (customer settled)
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
