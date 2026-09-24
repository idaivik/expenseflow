package com.expenseflow.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()

    /** Multiply saved + target on every goal/budget by [rate] — used when converting currencies. */
    @Query("UPDATE goals SET saved = saved * :rate, target = target * :rate")
    suspend fun scaleAmounts(rate: Double)
}
