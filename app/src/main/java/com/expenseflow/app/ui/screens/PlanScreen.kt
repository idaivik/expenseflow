package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.expenseflow.app.data.BillEntity
import com.expenseflow.app.data.GoalEntity
import com.expenseflow.app.model.BudgetCategory
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.CategoryVisuals
import com.expenseflow.app.ui.components.DayPicker
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFButtonVariant
import com.expenseflow.app.ui.components.EFCard
import com.expenseflow.app.ui.components.EFElevation
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.components.PlanVisuals
import com.expenseflow.app.ui.components.ProgressBar
import com.expenseflow.app.ui.components.SegmentedControl
import com.expenseflow.app.ui.components.pressScale
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.DisplayFont
import com.expenseflow.app.ui.theme.NumberFont
import com.expenseflow.app.ui.theme.Radii
import com.expenseflow.app.ui.theme.Spacing
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private data class PlanDraft(val kind: String, val goal: GoalEntity? = null, val bill: BillEntity? = null)

@Composable
fun PlanScreen(
    viewModel: ExpenseViewModel,
    onOpenBudgetDetail: (String) -> Unit,
    onManageBudgets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.efColors
    val goals by viewModel.goals.collectAsState()
    val budgets by viewModel.savingsBudgets.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val monthlyBudgets by viewModel.budgetCategories.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var draft by remember { mutableStateOf<PlanDraft?>(null) }
    // Bill awaiting the "merge into Bills / create a new category" choice before it's logged.
    var paidPrompt by remember { mutableStateOf<BillEntity?>(null) }
    // Monthly-budget row awaiting a limit edit (hold-menu → Edit).
    var editBudget by remember { mutableStateOf<BudgetCategory?>(null) }
    val billsDue = bills.filter { !it.paid }.sumOf { it.amount }

    fun onBillPaidToggle(b: BillEntity) {
        when {
            b.paid -> viewModel.unmarkBillPaid(b)                     // un-mark: also reverse the logged expense
            "Bills" in expenseCategories -> paidPrompt = b            // a Bills category exists — ask first
            else -> viewModel.markBillPaid(b, "Bills")               // none yet — log straight to Bills
        }
    }

    // Each section gets its own top-level LazyColumn `item {}` so the jump chips
    // below can scroll straight to it; later indices shift with the (variable)
    // length of whichever sections come before them.
    val billsIndex = 0
    val goalsIndex = 1
    val goalsSpan = if (goals.isEmpty()) 1 else goals.size
    val budgetsIndex = goalsIndex + 1 + goalsSpan
    val monthlyIndex = budgetsIndex + 1 + budgets.size

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    fun jumpTo(index: Int) { scope.launch { listState.animateScrollToItem(index) } }

    Column(modifier = modifier.fillMaxWidth().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = Spacing.xl).padding(top = Spacing.sm)) {
            Text("My Plan", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, fontFamily = DisplayFont)
            Text("Where your money is going next", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
            Spacer(Modifier.height(Spacing.lg))
            SectionJumpChips(
                onBills = { jumpTo(billsIndex) },
                onGoals = { jumpTo(goalsIndex) },
                onBudgets = { jumpTo(budgetsIndex) },
                onMonthly = { jumpTo(monthlyIndex) },
            )
        }
        Spacer(Modifier.height(Spacing.sm))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Column(Modifier.padding(horizontal = Spacing.xl)) {
                    PlanSectionHeader("Recurring & Bills", "Manage your monthly obligations") { draft = PlanDraft("bill") }
                    Spacer(Modifier.height(Spacing.md))
                    EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                        if (bills.isEmpty()) {
                            EmptyRow("No bills yet", "Tap + to add a recurring bill.")
                        } else {
                            bills.forEachIndexed { i, b ->
                                BillRow(
                                    bill = b, last = i == bills.lastIndex, currency = currency,
                                    onEdit = { draft = PlanDraft("bill", bill = b) },
                                    onTogglePaid = { onBillPaidToggle(b) },
                                    onDelete = { viewModel.deleteBill(b) },
                                )
                            }
                            // Kept inside the same card as the bills it totals, instead of
                            // floating as bare text below it.
                            Box(Modifier.fillMaxWidth().height(1.dp).background(c.borderSubtle))
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining this month", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
                                Text("$currency${billsDue.roundToInt()}", color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont)
                            }
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = Spacing.xl)) {
                    Spacer(Modifier.height(Spacing.md))
                    PlanSectionHeader("Goals", "Your big savings targets") { draft = PlanDraft("goal") }
                    Spacer(Modifier.height(Spacing.md))
                }
            }
            if (goals.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = Spacing.xl).padding(bottom = 14.dp)) {
                        EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(22.dp)) { EmptyRow("No goals yet", "Tap + to set your first savings target.") }
                    }
                }
            } else {
                items(goals) { g ->
                    Box(Modifier.padding(horizontal = Spacing.xl).padding(bottom = 14.dp)) {
                        GoalCard(g, currency = currency, onEdit = { draft = PlanDraft("goal", goal = g) }, onDelete = { viewModel.deleteGoal(g) })
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = Spacing.xl)) {
                    Spacer(Modifier.height(Spacing.xs))
                    PlanSectionHeader("Budgets", "Smaller savings pots") { draft = PlanDraft("budget") }
                    Spacer(Modifier.height(Spacing.md))
                }
            }
            items(budgets) { b ->
                Box(Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.md)) {
                    BudgetRow(
                        b, currency = currency,
                        onEdit = { draft = PlanDraft("budget", goal = b) },
                        onToggleComplete = { viewModel.setGoalCompleted(b, !b.completed) },
                        onDelete = { viewModel.deleteGoal(b) },
                    )
                }
            }

            item {
                Column(Modifier.padding(horizontal = Spacing.xl)) {
                    Spacer(Modifier.height(Spacing.xs))
                    PlanSectionHeader(
                        "Monthly Budgets", "Tap to see spend",
                        actionLabel = "Manage", actionIcon = Lucide.SlidersHorizontal,
                        onAction = onManageBudgets,
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
            }
            if (monthlyBudgets.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.md)) {
                        EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(22.dp)) {
                            EmptyRow("No monthly budgets", "Tap Add to set a spending limit for a category.")
                        }
                    }
                }
            } else {
                items(monthlyBudgets.sortedByDescending { if (it.total > 0) it.spent / it.total else 0.0 }) { b ->
                    Box(Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.md)) {
                        MonthlyBudgetRow(
                            b, currency = currency,
                            onClick = { onOpenBudgetDetail(b.name) },
                            onEdit = { editBudget = b },
                            onDelete = { viewModel.deleteBudget(b.name) },
                        )
                    }
                }
            }
        }
    }

    draft?.let { d ->
        PlanEditSheet(
            draft = d,
            onDismiss = { draft = null },
            onSaveGoal = { viewModel.saveGoal(it); draft = null },
            onSaveBill = { viewModel.saveBill(it); draft = null },
        )
    }

    paidPrompt?.let { b ->
        BillPaidCategoryDialog(
            bill = b, currency = currency,
            onMerge = { viewModel.markBillPaid(b, "Bills"); paidPrompt = null },
            onCreate = { viewModel.markBillPaid(b, b.name); paidPrompt = null },
            onDismiss = { paidPrompt = null },
        )
    }

    editBudget?.let { b ->
        EditBudgetLimitSheet(
            budget = b, currency = currency,
            onSave = { limit -> viewModel.updateBudget(b.name, limit); editBudget = null },
            onDismiss = { editBudget = null },
        )
    }
}

/** Horizontally-scrolling row of tap-to-jump chips, one per Plan section. */
@Composable
private fun SectionJumpChips(
    onBills: () -> Unit,
    onGoals: () -> Unit,
    onBudgets: () -> Unit,
    onMonthly: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val items = listOf("Bills" to onBills, "Goals" to onGoals, "Budgets" to onBudgets, "Monthly" to onMonthly)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items.forEach { (label, onClick) ->
            Text(
                label, color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.surfaceSunken)
                    .clickable(onClick = onClick)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )
        }
    }
}

@Composable
private fun PlanSectionHeader(
    title: String,
    sub: String,
    actionLabel: String = "Add",
    actionIcon: ImageVector = Lucide.Plus,
    onAction: () -> Unit,
) {
    val c = MaterialTheme.efColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column {
            Text(title, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
            // One step smaller/quieter than the page subtitle so section captions
            // clearly read as subordinate to it, not a repeat of the same rank.
            Text(sub, color = c.textMuted, fontSize = 12.sp, fontFamily = BodyFont)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(Spacing.xs)) {
            Icon(actionIcon, null, tint = c.brand, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(actionLabel, color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
        }
    }
}

@Composable
private fun EmptyRow(title: String, sub: String) {
    val c = MaterialTheme.efColors
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
        Text(sub, color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
    }
}

@Composable
private fun RowMenu(onEdit: () -> Unit, extraLabel: String?, onExtra: (() -> Unit)?, onDelete: () -> Unit) {
    val c = MaterialTheme.efColors
    var open by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable { open = true },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Lucide.EllipsisVertical, "Options", tint = c.textMuted, modifier = Modifier.size(22.dp))
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Edit", fontFamily = BodyFont) }, onClick = { open = false; onEdit() })
            if (extraLabel != null && onExtra != null) {
                DropdownMenuItem(text = { Text(extraLabel, fontFamily = BodyFont) }, onClick = { open = false; onExtra() })
            }
            DropdownMenuItem(text = { Text("Delete", color = c.moneyOut, fontFamily = BodyFont) }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun BillRow(bill: BillEntity, last: Boolean, currency: String, onEdit: () -> Unit, onTogglePaid: () -> Unit, onDelete: () -> Unit) {
    val c = MaterialTheme.efColors
    val tone = CategoryTone.from(bill.tone)
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(tone = tone, icon = PlanVisuals.icon(bill.iconKey), size = 40.dp, cornerRadius = 12.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    bill.name, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont,
                    textDecoration = if (bill.paid) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (bill.paid) "Paid" else "${recurrenceLabel(bill)} · Due ${bill.dueDate.ifBlank { "—" }}",
                    color = if (bill.paid) c.moneyIn else c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text("$currency${bill.amount.roundToInt()}", color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont)
            Spacer(Modifier.width(6.dp))
            RowMenu(onEdit, if (bill.paid) "Mark as unpaid" else "Mark as paid", onTogglePaid, onDelete)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.borderSubtle))
    }
}

@Composable
private fun GoalCard(g: GoalEntity, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val c = MaterialTheme.efColors
    val tone = CategoryTone.from(g.tone)
    val pct = if (g.completed) 100 else if (g.target > 0) min(100, ((g.saved / g.target) * 100).roundToInt()) else 0
    val left = (g.target - g.saved).coerceAtLeast(0.0)
    EFCard(elevation = EFElevation.Card, contentPadding = PaddingValues(Spacing.xl)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(tone = tone, icon = PlanVisuals.icon(g.iconKey), size = 46.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Target · ${g.targetDate.ifBlank { "—" }}", color = c.textSecondary, fontSize = 14.sp, fontFamily = BodyFont)
            }
            RowMenu(onEdit, null, null, onDelete)
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(money(g.saved, currency), color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, fontFamily = NumberFont)
            Spacer(Modifier.width(Spacing.sm))
            Text("of ${money(g.target, currency)}", color = c.textMuted, fontSize = 15.sp, fontFamily = NumberFont)
        }
        Spacer(Modifier.height(Spacing.md))
        ProgressBar(value = pct.toFloat(), tone = tone, height = 10.dp)
        Spacer(Modifier.height(Spacing.md))
        Row(
            // White text over a raw category color can fall well under WCAG AA
            // contrast on lighter tones (green/orange measure ~2.3-2.6:1 unmixed);
            // blending 35% black into the fill keeps the tone but reads clearly.
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(lerp(if (left > 0) c.cat(tone) else c.moneyIn, Color.Black, 0.35f))
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (left > 0) "Save ${money(left, currency)} more to reach your goal." else "You've fully funded ${g.name}!",
                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
            )
        }
    }
}

@Composable
private fun BudgetRow(b: GoalEntity, currency: String, onEdit: () -> Unit, onToggleComplete: () -> Unit, onDelete: () -> Unit) {
    val c = MaterialTheme.efColors
    val tone = CategoryTone.from(b.tone)
    val pct = if (b.completed) 100 else if (b.target > 0) min(100, ((b.saved / b.target) * 100).roundToInt()) else 0
    EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(Spacing.lg)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(tone = if (b.completed) CategoryTone.GREEN else tone, icon = PlanVisuals.icon(b.iconKey), size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(b.name, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(if (b.completed) "Done" else "$pct%", color = if (b.completed) c.moneyIn else c.cat(tone), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont)
                }
                Text("${money(b.saved, currency)} of ${money(b.target, currency)}", color = c.textSecondary, fontSize = 13.sp, fontFamily = NumberFont)
                Spacer(Modifier.height(Spacing.sm))
                ProgressBar(value = pct.toFloat(), tone = if (b.completed) CategoryTone.GREEN else tone, height = 7.dp)
            }
            Spacer(Modifier.width(Spacing.sm))
            RowMenu(onEdit, if (b.completed) "Mark as active" else "Mark as completed", onToggleComplete, onDelete)
        }
    }
}

/**
 * A monthly SPENDING budget row (limit + spent this month). Tapping opens the
 * Budget Detail insight screen; the ⋮ menu (same as Bills/Goals/Budgets rows)
 * reveals Edit / Delete.
 */
@Composable
private fun MonthlyBudgetRow(
    b: BudgetCategory,
    currency: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val tone = CategoryVisuals.tone(b.name)
    val rawPct = if (b.total > 0) ((b.spent / b.total) * 100).roundToInt() else 0
    val over = b.spent - b.total
    val exceeded = over > 0 && b.total > 0
    val near = !exceeded && b.total > 0 && rawPct >= 85
    val barTone = if (exceeded) CategoryTone.RED else if (near) CategoryTone.ORANGE else tone
    val pctColor = if (exceeded) c.catRed else if (near) c.catOrange else c.catGreen

    val interaction = remember { MutableInteractionSource() }

    EFCard(
        modifier = Modifier
            .pressScale(interaction, 0.98f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        elevation = EFElevation.Sm,
        contentPadding = PaddingValues(Spacing.lg),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(tone = tone, icon = CategoryVisuals.icon(b.name), size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        b.name, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    // "Near limit" used to be color-only (orange bar/%, no icon or text) —
                    // add a badge with its own icon shape (not just exceeded's triangle,
                    // recolored) so the warning doesn't rely on hue alone.
                    if (exceeded) {
                        Row(
                            modifier = Modifier.clip(CircleShape).background(c.tintRed).padding(horizontal = Spacing.sm, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(Lucide.TriangleAlert, null, tint = c.catRed, modifier = Modifier.size(11.dp))
                            Text("$currency${over.roundToInt()} over", color = c.catRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont)
                        }
                    } else if (near) {
                        Row(
                            modifier = Modifier.clip(CircleShape).background(c.tintOrange).padding(horizontal = Spacing.sm, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(Lucide.CircleAlert, null, tint = c.catOrange, modifier = Modifier.size(11.dp))
                            Text("Almost at limit", color = c.catOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = if (exceeded) c.catRed else c.textPrimary, fontWeight = FontWeight.Bold)) {
                            append("$currency${b.spent.toInt()}")
                        }
                        append(" of $currency${b.total.toInt()} spent")
                    },
                    color = c.textSecondary, fontSize = 14.sp, fontFamily = NumberFont,
                )
                Spacer(Modifier.height(10.dp))
                ProgressBar(value = rawPct.coerceAtMost(100).toFloat(), tone = barTone, height = 7.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("$rawPct%", color = pctColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowMenu(onEdit, null, null, onDelete)
                    Icon(Lucide.ChevronRight, null, tint = c.textMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Edit sheet for a monthly budget's spending limit (Plan hold-menu → Edit). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetLimitSheet(
    budget: BudgetCategory,
    currency: String,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tone = CategoryVisuals.tone(budget.name)
    var limit by remember { mutableStateOf(budget.total.takeIf { it > 0 }?.toInt()?.toString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surfaceCard) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = Spacing.xxxl).navigationBarsPadding().imePadding()) {
            Text("Edit Budget", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(tone = tone, icon = CategoryVisuals.icon(budget.name), size = 46.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(budget.name, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                    Text("Spent $currency${budget.spent.toInt()} this month", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
                }
            }
            Spacer(Modifier.height(18.dp))
            EFTextField(limit, { limit = it.filter { ch -> ch.isDigit() } }, label = "Monthly limit", placeholder = "0", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(Spacing.xl))
            EFButton(
                text = "Save Changes",
                onClick = { (limit.toDoubleOrNull() ?: 0.0).takeIf { it > 0 }?.let { onSave(it) } },
                fullWidth = true,
                enabled = (limit.toDoubleOrNull() ?: 0.0) > 0.0,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanEditSheet(
    draft: PlanDraft,
    onDismiss: () -> Unit,
    onSaveGoal: (GoalEntity) -> Unit,
    onSaveBill: (BillEntity) -> Unit,
) {
    val c = MaterialTheme.efColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isBill = draft.kind == "bill"
    val isGoal = draft.kind == "goal"
    val presets = if (isBill) PlanVisuals.billCategories else PlanVisuals.goalCategories

    var name by remember { mutableStateOf(draft.goal?.name ?: draft.bill?.name ?: "") }
    var target by remember { mutableStateOf(draft.goal?.target?.takeIf { it > 0 }?.roundToInt()?.toString() ?: "") }
    var saved by remember { mutableStateOf(draft.goal?.saved?.takeIf { it > 0 }?.roundToInt()?.toString() ?: "") }
    var amount by remember { mutableStateOf(draft.bill?.amount?.takeIf { it > 0 }?.roundToInt()?.toString() ?: "") }
    var date by remember { mutableStateOf(draft.goal?.targetDate ?: draft.bill?.dueDate ?: "") }
    // Goals pick their target date with the app's glassmorphism day calendar; it's
    // held as a LocalDate here and only formatted back to a string on save.
    var goalDate by remember { mutableStateOf(parseGoalLocalDate(draft.goal?.targetDate ?: "")) }
    var recurrenceType by remember { mutableStateOf(draft.bill?.recurrenceType ?: "monthly") }
    var intervalCount by remember { mutableStateOf(draft.bill?.intervalCount?.toString() ?: "1") }
    var intervalUnit by remember { mutableStateOf(draft.bill?.intervalUnit ?: "month") }
    val initialKey = draft.goal?.iconKey ?: draft.bill?.iconKey ?: presets.first().key
    var selectedKey by remember { mutableStateOf(initialKey) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surfaceCard) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.lg)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                when {
                    draft.goal != null || draft.bill != null -> if (isGoal) "Edit Goal" else if (isBill) "Edit Bill" else "Edit Budget"
                    isGoal -> "New Goal"; isBill -> "New Bill"; else -> "New Budget"
                },
                color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont,
            )
            Spacer(Modifier.height(18.dp))
            EFTextField(name, { name = it }, label = if (isBill) "Bill name" else if (isGoal) "Goal name" else "Budget name", placeholder = "e.g. Save for a Car")
            Spacer(Modifier.height(Spacing.lg))
            if (isBill) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    EFTextField(amount, { amount = it }, label = "Amount", placeholder = "48", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    EFTextField(date, { date = it }, label = "Due date", placeholder = "Nov 12", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(Spacing.lg))
                Text("Repeats", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
                Spacer(Modifier.height(Spacing.sm))
                SegmentedControl(
                    options = listOf("Monthly" to "monthly", "Custom" to "custom"),
                    selected = recurrenceType, onSelect = { recurrenceType = it },
                )
                if (recurrenceType == "custom") {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Bottom) {
                        EFTextField(
                            intervalCount,
                            { intervalCount = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = "Every", placeholder = "2", keyboardType = KeyboardType.Number,
                            modifier = Modifier.width(96.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Unit", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
                            Spacer(Modifier.height(Spacing.sm))
                            SegmentedControl(
                                options = listOf("Day" to "day", "Week" to "week", "Month" to "month"),
                                selected = intervalUnit, onSelect = { intervalUnit = it },
                            )
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    EFTextField(target, { target = it }, label = "Target amount", placeholder = "7,500", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    EFTextField(saved, { saved = it }, label = "Saved so far", placeholder = "0", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
                if (isGoal) {
                    Spacer(Modifier.height(Spacing.lg))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Target date", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                goalDate?.let(::formatGoalLocalDate) ?: "Optional",
                                color = if (goalDate != null) c.textPrimary else c.textMuted,
                                fontSize = 15.sp, fontFamily = BodyFont,
                            )
                        }
                        DayPicker(
                            value = goalDate,
                            onChange = { goalDate = it },
                            initialMonth = goalDate ?: LocalDate.now(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Category", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
            Spacer(Modifier.height(10.dp))
            presets.chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { p ->
                        val active = p.key == selectedKey
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (active) c.brandSoft else c.surfaceInset)
                                .clickable { selectedKey = p.key }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CategoryIcon(tone = p.tone, icon = PlanVisuals.icon(p.key), size = 40.dp, cornerRadius = 12.dp)
                            Spacer(Modifier.height(7.dp))
                            Text(p.key, color = c.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
                        }
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            EFButton(
                text = if (draft.goal != null || draft.bill != null) "Save Changes" else "Create",
                onClick = {
                    val tone = presets.first { it.key == selectedKey }.tone
                    if (isBill) {
                        onSaveBill(
                            (draft.bill ?: BillEntity(name = "", iconKey = selectedKey, tone = tone.name.lowercase(), amount = 0.0)).copy(
                                name = name.ifBlank { "New bill" }, iconKey = selectedKey, tone = tone.name.lowercase(),
                                amount = amount.toDoubleOrNull() ?: 0.0, dueDate = date,
                                recurrenceType = recurrenceType,
                                intervalCount = if (recurrenceType == "custom") (intervalCount.toIntOrNull()?.coerceAtLeast(1) ?: 1) else 1,
                                intervalUnit = if (recurrenceType == "custom") intervalUnit else "month",
                            ),
                        )
                    } else {
                        onSaveGoal(
                            (draft.goal ?: GoalEntity(kind = draft.kind, name = "", iconKey = selectedKey, tone = tone.name.lowercase(), saved = 0.0, target = 0.0)).copy(
                                kind = draft.kind, name = name.ifBlank { if (isGoal) "New goal" else "New budget" }, iconKey = selectedKey,
                                tone = tone.name.lowercase(), saved = saved.toDoubleOrNull() ?: 0.0,
                                target = (target.toDoubleOrNull() ?: 0.0).coerceAtLeast(1.0),
                                targetDate = if (isGoal) goalDate?.let(::formatGoalLocalDate) ?: "" else date,
                            ),
                        )
                    }
                },
                fullWidth = true,
            )
        }
    }

}

/** Display/stored format for a goal's target date, e.g. "Dec 12, 2026". */
private val GOAL_DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

/** Format the calendar's picked date into the display string stored on the goal. */
private fun formatGoalLocalDate(date: LocalDate): String = date.format(GOAL_DATE_FMT)

/** Parse a stored goal target date into a LocalDate, tolerating the older "MMM yyyy" / "MMM d" forms. */
private fun parseGoalLocalDate(value: String): LocalDate? {
    if (value.isBlank()) return null
    for (pattern in listOf("MMM d, yyyy", "MMM yyyy", "MMM d")) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val parsed = sdf.parse(value) ?: continue
            return java.time.Instant.ofEpochMilli(parsed.time).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        } catch (_: Exception) {
        }
    }
    return null
}

/** Human-readable cadence for a bill row, e.g. "Monthly", "Every week", "Every 2 weeks". */
private fun recurrenceLabel(bill: BillEntity): String {
    if (bill.recurrenceType != "custom") return "Monthly"
    val n = bill.intervalCount.coerceAtLeast(1)
    return if (n == 1) "Every ${bill.intervalUnit}" else "Every $n ${bill.intervalUnit}s"
}

/**
 * Asked when a bill is marked paid and a "Bills" category already exists: log this
 * payment into that shared category, or split it out into its own bill-named one.
 */
@Composable
private fun BillPaidCategoryDialog(
    bill: BillEntity,
    currency: String,
    onMerge: () -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.efColors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.surfaceCard)
                .padding(Spacing.xxl),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Log to Report", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                // A third full-width "Cancel" button below two real choices just adds a
                // stacked, decreasing-weight button anti-pattern — a small close affordance
                // here (plus the dialog's own tap-outside/back dismiss) covers the same need.
                // 44dp tap target to match RowMenu's, even though the glyph stays small.
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Lucide.X, "Close", tint = c.textMuted, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "A \"Bills\" category already exists. Add this ${money(bill.amount, currency)} payment for ${bill.name} to it, or record it as its own category?",
                color = c.textSecondary, fontSize = 14.sp, fontFamily = BodyFont,
            )
            Spacer(Modifier.height(Spacing.xl))
            EFButton(text = "Merge into Bills", onClick = onMerge, fullWidth = true)
            Spacer(Modifier.height(10.dp))
            EFButton(text = "Create \"${bill.name}\"", onClick = onCreate, variant = EFButtonVariant.Secondary, fullWidth = true)
        }
    }
}

private fun money(v: Double, symbol: String): String = symbol + String.format(Locale.US, "%,.2f", v)
