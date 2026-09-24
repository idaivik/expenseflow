package com.expenseflow.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    /** Multiply every budget limit by [rate] — used when converting currencies. */
    @Query("UPDATE budgets SET budgetLimit = budgetLimit * :rate")
    suspend fun scaleLimits(rate: Double)

    @Query("SELECT SUM(budgetLimit) FROM budgets")
    fun getTotalBudget(): Flow<Double?>
}
