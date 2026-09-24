package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.model.BudgetCategory
import com.expenseflow.app.model.Transaction
import com.expenseflow.app.ui.components.Amount
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.CategoryVisuals
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFButtonVariant
import com.expenseflow.app.ui.components.EFCard
import com.expenseflow.app.ui.components.EFElevation
import com.expenseflow.app.ui.components.ProgressBar
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.DisplayFont
import com.expenseflow.app.ui.theme.Ink
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.smallShadow
import com.expenseflow.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * ExpenseFlow — Budget Detail screen. Answers the one question Plan/Report can't:
 * "what caused THIS budget to exceed?" Reached by tapping a monthly spending
 * budget on the Plan screen, or the over-budget insight on Home.
 * Mirrors ui_kits/mobile/BudgetDetailScreen.jsx.
 */
@Composable
fun BudgetDetailScreen(
    viewModel: ExpenseViewModel,
    category: String,
    onBack: () -> Unit,
    onAdjust: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val budgets by viewModel.budgetCategories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    BackHandler(onBack = onBack)

    val monthName = remember { SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().time) }
    val budget = budgets.find { it.name == category }

    Column(Modifier.fillMaxSize().background(c.bgApp)) {
        // Header
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).smallShadow(20.dp).clip(CircleShape).background(c.surfaceCard).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Lucide.ChevronLeft, "Back", tint = c.textPrimary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Monthly budget · $monthName", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
            }
        }

        if (budget == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This budget is no longer available.", color = c.textMuted, fontSize = 14.sp, fontFamily = BodyFont)
            }
        } else {
            BudgetDetailBody(budget, transactions, currency, monthName, onAdjust)
        }
    }
}

@Composable
private fun BudgetDetailBody(
    budget: BudgetCategory,
    transactions: List<Transaction>,
    currency: String,
    monthName: String,
    onAdjust: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val context = LocalContext.current

    val spent = budget.spent
    val limit = budget.total
    val pct = if (limit > 0) ((spent / limit) * 100).roundToInt() else 0
    val over = spent - limit
    val exceeded = over > 0 && limit > 0
    val near = !exceeded && limit > 0 && pct >= 85
    val categoryTone = CategoryVisuals.tone(budget.name)
    val stateTone = if (exceeded) CategoryTone.RED else if (near) CategoryTone.ORANGE else categoryTone

    // Where "today" sits in the month — drives the pace / ideal marker.
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysLeft = (daysInMonth - dayOfMonth).coerceAtLeast(0)
    val elapsed = dayOfMonth.toFloat() / daysInMonth
    val idealPct = (elapsed * 100).roundToInt()
    val aheadOfPace = pct - idealPct
    val projected = if (elapsed > 0f) spent / elapsed else spent

    val txns = remember(transactions, budget.name) {
        transactions.filter { it.isExpense && it.category == budget.name && isInCurrentMonth(it.date) }
            .sortedByDescending { it.amount }
    }
    val top3 = txns.take(3).sumOf { it.amount }
    val topShare = if (spent > 0) ((top3 / spent) * 100).roundToInt() else 0

    val statusLabel = if (exceeded) "Over budget" else if (near) "Almost there" else "On track"
    val statusIcon = if (exceeded) Lucide.TriangleAlert else if (near) Lucide.CircleAlert else Lucide.CircleCheck

    fun money(v: Double): String = currency + String.format(Locale.US, "%,.2f", v)

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
        // Summary card — allocated vs spent, overage, pace.
        item {
            EFCard(elevation = EFElevation.Card, contentPadding = PaddingValues(22.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(tone = categoryTone, icon = CategoryVisuals.icon(budget.name), size = 48.dp, cornerRadius = 15.dp)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(budget.name, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${txns.size} transactions this month", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier.clip(CircleShape).background(c.tint(stateTone)).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(statusIcon, null, tint = c.cat(stateTone), modifier = Modifier.size(14.dp))
                        Text(statusLabel, color = c.cat(stateTone), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Amount(value = spent, size = 34.sp, color = if (exceeded) c.catRed else c.textPrimary, weight = FontWeight.ExtraBold, currencySymbol = currency)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("of ", color = c.textMuted, fontSize = 15.sp, fontFamily = BodyFont)
                        Text(money(limit), color = c.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (exceeded) "${money(over)} over budget" else "${money(-over)} left to spend",
                    color = if (exceeded) c.catRed else if (near) c.catOrange else c.catGreen,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                )
                Spacer(Modifier.height(16.dp))
                // Spend bar with an ideal-pace marker.
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    ProgressBar(value = pct.toFloat(), tone = stateTone, height = 12.dp)
                    val markerX = maxWidth * (idealPct / 100f).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = markerX - 1.dp)
                            .size(width = 2.5.dp, height = 18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.textPrimary.copy(alpha = 0.55f)),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$pct%", color = c.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
                        Text(" used · $daysLeft days left", color = c.textSecondary, fontSize = 12.5.sp, fontFamily = BodyFont)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(width = 8.dp, height = 2.5.dp).clip(RoundedCornerShape(2.dp)).background(c.textPrimary.copy(alpha = 0.55f)))
                        Text("Steady pace $idealPct%", color = c.textSecondary, fontSize = 12.5.sp, fontFamily = BodyFont)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Insight banner — the signature dark card, tells you WHY plainly.
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (c.isDark) c.surfaceInset else Ink)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(if (exceeded) Lucide.Sparkles else Lucide.Lightbulb, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            exceeded -> "You went ${money(over)} over on ${budget.name}"
                            near -> "You're close to your ${budget.name} limit"
                            else -> "${budget.name} is well under control"
                        },
                        color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        when {
                            exceeded -> "Your top 3 purchases account for $topShare% of the spend, and you're $aheadOfPace% ahead of a steady pace. At this rate you'll finish near ${money(projected)}."
                            near -> "You've used $pct% with $daysLeft days to go — about ${max(0, aheadOfPace)}% ahead of pace. Ease off to stay under."
                            else -> "You're ${kotlin.math.abs(aheadOfPace)}% below a steady pace with $daysLeft days left. Nice and comfortable."
                        },
                        color = Color.White.copy(alpha = 0.72f), fontSize = 13.5.sp, fontFamily = BodyFont, lineHeight = 20.sp,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        // The culprits — transactions largest-first.
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    if (exceeded) "What pushed you over" else "Where it went",
                    color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont,
                )
                Text("Largest first", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (txns.isEmpty()) {
            item {
                EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(22.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Lucide.Receipt, null, tint = c.textMuted, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No spending logged in ${budget.name} this month.", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        } else {
            itemsIndexed(txns) { i, t ->
                val share = if (spent > 0) ((t.amount / spent) * 100).roundToInt() else 0
                val big = i < 3 && exceeded
                Box(Modifier.padding(bottom = 10.dp)) {
                    EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                            CategoryIcon(tone = categoryTone, icon = CategoryVisuals.icon(budget.name), size = 42.dp, cornerRadius = 13.dp)
                            Column(Modifier.weight(1f)) {
                                Text(t.title, color = c.textPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(shortDate(t.date), color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Amount(value = t.amount, size = 15.5.sp, color = c.moneyOut, currencySymbol = currency, prefix = "-")
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "$share% of spend",
                                    color = if (big) c.catRed else c.textMuted,
                                    fontSize = 12.sp, fontWeight = if (big) FontWeight.Bold else FontWeight.Medium, fontFamily = BodyFont,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actions — make it correctable, not just informational.
        item {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EFButton(
                    text = "Adjust limit", onClick = onAdjust, variant = EFButtonVariant.Primary,
                    leadingIcon = Lucide.SlidersHorizontal, modifier = Modifier.weight(1f),
                )
                EFButton(
                    text = "Set alert",
                    onClick = { Toast.makeText(context, "You'll be alerted automatically as you approach this budget.", Toast.LENGTH_SHORT).show() },
                    variant = EFButtonVariant.Secondary, leadingIcon = Lucide.Bell, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// Parsers/formatters for the stored "dd/MM/yyyy" transaction date.
private val storedFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val shortFmt = SimpleDateFormat("d MMM", Locale.getDefault())

private fun shortDate(s: String): String = try {
    storedFmt.parse(s)?.let { shortFmt.format(it) } ?: s
} catch (e: Exception) {
    s
}

private fun isInCurrentMonth(dateStr: String): Boolean = try {
    val d = storedFmt.parse(dateStr) ?: return false
    val cal = Calendar.getInstance().apply { time = d }
    val now = Calendar.getInstance()
    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
} catch (e: Exception) {
    false
}
