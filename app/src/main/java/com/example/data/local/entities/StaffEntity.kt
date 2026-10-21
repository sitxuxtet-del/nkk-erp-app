package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff_members")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val role: String, // "ADMIN", "CASHIER", "MANAGER", "ACCOUNTANT"
    val pin: String, // 4-digit PIN for quick POS unlocking
    val phone: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // "CREATE_ORDER", "UPDATE_STOCK", "CREATE_CUSTOMER"
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
