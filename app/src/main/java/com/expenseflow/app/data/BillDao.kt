package com.expenseflow.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY id ASC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("DELETE FROM bills")
    suspend fun deleteAll()

    /** Multiply every bill amount by [rate] — used when converting currencies. */
    @Query("UPDATE bills SET amount = amount * :rate")
    suspend fun scaleAmounts(rate: Double)
}
