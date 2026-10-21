package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.dao.OrderWithItems
import com.example.data.local.entities.ProductEntity
import com.example.data.preferences.BusinessSettings
import com.example.domain.models.DailyReportStats
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateAndShareDailyReportPdf(
        context: Context,
        settings: BusinessSettings,
        stats: DailyReportStats,
        products: List<ProductEntity>,
        orders: List<OrderWithItems>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size at 72 DPI
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 13f
            isFakeBoldText = true
        }

        var yPos = 40f
        val leftMargin = 40f

        // Title
        canvas.drawText("NKK ERP — Daily Sales & Inventory Summary", leftMargin, yPos, titlePaint)
        yPos += 22f

        paint.textSize = 10f
        val dateStr = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr | Store: ${settings.businessName}", leftMargin, yPos, paint)
        yPos += 28f

        // Section 1: Sales Summary
        canvas.drawText("1. Day's Sales Summary (KPIs)", leftMargin, yPos, subtitlePaint)
        yPos += 18f

        paint.textSize = 11f
        canvas.drawText("• Gross Revenue: ${settings.currencySymbol}%.2f".format(stats.totalRevenue), leftMargin + 10f, yPos, paint)
        yPos += 16f
        canvas.drawText("• Net Profit: ${settings.currencySymbol}%.2f".format(stats.totalProfit), leftMargin + 10f, yPos, paint)
        yPos += 16f
        canvas.drawText("• Total Completed Orders: ${stats.totalOrdersCount}", leftMargin + 10f, yPos, paint)
        yPos += 16f
        canvas.drawText("• Cash: ${settings.currencySymbol}%.2f | Card: ${settings.currencySymbol}%.2f | QR: ${settings.currencySymbol}%.2f | Credit: ${settings.currencySymbol}%.2f".format(stats.cashTotal, stats.cardTotal, stats.qrTotal, stats.creditTotal), leftMargin + 10f, yPos, paint)
        yPos += 26f

        // Section 2: Inventory Changes & Stock Valuation
        canvas.drawText("2. Inventory & Stock Valuation", leftMargin, yPos, subtitlePaint)
        yPos += 18f

        val totalStockValuation = products.sumOf { it.costPrice * it.stockQuantity }
        val lowStockCount = products.count { it.stockQuantity <= it.minStockAlert }

        canvas.drawText("• Total Catalog Products: ${products.size}", leftMargin + 10f, yPos, paint)
        yPos += 16f
        canvas.drawText("• Total Stock Cost Valuation: ${settings.currencySymbol}%.2f".format(totalStockValuation), leftMargin + 10f, yPos, paint)
        yPos += 16f
        canvas.drawText("• Low Stock Items Alert: $lowStockCount products requiring reorder", leftMargin + 10f, yPos, paint)
        yPos += 26f

        // Section 3: Recent Transactions
        canvas.drawText("3. Recent Transactions Log (${orders.size} total)", leftMargin, yPos, subtitlePaint)
        yPos += 18f

        val recentOrders = orders.take(6)
        for (ord in recentOrders) {
            val line = "${ord.order.orderId} - ${ord.order.customerName} - ${settings.currencySymbol}%.2f (${ord.order.paymentMethod})"
            canvas.drawText(line, leftMargin + 10f, yPos, paint)
            yPos += 15f
        }

        yPos += 35f
        paint.textSize = 9f
        canvas.drawText("Authorized Signature: ______________________", leftMargin, yPos, paint)
        canvas.drawText("Powered by NKK ERP", 420f, yPos, paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache file
        val file = File(context.cacheDir, "nkk_erp_daily_summary_report.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()

        // Share PDF Intent
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "NKK ERP Daily Summary Report - $dateStr")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share PDF Summary Report via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
