package com.example

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.NkkErpDatabase
import com.example.data.preferences.BusinessPreferencesRepository
import com.example.data.repository.ErpRepository
import com.example.workers.LowStockCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

class NkkErpApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { NkkErpDatabase.getDatabase(this, applicationScope) }
    val preferencesRepository by lazy { BusinessPreferencesRepository(this) }
    val repository by lazy { ErpRepository(database, preferencesRepository) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            val workRequest = PeriodicWorkRequestBuilder<LowStockCheckWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "LowStockCheckPeriodicWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: NkkErpApplication
            private set
    }
}
