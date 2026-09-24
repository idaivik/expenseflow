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

class QuickExpenseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(130.dp, 110.dp), // Compact (2x2 / 2x1)
            DpSize(240.dp, 120.dp), // Medium Horizontal (4x2 / 3x2)
            DpSize(260.dp, 220.dp)  // Large (4x3 / 4x4)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val settingsRepo = SettingsRepository(context)

        val settings = settingsRepo.settings.firstOrNull()
        val currencySymbol = settings?.currencySymbol ?: "$"

        // Calculate today's spent
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        val allTxns = database.transactionDao().getAllTransactions().firstOrNull() ?: emptyList()
        val spentToday = allTxns.filter { it.isExpense && it.date == todayStr }.sumOf { it.amount }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                QuickExpenseWidgetContent(
                    context = context,
                    currencySymbol = currencySymbol,
                    spentToday = spentToday,
                    widgetSize = size
                )
            }
        }
    }

    @Composable
    private fun QuickExpenseWidgetContent(
        context: Context,
        currencySymbol: String,
        spentToday: Double,
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
        ) {
            when {
                isLarge -> LargeQuickExpenseLayout(context, currencySymbol, spentToday)
                isMedium -> MediumQuickExpenseLayout(context, currencySymbol, spentToday)
                else -> CompactQuickExpenseLayout(context, currencySymbol)
            }
        }
    }

    @Composable
    private fun CompactQuickExpenseLayout(
        context: Context,
        currencySymbol: String
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Header
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(WidgetTheme.createNavigationIntent(context, "home"))),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_wallet),
                    contentDescription = null,
                    modifier = GlanceModifier.size(14.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "Quick Expense",
                    style = TextStyle(
                        color = WidgetTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 2 Quick Category Icons
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                QuickCategoryPill(
                    context = context,
                    name = "Food",
                    iconRes = R.drawable._cup,
                    bg = WidgetTheme.catFoodBg,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                QuickCategoryPill(
                    context = context,
                    name = "Transport",
                    iconRes = R.drawable._activity_1,
                    bg = WidgetTheme.catTransportBg,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Main Hero Add Button
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(WidgetTheme.brandButton)
                    .cornerRadius(12.dp)
                    .padding(vertical = 8.dp, horizontal = 10.dp)
                    .clickable(actionStartActivity(WidgetTheme.createAddExpenseIntent(context))),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_plus),
                    contentDescription = null,
                    modifier = GlanceModifier.size(14.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "Add Expense",
                    style = TextStyle(
                        color = WidgetTheme.textOnBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    @Composable
    private fun MediumQuickExpenseLayout(
        context: Context,
        currencySymbol: String,
        spentToday: Double
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
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(WidgetTheme.createNavigationIntent(context, "home"))),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_wallet),
                        contentDescription = null,
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Quick Expense",
                        style = TextStyle(
                            color = WidgetTheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Spent today pill
                Row(
                    modifier = GlanceModifier
                        .background(WidgetTheme.surfaceSunken)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "Today: ",
                        style = TextStyle(color = WidgetTheme.textMuted, fontSize = 10.sp)
                    )
                    Text(
                        text = WidgetTheme.formatCompactMoney(currencySymbol, spentToday),
                        style = TextStyle(
                            color = WidgetTheme.moneyNegative,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 4 Category Quick Buttons Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                QuickCategoryTile(
                    context = context,
                    name = "Food",
                    iconRes = R.drawable._cup,
                    bg = WidgetTheme.catFoodBg,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                QuickCategoryTile(
                    context = context,
                    name = "Shopping",
                    iconRes = R.drawable._tag,
                    bg = WidgetTheme.catShoppingBg,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                QuickCategoryTile(
                    context = context,
                    name = "Transport",
                    iconRes = R.drawable._activity_1,
                    bg = WidgetTheme.catTransportBg,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                QuickCategoryTile(
                    context = context,
                    name = "Bills",
                    iconRes = R.drawable._numerical_star,
                    bg = WidgetTheme.catBillsBg,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Hero Add Button
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(WidgetTheme.brandButton)
                    .cornerRadius(12.dp)
                    .padding(vertical = 7.dp, horizontal = 12.dp)
                    .clickable(actionStartActivity(WidgetTheme.createAddExpenseIntent(context))),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_plus),
                    contentDescription = null,
                    modifier = GlanceModifier.size(14.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "New Expense Entry",
                    style = TextStyle(
                        color = WidgetTheme.textOnBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    @Composable
    private fun LargeQuickExpenseLayout(
        context: Context,
        currencySymbol: String,
        spentToday: Double
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
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(WidgetTheme.createNavigationIntent(context, "home")))
                ) {
                    Text(
                        text = "ExpenseFlow",
                        style = TextStyle(
                            color = WidgetTheme.brandPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Quick Expense",
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
                        .cornerRadius(10.dp)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "Spent Today: ",
                        style = TextStyle(color = WidgetTheme.textSecondary, fontSize = 11.sp)
                    )
                    Text(
                        text = WidgetTheme.formatMoney(currencySymbol, spentToday),
                        style = TextStyle(
                            color = WidgetTheme.moneyNegative,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // 6 Category Grid (2 rows of 3)
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                QuickCategoryTile(context, "Food", R.drawable._cup, WidgetTheme.catFoodBg, GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(6.dp))
                QuickCategoryTile(context, "Shopping", R.drawable._tag, WidgetTheme.catShoppingBg, GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(6.dp))
                QuickCategoryTile(context, "Transport", R.drawable._activity_1, WidgetTheme.catTransportBg, GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                QuickCategoryTile(context, "Bills", R.drawable._numerical_star, WidgetTheme.catBillsBg, GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(6.dp))
                QuickCategoryTile(context, "Health", R.drawable._activity_2, WidgetTheme.catHealthBg, GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(6.dp))
                QuickCategoryTile(context, "Entertainment", R.drawable._ps5_2, WidgetTheme.catEntertainmentBg, GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Quick Preset Amount Chips
            Text(
                text = "Quick Amount Presets",
                style = TextStyle(color = WidgetTheme.textMuted, fontSize = 10.sp)
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                QuickAmountChip(context, currencySymbol, "5", GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(4.dp))
                QuickAmountChip(context, currencySymbol, "10", GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(4.dp))
                QuickAmountChip(context, currencySymbol, "25", GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(4.dp))
                QuickAmountChip(context, currencySymbol, "50", GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Hero Add Button
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(WidgetTheme.brandButton)
                    .cornerRadius(12.dp)
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .clickable(actionStartActivity(WidgetTheme.createAddExpenseIntent(context))),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_plus),
                    contentDescription = null,
                    modifier = GlanceModifier.size(15.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "Add Custom Expense",
                    style = TextStyle(
                        color = WidgetTheme.textOnBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    @Composable
    private fun QuickCategoryTile(
        context: Context,
        name: String,
        iconRes: Int,
        bg: ColorProvider,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Column(
            modifier = modifier
                .background(WidgetTheme.surfaceSunken)
                .cornerRadius(10.dp)
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .clickable(actionStartActivity(WidgetTheme.createAddExpenseIntent(context, category = name))),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(24.dp)
                    .background(bg)
                    .cornerRadius(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = name,
                    modifier = GlanceModifier.size(14.dp)
                )
            }
            Spacer(modifier = GlanceModifier.height(3.dp))
            Text(
                text = name,
                style = TextStyle(
                    color = WidgetTheme.textPrimary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun QuickCategoryPill(
        context: Context,
        name: String,
        iconRes: Int,
        bg: ColorProvider,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Row(
            modifier = modifier
                .background(WidgetTheme.surfaceSunken)
                .cornerRadius(8.dp)
                .padding(vertical = 5.dp, horizontal = 6.dp)
                .clickable(actionStartActivity(WidgetTheme.createAddExpenseIntent(context, category = name))),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(18.dp)
                    .background(bg)
                    .cornerRadius(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = name,
                    modifier = GlanceModifier.size(11.dp)
                )
            }
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = name,
                style = TextStyle(
                    color = WidgetTheme.textPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun QuickAmountChip(
        context: Context,
        currencySymbol: String,
        amount: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier
                .background(WidgetTheme.surfaceSunken)
                .cornerRadius(8.dp)
                .padding(vertical = 4.dp)
                .clickable(actionStartActivity(WidgetTheme.createAddExpenseIntent(context, amount = amount))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+$currencySymbol$amount",
                style = TextStyle(
                    color = WidgetTheme.brandPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
