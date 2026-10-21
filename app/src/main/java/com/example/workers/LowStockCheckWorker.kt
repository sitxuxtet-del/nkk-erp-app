package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.NkkErpApplication
import kotlinx.coroutines.flow.first

class LowStockCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = (applicationContext as NkkErpApplication).database
            val lowStockProducts = database.productDao().getLowStockProducts().first()

            if (lowStockProducts.isNotEmpty()) {
                val names = lowStockProducts.joinToString(", ") { it.name }
                showLowStockNotification(
                    context = applicationContext,
                    title = "Low Inventory Alert (${lowStockProducts.size} items)",
                    message = "Items below minimum stock threshold: $names"
                )
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showLowStockNotification(context: Context, title: String, message: String) {
        val channelId = "nkk_erp_inventory_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Inventory Low Stock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic alerts for products running low on stock"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(2026, builder.build())
            }
        } catch (e: SecurityException) {
            // Notification permission required on Android 13+
        }
    }
}
