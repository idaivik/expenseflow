package com.expenseflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A recurring bill / subscription shown on the Plan screen. */
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconKey: String,
    val tone: String,
    val amount: Double,
    val dueDate: String = "",
    val paid: Boolean = false,
    /** Id of the expense transaction logged when this bill was marked paid (0 = none); used to reverse it on un-mark. */
    val paidTxnId: Int = 0,
    /** How often the bill repeats: "monthly" (every month) or "custom" (every [intervalCount] [intervalUnit]s). */
    val recurrenceType: String = "monthly",
    /** For a custom schedule: how many units between occurrences (ignored when monthly). */
    val intervalCount: Int = 1,
    /** For a custom schedule: the unit — "day", "week" or "month" (ignored when monthly). */
    val intervalUnit: String = "month",
)
