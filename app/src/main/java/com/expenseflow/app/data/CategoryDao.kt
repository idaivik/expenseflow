package com.expenseflow.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isExpense DESC, sortOrder ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE name = :name AND isExpense = :isExpense")
    suspend fun delete(name: String, isExpense: Boolean)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
