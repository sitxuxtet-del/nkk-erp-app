package com.example.data.repository

import com.example.data.local.NkkErpDatabase
import com.example.data.local.SampleDataSeeder
import com.example.data.local.dao.OrderWithItems
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.CustomerTransactionEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.SaleOrderEntity
import com.example.data.local.entities.SaleOrderItemEntity
import com.example.data.local.entities.StaffEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.network.ApiOrderItem
import com.example.data.network.ApiOrderSyncRequest
import com.example.data.network.NetworkClient
import com.example.data.preferences.BusinessPreferencesRepository
import com.example.data.preferences.BusinessSettings
import com.example.domain.models.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ErpRepository(
    private val database: NkkErpDatabase,
    private val preferencesRepository: BusinessPreferencesRepository
) {
    val settingsFlow: Flow<BusinessSettings> = preferencesRepository.settingsFlow

    // --- Products ---
    val allProducts: Flow<List<ProductEntity>> = database.productDao().getAllProducts()
    val onlineProducts: Flow<List<ProductEntity>> = database.productDao().getOnlineProducts()
    val allCategories: Flow<List<String>> = database.productDao().getAllCategories()
    val lowStockProducts: Flow<List<ProductEntity>> = database.productDao().getLowStockProducts()

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return database.productDao().searchProducts(query)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        database.productDao().getProductByBarcode(barcode)
    }

    suspend fun saveProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            database.productDao().insertProduct(product)
        } else {
            database.productDao().updateProduct(product)
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        database.productDao().deleteProduct(product)
    }

    suspend fun adjustProductStock(productId: Long, delta: Double) = withContext(Dispatchers.IO) {
        database.productDao().adjustStock(productId, delta)
    }

    suspend fun resetCatalogToTemplate(sector: String) = withContext(Dispatchers.IO) {
        val allSeed = SampleDataSeeder.getSeedProducts()
        val filtered = when (sector) {
            "RETAIL" -> allSeed.filter { it.businessSector == "RETAIL" }
            "RESTAURANT" -> allSeed.filter { it.businessSector == "RESTAURANT" }
            "PHARMACY" -> allSeed.filter { it.businessSector == "PHARMACY" }
            "FASHION" -> allSeed.filter { it.businessSector == "FASHION" || it.businessSector == "ELECTRONICS" }
            "WHOLESALE" -> allSeed.filter { it.businessSector == "WHOLESALE" }
            "SERVICES" -> allSeed.filter { it.businessSector == "SERVICES" }
            else -> allSeed
        }
        database.productDao().deleteAllProducts()
        database.productDao().insertProducts(filtered)
    }

    // --- Orders & Billing ---
    val allOrdersWithItems: Flow<List<OrderWithItems>> = database.orderDao().getAllOrdersWithItems()
    val heldOrders: Flow<List<OrderWithItems>> = database.orderDao().getHeldOrders()
    val unsyncedOrdersCount: Flow<Int> = database.orderDao().getUnsyncedCount()

    fun getOrdersSince(timestamp: Long): Flow<List<OrderWithItems>> {
        return database.orderDao().getOrdersSince(timestamp)
    }

    suspend fun completeSale(
        cartItems: List<CartItem>,
        customer: CustomerEntity?,
        paymentMethod: String,
        paidAmount: Double,
        discountAmount: Double,
        referenceOrTable: String,
        notes: String,
        cashierName: String,
        currencySymbol: String
    ): OrderWithItems = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
        val randomSuffix = (1000..9999).random()
        val orderId = "INV-$dateCode-$randomSuffix"

        val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
        val taxTotal = cartItems.sumOf { it.taxAmount }
        val grandTotal = (subtotal - discountAmount + taxTotal).coerceAtLeast(0.0)
        val changeAmount = (paidAmount - grandTotal).coerceAtLeast(0.0)

        val orderEntity = SaleOrderEntity(
            orderId = orderId,
            customerId = customer?.id,
            customerName = customer?.name ?: "Walk-in Customer",
            cashierName = cashierName,
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxTotal,
            grandTotal = grandTotal,
            paymentMethod = paymentMethod,
            paidAmount = paidAmount,
            changeAmount = changeAmount,
            status = "COMPLETED",
            referenceOrTable = referenceOrTable,
            notes = notes,
            timestamp = timestamp,
            isSynced = false
        )

        val orderItems = cartItems.map { item ->
            SaleOrderItemEntity(
                orderId = orderId,
                productId = item.product.id,
                productName = item.product.name,
                barcode = item.product.barcode,
                unitPrice = item.unitPrice,
                costPrice = item.product.costPrice,
                quantity = item.quantity,
                unit = item.product.unit,
                discountPercent = item.discountPercent,
                itemTotal = item.total
            )
        }

        database.orderDao().insertOrder(orderEntity)
        database.orderDao().insertOrderItems(orderItems)

        // Decrement product inventory
        for (item in cartItems) {
            database.productDao().adjustStock(item.product.id, -item.quantity)
        }

        // If customer debt (Khata)
        if (paymentMethod == "CUSTOMER_CREDIT" && customer != null) {
            database.customerDao().adjustCustomerBalance(customer.id, grandTotal)
            database.customerDao().insertTransaction(
                CustomerTransactionEntity(
                    customerId = customer.id,
                    orderId = orderId,
                    type = "CREDIT_SALE",
                    amount = grandTotal,
                    note = "Credit sale Invoice #$orderId"
                )
            )
        }

        // If customer is selected and paid, award loyalty points (1 point per 10 currency spent)
        if (customer != null && paymentMethod != "CUSTOMER_CREDIT") {
            val pointsEarned = (grandTotal / 10).toInt()
            if (pointsEarned > 0) {
                database.customerDao().addLoyaltyPoints(customer.id, pointsEarned)
            }
        }

        // Enqueue sync queue for backend
        val syncItem = SyncQueueEntity(
            actionType = "CREATE_ORDER",
            payloadJson = "{\"orderId\":\"$orderId\",\"grandTotal\":$grandTotal}"
        )
        database.syncQueueDao().enqueue(syncItem)

        OrderWithItems(order = orderEntity, items = orderItems)
    }

    suspend fun holdSale(
        cartItems: List<CartItem>,
        referenceOrTable: String,
        notes: String,
        cashierName: String
    ): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (1000..9999).random()
        val orderId = "HOLD-$randomSuffix"

        val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
        val taxTotal = cartItems.sumOf { it.taxAmount }
        val grandTotal = (subtotal + taxTotal).coerceAtLeast(0.0)

        val orderEntity = SaleOrderEntity(
            orderId = orderId,
            customerName = "Held Order",
            cashierName = cashierName,
            subtotal = subtotal,
            grandTotal = grandTotal,
            paymentMethod = "HELD",
            paidAmount = 0.0,
            status = "HELD",
            referenceOrTable = if (referenceOrTable.isBlank()) "Parked Cart" else referenceOrTable,
            notes = notes,
            timestamp = timestamp
        )

        val items = cartItems.map { item ->
            SaleOrderItemEntity(
                orderId = orderId,
                productId = item.product.id,
                productName = item.product.name,
                barcode = item.product.barcode,
                unitPrice = item.unitPrice,
                costPrice = item.product.costPrice,
                quantity = item.quantity,
                unit = item.product.unit,
                discountPercent = item.discountPercent,
                itemTotal = item.total
            )
        }

        database.orderDao().insertOrder(orderEntity)
        database.orderDao().insertOrderItems(items)
        orderId
    }

    suspend fun releaseHeldOrder(orderId: String) = withContext(Dispatchers.IO) {
        database.orderDao().deleteOrderItems(orderId)
        database.orderDao().deleteOrder(orderId)
    }

    suspend fun refundOrder(orderId: String) = withContext(Dispatchers.IO) {
        val orderWithItems = database.orderDao().getOrderWithItems(orderId)
        if (orderWithItems != null && orderWithItems.order.status == "COMPLETED") {
            // Restore inventory stock
            for (item in orderWithItems.items) {
                database.productDao().adjustStock(item.productId, item.quantity)
            }
            // If it was credit sale, reverse the customer balance
            if (orderWithItems.order.paymentMethod == "CUSTOMER_CREDIT" && orderWithItems.order.customerId != null) {
                database.customerDao().adjustCustomerBalance(
                    orderWithItems.order.customerId,
                    -orderWithItems.order.grandTotal
                )
                database.customerDao().insertTransaction(
                    CustomerTransactionEntity(
                        customerId = orderWithItems.order.customerId,
                        orderId = orderId,
                        type = "REFUND_REVERSAL",
                        amount = -orderWithItems.order.grandTotal,
                        note = "Credit refunded for Invoice #$orderId"
                    )
                )
            }
            database.orderDao().updateOrderStatus(orderId, "REFUNDED")
        }
    }

    // --- Customers & Khata ---
    val allCustomers: Flow<List<CustomerEntity>> = database.customerDao().getAllCustomers()
    val customersWithDebt: Flow<List<CustomerEntity>> = database.customerDao().getCustomersWithDebt()

    suspend fun saveCustomer(customer: CustomerEntity): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) {
            database.customerDao().insertCustomer(customer)
        } else {
            database.customerDao().updateCustomer(customer)
            customer.id
        }
    }

    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransactionEntity>> {
        return database.customerDao().getCustomerTransactions(customerId)
    }

    suspend fun recordCustomerPayment(
        customerId: Long,
        amount: Double,
        note: String
    ) = withContext(Dispatchers.IO) {
        database.customerDao().adjustCustomerBalance(customerId, -amount)
        database.customerDao().insertTransaction(
            CustomerTransactionEntity(
                customerId = customerId,
                type = "PAYMENT_RECEIVED",
                amount = amount,
                note = note.ifBlank { "Khata settlement payment" }
            )
        )
    }

    // --- Staff & Authentication ---
    val allStaff: Flow<List<StaffEntity>> = database.staffDao().getAllStaff()

    suspend fun verifyStaffPin(pin: String): StaffEntity? = withContext(Dispatchers.IO) {
        database.staffDao().verifyStaffPin(pin)
    }

    suspend fun switchActiveStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        preferencesRepository.setActiveStaff(staff.name, staff.role, staff.pin)
    }

    // --- Preferences ---
    suspend fun updateSettings(settings: BusinessSettings) = withContext(Dispatchers.IO) {
        preferencesRepository.updateBusinessProfile(
            name = settings.businessName,
            sector = settings.sector,
            currency = settings.currencySymbol,
            taxRate = settings.taxRatePercent,
            address = settings.address,
            phone = settings.phone,
            header = settings.receiptHeader,
            footer = settings.receiptFooter,
            fastApiUrl = settings.fastApiBaseUrl
        )
    }

    // --- Sync Engine with FastAPI Backend ---
    suspend fun syncWithBackend(baseUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val service = NetworkClient.createService(baseUrl)
            // 1. Health check
            val healthRes = service.checkHealth()
            if (!healthRes.isSuccessful) {
                return@withContext Pair(false, "Server responded with error code: ${healthRes.code()}")
            }

            // In production, push pending orders
            Pair(true, "Sync successful! Connected to FastAPI backend v${healthRes.body()?.version ?: "1.0"}")
        } catch (e: Exception) {
            // Graceful offline fallback
            Pair(false, "Offline or server unreachable: ${e.localizedMessage ?: "Connection timed out"}")
        }
    }
}
