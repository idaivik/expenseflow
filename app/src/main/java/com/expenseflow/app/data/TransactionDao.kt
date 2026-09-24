package com.expenseflow.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE title LIKE '%' || :query || '%' " +
            "OR category LIKE '%' || :query || '%' ORDER BY date DESC, time DESC"
    )
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    /** Multiply every transaction amount by [rate] — used when converting currencies. */
    @Query("UPDATE transactions SET amount = amount * :rate")
    suspend fun scaleAmounts(rate: Double)

    @Query("SELECT SUM(amount) FROM transactions WHERE isExpense = 0")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isExpense = 1")
    fun getTotalExpense(): Flow<Double?>
}
