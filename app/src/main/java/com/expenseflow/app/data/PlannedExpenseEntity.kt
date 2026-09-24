package com.expenseflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A spend the user has already committed to but hasn't made yet — "₹200 haircut
 * on the 12th". Planned amounts are reserved out of the monthly budget the moment
 * they're added, so the "per day to stay on track" figure on the home screen
 * already accounts for them instead of only reacting once the money is gone.
 *
 * [loggedTxnId] links the plan to the real expense created when the user marks it
 * as spent (0 = still upcoming); the link lets that transaction be reversed if the
 * plan is un-logged, mirroring [BillEntity.paidTxnId].
 */
@Entity(tableName = "planned_expenses")
data class PlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val amount: Double,
    /** The day the spend is expected, "dd/MM/yyyy" — same format as [TransactionEntity.date]. */
    val date: String,
    /** Id of the expense logged when this plan was marked spent (0 = not logged yet). */
    val loggedTxnId: Int = 0,
)
