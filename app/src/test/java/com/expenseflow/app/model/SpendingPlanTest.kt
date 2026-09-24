package com.expenseflow.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reserve-then-spread arithmetic behind "per day to stay on track". */
class SpendingPlanTest {

    /**
     * The worked example the feature was specified from: it's 5 September, the
     * monthly budget is 2000, and 200 (12th) and 300 (18th) are already committed.
     * (2000 - 500) spread over the 26 days from the 5th to the 30th = 57.69/day.
     */
    @Test
    fun `reserves planned spends and spreads the rest over the days left`() {
        val stats = computeMonthlyPlanStats(
            monthlyBudget = 2000.0,
            spentThisMonth = 0.0,
            spentToday = 0.0,
            plannedThisMonth = listOf(
                PlannedSpend(day = 12, amount = 200.0, logged = false),
                PlannedSpend(day = 18, amount = 300.0, logged = false),
            ),
            todayDay = 5,
            daysInMonth = 30,
        )

        assertEquals(26, stats.daysRemaining)
        assertEquals(500.0, stats.plannedUpcoming, 0.001)
        assertEquals(1500.0, stats.freePool, 0.001)
        assertEquals(57.69, stats.perDay, 0.01)
        assertEquals(57.69, stats.safeToSpendToday, 0.01)
        assertFalse(stats.isOverCommitted)
    }

    /** With nothing planned it degrades to a flat budget-left / days-left split. */
    @Test
    fun `no plans means a flat split of what is left`() {
        val stats = computeMonthlyPlanStats(
            monthlyBudget = 2000.0,
            spentThisMonth = 0.0,
            spentToday = 0.0,
            plannedThisMonth = emptyList(),
            todayDay = 5,
            daysInMonth = 30,
        )

        assertEquals(2000.0 / 26, stats.perDay, 0.001)
    }

    /** Spending already made this month comes out of the pool before the split. */
    @Test
    fun `spending so far reduces the pool`() {
        val stats = computeMonthlyPlanStats(
            monthlyBudget = 2000.0,
            spentThisMonth = 460.0,
            spentToday = 0.0,
            plannedThisMonth = listOf(PlannedSpend(day = 12, amount = 200.0, logged = false)),
            todayDay = 5,
            daysInMonth = 30,
        )

        assertEquals(1340.0, stats.freePool, 0.001)
        assertEquals(1340.0 / 26, stats.perDay, 0.001)
    }

    /**
     * Logging a planned expense must not move the daily allowance: the money was
     * already reserved, so it only changes which bucket it sits in.
     */
    @Test
    fun `logging a planned spend leaves the daily allowance unchanged`() {
        val before = computeMonthlyPlanStats(
            monthlyBudget = 2000.0,
            spentThisMonth = 0.0,
            spentToday = 0.0,
            plannedThisMonth = listOf(PlannedSpend(day = 5, amount = 200.0, logged = false)),
            todayDay = 5,
            daysInMonth = 30,
        )
        val after = computeMonthlyPlanStats(
            monthlyBudget = 2000.0,
            spentThisMonth = 200.0,
            spentToday = 200.0,
            plannedThisMonth = listOf(PlannedSpend(day = 5, amount = 200.0, logged = true)),
            todayDay = 5,
            daysInMonth = 30,
        )

        assertEquals(before.perDay, after.perDay, 0.001)
        assertEquals(before.safeToSpendToday, after.safeToSpendToday, 0.001)
        assertEquals(0.0, after.unplannedSpentToday, 0.001)
        assertFalse(after.isOverDailyAllowance)
    }

    /** Unplanned spending today does eat into today's allowance. */
    @Test
    fun `unplanned spending today eats into today's allowance`() {
        val stats = computeMonthlyPlanStats(
            monthlyBudget = 2000.0,
            spentThisMonth = 100.0,
            spentToday = 100.0,
            plannedThisMonth = emptyList(),
            todayDay = 5,
            daysInMonth = 30,
        )

        assertEquals(2000.0 / 26, stats.perDay, 0.001)
        assertEquals(2000.0 / 26 - 100.0, stats.safeToSpendToday, 0.001)
        assertTrue(stats.isOverDailyAllowance)
    }

    /**
     * A plan whose date passed without being logged reserves nothing — whatever
     * really happened is already in the transactions, so reserving would
     * double-count it.
     */
    @Test
    fun `plans past their date stop being reserved`() {
        val stats = computeMonthlyPlanStats(
            monthlyBudget = 2000.0,
            spentThisMonth = 0.0,
            spentToday = 0.0,
            plannedThisMonth = listOf(PlannedSpend(day = 2, amount = 300.0, logged = false)),
            todayDay = 5,
            daysInMonth = 30,
        )

        assertEquals(0.0, stats.plannedUpcoming, 0.001)
        assertEquals(300.0, stats.overduePlanned, 0.001)
        assertEquals(2000.0 / 26, stats.perDay, 0.001)
    }

    /** Committing to more than is left flags over-commitment and floors per-day at 0. */
    @Test
    fun `over-committing is flagged rather than shown as a negative allowance`() {
        val stats = computeMonthlyPlanStats(
            monthlyBudget = 1000.0,
            spentThisMonth = 400.0,
            spentToday = 0.0,
            plannedThisMonth = listOf(PlannedSpend(day = 20, amount = 900.0, logged = false)),
            todayDay = 5,
            daysInMonth = 30,
        )

        assertTrue(stats.isOverCommitted)
        assertEquals(-300.0, stats.freePool, 0.001)
        assertEquals(0.0, stats.perDay, 0.001)
    }

    /** On the last day of the month everything left is spendable today. */
    @Test
    fun `last day of the month leaves one day in the split`() {
        val stats = computeMonthlyPlanStats(
            monthlyBudget = 2000.0,
            spentThisMonth = 1800.0,
            spentToday = 0.0,
            plannedThisMonth = emptyList(),
            todayDay = 30,
            daysInMonth = 30,
        )

        assertEquals(1, stats.daysRemaining)
        assertEquals(200.0, stats.perDay, 0.001)
    }

    /** With no budget set there is nothing to spread, so no allowance is claimed. */
    @Test
    fun `no budget means no daily allowance`() {
        val stats = computeMonthlyPlanStats(
            monthlyBudget = 0.0,
            spentThisMonth = 120.0,
            spentToday = 120.0,
            plannedThisMonth = listOf(PlannedSpend(day = 12, amount = 200.0, logged = false)),
            todayDay = 5,
            daysInMonth = 30,
        )

        assertFalse(stats.isConfigured)
        assertEquals(0.0, stats.perDay, 0.001)
        assertEquals(-120.0, stats.safeToSpendToday, 0.001)
    }
}
