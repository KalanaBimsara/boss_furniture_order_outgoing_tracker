package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedRecordDao {
    @Query("SELECT * FROM scanned_records ORDER BY scannedAt DESC")
    fun getAllRecords(): Flow<List<ScannedRecord>>

    @Query("SELECT * FROM scanned_records WHERE syncStatus = 'PENDING' ORDER BY scannedAt ASC")
    suspend fun getPendingRecords(): List<ScannedRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScannedRecord): Long

    @Update
    suspend fun updateRecord(record: ScannedRecord)

    @Query("DELETE FROM scanned_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM scanned_records")
    suspend fun clearAllRecords()
}
