package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NkkErpDatabase
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.CustomerTransactionEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.SaleOrderEntity
import com.example.data.local.entities.SaleOrderItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: NkkErpDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NkkErpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testProductStorageAndStockAdjustment() = runBlocking {
        val product = ProductEntity(
            barcode = "8901234567",
            name = "Organic Green Tea",
            category = "Beverages",
            costPrice = 3.50,
            sellingPrice = 6.99,
            stockQuantity = 25.0,
            minStockAlert = 5.0,
            unit = "box"
        )
        val id = db.productDao().insertProduct(product)

        val retrieved = db.productDao().getProductById(id)
        assertNotNull(retrieved)
        assertEquals("Organic Green Tea", retrieved?.name)
        assertEquals(25.0, retrieved?.stockQuantity ?: 0.0, 0.01)

        // Decrement stock when sold
        db.productDao().adjustStock(id, -3.0)
        val updated = db.productDao().getProductById(id)
        assertEquals(22.0, updated?.stockQuantity ?: 0.0, 0.01)

        // Verify search
        val searchResult = db.productDao().searchProducts("Green Tea").first()
        assertEquals(1, searchResult.size)
        assertEquals("8901234567", searchResult[0].barcode)
    }

    @Test
    fun testOrderWithItemsRelation() = runBlocking {
        val order = SaleOrderEntity(
            orderId = "INV-20261021-9999",
            customerName = "Jane Doe",
            cashierName = "Cashier Bob",
            subtotal = 30.00,
            grandTotal = 31.50,
            paymentMethod = "CASH",
            paidAmount = 40.00,
            changeAmount = 8.50,
            status = "COMPLETED"
        )
        db.orderDao().insertOrder(order)

        val item1 = SaleOrderItemEntity(
            orderId = order.orderId,
            productId = 101,
            productName = "Product A",
            unitPrice = 15.00,
            quantity = 2.0,
            itemTotal = 30.00
        )
        db.orderDao().insertOrderItems(listOf(item1))

        val orderWithItems = db.orderDao().getOrderWithItems(order.orderId)
        assertNotNull(orderWithItems)
        assertEquals("INV-20261021-9999", orderWithItems?.order?.orderId)
        assertEquals(1, orderWithItems?.items?.size)
        assertEquals("Product A", orderWithItems?.items?.get(0)?.productName)
    }

    @Test
    fun testCustomerKhataLedger() = runBlocking {
        val customer = CustomerEntity(
            name = "Apex Industries",
            phone = "+1 555-4321",
            outstandingBalance = 0.0,
            creditLimit = 1500.0
        )
        val customerId = db.customerDao().insertCustomer(customer)

        // Record credit sale
        db.customerDao().adjustCustomerBalance(customerId, 250.00)
        db.customerDao().insertTransaction(
            CustomerTransactionEntity(
                customerId = customerId,
                type = "CREDIT_SALE",
                amount = 250.00,
                note = "Delivery of dry goods"
            )
        )

        var loaded = db.customerDao().getCustomerById(customerId)
        assertEquals(250.00, loaded?.outstandingBalance ?: 0.0, 0.01)

        // Record payment settlement
        db.customerDao().adjustCustomerBalance(customerId, -100.00)
        db.customerDao().insertTransaction(
            CustomerTransactionEntity(
                customerId = customerId,
                type = "PAYMENT_RECEIVED",
                amount = 100.00,
                note = "Check #1092"
            )
        )

        loaded = db.customerDao().getCustomerById(customerId)
        assertEquals(150.00, loaded?.outstandingBalance ?: 0.0, 0.01)

        val transactions = db.customerDao().getCustomerTransactions(customerId).first()
        assertEquals(2, transactions.size)
    }
}
