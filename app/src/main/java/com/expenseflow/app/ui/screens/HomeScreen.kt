package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.expenseflow.app.data.PlannedExpenseEntity
import com.expenseflow.app.model.BudgetCategory
import com.expenseflow.app.model.Transaction
import com.expenseflow.app.model.MonthlyPlanStats
import com.expenseflow.app.ui.components.Amount
import com.expenseflow.app.ui.components.Avatar
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.CategoryVisuals
import com.expenseflow.app.ui.components.DayPicker
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFButtonSize
import com.expenseflow.app.ui.components.EFButtonVariant
import com.expenseflow.app.ui.components.EFCard
import com.expenseflow.app.ui.components.EFElevation
import com.expenseflow.app.ui.components.EFIconButton
import com.expenseflow.app.ui.components.EFIconButtonVariant
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.components.MonthPicker
import com.expenseflow.app.ui.components.PickerTheme
import com.expenseflow.app.ui.components.gridPopDelay
import com.expenseflow.app.ui.components.popIn
import com.expenseflow.app.ui.components.StatCard
import com.expenseflow.app.ui.components.TransactionRow
import com.expenseflow.app.ui.components.pressScale
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.DisplayFont
import com.expenseflow.app.ui.theme.NumberFont
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.heroBrush
import com.expenseflow.app.ui.theme.smallShadow
import com.expenseflow.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** How many transactions the home ledger shows per "View More" step. */
private const val HOME_PAGE_SIZE = 5

@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    onAdd: () -> Unit,
    onBell: () -> Unit,
    onProfile: () -> Unit,
    onViewAll: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenBudgetDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.efColors
    val balance by viewModel.balance.collectAsState()
    val income by viewModel.totalIncome.collectAsState()
    val expense by viewModel.totalExpense.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val budgetLimit by viewModel.totalBudgetLimit.collectAsState()
    val spent by viewModel.totalSpentThisMonth.collectAsState()
    val budgetCategories by viewModel.budgetCategories.collectAsState()
    val hasNotifications by viewModel.hasNotifications.collectAsState()
    val txDisplayMode by viewModel.transactionDisplayMode.collectAsState()
    val planStats by viewModel.monthlyPlanStats.collectAsState()
    val plannedExpenses by viewModel.plannedExpenses.collectAsState()
    val expenseCategories by viewModel.expenseCategoryList.collectAsState()
    // The day whose plans are open in the bottom sheet (null = closed).
    var planDay by remember { mutableStateOf<LocalDate?>(null) }

    var heroDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    val weekDelta = remember(transactions) { weekOverWeekDelta(transactions) }

    // Highest over-budget category — the "why am I over?" answer, one tap from home.
    val worstBudget = remember(budgetCategories) {
        budgetCategories.filter { it.total > 0 && it.spent > it.total }.maxByOrNull { it.spent - it.total }
    }

    // Pin state lives on the screen (design: pinned rows float to the top,
    // the pin button in the header filters to pinned only).
    var pinnedIds by remember { mutableStateOf(setOf<String>()) }
    var pinnedOnly by remember { mutableStateOf(false) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    // Ledger controls (design: search field, type chips, single-day calendar filter).
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") } // all | in | out
    var dayFilter by remember { mutableStateOf<LocalDate?>(null) }

    // Days that carry transactions — surfaced as dots in the calendar.
    val marks = remember(transactions) { transactions.mapNotNull { parseLedgerDate(it.date) }.toSet() }
    val latestMonth = remember(transactions) {
        transactions.mapNotNull { parseLedgerDate(it.date) }.maxOrNull() ?: LocalDate.now()
    }

    // "Top 5, then View More" — the home ledger reveals HOME_PAGE_SIZE rows at a time.
    // Recreating the state whenever the filters (or a refresh) change resets the
    // window back to the top 5.
    var visibleCount by remember(query, typeFilter, dayFilter, pinnedOnly, refreshTick) { mutableStateOf(HOME_PAGE_SIZE) }

    // All matching transactions in display order; the grouped `entries` only cover
    // the first `visibleCount` of them.
    val ordered = remember(transactions, query, typeFilter, dayFilter, pinnedOnly, pinnedIds) {
        orderedLedgerTx(transactions, query, typeFilter, dayFilter, pinnedOnly, pinnedIds)
    }
    val entries = remember(ordered, visibleCount) { groupLedger(ordered.take(visibleCount), txDisplayMode) }
    // Total stays a summary of everything that matches, independent of the window.
    val visibleTotal = remember(ordered, txDisplayMode) {
        ledgerSum(ordered, txDisplayMode)
    }
    val anyPinned = transactions.any { it.id in pinnedIds }
    val anyFilter = pinnedOnly || typeFilter != "all" || query.isNotBlank() || dayFilter != null

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        item {
            // ---- Gradient hero ----
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(heroBrush())
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 26.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(CircleShape).clickable(onClick = onProfile)) {
                        Avatar(name = userName, size = 48.dp, badge = Lucide.Wallet)
                    }
                    // Liquid-morph month picker — frosted pill over the gradient.
                    MonthPicker(date = heroDate, onChange = { heroDate = it }, theme = PickerTheme.Hero)
                    EFIconButton(Lucide.Bell, onBell, variant = EFIconButtonVariant.Soft, dot = hasNotifications, contentDescription = "Notifications")
                }
                Spacer(Modifier.height(22.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current Balance", color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp, fontFamily = BodyFont)
                    Spacer(Modifier.height(8.dp))
                    Amount(
                        value = balance, size = 46.sp,
                        color = if (balance < 0) c.catRed else Color.White,
                        currencySymbol = currency,
                        prefix = if (balance < 0) "-" else "",
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 14.dp, vertical = 7.dp)) {
                        val sign = if (weekDelta >= 0) "+" else "-"
                        Text(
                            "$sign$currency${abs(weekDelta).roundToInt()} than last week",
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 22.dp)) {
                // ---- Your Money ----
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        SectionTitle("Your Money")
                        Icon(Lucide.Info, null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                    }
                    DetailsPill(label = "Details", onClick = onViewAll)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    StatCard("Income", income, CategoryTone.GREEN, Lucide.PiggyBank, currency, Modifier.weight(1f))
                    StatCard("Expenses", expense, CategoryTone.RED, Lucide.Wallet, currency, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                // The budget pool, the daily rate it produces, and the calendar that
                // spends against it are one chain — drawn joined, with no spacers
                // breaking the connector, so the dependency is visible.
                BudgetSummaryCard(budget = budgetLimit, spent = spent, currency = currency, planStats = planStats, onClick = onOpenBudget)
                PlanConnector(stats = planStats, currency = currency)
                SpendingPlanCard(
                    stats = planStats,
                    planned = plannedExpenses,
                    currency = currency,
                    onPickDay = { planDay = it },
                )
                // Over-budget insight — deep-links into the same budget detail the Plan
                // tab uses. It sits below the chain rather than inside it.
                if (worstBudget != null) {
                    Spacer(Modifier.height(12.dp))
                    BudgetAlert(worst = worstBudget, currency = currency, onOpen = onOpenBudgetDetail)
                }
            }
        }

        item {
            Column(Modifier.padding(top = 22.dp)) {
                // ---- Transactions header: search · pin filter · refresh ----
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("Transactions")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (searchOpen) c.brandSoft else Color.Transparent)
                                .clickable { searchOpen = !searchOpen },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (searchOpen) Lucide.X else Lucide.Search,
                                contentDescription = if (searchOpen) "Close search" else "Search transactions",
                                tint = if (searchOpen) c.brand else c.textMuted, modifier = Modifier.size(18.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .alpha(if (anyPinned) 1f else 0.45f)
                                .clip(CircleShape)
                                .background(if (pinnedOnly) c.brandSoft else Color.Transparent)
                                .clickable(enabled = anyPinned) { pinnedOnly = !pinnedOnly },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Lucide.Pin, contentDescription = if (pinnedOnly) "Show all transactions" else "Show pinned only",
                                tint = if (pinnedOnly) c.brand else c.textMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        val spin by animateFloatAsState(refreshTick * 360f, animationSpec = tween(600), label = "refresh")
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    refreshTick++
                                    activeId = null
                                    pinnedOnly = false
                                    pinnedIds = emptySet()
                                    query = ""
                                    typeFilter = "all"
                                    dayFilter = null
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Lucide.RefreshCw, contentDescription = "Refresh transactions", tint = c.textMuted, modifier = Modifier.size(18.dp).rotate(spin))
                        }
                    }
                }

                // ---- Expandable search + filters ----
                AnimatedVisibility(
                    visible = searchOpen,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
                        EFTextField(
                            query, { query = it }, placeholder = "Search transactions", leadingIcon = Lucide.Search,
                            trailingContent = if (query.isNotEmpty()) {
                                {
                                    Box(Modifier.clip(CircleShape).clickable { query = "" }.padding(2.dp)) {
                                        Icon(Lucide.X, "Clear search", tint = c.textMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                            LedgerChip(typeFilter == "all", "All", null) { typeFilter = "all" }
                            LedgerChip(typeFilter == "in", "Income", Lucide.ArrowDownLeft) { typeFilter = "in" }
                            LedgerChip(typeFilter == "out", "Expenses", Lucide.ArrowUpRight) { typeFilter = "out" }
                            Spacer(Modifier.weight(1f))
                            DayPicker(value = dayFilter, onChange = { dayFilter = it }, initialMonth = latestMonth, marks = marks)
                        }
                    }
                }

                // ---- Filter summary line ----
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    val df = dayFilter
                    if (df != null) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(c.brandSoft)
                                .clickable { dayFilter = null }
                                .padding(start = 11.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(efFullLabel(df), color = c.brand, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
                            Box(Modifier.size(18.dp).clip(CircleShape).background(c.brand), contentAlignment = Alignment.Center) {
                                Icon(Lucide.X, "Clear date", tint = c.onBrand, modifier = Modifier.size(12.dp))
                            }
                        }
                    } else {
                        Text("All transactions", color = c.textSecondary, fontSize = 14.sp, fontFamily = BodyFont)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Total ", color = c.textSecondary, fontSize = 14.sp, fontFamily = BodyFont)
                        Text(signedMoney(visibleTotal, currency), color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
                    }
                }
            }
        }

        items(entries, key = { it.key }) { entry ->
            when (entry) {
                is LedgerEntry.TxHeader -> Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = if (entry.first) 6.dp else 20.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom,
                ) {
                    Text(entry.label, color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
                    Text(
                        signedMoney(entry.net, currency),
                        color = if (entry.net < 0) c.textSecondary else c.catGreen,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
                    )
                }

                is LedgerEntry.TxRow -> Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    val t = entry.t
                    TransactionItem(
                        t = t,
                        currency = currency,
                        active = activeId == t.id,
                        pinned = t.id in pinnedIds,
                        onActivate = { activeId = it },
                        onDeactivate = { if (activeId == it) activeId = null },
                        onPin = { id ->
                            pinnedIds = if (id in pinnedIds) pinnedIds - id else pinnedIds + id
                            activeId = null
                        },
                        onDelete = { id ->
                            viewModel.deleteTransaction(id)
                            activeId = null
                        },
                    )
                }
            }
        }

        if (ordered.size > visibleCount) {
            item {
                ViewMoreButton(
                    remaining = ordered.size - visibleCount,
                    onClick = { visibleCount += HOME_PAGE_SIZE },
                )
            }
        }

        if (entries.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(if (anyFilter) Lucide.SearchX else Lucide.Receipt, null, tint = c.textMuted, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            query.isNotBlank() -> "No transactions match “${query.trim()}”."
                            dayFilter != null -> "No transactions on this date."
                            pinnedOnly -> "No pinned transactions yet."
                            else -> "No transactions."
                        },
                        color = c.textMuted, fontSize = 14.sp, fontFamily = BodyFont,
                    )
                    if (anyFilter) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Clear filters", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                query = ""; typeFilter = "all"; dayFilter = null; pinnedOnly = false
                            }.padding(6.dp),
                        )
                    }
                }
            }
        }
    }

    planDay?.let { day ->
        DayPlanSheet(
            date = day,
            plans = plannedExpenses.filter { parseLedgerDate(it.date) == day },
            categories = expenseCategories.map { it.name },
            currency = currency,
            onAdd = { title, category, amount ->
                viewModel.addPlannedExpense(title, category, amount, formatLedgerDate(day))
            },
            onUpdate = { viewModel.updatePlannedExpense(it) },
            onDelete = { viewModel.deletePlannedExpense(it) },
            onLog = { viewModel.logPlannedExpense(it) },
            onUnlog = { viewModel.unlogPlannedExpense(it) },
            onDismiss = { planDay = null },
        )
    }
}

/** Centered "View 5 more" pill that reveals the next page of transactions. */
@Composable
private fun ViewMoreButton(remaining: Int, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        EFButton(
            text = "View ${minOf(HOME_PAGE_SIZE, remaining)} more",
            onClick = onClick,
            variant = EFButtonVariant.Secondary,
            size = EFButtonSize.Md,
            trailingIcon = Lucide.ChevronDown,
        )
    }
}

/** Section heading — 22px bold display type. */
@Composable
fun SectionTitle(title: String) {
    val c = MaterialTheme.efColors
    Text(title, color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont, maxLines = 1)
}

/** Legacy header kept for other screens: title + optional text action. */
@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val c = MaterialTheme.efColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(title)
        if (actionLabel != null && onAction != null) {
            Text(actionLabel, color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

/** "Details ›" — small white pill button with hairline border and soft shadow. */
@Composable
private fun DetailsPill(label: String, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .pressScale(interaction, 0.95f)
            .smallShadow(999.dp)
            .clip(CircleShape)
            .background(c.surfaceCard)
            .border(1.dp, c.borderDefault, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
        Icon(Lucide.ChevronRight, null, tint = c.textPrimary, modifier = Modifier.size(15.dp))
    }
}

/** All / Income / Expenses filter chip in the ledger search panel. */
@Composable
private fun LedgerChip(active: Boolean, label: String, icon: ImageVector?, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    Row(
        modifier = Modifier
            .then(if (!active) Modifier.smallShadow(999.dp) else Modifier)
            .clip(CircleShape)
            .background(if (active) c.brand else c.surfaceCard)
            .then(if (!active) Modifier.border(1.dp, c.borderDefault, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (active) c.onBrand else c.textMuted, modifier = Modifier.size(13.dp))
        Text(label, color = if (active) c.onBrand else c.textSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
    }
}

/**
 * A ledger row with a long-press-revealed action rail (Pin + Delete), mirroring
 * the design's hover/long-press interaction: the row springs left to expose the
 * two 46px action tiles behind it.
 */
@Composable
private fun TransactionItem(
    t: Transaction,
    currency: String,
    active: Boolean,
    pinned: Boolean,
    onActivate: (String) -> Unit,
    onDeactivate: (String) -> Unit,
    onPin: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val c = MaterialTheme.efColors
    val railWidth = 116.dp
    val offsetX by animateDpAsState(
        targetValue = if (active) -railWidth else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "rowReveal",
    )

    Box {
        // Action rail behind the row.
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionTile(
                icon = if (pinned) Lucide.PinOff else Lucide.Pin,
                bg = if (pinned) c.brand else c.brandSoft,
                tint = if (pinned) c.onBrand else c.brand,
                contentDescription = if (pinned) "Unpin" else "Pin",
                enabled = active,
            ) { onPin(t.id) }
            ActionTile(
                icon = Lucide.Trash2,
                bg = c.tintRed,
                tint = c.catRed,
                contentDescription = "Delete",
                enabled = active,
            ) { onDelete(t.id) }
        }

        // Foreground card — slides left to reveal the rail.
        Box(
            Modifier
                .offset(x = offsetX)
                .pointerInput(t.id, active) {
                    detectTapGestures(
                        onLongPress = { onActivate(t.id) },
                        onTap = { if (active) onDeactivate(t.id) },
                    )
                },
        ) {
            TransactionRow(
                title = t.title, category = t.category, time = t.time, date = t.date,
                amount = t.amount, isExpense = t.isExpense, currencySymbol = currency,
                isEdited = t.isEdited, pinned = pinned,
            )
        }
    }
}

/** 46px rounded action button used in the reveal rail. */
@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    tint: Color,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(46.dp)
            .pressScale(interaction, 0.92f)
            .smallShadow(14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/**
 * Over-budget insight — the highest over-budget category surfaced under the month
 * summary, deep-linking into the Budget Detail screen (mirrors HomeScreen.jsx BudgetAlert).
 */
@Composable
private fun BudgetAlert(worst: BudgetCategory, currency: String, onOpen: (String) -> Unit) {
    val c = MaterialTheme.efColors
    val over = (worst.spent - worst.total).roundToInt()
    val pct = ((worst.spent / worst.total) * 100).roundToInt()
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .pressScale(interaction, 0.985f)
            .smallShadow(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surfaceCard)
            .border(1.dp, c.borderSubtle, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null) { onOpen(worst.name) }
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fillMaxHeight().width(3.dp).background(c.catRed))
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(c.tintRed), contentAlignment = Alignment.Center) {
                Icon(Lucide.TriangleAlert, null, tint = c.catRed, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.catRed, fontWeight = FontWeight.Bold)) { append(worst.name) }
                        append(" is $currency$over over budget")
                    },
                    color = c.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Biggest overspend this month · $pct% of $currency${worst.total.toInt()} · see what caused it",
                    color = c.textSecondary, fontSize = 12.5.sp, fontFamily = BodyFont, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Lucide.ChevronRight, null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BudgetSummaryCard(budget: Double, spent: Double, currency: String, planStats: MonthlyPlanStats, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    val left = (budget - spent).coerceAtLeast(0.0)
    val pctSpent = if (budget > 0) ((spent / budget) * 100).toFloat() else 0f

    val cal = Calendar.getInstance()
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Today counts as a day you still have to eat from the budget, so it's included.
    val daysLeft = planStats.daysRemaining
    val pctTimeElapsed = ((daysInMonth - daysLeft).toFloat() / daysInMonth) * 100f
    val monthName = remember { SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time) }

    // On pace = spending no faster than time is passing, with a small grace band.
    // This card owns every verdict about the month's pool — the plan card below
    // reads from the same numbers and doesn't repeat the judgement.
    val overBudget = spent > budget && budget > 0
    val overPace = pctSpent > pctTimeElapsed + 8f
    val tone = when {
        overBudget -> CategoryTone.RED
        planStats.isOverCommitted -> CategoryTone.RED
        overPace -> CategoryTone.ORANGE
        else -> CategoryTone.GREEN
    }
    val status = when {
        budget <= 0 -> "Set budget"
        overBudget -> "Over budget"
        planStats.isOverCommitted -> "Over-committed"
        overPace -> "Watch pace"
        else -> "On track"
    }

    EFCard(elevation = EFElevation.Sm, cornerRadius = 18.dp, onClick = onClick) {
        // Header: label + pace status pill (with leading dot).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(c.brandSoft), contentAlignment = Alignment.Center) {
                    Icon(Lucide.Wallet, null, tint = c.brand, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text("$monthName budget", color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
            }
            Row(
                modifier = Modifier.clip(CircleShape).background(c.tint(tone)).padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.cat(tone)))
                Text(status, color = c.cat(tone), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont, maxLines = 1)
            }
        }
        Spacer(Modifier.height(14.dp))
        // One headline, then the breakdown. What's been spent isn't repeated here —
        // it's a slice of the bar below, and the legend names it once.
        Column {
            Amount(left, 30.sp, c.cat(tone), currencySymbol = currency)
            Spacer(Modifier.height(2.dp))
            Text(
                "left of $currency${"%,d".format(budget.toInt())} this month",
                color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont,
            )
        }
        Spacer(Modifier.height(14.dp))
        // Where the month's money stands: already gone, promised to a day on the
        // calendar below, or still free. Carving the planned slice out of the same
        // bar is what makes the two cards legible as one budget rather than two.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            BudgetSplitBar(
                spentFraction = if (budget > 0) (spent / budget).toFloat() else 0f,
                plannedFraction = if (budget > 0) (planStats.plannedUpcoming / budget).toFloat() else 0f,
                spentTone = tone,
                height = 10.dp,
            )
            val markerX = maxWidth * (pctTimeElapsed / 100f).coerceIn(0f, 1f)
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = markerX - 1.dp)
                    .size(width = 2.dp, height = 16.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(c.textMuted),
            )
        }
        Spacer(Modifier.height(12.dp))
        // Legend for the three slices above. The daily rate and the day count live
        // on the connector below, where they hand off to the calendar.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            SplitLegend("Spent", spent, currency, c.cat(tone), Alignment.Start, Modifier.weight(1f))
            SplitLegend("Planned", planStats.plannedUpcoming, currency, c.brand, Alignment.CenterHorizontally, Modifier.weight(1f))
            SplitLegend("Free", planStats.freePool.coerceAtLeast(0.0), currency, null, Alignment.End, Modifier.weight(1f))
        }
    }
}

/**
 * The month's budget as three slices — spent, promised to a planned day, still
 * free. Anything the plans overrun by is simply not drawn: the "Over-committed"
 * status and the connector's zero daily rate carry that, and a bar that lies
 * about its own width would be worse than one that runs out.
 */
@Composable
private fun BudgetSplitBar(
    spentFraction: Float,
    plannedFraction: Float,
    spentTone: CategoryTone,
    height: Dp,
) {
    val c = MaterialTheme.efColors
    val spentTarget = spentFraction.coerceIn(0f, 1f)
    val plannedTarget = plannedFraction.coerceIn(0f, 1f - spentTarget)
    val spentFrac by animateFloatAsState(spentTarget, label = "splitSpent")
    val plannedFrac by animateFloatAsState(plannedTarget, label = "splitPlanned")
    val freeFrac = (1f - spentFrac - plannedFrac).coerceAtLeast(0f)

    Row(
        Modifier.fillMaxWidth().height(height).clip(CircleShape).background(c.surfaceSunken),
    ) {
        // A zero-weight child isn't allowed, so empty slices are left out entirely.
        if (spentFrac > 0f) Box(Modifier.weight(spentFrac).fillMaxHeight().background(c.cat(spentTone)))
        if (plannedFrac > 0f) Box(Modifier.weight(plannedFrac).fillMaxHeight().background(c.brand))
        if (freeFrac > 0f) Spacer(Modifier.weight(freeFrac))
    }
}

/**
 * One slice's key under [BudgetSplitBar]. The amount sits under the label rather
 * than beside it: three inline label+amount pairs don't fit the card's width once
 * the figures reach five digits. A null [dotColor] is the empty-track slice, drawn
 * as a ring so it reads as "nothing here yet" rather than another colour.
 */
@Composable
private fun SplitLegend(
    label: String,
    value: Double,
    currency: String,
    dotColor: Color?,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.efColors
    Column(modifier, horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (dotColor != null) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            } else {
                Box(Modifier.size(7.dp).clip(CircleShape).border(1.5.dp, c.borderDefault, CircleShape))
            }
            Text(label, color = c.textSecondary, fontSize = 12.sp, fontFamily = BodyFont, maxLines = 1)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "$currency${"%,d".format(value.roundToInt())}",
            color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            fontFamily = NumberFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The seam between the budget card and the plan calendar. It carries the one
 * number both of them are about — what's left of the pool divided by the days
 * still to come — so neither card has to state it, and draws the dependency as a
 * literal line: the budget above produces the rate, the calendar below spends
 * against it and feeds planned days back up into the bar.
 */
@Composable
private fun PlanConnector(stats: MonthlyPlanStats, currency: String) {
    val c = MaterialTheme.efColors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PlanConnectorSpine()
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(c.brandSoft)
                .padding(horizontal = 13.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (stats.isConfigured) {
                Text(
                    "$currency${stats.perDay.roundToInt()}",
                    color = c.brand, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont,
                )
                Text(
                    "/day for ${stats.daysRemaining} days left",
                    color = c.textSecondary, fontSize = 12.5.sp, fontFamily = BodyFont,
                )
            } else {
                Text(
                    "Set a monthly budget to get a daily figure",
                    color = c.textSecondary, fontSize = 12.5.sp, fontFamily = BodyFont,
                )
            }
            Icon(Lucide.ArrowDown, null, tint = c.brand, modifier = Modifier.size(13.dp))
        }
        PlanConnectorSpine()
    }
}

/** The 2dp rule above and below the connector pill. */
@Composable
private fun PlanConnectorSpine() {
    val c = MaterialTheme.efColors
    Box(Modifier.size(width = 2.dp, height = 10.dp).background(c.borderDefault))
}

/**
 * The month's spending plan as a calendar. Each day the user has committed to a
 * spend shows the amount on it, and every planned amount is reserved out of the
 * monthly budget straight away — so the "free to spend" figure here and the
 * "/day to stay on track" figure on the budget card above both already account
 * for the haircut booked for the 12th, instead of only reacting once it's paid.
 *
 * Tapping a day opens [DayPlanSheet] to add, edit, or log that day's plans.
 *
 * The grid retracts into the header so the card can sit on a busy home screen
 * without owning half of it. Retracting narrows the calendar to the current week
 * rather than removing it: the days the user is actually about to spend on stay
 * on screen and stay tappable, and only the rest of the month folds away. The
 * numbers and what's coming up show either way, and the choice survives tab
 * switches and rotation, so a user who keeps it narrow isn't fighting it on
 * every visit.
 */
@Composable
private fun SpendingPlanCard(
    stats: MonthlyPlanStats,
    planned: List<PlannedExpenseEntity>,
    currency: String,
    onPickDay: (LocalDate) -> Unit,
) {
    val c = MaterialTheme.efColors
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var expanded by rememberSaveable { mutableStateOf(true) }

    // Bumped on each switch between week and month so the cells replay their pop;
    // paging between months leaves it alone, letting the horizontal slide carry
    // that motion instead.
    var popRun by remember { mutableIntStateOf(0) }
    var popOnNextGrid by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) { popRun++; popOnNextGrid = true }
    // Which way the month strip should slide, from the control that changed it.
    var monthForward by remember { mutableStateOf(true) }
    val chevronTurn by animateFloatAsState(
        if (expanded) 180f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "planChevron",
    )

    // No status pill here: the budget card above owns the month's verdict. This
    // card only colours today's number, and red stays reserved for the month-level
    // failure so a single overspent day doesn't read as a blown budget.
    val tone = if (stats.isOverDailyAllowance) CategoryTone.ORANGE else CategoryTone.BLUE

    EFCard(elevation = EFElevation.Sm, cornerRadius = 18.dp) {
        // Header doubles as the disclosure control: the whole row is the hit target,
        // and the chevron spins to point at whichever state the tap will produce.
        val headerInteraction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(interactionSource = headerInteraction, indication = null) {
                    // Narrowing to the week also drops any month the user paged to, so
                    // re-opening lands on the month that week belongs to rather than on
                    // wherever the strip was left months ago.
                    if (expanded) month = YearMonth.from(today)
                    expanded = !expanded
                }
                .pressScale(headerInteraction, pressedScale = 0.985f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(c.brandSoft), contentAlignment = Alignment.Center) {
                    Icon(Lucide.CalendarRange, null, tint = c.brand, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text("Spending plan", color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (expanded) "Week" else "Month",
                    color = c.textMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
                )
                Icon(
                    Lucide.ChevronDown,
                    contentDescription = if (expanded) "Show this week only" else "Show the full month",
                    tint = c.textMuted,
                    modifier = Modifier.size(16.dp).rotate(chevronTurn),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Amount(
                    value = if (stats.isOverDailyAllowance) -stats.safeToSpendToday else stats.safeToSpendToday,
                    size = 26.sp, color = c.cat(tone), currencySymbol = currency,
                    prefix = if (stats.isOverDailyAllowance) "-" else "",
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (stats.isOverDailyAllowance) "over today's allowance" else "free to spend today",
                    color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$currency${stats.spentToday.roundToInt()}",
                    color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                )
                Spacer(Modifier.height(2.dp))
                Text("spent today", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
            }
        }

        // ---- The retractable part: full month, or just the week we're in ----
        // Retracting swaps the month grid for the current week instead of clearing
        // the calendar out: collapsing is about reclaiming screen from the other
        // five weeks, not about giving up the days the user is spending on now.
        // The height is a plain eased tween rather than a spring — a bouncing
        // height would shove every row below the card up and down — so the
        // personality lives in the cells instead (see [popIn]).
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(tween(200, delayMillis = 80)),
                    initialContentExit = fadeOut(tween(110)),
                    sizeTransform = SizeTransform(clip = true) { _, _ -> tween(280, easing = EaseOutCubic) },
                )
            },
            label = "planCalendarScope",
        ) { showMonth ->
            Column {
                Spacer(Modifier.height(16.dp))
                if (showMonth) {
                    // ---- Month strip: navigate the calendar without leaving home ----
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        PlanNavButton(Lucide.ChevronLeft, "Previous month") {
                            monthForward = false; popOnNextGrid = false; month = month.minusMonths(1)
                        }
                        // The label rides along with the grid so both read as one page turn.
                        AnimatedContent(
                            targetState = month,
                            transitionSpec = { monthSlide(monthForward, distance = 24) },
                            label = "planMonthLabel",
                        ) { m ->
                            Text(
                                m.format(planMonthFormatter),
                                color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                            )
                        }
                        PlanNavButton(Lucide.ChevronRight, "Next month") {
                            monthForward = true; popOnNextGrid = false; month = month.plusMonths(1)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    AnimatedContent(
                        targetState = month,
                        transitionSpec = { monthSlide(monthForward, distance = 60) },
                        label = "planMonthGrid",
                    ) { m ->
                        PlanCalendar(
                            month = m,
                            today = today,
                            planned = planned,
                            currency = currency,
                            popRun = popRun,
                            // Opening the card pops the cells; paging months doesn't, so
                            // the slide isn't fighting 30 separate scale animations.
                            popCells = popOnNextGrid,
                            onPickDay = onPickDay,
                        )
                    }
                } else {
                    PlanWeekStrip(
                        today = today,
                        planned = planned,
                        currency = currency,
                        popRun = popRun,
                        popCells = popOnNextGrid,
                        onPickDay = onPickDay,
                    )
                }
            }
        }

        // ---- Overdue plans: the date passed and the money was never logged ----
        val overdue = remember(planned, today) {
            planned.filter { p ->
                p.loggedTxnId == 0 && parseLedgerDate(p.date)?.isBefore(today) == true
            }
        }
        if (overdue.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.tint(CategoryTone.ORANGE))
                    .clickable { parseLedgerDate(overdue.first().date)?.let(onPickDay) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Lucide.TriangleAlert, null, tint = c.cat(CategoryTone.ORANGE), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${overdue.size} plan${if (overdue.size == 1) "" else "s"} past their date — log or remove ${if (overdue.size == 1) "it" else "them"}",
                    color = c.textSecondary, fontSize = 12.5.sp, fontFamily = BodyFont, modifier = Modifier.weight(1f),
                )
                Icon(Lucide.ChevronRight, null, tint = c.textMuted, modifier = Modifier.size(16.dp))
            }
        }

        // ---- What's next ----
        val upcoming = remember(planned, today) {
            planned.filter { p ->
                p.loggedTxnId == 0 && parseLedgerDate(p.date)?.isBefore(today) == false
            }.take(3)
        }
        if (upcoming.isEmpty() && overdue.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                // Days stay tappable in both states, so the prompt doesn't change —
                // only how far ahead the visible calendar reaches.
                if (expanded) {
                    "Tap a day to plan a spend you already know about. It's set aside from the budget straight away, so today's allowance stays honest."
                } else {
                    "Tap a day this week — or open the month — to plan a spend you already know about. It's set aside from the budget straight away, so today's allowance stays honest."
                },
                color = c.textMuted, fontSize = 12.5.sp, lineHeight = 18.sp, fontFamily = BodyFont,
            )
        } else if (upcoming.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Coming up", color = c.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
            Spacer(Modifier.height(8.dp))
            upcoming.forEach { p ->
                val day = parseLedgerDate(p.date)
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { day?.let(onPickDay) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIcon(
                        tone = CategoryVisuals.tone(p.category),
                        icon = CategoryVisuals.icon(p.category),
                        size = 32.dp, cornerRadius = 10.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.title.ifBlank { p.category },
                            color = c.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            day?.format(planDayFormatter) ?: p.date,
                            color = c.textSecondary, fontSize = 12.sp, fontFamily = BodyFont,
                        )
                    }
                    Text(
                        "$currency${p.amount.roundToInt()}",
                        color = c.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont,
                    )
                }
            }
        }
    }
}

/** Small circular chevron used to step the plan calendar a month at a time. */
@Composable
private fun PlanNavButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(28.dp)
            .pressScale(interaction, pressedScale = 0.88f)
            .clip(CircleShape)
            .background(c.surfaceInset)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.textSecondary, modifier = Modifier.size(15.dp))
    }
}

/**
 * Month grid for [SpendingPlanCard]. A day carrying plans is tinted and labelled
 * with its total; days already logged read green, so "still to come" and "already
 * paid" are tellable apart at a glance. Every day is tappable, including past ones
 * (a plan can be logged late) and future months (plan as far ahead as you like).
 */
@Composable
private fun PlanCalendar(
    month: YearMonth,
    today: LocalDate,
    planned: List<PlannedExpenseEntity>,
    currency: String,
    popRun: Int,
    popCells: Boolean,
    onPickDay: (LocalDate) -> Unit,
) {
    val c = MaterialTheme.efColors
    // Grouped per grid rather than by the caller so a month sliding out of view
    // keeps showing its own amounts all the way off the edge.
    val byDay = remember(planned, month) {
        planned.mapNotNull { p -> parseLedgerDate(p.date)?.let { it to p } }
            .filter { YearMonth.from(it.first) == month }
            .groupBy({ it.first }, { it.second })
    }
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value % 7 // Sunday-based leading blanks
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val rows = (cells.size + 6) / 7

    Column {
        PlanWeekdayHeader()
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            (0 until rows).forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0..6).forEach { col ->
                        val cell = cells.getOrNull(r * 7 + col)
                        if (cell == null) {
                            Box(Modifier.weight(1f).height(44.dp))
                        } else {
                            PlanDayCell(
                                date = cell,
                                plans = byDay[cell].orEmpty(),
                                today = today,
                                currency = currency,
                                modifier = Modifier
                                    .weight(1f)
                                    .popIn(popRun, gridPopDelay(r * 7 + col, 7, rows, base = 40, step = 26), popCells),
                                onPickDay = onPickDay,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The calendar collapsed to the week today falls in — what [SpendingPlanCard]
 * shows in place of the month grid. Folding the whole calendar away would take
 * the next few days with it, which is exactly the stretch a spending plan is
 * about; this keeps that line and drops the other five weeks. Days spill over
 * from the neighbouring month at the turn of a month and stay tappable, so the
 * week is never cut short at the 1st or the 31st.
 */
@Composable
private fun PlanWeekStrip(
    today: LocalDate,
    planned: List<PlannedExpenseEntity>,
    currency: String,
    popRun: Int,
    popCells: Boolean,
    onPickDay: (LocalDate) -> Unit,
) {
    val c = MaterialTheme.efColors
    // Sunday-based, so the columns sit where the month grid's do and the switch
    // between the two reads as a fold rather than a reshuffle.
    val days = remember(today) {
        val start = today.minusDays((today.dayOfWeek.value % 7).toLong())
        (0L..6L).map { start.plusDays(it) }
    }
    val byDay = remember(planned, days) {
        val start = days.first()
        val end = days.last()
        planned.mapNotNull { p -> parseLedgerDate(p.date)?.let { it to p } }
            .filter { !it.first.isBefore(start) && !it.first.isAfter(end) }
            .groupBy({ it.first }, { it.second })
    }

    Column {
        // Stands in for the month strip: no paging here, because the point of the
        // collapsed state is "the week you're in", not another thing to navigate.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("This week", color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
            Text(
                "${days.first().format(planWeekEdgeFormatter)} – ${days.last().format(planWeekEdgeFormatter)}",
                color = c.textMuted, fontSize = 12.sp, fontFamily = BodyFont,
            )
        }
        Spacer(Modifier.height(10.dp))
        PlanWeekdayHeader()
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            days.forEachIndexed { i, day ->
                PlanDayCell(
                    date = day,
                    plans = byDay[day].orEmpty(),
                    today = today,
                    currency = currency,
                    modifier = Modifier
                        .weight(1f)
                        .popIn(popRun, gridPopDelay(i, 7, 1, base = 40, step = 26), popCells),
                    onPickDay = onPickDay,
                )
            }
        }
    }
}

/** The Su–Sa column labels, shared so the week strip and the month grid line up. */
@Composable
private fun PlanWeekdayHeader() {
    val c = MaterialTheme.efColors
    Row {
        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
            Text(
                it, color = c.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).padding(vertical = 2.dp),
            )
        }
    }
}

/**
 * One day in the plan calendar, drawn the same whether it's sitting in the month
 * grid or in the collapsed week strip: tinted and totalled when it carries plans,
 * green once they're all logged, ringed when it's today.
 */
@Composable
private fun PlanDayCell(
    date: LocalDate,
    plans: List<PlannedExpenseEntity>,
    today: LocalDate,
    currency: String,
    modifier: Modifier = Modifier,
    onPickDay: (LocalDate) -> Unit,
) {
    val c = MaterialTheme.efColors
    val total = plans.sumOf { it.amount }
    val allLogged = plans.isNotEmpty() && plans.all { it.loggedTxnId != 0 }
    val isToday = date == today
    val isPast = date.isBefore(today)
    val dayTone = if (allLogged) CategoryTone.GREEN else CategoryTone.BLUE
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (plans.isNotEmpty()) c.tint(dayTone) else Color.Transparent)
            .then(if (isToday) Modifier.border(1.5.dp, c.brand, shape) else Modifier)
            .clickable { onPickDay(date) }
            .padding(top = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${date.dayOfMonth}",
            color = when {
                isToday -> c.brand
                plans.isNotEmpty() -> c.cat(dayTone)
                isPast -> c.textMuted
                else -> c.textPrimary
            },
            fontSize = 13.sp,
            fontWeight = if (isToday || plans.isNotEmpty()) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = NumberFont,
        )
        if (plans.isNotEmpty()) {
            Spacer(Modifier.height(1.dp))
            Text(
                "$currency${compactAmount(total)}",
                color = c.cat(dayTone), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                fontFamily = NumberFont, maxLines = 1,
            )
        }
    }
}

/**
 * One day's plans, plus the form to add another. Editing an existing plan loads it
 * into the same form rather than stacking a second sheet on top of this one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPlanSheet(
    date: LocalDate,
    plans: List<PlannedExpenseEntity>,
    categories: List<String>,
    currency: String,
    onAdd: (title: String, category: String, amount: Double) -> Unit,
    onUpdate: (PlannedExpenseEntity) -> Unit,
    onDelete: (PlannedExpenseEntity) -> Unit,
    onLog: (PlannedExpenseEntity) -> Unit,
    onUnlog: (PlannedExpenseEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Null = the form is adding a new plan; non-null = it's editing that one.
    var editing by remember { mutableStateOf<PlannedExpenseEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "") }
    // Categories arrive asynchronously, and one can be deleted while the sheet is
    // open — either way, fall back to the first available rather than a dead value.
    if (category !in categories) category = categories.firstOrNull() ?: ""

    fun resetForm() {
        editing = null; title = ""; amount = ""; category = categories.firstOrNull() ?: ""
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surfaceCard) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp).padding(bottom = 28.dp).navigationBarsPadding().imePadding(),
        ) {
            Text(date.format(planSheetFormatter), color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
            Spacer(Modifier.height(6.dp))
            Text(
                "Money you've already committed to this day. It's set aside from the monthly budget as soon as you add it, so the daily allowance on your home screen adjusts for it.",
                color = c.textSecondary, fontSize = 13.sp, lineHeight = 19.sp, fontFamily = BodyFont,
            )

            if (plans.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                plans.forEach { p ->
                    val logged = p.loggedTxnId != 0
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(14.dp)).background(c.surfaceInset)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(
                            tone = CategoryVisuals.tone(p.category),
                            icon = CategoryVisuals.icon(p.category),
                            size = 36.dp, cornerRadius = 12.dp,
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.title.ifBlank { p.category },
                                color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (logged) "$currency${p.amount.roundToInt()} · logged as spent" else "$currency${p.amount.roundToInt()} · ${p.category}",
                                color = if (logged) c.cat(CategoryTone.GREEN) else c.textSecondary,
                                fontSize = 12.5.sp, fontFamily = BodyFont,
                            )
                        }
                        // Logged plans can only be reversed or removed; the amount is
                        // a real transaction now, so editing it here would desync them.
                        if (!logged) {
                            PlanRowAction(Lucide.Pencil, "Edit plan") {
                                editing = p; title = p.title; amount = p.amount.roundToInt().toString(); category = p.category
                            }
                            Spacer(Modifier.width(4.dp))
                            PlanRowAction(Lucide.Check, "Mark as spent") { onLog(p) }
                        } else {
                            PlanRowAction(Lucide.Undo2, "Undo logging") { onUnlog(p) }
                        }
                        Spacer(Modifier.width(4.dp))
                        PlanRowAction(Lucide.Trash2, "Delete plan", danger = true) {
                            if (editing?.id == p.id) resetForm()
                            onDelete(p)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (editing != null) "Edit plan" else "Add a planned spend",
                color = c.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
            )
            Spacer(Modifier.height(12.dp))
            EFTextField(
                value = amount,
                onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = "Amount",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(14.dp))
            EFTextField(value = title, onValueChange = { title = it }, label = "What for?", placeholder = "Haircut")
            Spacer(Modifier.height(16.dp))
            Text("Category", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
            Spacer(Modifier.height(10.dp))
            if (categories.isEmpty()) {
                Text("No expense categories yet — add one from the Add Transaction sheet.", color = c.textMuted, fontSize = 13.sp, fontFamily = BodyFont)
            }
            categories.chunked(4).forEach { rowItems ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { cat ->
                        val active = cat == category
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (active) c.brandSoft else c.surfaceInset)
                                .clickable { category = cat }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CategoryIcon(tone = CategoryVisuals.tone(cat), icon = CategoryVisuals.icon(cat), size = 30.dp, cornerRadius = 10.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                cat, color = c.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            val value = amount.toDoubleOrNull() ?: 0.0
            EFButton(
                text = if (editing != null) "Save changes" else "Add to plan",
                onClick = {
                    val current = editing
                    if (current != null) {
                        onUpdate(current.copy(title = title.ifBlank { category }, category = category, amount = value))
                    } else {
                        onAdd(title.ifBlank { category }, category, value)
                    }
                    resetForm()
                },
                fullWidth = true,
                enabled = value > 0.0 && category.isNotBlank(),
            )
            if (editing != null) {
                Spacer(Modifier.height(10.dp))
                EFButton(text = "Cancel", variant = EFButtonVariant.Ghost, onClick = { resetForm() }, fullWidth = true)
            }
        }
    }
}

/** Compact circular icon button used by the plan rows in [DayPlanSheet]. */
@Composable
private fun PlanRowAction(icon: ImageVector, contentDescription: String, danger: Boolean = false, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(30.dp)
            .pressScale(interaction, pressedScale = 0.86f)
            .clip(CircleShape)
            .background(c.surfaceCard)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription = contentDescription,
            tint = if (danger) c.cat(CategoryTone.RED) else c.textSecondary,
            modifier = Modifier.size(15.dp),
        )
    }
}

/**
 * The page-turn used by both halves of the plan calendar's month strip: the
 * outgoing month leaves the way you're heading and the incoming one arrives from
 * the other side, so the label and the grid read as one sheet of paper moving.
 * [distance] is smaller for the label than the grid — near content travelling
 * further than far content is what makes the pair feel layered rather than flat.
 */
private fun monthSlide(forward: Boolean, distance: Int): ContentTransform {
    val dir = if (forward) 1 else -1
    return (
        slideInHorizontally(tween(280, easing = EaseOutCubic)) { dir * distance } +
            fadeIn(tween(200, delayMillis = 40))
        ) togetherWith (
        slideOutHorizontally(tween(240, easing = EaseInCubic)) { -dir * distance } +
            fadeOut(tween(140))
        )
}

private val planMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val planDayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
/** Either end of the collapsed week strip's range label: "31 Aug – 6 Sep". */
private val planWeekEdgeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val planSheetFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

/** "dd/MM/yyyy" — the format transactions and plans are stored in. */
private fun formatLedgerDate(d: LocalDate): String =
    "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)

/** Money that has to fit inside a 40dp calendar cell: 1450 -> "1.5k". */
private fun compactAmount(v: Double): String = when {
    v >= 10_000 -> "${(v / 1000).roundToInt()}k"
    v >= 1_000 -> "%.1fk".format(v / 1000.0)
    else -> v.roundToInt().toString()
}

// ---- Ledger grouping helpers ----

/** A flattened ledger entry: either a day header or a transaction row. */
private sealed interface LedgerEntry {
    val key: String
    data class TxHeader(val label: String, val net: Double, val first: Boolean) : LedgerEntry {
        override val key: String get() = "h_$label"
    }
    data class TxRow(val t: Transaction) : LedgerEntry {
        override val key: String get() = "t_${t.id}"
    }
}

private val FULL_DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())

private fun efFullLabel(d: LocalDate): String = d.format(FULL_DATE_FMT)

/** Parse a stored "dd/MM/yyyy" transaction date into a LocalDate, or null if unparseable. */
private fun parseLedgerDate(s: String): LocalDate? = try {
    val p = s.split("/")
    if (p.size == 3) LocalDate.of(p[2].toInt(), p[1].toInt(), p[0].toInt()) else null
} catch (e: Exception) {
    null
}

/** Currency-prefixed signed money, keeping the app's symbol-before-number convention. */
private fun signedMoney(v: Double, currency: String): String =
    (if (v < 0) "-" else "") + currency + String.format(Locale.US, "%,.2f", abs(v))

/**
 * Filter (pin → type → text → day) then order the ledger for display: newest day
 * first, pinned rows floating to the top within their day. Returns a flat list of
 * transactions in the exact order they're rendered, so callers can window it (e.g.
 * "top 5, then View More"). Mirrors HomeScreen.jsx.
 */
private fun orderedLedgerTx(
    transactions: List<Transaction>,
    query: String,
    typeFilter: String,
    dayFilter: LocalDate?,
    pinnedOnly: Boolean,
    pinnedIds: Set<String>,
): List<Transaction> {
    val q = query.trim()
    val filtered = transactions.filter { t ->
        if (pinnedOnly && t.id !in pinnedIds) return@filter false
        if (typeFilter == "in" && t.isExpense) return@filter false
        if (typeFilter == "out" && !t.isExpense) return@filter false
        if (q.isNotEmpty() && !"${t.title} ${t.category}".contains(q, ignoreCase = true)) return@filter false
        if (dayFilter != null && parseLedgerDate(t.date) != dayFilter) return@filter false
        true
    }
    val byDay = LinkedHashMap<LocalDate?, MutableList<Transaction>>()
    filtered.forEach { byDay.getOrPut(parseLedgerDate(it.date)) { mutableListOf() }.add(it) }
    val sorted = byDay.entries.sortedByDescending { it.key ?: LocalDate.MIN }

    return buildList {
        sorted.forEach { (_, items) ->
            items.sortedByDescending { it.id in pinnedIds }.forEach { add(it) }
        }
    }
}

/**
 * Sum a set of transactions per the "Transactions" display-mode setting: the signed
 * net (income − expense), expenses only (shown as an outflow), or income only.
 */
private fun ledgerSum(items: List<Transaction>, mode: String): Double = when (mode) {
    "income" -> items.filter { !it.isExpense }.sumOf { it.amount }
    "net" -> items.sumOf { if (it.isExpense) -it.amount else it.amount }
    else -> items.filter { it.isExpense }.sumOf { -it.amount } // "expense" (default)
}

/**
 * Group an already-ordered (newest-first) transaction list into day headers + rows.
 * Each day header's total reflects only the rows present in [ordered] and honors
 * [mode], so the running totals stay consistent when the "View More" window reveals
 * just part of a day.
 */
private fun groupLedger(ordered: List<Transaction>, mode: String): List<LedgerEntry> = buildList {
    var firstGroup = true
    var i = 0
    while (i < ordered.size) {
        val date = parseLedgerDate(ordered[i].date)
        val group = mutableListOf<Transaction>()
        while (i < ordered.size && parseLedgerDate(ordered[i].date) == date) {
            group.add(ordered[i]); i++
        }
        val net = ledgerSum(group, mode)
        add(LedgerEntry.TxHeader(date?.let(::efFullLabel) ?: "Undated", net, firstGroup))
        group.forEach { add(LedgerEntry.TxRow(it)) }
        firstGroup = false
    }
}

/** Net (income − expense) for the current week minus the previous week. */
private fun weekOverWeekDelta(transactions: List<Transaction>): Double {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    fun weekStart(offsetWeeks: Int): Calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        add(Calendar.WEEK_OF_YEAR, offsetWeeks)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    val thisWeek = weekStart(0).timeInMillis
    val lastWeek = weekStart(-1).timeInMillis
    val nextWeek = weekStart(1).timeInMillis

    var thisNet = 0.0
    var lastNet = 0.0
    transactions.forEach { t ->
        val time = try { sdf.parse(t.date)?.time } catch (e: Exception) { null } ?: return@forEach
        val net = if (t.isExpense) -t.amount else t.amount
        when {
            time in thisWeek until nextWeek -> thisNet += net
            time in lastWeek until thisWeek -> lastNet += net
        }
    }
    return thisNet - lastNet
}
