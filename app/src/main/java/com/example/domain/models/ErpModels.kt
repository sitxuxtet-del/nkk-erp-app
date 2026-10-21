package com.example.domain.models

import com.example.data.local.entities.ProductEntity

enum class BusinessSector(val displayName: String, val iconName: String, val defaultTax: Double) {
    RETAIL("Supermarket & Retail", "store", 5.0),
    RESTAURANT("Restaurant & Cafe", "restaurant", 8.0),
    PHARMACY("Pharmacy & Health", "medication", 0.0),
    FASHION("Fashion & Electronics", "shopping_bag", 10.0),
    WHOLESALE("Wholesale & Distribution", "warehouse", 5.0),
    SERVICES("Salon & Services", "spa", 10.0)
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    CARD("Debit / Credit Card"),
    UPI_QR_WALLET("Mobile / QR / UPI"),
    CUSTOMER_CREDIT("Customer Credit (Khata)"),
    SPLIT("Split Payment")
}

data class CartItem(
    val product: ProductEntity,
    val quantity: Double = 1.0,
    val customPrice: Double? = null,
    val discountPercent: Double = 0.0,
    val notes: String = ""
) {
    val unitPrice: Double
        get() = customPrice ?: product.sellingPrice

    val subtotal: Double
        get() = (unitPrice * quantity) * (1.0 - (discountPercent / 100.0))

    val taxAmount: Double
        get() = subtotal * (product.taxRatePercent / 100.0)

    val total: Double
        get() = subtotal + taxAmount
}

data class DailyReportStats(
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalOrdersCount: Int = 0,
    val cashTotal: Double = 0.0,
    val cardTotal: Double = 0.0,
    val qrTotal: Double = 0.0,
    val creditTotal: Double = 0.0,
    val totalTaxCollected: Double = 0.0,
    val totalDiscountGiven: Double = 0.0,
    val averageTicketValue: Double = 0.0,
    val topSellingItems: List<Pair<String, Double>> = emptyList()
)
