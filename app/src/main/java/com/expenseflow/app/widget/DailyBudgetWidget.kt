package com.expenseflow.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.expenseflow.app.R
import com.expenseflow.app.data.AppDatabase
import com.expenseflow.app.data.SettingsRepository
import com.expenseflow.app.model.PlannedSpend
import com.expenseflow.app.model.computeMonthlyPlanStats
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyBudgetWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(130.dp, 110.dp), // Compact (2x2 / 2x1)
            DpSize(240.dp, 120.dp), // Medium (4x2 / 3x2)
            DpSize(260.dp, 220.dp)  // Large (4x3 / 4x4)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val settingsRepo = SettingsRepository(context)

        val settings = settingsRepo.settings.firstOrNull()
        val currencySymbol = settings?.currencySymbol ?: "$"

        // Calendar & Days
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)

        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Budgets & Transactions
        val budgets = database.budgetDao().getAllBudgets().firstOrNull() ?: emptyList()
        val totalMonthlyBudget = budgets.sumOf { it.budgetLimit }

        val allTxns = database.transactionDao().getAllTransactions().firstOrNull() ?: emptyList()

        val monthlyExpenses = allTxns.filter { txn ->
            txn.isExpense && isInMonth(txn.date, currentMonth, currentYear)
        }.sumOf { it.amount }

        val monthlyIncome = allTxns.filter { txn ->
            !txn.isExpense && isInMonth(txn.date, currentMonth, currentYear)
        }.sumOf { it.amount }

        val spentToday = allTxns.filter { it.isExpense && it.date == todayStr }.sumOf { it.amount }

        // Spends the user has committed to on specific days later this month are
        // reserved out of the pool up front. The arithmetic is shared with the home
        // screen (computeMonthlyPlanStats) so the two "per day" figures can't disagree.
        val planned = database.plannedExpenseDao().getAllPlannedExpenses().firstOrNull() ?: emptyList()
        val plannedThisMonth = planned.mapNotNull { p ->
            if (!isInMonth(p.date, currentMonth, currentYear)) return@mapNotNull null
            dayOfMonth(p.date)?.let { PlannedSpend(it, p.amount, p.loggedTxnId != 0) }
        }

        // Budget Calculations
        val hasConfiguredBudget = totalMonthlyBudget > 0.0
        // Without a configured budget the month's income stands in as the pool.
        val effectiveMonthlyPool = if (hasConfiguredBudget) totalMonthlyBudget else monthlyIncome

        val stats = computeMonthlyPlanStats(
            monthlyBudget = effectiveMonthlyPool,
            spentThisMonth = monthlyExpenses,
            spentToday = spentToday,
            plannedThisMonth = plannedThisMonth,
            todayDay = currentDay,
            daysInMonth = daysInMonth,
        )

        val remainingMonthPool = stats.freePool
        val dailyAllowance = stats.perDay
        val safeToSpendToday = stats.safeToSpendToday
        val unplannedSpentToday = stats.unplannedSpentToday

        val progressToday = if (dailyAllowance > 0.0) {
            (unplannedSpentToday / dailyAllowance).toFloat().coerceIn(0f, 1f)
        } else if (unplannedSpentToday > 0.0) 1f else 0f

        val isOverBudget = safeToSpendToday < 0.0
        val isNearCap = !isOverBudget && dailyAllowance > 0.0 && unplannedSpentToday >= dailyAllowance * 0.8

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                DailyBudgetWidgetContent(
                    context = context,
                    currencySymbol = currencySymbol,
                    safeToSpendToday = safeToSpendToday,
                    dailyAllowance = dailyAllowance,
                    spentToday = spentToday,
                    remainingMonthPool = remainingMonthPool,
                    daysRemaining = daysRemaining,
                    hasConfiguredBudget = hasConfiguredBudget || monthlyIncome > 0.0,
                    progressToday = progressToday,
                    isOverBudget = isOverBudget,
                    isNearCap = isNearCap,
                    widgetSize = size
                )
            }
        }
    }

    /** Day-of-month for a "dd/MM/yyyy" string, or null if it isn't one. */
    private fun dayOfMonth(dateStr: String): Int? = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr)
            ?.let { Calendar.getInstance().apply { time = it }.get(Calendar.DAY_OF_MONTH) }
    } catch (_: Exception) {
        null
    }

    private fun isInMonth(dateStr: String, month: Int, year: Int): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return false
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        } catch (_: Exception) {
            false
        }
    }

    @Composable
    private fun DailyBudgetWidgetContent(
        context: Context,
        currencySymbol: String,
        safeToSpendToday: Double,
        dailyAllowance: Double,
        spentToday: Double,
        remainingMonthPool: Double,
        daysRemaining: Int,
        hasConfiguredBudget: Boolean,
        progressToday: Float,
        isOverBudget: Boolean,
        isNearCap: Boolean,
        widgetSize: DpSize
    ) {
        val isLarge = widgetSize.height >= 200.dp
        val isMedium = widgetSize.width >= 220.dp && !isLarge

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetTheme.surfaceBackground)
                .cornerRadius(20.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(WidgetTheme.createNavigationIntent(context, "plan", "budgets")))
        ) {
            when {
                isLarge -> LargeDailyBudgetLayout(
                    context = context,
                    currencySymbol = currencySymbol,
                    safeToSpendToday = safeToSpendToday,
                    dailyAllowance = dailyAllowance,
                    spentToday = spentToday,
                    remainingMonthPool = remainingMonthPool,
                    daysRemaining = daysRemaining,
                    hasConfiguredBudget = hasConfiguredBudget,
                    progressToday = progressToday,
                    isOverBudget = isOverBudget,
                    isNearCap = isNearCap
                )
                isMedium -> MediumDailyBudgetLayout(
                    context = context,
                    currencySymbol = currencySymbol,
                    safeToSpendToday = safeToSpendToday,
                    dailyAllowance = dailyAllowance,
                    spentToday = spentToday,
                    remainingMonthPool = remainingMonthPool,
                    daysRemaining = daysRemaining,
                    hasConfiguredBudget = hasConfiguredBudget,
                    progressToday = progressToday,
                    isOverBudget = isOverBudget,
                    isNearCap = isNearCap
                )
                else -> CompactDailyBudgetLayout(
                    currencySymbol = currencySymbol,
                    safeToSpendToday = safeToSpendToday,
                    spentToday = spentToday,
                    hasConfiguredBudget = hasConfiguredBudget,
                    progressToday = progressToday,
                    isOverBudget = isOverBudget,
                    isNearCap = isNearCap
                )
            }
        }
    }

    @Composable
    private fun CompactDailyBudgetLayout(
        currencySymbol: String,
        safeToSpendToday: Double,
        spentToday: Double,
        hasConfiguredBudget: Boolean,
        progressToday: Float,
        isOverBudget: Boolean,
        isNearCap: Boolean
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_sparkle),
                    contentDescription = null,
                    modifier = GlanceModifier.size(13.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "Spend Today",
                    style = TextStyle(
                        color = WidgetTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                StatusBadge(isOverBudget = isOverBudget, isNearCap = isNearCap, compact = true)
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Hero Amount
            if (hasConfiguredBudget) {
                Text(
                    text = if (isOverBudget) {
                        WidgetTheme.formatMoney(currencySymbol, -safeToSpendToday) + " over"
                    } else {
                        WidgetTheme.formatMoney(currencySymbol, safeToSpendToday)
                    },
                    style = TextStyle(
                        color = if (isOverBudget) WidgetTheme.moneyNegative else WidgetTheme.moneyPositive,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = if (isOverBudget) "Exceeded daily cap" else "Safe to spend today",
                    style = TextStyle(
                        color = WidgetTheme.textMuted,
                        fontSize = 10.sp
                    )
                )
            } else {
                Text(
                    text = WidgetTheme.formatMoney(currencySymbol, spentToday),
                    style = TextStyle(
                        color = WidgetTheme.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Spent today • Tap to set budget",
                    style = TextStyle(
                        color = WidgetTheme.brandPrimary,
                        fontSize = 9.5.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = progressToday,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                color = if (isOverBudget) WidgetTheme.moneyNegative else if (isNearCap) WidgetTheme.warningAmber else WidgetTheme.moneyPositive,
                backgroundColor = WidgetTheme.surfaceInset
            )
        }
    }

    @Composable
    private fun MediumDailyBudgetLayout(
        context: Context,
        currencySymbol: String,
        safeToSpendToday: Double,
        dailyAllowance: Double,
        spentToday: Double,
        remainingMonthPool: Double,
        daysRemaining: Int,
        hasConfiguredBudget: Boolean,
        progressToday: Float,
        isOverBudget: Boolean,
        isNearCap: Boolean
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_sparkle),
                        contentDescription = null,
                        modifier = GlanceModifier.size(15.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Daily Safe Spend",
                        style = TextStyle(
                            color = WidgetTheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                StatusBadge(isOverBudget = isOverBudget, isNearCap = isNearCap, compact = false)

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Days remaining pill
                Row(
                    modifier = GlanceModifier
                        .background(WidgetTheme.surfaceSunken)
                        .cornerRadius(6.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "$daysRemaining d left",
                        style = TextStyle(color = WidgetTheme.textSecondary, fontSize = 9.5.sp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Split metrics row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Left Column: Safe spend hero
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Safe to spend today",
                        style = TextStyle(color = WidgetTheme.textSecondary, fontSize = 10.5.sp)
                    )
                    Text(
                        text = if (isOverBudget) {
                            "-" + WidgetTheme.formatMoney(currencySymbol, -safeToSpendToday)
                        } else {
                            WidgetTheme.formatMoney(currencySymbol, safeToSpendToday)
                        },
                        style = TextStyle(
                            color = if (isOverBudget) WidgetTheme.moneyNegative else WidgetTheme.moneyPositive,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Right Column: Spent vs Target card
                Row(
                    modifier = GlanceModifier
                        .background(WidgetTheme.surfaceSunken)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Spent Today",
                            style = TextStyle(color = WidgetTheme.textMuted, fontSize = 9.sp)
                        )
                        Text(
                            text = WidgetTheme.formatMoney(currencySymbol, spentToday),
                            style = TextStyle(
                                color = WidgetTheme.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Column {
                        Text(
                            text = "Daily Target",
                            style = TextStyle(color = WidgetTheme.textMuted, fontSize = 9.sp)
                        )
                        Text(
                            text = WidgetTheme.formatMoney(currencySymbol, dailyAllowance),
                            style = TextStyle(
                                color = WidgetTheme.brandPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Quick Add button
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .background(WidgetTheme.brandButton)
                            .cornerRadius(12.dp)
                            .clickable(actionStartActivity(WidgetTheme.createAddExpenseIntent(context))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_plus),
                            contentDescription = "Add",
                            modifier = GlanceModifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = progressToday,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                color = if (isOverBudget) WidgetTheme.moneyNegative else if (isNearCap) WidgetTheme.warningAmber else WidgetTheme.moneyPositive,
                backgroundColor = WidgetTheme.surfaceInset
            )
        }
    }

    @Composable
    private fun LargeDailyBudgetLayout(
        context: Context,
        currencySymbol: String,
        safeToSpendToday: Double,
        dailyAllowance: Double,
        spentToday: Double,
        remainingMonthPool: Double,
        daysRemaining: Int,
        hasConfiguredBudget: Boolean,
        progressToday: Float,
        isOverBudget: Boolean,
        isNearCap: Boolean
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "ExpenseFlow",
                        style = TextStyle(
                            color = WidgetTheme.brandPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Daily Spending Control",
                        style = TextStyle(
                            color = WidgetTheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                StatusBadge(isOverBudget = isOverBudget, isNearCap = isNearCap, compact = false)
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Hero Card
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(WidgetTheme.surfaceSunken)
                    .cornerRadius(12.dp)
                    .padding(10.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "SAFE TO SPEND TODAY",
                        style = TextStyle(
                            color = WidgetTheme.textMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (isOverBudget) {
                            "-" + WidgetTheme.formatMoney(currencySymbol, -safeToSpendToday)
                        } else {
                            WidgetTheme.formatMoney(currencySymbol, safeToSpendToday)
                        },
                        style = TextStyle(
                            color = if (isOverBudget) WidgetTheme.moneyNegative else WidgetTheme.moneyPositive,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (isOverBudget) {
                            "You're over your calculated daily allowance."
                        } else {
                            "Calculated from ${WidgetTheme.formatCompactMoney(currencySymbol, remainingMonthPool)} of unplanned budget left."
                        },
                        style = TextStyle(
                            color = WidgetTheme.textSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // 3 Stat Tiles Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                StatTile(
                    title = "Spent Today",
                    value = WidgetTheme.formatMoney(currencySymbol, spentToday),
                    color = WidgetTheme.moneyNegative,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                StatTile(
                    title = "Daily Target",
                    value = WidgetTheme.formatMoney(currencySymbol, dailyAllowance),
                    color = WidgetTheme.brandPrimary,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                StatTile(
                    title = "Days Left",
                    value = "$daysRemaining days",
                    color = WidgetTheme.textPrimary,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Progress with percentage label
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Daily Budget Usage",
                    style = TextStyle(color = WidgetTheme.textMuted, fontSize = 9.5.sp)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${(progressToday * 100).toInt()}%",
                    style = TextStyle(
                        color = if (isOverBudget) WidgetTheme.moneyNegative else WidgetTheme.textSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            LinearProgressIndicator(
                progress = progressToday,
                modifier = GlanceModifier.fillMaxWidth().height(5.dp).cornerRadius(3.dp),
                color = if (isOverBudget) WidgetTheme.moneyNegative else if (isNearCap) WidgetTheme.warningAmber else WidgetTheme.moneyPositive,
                backgroundColor = WidgetTheme.surfaceInset
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(WidgetTheme.surfaceSunken)
                        .cornerRadius(10.dp)
                        .padding(vertical = 7.dp)
                        .clickable(actionStartActivity(WidgetTheme.createNavigationIntent(context, "plan", "budgets"))),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "Manage Budgets",
                        style = TextStyle(
                            color = WidgetTheme.brandPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(WidgetTheme.brandButton)
                        .cornerRadius(10.dp)
                        .padding(vertical = 7.dp)
                        .clickable(actionStartActivity(WidgetTheme.createAddExpenseIntent(context))),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_plus),
                        contentDescription = null,
                        modifier = GlanceModifier.size(13.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Add Expense",
                        style = TextStyle(
                            color = WidgetTheme.textOnBrand,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun StatTile(
        title: String,
        value: String,
        color: ColorProvider,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Column(
            modifier = modifier
                .background(WidgetTheme.surfaceSunken)
                .cornerRadius(8.dp)
                .padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = title,
                style = TextStyle(color = WidgetTheme.textMuted, fontSize = 8.5.sp),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = value,
                style = TextStyle(
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun StatusBadge(
        isOverBudget: Boolean,
        isNearCap: Boolean,
        compact: Boolean
    ) {
        val bg = if (isOverBudget) WidgetTheme.catBillsBg else if (isNearCap) WidgetTheme.catShoppingBg else WidgetTheme.catTransportBg
        val fg = if (isOverBudget) WidgetTheme.moneyNegative else if (isNearCap) WidgetTheme.warningAmber else WidgetTheme.moneyPositive
        val label = if (isOverBudget) "Over Limit" else if (isNearCap) "Near Cap" else "On Track"

        Row(
            modifier = GlanceModifier
                .background(bg)
                .cornerRadius(6.dp)
                .padding(horizontal = if (compact) 5.dp else 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = fg,
                    fontSize = if (compact) 8.5.sp else 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
