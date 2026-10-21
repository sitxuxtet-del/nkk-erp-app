package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.OrderWithItems
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.StaffEntity
import com.example.data.preferences.BusinessSettings
import com.example.data.repository.ErpRepository
import com.example.domain.models.BusinessSector
import com.example.domain.models.CartItem
import com.example.domain.models.DailyReportStats
import com.example.domain.models.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ErpScreen {
    POS_REGISTER,
    INVENTORY,
    ORDERS_BILLING,
    CUSTOMERS_KHATA,
    ANALYTICS,
    ONLINE_STORE,
    SETTINGS
}

data class PosUiState(
    val cartItems: List<CartItem> = emptyList(),
    val selectedCustomer: CustomerEntity? = null,
    val tableOrReference: String = "",
    val notes: String = "",
    val discountPercent: Double = 0.0,
    val selectedCategory: String = "ALL",
    val searchQuery: String = "",
    val isPaymentModalOpen: Boolean = false,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val tenderedAmountText: String = "",
    val lastCompletedOrder: OrderWithItems? = null,
    val isReceiptDialogOpen: Boolean = false,
    val isBarcodeScannerOpen: Boolean = false,
    val scanMessage: String? = null
)

class ErpViewModel(
    private val repository: ErpRepository
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(ErpScreen.POS_REGISTER)
    val currentScreen: StateFlow<ErpScreen> = _currentScreen.asStateFlow()

    val settings: StateFlow<BusinessSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BusinessSettings()
    )

    private val _posState = MutableStateFlow(PosUiState())
    val posState: StateFlow<PosUiState> = _posState.asStateFlow()

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val onlineProducts: StateFlow<List<ProductEntity>> = repository.onlineProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<String>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allOrdersWithItems: StateFlow<List<OrderWithItems>> = repository.allOrdersWithItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val heldOrders: StateFlow<List<OrderWithItems>> = repository.heldOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unsyncedOrdersCount: StateFlow<Int> = repository.unsyncedOrdersCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allStaff: StateFlow<List<StaffEntity>> = repository.allStaff.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered POS products based on category and search
    val filteredPosProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _posState
    ) { products, pos ->
        products.filter { p ->
            val matchCategory = pos.selectedCategory == "ALL" || p.category.equals(pos.selectedCategory, ignoreCase = true)
            val matchSearch = pos.searchQuery.isBlank() ||
                    p.name.contains(pos.searchQuery, ignoreCase = true) ||
                    p.barcode.contains(pos.searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Analytics
    val dailyStats: StateFlow<DailyReportStats> = allOrdersWithItems.map { orders ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val todayOrders = orders.filter { it.order.timestamp >= startOfDay && it.order.status == "COMPLETED" }

        var revenue = 0.0
        var totalCost = 0.0
        var cash = 0.0
        var card = 0.0
        var qr = 0.0
        var credit = 0.0
        var tax = 0.0
        var discount = 0.0
        val itemQtyMap = mutableMapOf<String, Double>()

        for (ordWithItems in todayOrders) {
            val ord = ordWithItems.order
            revenue += ord.grandTotal
            tax += ord.taxAmount
            discount += ord.discountAmount

            when (ord.paymentMethod) {
                PaymentMethod.CASH.name -> cash += ord.grandTotal
                PaymentMethod.CARD.name -> card += ord.grandTotal
                PaymentMethod.UPI_QR_WALLET.name -> qr += ord.grandTotal
                PaymentMethod.CUSTOMER_CREDIT.name -> credit += ord.grandTotal
                else -> cash += ord.grandTotal
            }

            for (item in ordWithItems.items) {
                totalCost += (item.costPrice * item.quantity)
                itemQtyMap[item.productName] = (itemQtyMap[item.productName] ?: 0.0) + item.quantity
            }
        }

        val profit = revenue - totalCost
        val avgTicket = if (todayOrders.isNotEmpty()) revenue / todayOrders.size else 0.0
        val topItems = itemQtyMap.toList().sortedByDescending { it.second }.take(5)

        DailyReportStats(
            totalRevenue = revenue,
            totalCost = totalCost,
            totalProfit = profit,
            totalOrdersCount = todayOrders.size,
            cashTotal = cash,
            cardTotal = card,
            qrTotal = qr,
            creditTotal = credit,
            totalTaxCollected = tax,
            totalDiscountGiven = discount,
            averageTicketValue = avgTicket,
            topSellingItems = topItems
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyReportStats())

    // --- Screen Navigation ---
    fun navigateTo(screen: ErpScreen) {
        _currentScreen.value = screen
    }

    // --- POS Cart Operations ---
    fun selectCategory(category: String) {
        _posState.value = _posState.value.copy(selectedCategory = category)
    }

    fun setSearchQuery(query: String) {
        _posState.value = _posState.value.copy(searchQuery = query)
    }

    fun addToCart(product: ProductEntity, quantity: Double = 1.0) {
        val currentItems = _posState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentItems[index]
            currentItems[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentItems.add(CartItem(product = product, quantity = quantity))
        }
        _posState.value = _posState.value.copy(cartItems = currentItems)
    }

    fun updateCartItemQuantity(index: Int, newQuantity: Double) {
        val currentItems = _posState.value.cartItems.toMutableList()
        if (index in currentItems.indices) {
            if (newQuantity <= 0.0) {
                currentItems.removeAt(index)
            } else {
                currentItems[index] = currentItems[index].copy(quantity = newQuantity)
            }
            _posState.value = _posState.value.copy(cartItems = currentItems)
        }
    }

    fun removeCartItem(index: Int) {
        val currentItems = _posState.value.cartItems.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _posState.value = _posState.value.copy(cartItems = currentItems)
        }
    }

    fun clearCart() {
        _posState.value = _posState.value.copy(
            cartItems = emptyList(),
            selectedCustomer = null,
            tableOrReference = "",
            notes = "",
            discountPercent = 0.0,
            isPaymentModalOpen = false,
            tenderedAmountText = ""
        )
    }

    fun selectCustomerForCart(customer: CustomerEntity?) {
        _posState.value = _posState.value.copy(selectedCustomer = customer)
    }

    fun setTableOrReference(ref: String) {
        _posState.value = _posState.value.copy(tableOrReference = ref)
    }

    fun setCartNotes(notes: String) {
        _posState.value = _posState.value.copy(notes = notes)
    }

    fun setCartDiscountPercent(percent: Double) {
        _posState.value = _posState.value.copy(discountPercent = percent)
    }

    fun openPaymentModal() {
        val total = calculateCartGrandTotal()
        _posState.value = _posState.value.copy(
            isPaymentModalOpen = true,
            tenderedAmountText = String.format("%.2f", total)
        )
    }

    fun closePaymentModal() {
        _posState.value = _posState.value.copy(isPaymentModalOpen = false)
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _posState.value = _posState.value.copy(selectedPaymentMethod = method)
    }

    fun setTenderedAmountText(amount: String) {
        _posState.value = _posState.value.copy(tenderedAmountText = amount)
    }

    fun calculateCartSubtotal(): Double {
        return _posState.value.cartItems.sumOf { it.unitPrice * it.quantity }
    }

    fun calculateCartTax(): Double {
        return _posState.value.cartItems.sumOf { it.taxAmount }
    }

    fun calculateCartDiscountAmount(): Double {
        val subtotal = calculateCartSubtotal()
        return subtotal * (_posState.value.discountPercent / 100.0)
    }

    fun calculateCartGrandTotal(): Double {
        val subtotal = calculateCartSubtotal()
        val discount = calculateCartDiscountAmount()
        val tax = calculateCartTax()
        return (subtotal - discount + tax).coerceAtLeast(0.0)
    }

    fun completeCheckout() {
        val cartItems = _posState.value.cartItems
        if (cartItems.isEmpty()) return

        val grandTotal = calculateCartGrandTotal()
        val tendered = _posState.value.tenderedAmountText.toDoubleOrNull() ?: grandTotal

        viewModelScope.launch {
            val orderWithItems = repository.completeSale(
                cartItems = cartItems,
                customer = _posState.value.selectedCustomer,
                paymentMethod = _posState.value.selectedPaymentMethod.name,
                paidAmount = tendered,
                discountAmount = calculateCartDiscountAmount(),
                referenceOrTable = _posState.value.tableOrReference,
                notes = _posState.value.notes,
                cashierName = settings.value.currentStaffName,
                currencySymbol = settings.value.currencySymbol
            )

            _posState.value = _posState.value.copy(
                cartItems = emptyList(),
                selectedCustomer = null,
                tableOrReference = "",
                notes = "",
                discountPercent = 0.0,
                isPaymentModalOpen = false,
                lastCompletedOrder = orderWithItems,
                isReceiptDialogOpen = true
            )
        }
    }

    fun holdCurrentCart() {
        val cartItems = _posState.value.cartItems
        if (cartItems.isEmpty()) return

        viewModelScope.launch {
            repository.holdSale(
                cartItems = cartItems,
                referenceOrTable = _posState.value.tableOrReference,
                notes = _posState.value.notes,
                cashierName = settings.value.currentStaffName
            )
            clearCart()
        }
    }

    fun resumeHeldOrder(orderWithItems: OrderWithItems) {
        viewModelScope.launch {
            repository.releaseHeldOrder(orderWithItems.order.orderId)
            val resumedItems = orderWithItems.items.map { item ->
                val p = allProducts.value.find { it.id == item.productId } ?: ProductEntity(
                    id = item.productId,
                    barcode = item.barcode,
                    name = item.productName,
                    category = "General",
                    costPrice = item.costPrice,
                    sellingPrice = item.unitPrice,
                    stockQuantity = 100.0
                )
                CartItem(
                    product = p,
                    quantity = item.quantity,
                    discountPercent = item.discountPercent
                )
            }
            _posState.value = _posState.value.copy(
                cartItems = resumedItems,
                tableOrReference = orderWithItems.order.referenceOrTable,
                notes = orderWithItems.order.notes
            )
            _currentScreen.value = ErpScreen.POS_REGISTER
        }
    }

    fun refundOrder(orderId: String) {
        viewModelScope.launch {
            repository.refundOrder(orderId)
        }
    }

    fun closeReceiptDialog() {
        _posState.value = _posState.value.copy(isReceiptDialogOpen = false)
    }

    fun openReceiptForOrder(orderWithItems: OrderWithItems) {
        _posState.value = _posState.value.copy(
            lastCompletedOrder = orderWithItems,
            isReceiptDialogOpen = true
        )
    }

    // --- Barcode / QR Scanner Operations ---
    fun openBarcodeScanner() {
        _posState.value = _posState.value.copy(isBarcodeScannerOpen = true, scanMessage = null)
    }

    fun closeBarcodeScanner() {
        _posState.value = _posState.value.copy(isBarcodeScannerOpen = false)
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode.trim())
            if (product != null) {
                addToCart(product, 1.0)
                _posState.value = _posState.value.copy(
                    scanMessage = "Added ${product.name} (${product.barcode}) to cart!"
                )
            } else {
                _posState.value = _posState.value.copy(
                    scanMessage = "No product found with barcode: $barcode"
                )
            }
        }
    }

    // --- Inventory Operations ---
    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun adjustStock(productId: Long, delta: Double) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, delta)
        }
    }

    fun resetCatalogToIndustry(sector: BusinessSector) {
        viewModelScope.launch {
            repository.resetCatalogToTemplate(sector.name)
            repository.updateSettings(
                settings.value.copy(
                    sector = sector.name,
                    taxRatePercent = sector.defaultTax
                )
            )
        }
    }

    // --- Customer & Khata Operations ---
    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
        }
    }

    fun recordCustomerPayment(customerId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, amount, note)
        }
    }

    // --- Staff Authentication & Switch ---
    private val _staffAuthError = MutableStateFlow<String?>(null)
    val staffAuthError: StateFlow<String?> = _staffAuthError.asStateFlow()

    fun switchStaffWithPin(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val staff = repository.verifyStaffPin(pin)
            if (staff != null) {
                repository.switchActiveStaff(staff)
                _staffAuthError.value = null
                onSuccess()
            } else {
                _staffAuthError.value = "Invalid Staff PIN code"
            }
        }
    }

    fun clearStaffAuthError() {
        _staffAuthError.value = null
    }

    // --- Business Profile Settings ---
    fun updateSettings(newSettings: BusinessSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    // --- Backend Sync Engine ---
    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun triggerBackendSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMessage.value = "Connecting to FastAPI backend at ${settings.value.fastApiBaseUrl}..."
            val (success, message) = repository.syncWithBackend(settings.value.fastApiBaseUrl)
            _isSyncing.value = false
            _syncStatusMessage.value = message
        }
    }
}

class ErpViewModelFactory(
    private val repository: ErpRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ErpViewModel::class.java)) {
            return ErpViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
