package com.expenseflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val budgetLimit: Double
)
