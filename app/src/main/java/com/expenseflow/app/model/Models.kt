package com.expenseflow.app.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Transaction(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val date: String,
    val time: String,
    val iconResId: Int,
    val iconBg: Color,
    val isExpense: Boolean = true,
    val isEdited: Boolean = false
)

data class BudgetCategory(
    val name: String,
    val spent: Double,
    val total: Double,
    val iconResId: Int, // Changed from ImageVector
    val color: Color
) {
    val remaining: Double get() = total - spent
    val progress: Float get() = (spent / total).toFloat()
}

data class SpendingCategory(
    val name: String,
    val amount: Double,
    val percentage: Int,
    val color: Color
)

data class DaySpending(
    val dayName: String,
    val amount: Double,
    val isToday: Boolean
)

data class WeeklyStats(
    val totalSpent: Double,
    val days: List<DaySpending>,
    val weekRange: String
)

/**
 * The month's spending plan: what's left of the monthly budget once the spends
 * the user has already committed to (see PlannedExpenseEntity) are reserved out
 * of it, spread over the days that remain.
 *
 * Example — it's 5 Sept, the monthly budget is 2000, and 200 (haircut, 12th) and
 * 300 (pizza, 18th) are planned. 2000 - 500 planned = 1500 free, spread over the
 * 26 days from the 5th to the 30th inclusive, so [perDay] is 57.69: planned days
 * still get their normal daily allowance on top of the amount already reserved.
 */
data class MonthlyPlanStats(
    val monthlyBudget: Double = 0.0,
    val spentThisMonth: Double = 0.0,
    /** Every expense logged today, planned or not. */
    val spentToday: Double = 0.0,
    /** Today's spending that wasn't part of the plan — what eats into [perDay]. */
    val unplannedSpentToday: Double = 0.0,
    /** Committed-but-unspent amounts dated today or later, this month only. */
    val plannedUpcoming: Double = 0.0,
    /** Planned amounts dated today, still unspent. */
    val plannedToday: Double = 0.0,
    /** Plans dated before today that were never logged — nothing is reserved for them. */
    val overduePlanned: Double = 0.0,
    /** Budget left for unplanned, day-to-day spending: budget - spent - [plannedUpcoming]. */
    val freePool: Double = 0.0,
    /** Days left in the month, today included. */
    val daysRemaining: Int = 1,
    /** [freePool] spread over [daysRemaining] — the "per day to stay on track" figure. */
    val perDay: Double = 0.0,
    /** [perDay] minus the unplanned spending already made today. */
    val safeToSpendToday: Double = 0.0,
    /** This month's actual spending has passed the budget. */
    val isOverBudget: Boolean = false,
    /** Today's unplanned spending has passed today's allowance. */
    val isOverDailyAllowance: Boolean = false,
) {
    val isConfigured: Boolean get() = monthlyBudget > 0.0

    /** True when what's committed already exceeds what's left of the budget. */
    val isOverCommitted: Boolean get() = isConfigured && freePool < 0.0
}

enum class InsightSeverity { POSITIVE, NEUTRAL, WARNING, DANGER }

/**
 * A pre-written, data-driven spending summary shown on the dashboard.
 * The [message] is selected from a set of premade templates based on the
 * user's spending stored in the database (see ExpenseViewModel.spendingInsight).
 */
data class SpendingInsight(
    val message: String,
    val severity: InsightSeverity = InsightSeverity.NEUTRAL
)

/**
 * A budget threshold event emitted when a category's monthly spending crosses a
 * warning (near-limit) or over-budget threshold. Consumed by the UI layer to
 * post a system notification.
 */
data class BudgetAlert(
    val category: String,
    val spent: Double,
    val limit: Double,
    val isOverBudget: Boolean
)
