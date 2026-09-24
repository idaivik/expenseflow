package com.expenseflow.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseWidget : GlanceAppWidget() {

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

        val totalIncome = database.transactionDao().getTotalIncome().firstOrNull() ?: 0.0
        val totalExpense = database.transactionDao().getTotalExpense().firstOrNull() ?: 0.0
        val balance = totalIncome - totalExpense
        val settings = settingsRepo.settings.firstOrNull()
        val currencySymbol = settings?.currencySymbol ?: "$"

        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        val allTxns = database.transactionDao().getAllTransactions().firstOrNull() ?: emptyList()
        val spentToday = allTxns.filter { it.isExpense && it.date == todayStr }.sumOf { it.amount }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                ExpenseWidgetContent(
                    context = context,
                    balance = balance,
                    income = totalIncome,
                    expense = totalExpense,
                    spentToday = spentToday,
                    currencySymbol = currencySymbol,
                    widgetSize = size
                )
            }
        }
    }

    @Composable
    private fun ExpenseWidgetContent(
        context: Context,
        balance: Double,
        income: Double,
        expense: Double,
        spentToday: Double,
        currencySymbol: String,
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
                .clickable(actionStartActivity(WidgetTheme.createNavigationIntent(context, "home")))
        ) {
            when {
                isLarge -> LargeBalanceLayout(context, balance, income, expense, spentToday, currencySymbol)
                isMedium -> MediumBalanceLayout(context, balance, income, expense, currencySymbol)
                else -> CompactBalanceLayout(balance, income, expense, currencySymbol)
            }
        }
    }

    @Composable
    private fun CompactBalanceLayout(
        balance: Double,
        income: Double,
        expense: Double,
        currencySymbol: String
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "ExpenseFlow",
                    style = TextStyle(
                        color = WidgetTheme.brandPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = "Net Balance",
                style = TextStyle(
                    color = WidgetTheme.textSecondary,
                    fontSize = 10.5.sp
                )
            )
            Text(
                text = WidgetTheme.formatMoney(currencySymbol, balance),
                style = TextStyle(
                    color = if (balance >= 0) WidgetTheme.moneyPositive else WidgetTheme.moneyNegative,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(WidgetTheme.surfaceSunken)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "+" + WidgetTheme.formatCompactMoney(currencySymbol, income),
                    style = TextStyle(
                        color = WidgetTheme.moneyPositive,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "-" + WidgetTheme.formatCompactMoney(currencySymbol, expense),
                    style = TextStyle(
                        color = WidgetTheme.moneyNegative,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    @Composable
    private fun MediumBalanceLayout(
        context: Context,
        balance: Double,
        income: Double,
        expense: Double,
        currencySymbol: String
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "ExpenseFlow",
                    style = TextStyle(
                        color = WidgetTheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Quick Add Button
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

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Net Balance",
                        style = TextStyle(color = WidgetTheme.textSecondary, fontSize = 10.5.sp)
                    )
                    Text(
                        text = WidgetTheme.formatMoney(currencySymbol, balance),
                        style = TextStyle(
                            color = if (balance >= 0) WidgetTheme.moneyPositive else WidgetTheme.moneyNegative,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Row(
                    modifier = GlanceModifier
                        .background(WidgetTheme.surfaceSunken)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Income",
                            style = TextStyle(color = WidgetTheme.textMuted, fontSize = 9.sp)
                        )
                        Text(
                            text = "+" + WidgetTheme.formatCompactMoney(currencySymbol, income),
                            style = TextStyle(
                                color = WidgetTheme.moneyPositive,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Column {
                        Text(
                            text = "Expense",
                            style = TextStyle(color = WidgetTheme.textMuted, fontSize = 9.sp)
                        )
                        Text(
                            text = "-" + WidgetTheme.formatCompactMoney(currencySymbol, expense),
                            style = TextStyle(
                                color = WidgetTheme.moneyNegative,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LargeBalanceLayout(
        context: Context,
        balance: Double,
        income: Double,
        expense: Double,
        spentToday: Double,
        currencySymbol: String
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
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
                        text = "Financial Overview",
                        style = TextStyle(
                            color = WidgetTheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Row(
                    modifier = GlanceModifier
                        .background(WidgetTheme.surfaceSunken)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "Today: " + WidgetTheme.formatCompactMoney(currencySymbol, spentToday),
                        style = TextStyle(
                            color = WidgetTheme.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Main Balance Card
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
                        text = "TOTAL NET BALANCE",
                        style = TextStyle(
                            color = WidgetTheme.textMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = WidgetTheme.formatMoney(currencySymbol, balance),
                        style = TextStyle(
                            color = if (balance >= 0) WidgetTheme.moneyPositive else WidgetTheme.moneyNegative,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Income / Expense Split Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(WidgetTheme.surfaceSunken)
                        .cornerRadius(10.dp)
                        .padding(8.dp)
                ) {
                    Text(text = "Total Income", style = TextStyle(color = WidgetTheme.textMuted, fontSize = 9.5.sp))
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "+" + WidgetTheme.formatMoney(currencySymbol, income),
                        style = TextStyle(color = WidgetTheme.moneyPositive, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(WidgetTheme.surfaceSunken)
                        .cornerRadius(10.dp)
                        .padding(8.dp)
                ) {
                    Text(text = "Total Expense", style = TextStyle(color = WidgetTheme.textMuted, fontSize = 9.5.sp))
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "-" + WidgetTheme.formatMoney(currencySymbol, expense),
                        style = TextStyle(color = WidgetTheme.moneyNegative, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Quick Add Action
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
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
                    text = "Add Transaction",
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
