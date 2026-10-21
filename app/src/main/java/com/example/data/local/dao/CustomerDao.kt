package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.CustomerTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE outstandingBalance > 0 ORDER BY outstandingBalance DESC")
    fun getCustomersWithDebt(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET outstandingBalance = outstandingBalance + :balanceChange WHERE id = :customerId")
    suspend fun adjustCustomerBalance(customerId: Long, balanceChange: Double)

    @Query("UPDATE customers SET loyaltyPoints = loyaltyPoints + :points WHERE id = :customerId")
    suspend fun addLoyaltyPoints(customerId: Long, points: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CustomerTransactionEntity): Long

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransactionEntity>>
}
