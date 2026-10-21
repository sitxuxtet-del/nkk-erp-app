package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.OrderWithItems
import com.example.ui.components.ThermalReceiptDialog
import com.example.ui.viewmodel.ErpViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersBillingScreen(
    viewModel: ErpViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val allOrdersWithItems by viewModel.allOrdersWithItems.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedOrderForReceipt by remember { mutableStateOf<OrderWithItems?>(null) }

    val filteredOrders = allOrdersWithItems.filter { orderWithItems ->
        when (selectedStatusFilter) {
            "COMPLETED" -> orderWithItems.order.status == "COMPLETED"
            "HELD" -> orderWithItems.order.status == "HELD"
            "REFUNDED" -> orderWithItems.order.status == "REFUNDED"
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("orders_billing_screen")
    ) {
        Text(
            text = "Billing & Orders History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "View transaction log, re-print thermal receipts, or process returns",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        val filters = listOf("ALL", "COMPLETED", "HELD", "REFUNDED")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                val isSelected = selectedStatusFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedStatusFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No orders match the filter", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredOrders) { orderWithItems ->
                    OrderBillingCard(
                        orderWithItems = orderWithItems,
                        currency = settings.currencySymbol,
                        onViewReceipt = { selectedOrderForReceipt = orderWithItems },
                        onResumeHeld = { viewModel.resumeHeldOrder(orderWithItems) },
                        onRefund = { viewModel.refundOrder(orderWithItems.order.orderId) }
                    )
                }
            }
        }
    }

    selectedOrderForReceipt?.let { order ->
        ThermalReceiptDialog(
            orderWithItems = order,
            settings = settings,
            onDismiss = { selectedOrderForReceipt = null }
        )
    }
}

@Composable
fun OrderBillingCard(
    orderWithItems: OrderWithItems,
    currency: String,
    onViewReceipt: () -> Unit,
    onResumeHeld: () -> Unit,
    onRefund: () -> Unit
) {
    val order = orderWithItems.order
    val items = orderWithItems.items
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.orderId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.orderId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$dateStr • ${order.cashierName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val statusColor = when (order.status) {
                    "COMPLETED" -> Color(0xFFDCFCE7) to Color(0xFF166534)
                    "HELD" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
                    "REFUNDED" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    color = statusColor.first,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status,
                        color = statusColor.second,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items summary
            val itemsSummary = items.joinToString(", ") { "${it.productName} (${it.quantity.toInt()})" }
            Text(
                text = itemsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            if (order.referenceOrTable.isNotBlank()) {
                Text(
                    text = "Ref: ${order.referenceOrTable}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$currency%.2f".format(order.grandTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pay: ${order.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.status == "HELD") {
                        OutlinedButton(onClick = onResumeHeld, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp)
                        }
                    } else if (order.status == "COMPLETED") {
                        OutlinedButton(onClick = onRefund, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refund", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = onViewReceipt, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receipt", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
