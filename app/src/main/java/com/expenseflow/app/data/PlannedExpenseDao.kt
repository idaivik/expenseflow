package com.expenseflow.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedExpenseDao {
    @Query("SELECT * FROM planned_expenses ORDER BY id ASC")
    fun getAllPlannedExpenses(): Flow<List<PlannedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedExpense(planned: PlannedExpenseEntity): Long

    @Update
    suspend fun updatePlannedExpense(planned: PlannedExpenseEntity)

    @Delete
    suspend fun deletePlannedExpense(planned: PlannedExpenseEntity)

    @Query("DELETE FROM planned_expenses")
    suspend fun deleteAll()

    /** Multiply every planned amount by [rate] — used when converting currencies. */
    @Query("UPDATE planned_expenses SET amount = amount * :rate")
    suspend fun scaleAmounts(rate: Double)
}
