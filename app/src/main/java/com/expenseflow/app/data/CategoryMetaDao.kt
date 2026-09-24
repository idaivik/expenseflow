package com.expenseflow.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryMetaDao {
    @Query("SELECT * FROM category_meta")
    fun getAllCategoryMeta(): Flow<List<CategoryMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryMeta(meta: CategoryMetaEntity)

    @Query("DELETE FROM category_meta WHERE category = :category")
    suspend fun deleteCategoryMeta(category: String)

    @Query("DELETE FROM category_meta")
    suspend fun deleteAll()

    /** Multiply the manual amount override (where set) by [rate] — used when converting currencies. */
    @Query("UPDATE category_meta SET amountOverride = amountOverride * :rate WHERE amountOverride IS NOT NULL")
    suspend fun scaleOverrides(rate: Double)
}
