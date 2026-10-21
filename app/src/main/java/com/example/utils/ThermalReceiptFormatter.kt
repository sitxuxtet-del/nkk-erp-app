package com.example.utils

import android.content.Context
import android.content.Intent
import com.example.data.local.dao.OrderWithItems
import com.example.data.preferences.BusinessSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThermalReceiptFormatter {

    fun formatReceipt(orderWithItems: OrderWithItems, settings: BusinessSettings): String {
        val order = orderWithItems.order
        val items = orderWithItems.items
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(order.timestamp))
        val currency = settings.currencySymbol

        val sb = StringBuilder()
        val width = 40
        fun center(text: String): String {
            if (text.length >= width) return text.take(width)
            val pad = (width - text.length) / 2
            return " ".repeat(pad) + text
        }

        fun divider(char: Char = '-'): String = char.toString().repeat(width)

        sb.appendLine(center(settings.businessName.uppercase()))
        sb.appendLine(center(settings.address))
        sb.appendLine(center("Tel: ${settings.phone}"))
        if (settings.receiptHeader.isNotBlank()) {
            sb.appendLine(center(settings.receiptHeader))
        }
        sb.appendLine(divider('='))
        sb.appendLine("INV: ${order.orderId}")
        sb.appendLine("Date: $dateStr")
        sb.appendLine("Cashier: ${order.cashierName}")
        if (!order.customerName.isNullOrBlank()) {
            sb.appendLine("Customer: ${order.customerName}")
        }
        if (order.referenceOrTable.isNotBlank()) {
            sb.appendLine("Ref / Table: ${order.referenceOrTable}")
        }
        sb.appendLine(divider('-'))

        // Table header: Item (22 chars) Qty (6 chars) Total (12 chars)
        sb.appendLine(String.format(Locale.US, "%-20s %5s %12s", "ITEM", "QTY", "TOTAL"))
        sb.appendLine(divider('-'))

        for (item in items) {
            val itemName = if (item.productName.length > 20) item.productName.take(18) + ".." else item.productName
            val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else String.format(Locale.US, "%.1f", item.quantity)
            val totalStr = String.format(Locale.US, "%s%.2f", currency, item.itemTotal)
            sb.appendLine(String.format(Locale.US, "%-20s %5s %12s", itemName, qtyStr, totalStr))
            if (item.barcode.isNotBlank()) {
                sb.appendLine(String.format(Locale.US, "  #%s (@ %s%.2f)", item.barcode, currency, item.unitPrice))
            }
        }

        sb.appendLine(divider('-'))
        sb.appendLine(String.format(Locale.US, "%-24s %14s", "Subtotal:", String.format(Locale.US, "%s%.2f", currency, order.subtotal)))

        if (order.discountAmount > 0.0) {
            sb.appendLine(String.format(Locale.US, "%-24s %14s", "Discount:", String.format(Locale.US, "-%s%.2f", currency, order.discountAmount)))
        }

        if (order.taxAmount > 0.0) {
            sb.appendLine(String.format(Locale.US, "%-24s %14s", "Tax / VAT:", String.format(Locale.US, "+%s%.2f", currency, order.taxAmount)))
        }

        sb.appendLine(divider('='))
        sb.appendLine(String.format(Locale.US, "%-20s %18s", "GRAND TOTAL:", String.format(Locale.US, "%s%.2f", currency, order.grandTotal)))
        sb.appendLine(divider('='))

        sb.appendLine("Payment: ${order.paymentMethod}")
        sb.appendLine(String.format(Locale.US, "%-24s %14s", "Amount Tendered:", String.format(Locale.US, "%s%.2f", currency, order.paidAmount)))
        if (order.changeAmount > 0.0) {
            sb.appendLine(String.format(Locale.US, "%-24s %14s", "Change Returned:", String.format(Locale.US, "%s%.2f", currency, order.changeAmount)))
        }

        sb.appendLine()
        sb.appendLine(center("||| || |||| ||| ||||| ||| |||"))
        sb.appendLine(center("* ${order.orderId} *"))
        sb.appendLine()
        if (settings.receiptFooter.isNotBlank()) {
            sb.appendLine(center(settings.receiptFooter))
        }
        sb.appendLine(center("Powered by NKK ERP"))
        sb.appendLine(divider('~'))

        return sb.toString()
    }

    fun shareReceipt(context: Context, receiptText: String, orderId: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptText)
            putExtra(Intent.EXTRA_SUBJECT, "Receipt #$orderId")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Receipt via")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
