package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.StaffDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.CustomerTransactionEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.SaleOrderEntity
import com.example.data.local.entities.SaleOrderItemEntity
import com.example.data.local.entities.StaffEntity
import com.example.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        SaleOrderEntity::class,
        SaleOrderItemEntity::class,
        CustomerEntity::class,
        CustomerTransactionEntity::class,
        StaffEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NkkErpDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun customerDao(): CustomerDao
    abstract fun staffDao(): StaffDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: NkkErpDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NkkErpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NkkErpDatabase::class.java,
                    "nkk_erp_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    SampleDataSeeder.seedInitialData(database)
                }
            }
        }
    }
}
