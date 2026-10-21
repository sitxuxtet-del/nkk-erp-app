package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.StaffEntity
import com.example.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_members WHERE isActive = 1 ORDER BY name ASC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff_members WHERE pin = :pin AND isActive = 1 LIMIT 1")
    suspend fun verifyStaffPin(pin: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffList(staffList: List<StaffEntity>)

    @Update
    suspend fun updateStaff(staff: StaffEntity)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingSyncItems(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("UPDATE sync_queue SET status = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE sync_queue SET status = 'FAILED', retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearCompletedSyncItems()
}
