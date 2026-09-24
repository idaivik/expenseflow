package com.expenseflow.app.model

import kotlin.math.roundToInt

/**
 * A committed-but-unmade spend, reduced to what the budget maths needs: which day
 * of the month it falls on, how much it is, and whether it has already been logged
 * as a real expense.
 */
data class PlannedSpend(val day: Int, val amount: Double, val logged: Boolean)

/**
 * The single source of the "per day to stay on track" figure — shared by the home
 * screen and the daily-budget widget so the two can never disagree.
 *
 * Planned amounts are reserved out of the pool but do *not* come out of the day
 * count, so a haircut booked for the 12th shaves a little off every remaining
 * day's allowance rather than only landing on the 12th. With a 2000 budget on
 * 5 September and 200 + 300 planned, that's (2000 - 500) / 26 days = 57.69 a day.
 *
 * Two kinds of plan are deliberately not reserved: ones already logged (they're
 * counted in [spentThisMonth] as real expenses instead) and ones whose date has
 * passed without being logged (whatever actually happened is in the transactions
 * already, so reserving for them would double-count).
 *
 * Today's *unplanned* spending is added back before the split and subtracted again
 * afterwards, which keeps two things true: spending early in the day doesn't shrink
 * today's own allowance, and logging a planned expense doesn't move the allowance
 * at all — only unplanned spending does.
 *
 * @param plannedThisMonth plans falling in the current month only; other months
 *   don't affect this month's budget.
 * @param todayDay day-of-month, 1-based. @param daysInMonth length of the month.
 */
fun computeMonthlyPlanStats(
    monthlyBudget: Double,
    spentThisMonth: Double,
    spentToday: Double,
    plannedThisMonth: List<PlannedSpend>,
    todayDay: Int,
    daysInMonth: Int,
): MonthlyPlanStats {
    val daysRemaining = (daysInMonth - todayDay + 1).coerceAtLeast(1)

    var plannedUpcoming = 0.0
    var plannedToday = 0.0
    var overduePlanned = 0.0
    var plannedLoggedToday = 0.0
    plannedThisMonth.forEach { p ->
        if (p.logged) {
            if (p.day == todayDay) plannedLoggedToday += p.amount
            return@forEach
        }
        when {
            p.day < todayDay -> overduePlanned += p.amount
            p.day == todayDay -> { plannedUpcoming += p.amount; plannedToday += p.amount }
            else -> plannedUpcoming += p.amount
        }
    }

    val unplannedSpentToday = (spentToday - plannedLoggedToday).coerceAtLeast(0.0)
    val freePool = monthlyBudget - spentThisMonth - plannedUpcoming
    val perDay = if (monthlyBudget > 0.0) {
        ((freePool + unplannedSpentToday) / daysRemaining).coerceAtLeast(0.0)
    } else 0.0
    val safeToSpendToday =
        if (monthlyBudget > 0.0) perDay - unplannedSpentToday else -unplannedSpentToday

    return MonthlyPlanStats(
        monthlyBudget = monthlyBudget,
        spentThisMonth = spentThisMonth,
        spentToday = spentToday,
        unplannedSpentToday = unplannedSpentToday,
        plannedUpcoming = plannedUpcoming,
        plannedToday = plannedToday,
        overduePlanned = overduePlanned,
        freePool = freePool,
        daysRemaining = daysRemaining,
        perDay = perDay,
        safeToSpendToday = safeToSpendToday,
        isOverBudget = monthlyBudget > 0.0 && spentThisMonth > monthlyBudget,
        isOverDailyAllowance = monthlyBudget > 0.0 && safeToSpendToday < 0.0,
    )
}
