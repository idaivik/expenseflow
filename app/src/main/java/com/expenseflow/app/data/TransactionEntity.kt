package com.expenseflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val amount: Double,
    val date: String,
    val time: String,
    val iconName: String,
    val iconColor: Long,
    val isExpense: Boolean,
    val isEdited: Boolean = false
)
