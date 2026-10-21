package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.data.local.entities.SaleOrderEntity
import com.example.data.local.entities.SaleOrderItemEntity
import kotlinx.coroutines.flow.Flow

data class OrderWithItems(
    @Embedded val order: SaleOrderEntity,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val items: List<SaleOrderItemEntity>
)

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM sale_orders ORDER BY timestamp DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM sale_orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderWithItems(orderId: String): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM sale_orders WHERE status = 'HELD' ORDER BY timestamp DESC")
    fun getHeldOrders(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM sale_orders WHERE timestamp >= :startOfDayTimestamp ORDER BY timestamp DESC")
    fun getOrdersSince(startOfDayTimestamp: Long): Flow<List<OrderWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SaleOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<SaleOrderItemEntity>)

    @Query("UPDATE sale_orders SET status = :status WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("UPDATE sale_orders SET isSynced = 1 WHERE orderId = :orderId")
    suspend fun markOrderSynced(orderId: String)

    @Query("SELECT COUNT(*) FROM sale_orders WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("DELETE FROM sale_orders WHERE orderId = :orderId")
    suspend fun deleteOrder(orderId: String)

    @Query("DELETE FROM sale_order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: String)
}
