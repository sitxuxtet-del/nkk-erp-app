package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.ProductEntity
import com.example.domain.models.CartItem
import com.example.ui.components.PaymentCheckoutDialog
import com.example.ui.components.QuickBarcodeScannerDialog
import com.example.ui.components.ThermalReceiptDialog
import com.example.ui.viewmodel.ErpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosRegisterScreen(
    viewModel: ErpViewModel,
    onOpenStaffDialog: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val posState by viewModel.posState.collectAsState()
    val filteredProducts by viewModel.filteredPosProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val heldOrders by viewModel.heldOrders.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()

    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }

    val cartItemCount = posState.cartItems.sumOf { it.quantity }.toInt()
    val grandTotal = viewModel.calculateCartGrandTotal()
    val subtotal = viewModel.calculateCartSubtotal()
    val discountAmount = viewModel.calculateCartDiscountAmount()
    val taxAmount = viewModel.calculateCartTax()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pos_register_screen")
    ) {
        // Top POS Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = settings.businessName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onOpenStaffDialog() }
                                .padding(vertical = 2.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${settings.currentStaffRole}: ${settings.currentStaffName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Barcode Scan Icon Button
                        IconButton(
                            onClick = { viewModel.openBarcodeScanner() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .size(40.dp)
                                .testTag("open_scanner_icon")
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Held Orders badge
                        if (heldOrders.isNotEmpty()) {
                            BadgedBox(
                                badge = { Badge { Text("${heldOrders.size}") } }
                            ) {
                                IconButton(
                                    onClick = {
                                        heldOrders.firstOrNull()?.let {
                                            viewModel.resumeHeldOrder(it)
                                        }
                                    },
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkBorder,
                                        contentDescription = "Resume Held",
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Cart View Button
                        Button(
                            onClick = { showCartSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("open_cart_button")
                        ) {
                            Text(
                                text = "Cart ($cartItemCount) • ${settings.currencySymbol}%.2f".format(grandTotal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = posState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pos_search_field"),
                    placeholder = { Text("Search products by name or barcode...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Horizontal Chips
                val categoryList = listOf("ALL") + categories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryList) { cat ->
                        val isSelected = posState.selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // Product Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try clearing search or check Inventory tab to add products.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(135.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts) { product ->
                        ProductPosCard(
                            product = product,
                            currency = settings.currencySymbol,
                            onAddToCart = { viewModel.addToCart(product, 1.0) }
                        )
                    }
                }
            }
        }

        // Bottom Fast Bar if cart has items
        if (posState.cartItems.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${posState.cartItems.size} items • ${settings.currencySymbol}%.2f".format(grandTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (posState.selectedCustomer != null)
                                "Customer: ${posState.selectedCustomer?.name}"
                            else
                                "Walk-in Guest",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.holdCurrentCart() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Park/Hold", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.openPaymentModal() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("fast_charge_button")
                        ) {
                            Text("Charge ${settings.currencySymbol}%.2f".format(grandTotal), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Cart Bottom Sheet
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current POS Cart",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.clearCart() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Cart", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Table / Ref & Customer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showCustomerPicker = true }
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = posState.selectedCustomer?.name ?: "Select Customer",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedTextField(
                        value = posState.tableOrReference,
                        onValueChange = { viewModel.setTableOrReference(it) },
                        placeholder = { Text("Table / Order Ref", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        leadingIcon = {
                            Icon(Icons.Default.TableBar, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cart Items List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 240.dp)
                ) {
                    if (posState.cartItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cart is currently empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        posState.cartItems.forEachIndexed { index, item ->
                            CartItemRow(
                                item = item,
                                currency = settings.currencySymbol,
                                onQuantityChange = { delta ->
                                    viewModel.updateCartItemQuantity(index, item.quantity + delta)
                                },
                                onRemove = { viewModel.removeCartItem(index) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calculation Summary
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                            Text("${settings.currencySymbol}%.2f".format(subtotal), style = MaterialTheme.typography.bodySmall)
                        }
                        if (discountAmount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount (${posState.discountPercent}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text("-${settings.currencySymbol}%.2f".format(discountAmount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax (${settings.taxRatePercent}%)", style = MaterialTheme.typography.bodySmall)
                            Text("+${settings.currencySymbol}%.2f".format(taxAmount), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${settings.currencySymbol}%.2f".format(grandTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Checkout & Hold Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.holdCurrentCart()
                            showCartSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hold Cart")
                    }

                    Button(
                        onClick = {
                            showCartSheet = false
                            viewModel.openPaymentModal()
                        },
                        enabled = posState.cartItems.isNotEmpty(),
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Checkout (${settings.currencySymbol}%.2f)".format(grandTotal), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Customer Picker Modal
    if (showCustomerPicker) {
        CustomerPickerModal(
            customers = customers,
            onCustomerSelected = { customer ->
                viewModel.selectCustomerForCart(customer)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false }
        )
    }

    // Barcode Scanner Dialog
    if (posState.isBarcodeScannerOpen) {
        QuickBarcodeScannerDialog(
            products = allProducts,
            scanMessage = posState.scanMessage,
            onScan = { barcode -> viewModel.onBarcodeScanned(barcode) },
            onDismiss = { viewModel.closeBarcodeScanner() }
        )
    }

    // Payment Modal
    if (posState.isPaymentModalOpen) {
        PaymentCheckoutDialog(
            posState = posState,
            settings = settings,
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            onPaymentMethodSelected = { viewModel.selectPaymentMethod(it) },
            onTenderedChanged = { viewModel.setTenderedAmountText(it) },
            onComplete = { viewModel.completeCheckout() },
            onDismiss = { viewModel.closePaymentModal() }
        )
    }

    // Thermal Receipt Preview Dialog
    if (posState.isReceiptDialogOpen && posState.lastCompletedOrder != null) {
        ThermalReceiptDialog(
            orderWithItems = posState.lastCompletedOrder!!,
            settings = settings,
            onDismiss = { viewModel.closeReceiptDialog() }
        )
    }
}

@Composable
fun ProductPosCard(
    product: ProductEntity,
    currency: String,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }

                val isLow = product.stockQuantity <= product.minStockAlert
                Surface(
                    color = if (isLow) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (product.stockQuantity > 500) "In Stock" else "${product.stockQuantity.toInt()} left",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLow) Color(0xFF991B1B) else Color(0xFF166534),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currency%.2f".format(product.sellingPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    currency: String,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$currency%.2f / ${item.product.unit}".format(item.unitPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChange(-1.0) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }

                val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else "%.1f".format(item.quantity)
                Text(
                    text = qtyStr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { onQuantityChange(1.0) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "$currency%.2f".format(item.total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CustomerPickerModal(
    customers: List<CustomerEntity>,
    onCustomerSelected: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Assign Customer to Sale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomerSelected(null) }
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Walk-in Guest (No Customer Record)",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                customers.forEach { customer ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCustomerSelected(customer) }
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(customer.name, fontWeight = FontWeight.SemiBold)
                                Text("Tel: ${customer.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (customer.outstandingBalance > 0) {
                                Text(
                                    text = "Debt: $%.2f".format(customer.outstandingBalance),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
