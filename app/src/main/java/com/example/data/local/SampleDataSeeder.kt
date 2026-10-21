package com.example.data.local

import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.CustomerTransactionEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.SaleOrderEntity
import com.example.data.local.entities.SaleOrderItemEntity
import com.example.data.local.entities.StaffEntity

object SampleDataSeeder {

    suspend fun seedInitialData(database: NkkErpDatabase) {
        val productDao = database.productDao()
        val customerDao = database.customerDao()
        val staffDao = database.staffDao()
        val orderDao = database.orderDao()

        // 1. Seed Staff Members
        val staffList = listOf(
            StaffEntity(id = 1, name = "Alex Vance (Owner/Admin)", role = "ADMIN", pin = "1234"),
            StaffEntity(id = 2, name = "Sara Connor (Cashier)", role = "CASHIER", pin = "0000"),
            StaffEntity(id = 3, name = "David Miller (Stock Mgr)", role = "INVENTORY_MANAGER", pin = "5555"),
            StaffEntity(id = 4, name = "Elena Rostova (Accountant)", role = "ACCOUNTANT", pin = "8888")
        )
        staffDao.insertStaffList(staffList)

        // 2. Seed Customers
        val customer1Id = customerDao.insertCustomer(
            CustomerEntity(
                name = "Acme Retailers",
                phone = "+1 555-0192",
                email = "buyer@acmeretail.com",
                address = "742 Evergreen Terrace",
                outstandingBalance = 145.50,
                creditLimit = 1000.0,
                loyaltyPoints = 320
            )
        )
        val customer2Id = customerDao.insertCustomer(
            CustomerEntity(
                name = "Dr. Michael Chen",
                phone = "+1 555-0843",
                email = "dr.chen@clinic.org",
                address = "12 Medical Plaza Suite 4",
                outstandingBalance = 0.0,
                creditLimit = 500.0,
                loyaltyPoints = 150
            )
        )
        val customer3Id = customerDao.insertCustomer(
            CustomerEntity(
                name = "Sophia Martinez",
                phone = "+1 555-7391",
                email = "sophia.m@gmail.com",
                address = "45 Ocean Drive Apt 2B",
                outstandingBalance = 65.00,
                creditLimit = 300.0,
                loyaltyPoints = 480
            )
        )

        // Log initial ledger transactions
        customerDao.insertTransaction(
            CustomerTransactionEntity(
                customerId = customer1Id,
                type = "CREDIT_SALE",
                amount = 145.50,
                note = "Previous credit invoice #INV-OLD-901"
            )
        )
        customerDao.insertTransaction(
            CustomerTransactionEntity(
                customerId = customer3Id,
                type = "CREDIT_SALE",
                amount = 65.00,
                note = "Unpaid balance for dry goods"
            )
        )

        // 3. Seed Products covering Retail, Cafe, Pharmacy, Electronics, Wholesale, Salon
        val products = getSeedProducts()
        productDao.insertProducts(products)

        // 4. Seed a couple of historical sale orders for reporting & analytics
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000L
        val threeHoursAgo = now - 10800000L
        val yesterday = now - 86400000L

        // Order 1
        val order1 = SaleOrderEntity(
            orderId = "INV-20261021-0001",
            customerId = customer2Id,
            customerName = "Dr. Michael Chen",
            cashierName = "Sara Connor",
            subtotal = 38.50,
            discountAmount = 0.0,
            taxAmount = 1.93,
            grandTotal = 40.43,
            paymentMethod = "CARD",
            paidAmount = 40.43,
            changeAmount = 0.0,
            status = "COMPLETED",
            referenceOrTable = "Counter 1",
            timestamp = threeHoursAgo,
            isSynced = true
        )
        orderDao.insertOrder(order1)
        orderDao.insertOrderItems(
            listOf(
                SaleOrderItemEntity(
                    orderId = order1.orderId,
                    productId = 1,
                    productName = "Organic Whole Milk 1L",
                    barcode = "8901234001",
                    unitPrice = 3.50,
                    costPrice = 2.10,
                    quantity = 2.0,
                    itemTotal = 7.00
                ),
                SaleOrderItemEntity(
                    orderId = order1.orderId,
                    productId = 3,
                    productName = "Artisan Espresso Beans 500g",
                    barcode = "8901234003",
                    unitPrice = 14.50,
                    costPrice = 8.00,
                    quantity = 1.0,
                    itemTotal = 14.50
                ),
                SaleOrderItemEntity(
                    orderId = order1.orderId,
                    productId = 7,
                    productName = "Paracetamol 500mg (20 Tabs)",
                    barcode = "8901234007",
                    unitPrice = 8.50,
                    costPrice = 3.50,
                    quantity = 2.0,
                    itemTotal = 17.00
                )
            )
        )

        // Order 2 (Held order / Table order)
        val heldOrder = SaleOrderEntity(
            orderId = "INV-20261021-0002",
            customerName = "Walk-in Guest",
            cashierName = "Alex Vance",
            subtotal = 22.00,
            discountAmount = 2.00,
            taxAmount = 1.00,
            grandTotal = 21.00,
            paymentMethod = "CASH",
            paidAmount = 0.0,
            status = "HELD",
            referenceOrTable = "Table 04",
            notes = "Customer seated on terrace",
            timestamp = oneHourAgo,
            isSynced = false
        )
        orderDao.insertOrder(heldOrder)
        orderDao.insertOrderItems(
            listOf(
                SaleOrderItemEntity(
                    orderId = heldOrder.orderId,
                    productId = 4,
                    productName = "Caramel Macchiato Latte",
                    barcode = "8901234004",
                    unitPrice = 5.50,
                    costPrice = 1.80,
                    quantity = 2.0,
                    itemTotal = 11.00
                ),
                SaleOrderItemEntity(
                    orderId = heldOrder.orderId,
                    productId = 5,
                    productName = "Blueberry Danish Pastry",
                    barcode = "8901234005",
                    unitPrice = 5.50,
                    costPrice = 2.00,
                    quantity = 2.0,
                    itemTotal = 11.00
                )
            )
        )
    }

    fun getSeedProducts(): List<ProductEntity> {
        return listOf(
            // Retail & Supermarket
            ProductEntity(
                id = 1,
                barcode = "8901234001",
                name = "Organic Whole Milk 1L",
                category = "Dairy & Groceries",
                costPrice = 2.10,
                sellingPrice = 3.50,
                stockQuantity = 48.0,
                minStockAlert = 10.0,
                unit = "pcs",
                taxRatePercent = 5.0,
                isOnlineShopVisible = true,
                description = "Pasteurized farm-fresh whole milk",
                businessSector = "RETAIL"
            ),
            ProductEntity(
                id = 2,
                barcode = "8901234002",
                name = "Sourdough Bread Loaf",
                category = "Bakery",
                costPrice = 1.80,
                sellingPrice = 4.20,
                stockQuantity = 15.0,
                minStockAlert = 5.0,
                unit = "pcs",
                taxRatePercent = 5.0,
                isOnlineShopVisible = true,
                description = "Handcrafted artisan rustic sourdough",
                businessSector = "RETAIL"
            ),
            ProductEntity(
                id = 3,
                barcode = "8901234003",
                name = "Artisan Espresso Beans 500g",
                category = "Beverages & Coffee",
                costPrice = 8.00,
                sellingPrice = 14.50,
                stockQuantity = 22.0,
                minStockAlert = 6.0,
                unit = "pack",
                taxRatePercent = 5.0,
                isOnlineShopVisible = true,
                description = "Single-origin Arabica dark roast",
                businessSector = "RETAIL"
            ),

            // Restaurant & Cafe
            ProductEntity(
                id = 4,
                barcode = "8901234004",
                name = "Caramel Macchiato Latte",
                category = "Hot Drinks",
                costPrice = 1.80,
                sellingPrice = 5.50,
                stockQuantity = 999.0,
                minStockAlert = 20.0,
                unit = "cup",
                taxRatePercent = 8.0,
                isOnlineShopVisible = true,
                description = "Steamed milk with vanilla and espresso",
                businessSector = "RESTAURANT"
            ),
            ProductEntity(
                id = 5,
                barcode = "8901234005",
                name = "Blueberry Danish Pastry",
                category = "Pastries & Desserts",
                costPrice = 2.00,
                sellingPrice = 5.50,
                stockQuantity = 12.0,
                minStockAlert = 4.0,
                unit = "pcs",
                taxRatePercent = 8.0,
                isOnlineShopVisible = true,
                description = "Flaky butter crust with wild blueberries",
                businessSector = "RESTAURANT"
            ),
            ProductEntity(
                id = 6,
                barcode = "8901234006",
                name = "Truffle Mushroom Burger",
                category = "Kitchen Mains",
                costPrice = 4.50,
                sellingPrice = 13.90,
                stockQuantity = 50.0,
                minStockAlert = 10.0,
                unit = "portion",
                taxRatePercent = 8.0,
                isOnlineShopVisible = true,
                description = "Brioche bun, melted provolone, black truffle aioli",
                businessSector = "RESTAURANT"
            ),

            // Pharmacy
            ProductEntity(
                id = 7,
                barcode = "8901234007",
                name = "Paracetamol 500mg (20 Tabs)",
                category = "Pain Relief",
                costPrice = 3.50,
                sellingPrice = 8.50,
                stockQuantity = 85.0,
                minStockAlert = 15.0,
                unit = "box",
                taxRatePercent = 0.0,
                isOnlineShopVisible = true,
                expiryDate = "2027-08-30",
                batchNumber = "BATCH-PCT-441",
                description = "Fast acting analgesic & antipyretic tablets",
                businessSector = "PHARMACY"
            ),
            ProductEntity(
                id = 8,
                barcode = "8901234008",
                name = "Vitamin C 1000mg + Zinc Effervescent",
                category = "Vitamins & Supplements",
                costPrice = 5.20,
                sellingPrice = 11.90,
                stockQuantity = 3.0, // LOW STOCK TRIGGER
                minStockAlert = 8.0,
                unit = "tube",
                taxRatePercent = 0.0,
                isOnlineShopVisible = true,
                expiryDate = "2028-01-15",
                batchNumber = "BATCH-VTC-902",
                description = "Immune support orange flavor 20 tablets",
                businessSector = "PHARMACY"
            ),

            // Fashion & Electronics
            ProductEntity(
                id = 9,
                barcode = "8901234009",
                name = "USB-C Fast Charging Cable 65W",
                category = "Accessories",
                costPrice = 4.00,
                sellingPrice = 12.00,
                stockQuantity = 34.0,
                minStockAlert = 10.0,
                unit = "pcs",
                taxRatePercent = 10.0,
                isOnlineShopVisible = true,
                description = "Braided 2m heavy duty cable",
                businessSector = "ELECTRONICS"
            ),
            ProductEntity(
                id = 10,
                barcode = "8901234010",
                name = "Oversized Cotton Crewneck (L/Black)",
                category = "Apparel",
                costPrice = 11.00,
                sellingPrice = 29.99,
                stockQuantity = 18.0,
                minStockAlert = 5.0,
                unit = "pcs",
                taxRatePercent = 10.0,
                isOnlineShopVisible = true,
                description = "100% heavyweight 280gsm combed cotton",
                businessSector = "FASHION"
            ),

            // Wholesale & Distribution
            ProductEntity(
                id = 11,
                barcode = "8901234011",
                name = "Basmati Rice Master Carton (10 x 5kg)",
                category = "Bulk Grains",
                costPrice = 65.00,
                sellingPrice = 92.00,
                stockQuantity = 14.0,
                minStockAlert = 5.0,
                unit = "carton",
                taxRatePercent = 5.0,
                isOnlineShopVisible = false,
                description = "Premium long grain aged basmati wholesale carton",
                businessSector = "WHOLESALE"
            ),

            // Salon & Services
            ProductEntity(
                id = 12,
                barcode = "8901234012",
                name = "Signature Styling & Haircut",
                category = "Salon Hair",
                costPrice = 2.00,
                sellingPrice = 28.00,
                stockQuantity = 999.0,
                minStockAlert = 0.0,
                unit = "session",
                taxRatePercent = 10.0,
                isOnlineShopVisible = true,
                description = "Wash, scalp massage, master styling",
                businessSector = "SERVICES"
            )
        )
    }
}
