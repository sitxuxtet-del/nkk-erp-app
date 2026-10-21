package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.ProductEntity
import com.example.domain.models.BusinessSector
import com.example.ui.viewmodel.ErpViewModel

@Composable
fun InventoryScreen(
    viewModel: ErpViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LOW_STOCK
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showSectorTemplateDialog by remember { mutableStateOf(false) }

    val filteredProducts = allProducts.filter { product ->
        val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.barcode.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "LOW_STOCK" -> product.stockQuantity <= product.minStockAlert
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val totalStockValuation = allProducts.sumOf { it.costPrice * it.stockQuantity }
    val totalRetailValuation = allProducts.sumOf { it.sellingPrice * it.stockQuantity }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .testTag("inventory_screen")
        ) {
            // Header with Valuation & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inventory Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allProducts.size} Products • Valuation: ${settings.currencySymbol}%.2f".format(totalRetailValuation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { showSectorTemplateDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Industry Preset", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Low Stock Warning Banner if any
            if (lowStockProducts.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedFilter = if (selectedFilter == "LOW_STOCK") "ALL" else "LOW_STOCK" }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${lowStockProducts.size} Items Below Minimum Stock Alert!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Tap to filter low stock items and reorder quickly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Search Bar & Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search inventory...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("inventory_search_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = selectedFilter == "LOW_STOCK",
                    onClick = { selectedFilter = if (selectedFilter == "LOW_STOCK") "ALL" else "LOW_STOCK" },
                    label = { Text("Low Stock (${lowStockProducts.size})", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inventory Item List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { product ->
                    InventoryProductCard(
                        product = product,
                        currency = settings.currencySymbol,
                        onAdjustStock = { delta -> viewModel.adjustStock(product.id, delta) },
                        onEdit = {
                            editingProduct = product
                            showAddEditDialog = true
                        },
                        onDelete = { viewModel.deleteProduct(product) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
                }
            }
        }

        // Add Product FAB
        FloatingActionButton(
            onClick = {
                editingProduct = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_product_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        ProductAddEditDialog(
            productToEdit = editingProduct,
            currency = settings.currencySymbol,
            defaultTax = settings.taxRatePercent,
            onSave = { product ->
                viewModel.saveProduct(product)
                showAddEditDialog = false
            },
            onDismiss = { showAddEditDialog = false }
        )
    }

    // Industry Sector Template Dialog
    if (showSectorTemplateDialog) {
        IndustrySectorModal(
            currentSector = settings.sector,
            onSectorSelect = { sector ->
                viewModel.resetCatalogToIndustry(sector)
                showSectorTemplateDialog = false
            },
            onDismiss = { showSectorTemplateDialog = false }
        )
    }
}

@Composable
fun InventoryProductCard(
    product: ProductEntity,
    currency: String,
    onAdjustStock: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SKU/Barcode: ${product.barcode} • Cat: ${product.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!product.expiryDate.isNullOrBlank() || !product.batchNumber.isNullOrBlank()) {
                        Text(
                            text = "Exp: ${product.expiryDate ?: "N/A"} • Batch: ${product.batchNumber ?: "N/A"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0369A1)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Stock Adjustments Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Sell: $currency%.2f".format(product.sellingPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cost: $currency%.2f".format(product.costPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val margin = if (product.sellingPrice > 0) ((product.sellingPrice - product.costPrice) / product.sellingPrice * 100).toInt() else 0
                    Text(
                        text = "Margin: $margin%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF059669)
                    )
                }

                // Quick Stock Adjustment (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onAdjustStock(-1.0) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("-1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    val isLow = product.stockQuantity <= product.minStockAlert
                    Surface(
                        color = if (isLow) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${product.stockQuantity.toInt()} ${product.unit}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLow) Color(0xFF991B1B) else Color(0xFF166534)
                        )
                    }

                    IconButton(
                        onClick = { onAdjustStock(1.0) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductAddEditDialog(
    productToEdit: ProductEntity?,
    currency: String,
    defaultTax: Double,
    onSave: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var barcode by remember { mutableStateOf(productToEdit?.barcode ?: "890${(1000000..9999999).random()}") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "General") }
    var costPrice by remember { mutableStateOf(productToEdit?.costPrice?.toString() ?: "0.0") }
    var sellingPrice by remember { mutableStateOf(productToEdit?.sellingPrice?.toString() ?: "0.0") }
    var stockQuantity by remember { mutableStateOf(productToEdit?.stockQuantity?.toString() ?: "10.0") }
    var minStockAlert by remember { mutableStateOf(productToEdit?.minStockAlert?.toString() ?: "5.0") }
    var unit by remember { mutableStateOf(productToEdit?.unit ?: "pcs") }
    var isOnlineVisible by remember { mutableStateOf(productToEdit?.isOnlineShopVisible ?: true) }
    var expiryDate by remember { mutableStateOf(productToEdit?.expiryDate ?: "") }
    var batchNumber by remember { mutableStateOf(productToEdit?.batchNumber ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = if (productToEdit == null) "Add New Product" else "Edit Product",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode / SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Selling ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Current Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs/kg/box)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show in Online Store", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isOnlineVisible, onCheckedChange = { isOnlineVisible = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val product = ProductEntity(
                                    id = productToEdit?.id ?: 0L,
                                    barcode = barcode.trim(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                                    sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                                    stockQuantity = stockQuantity.toDoubleOrNull() ?: 0.0,
                                    minStockAlert = minStockAlert.toDoubleOrNull() ?: 5.0,
                                    unit = unit.trim(),
                                    isOnlineShopVisible = isOnlineVisible,
                                    taxRatePercent = productToEdit?.taxRatePercent ?: defaultTax,
                                    expiryDate = expiryDate.ifBlank { null },
                                    batchNumber = batchNumber.ifBlank { null }
                                )
                                onSave(product)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Product", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun IndustrySectorModal(
    currentSector: String,
    onSectorSelect: (BusinessSector) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Load Industry Catalog Template",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pre-loads real products, categories and tax settings designed for your business sector.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                BusinessSector.values().forEach { sector ->
                    val isSelected = currentSector.equals(sector.name, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSectorSelect(sector) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sector.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Default Tax: ${sector.defaultTax}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
