package com.expenseflow.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.ChartColumnBig
import com.composables.icons.lucide.ChartPie
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.PinOff
import com.composables.icons.lucide.Trash2
import com.expenseflow.app.ui.components.Amount
import com.expenseflow.app.ui.components.Badge
import com.expenseflow.app.ui.components.BadgeTone
import com.expenseflow.app.ui.components.BarChart
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.CategoryVisuals
import com.expenseflow.app.ui.components.ChartSegment
import com.expenseflow.app.ui.components.DonutChart
import com.expenseflow.app.ui.components.EFCard
import com.expenseflow.app.ui.components.EFElevation
import com.expenseflow.app.ui.components.PeriodPicker
import com.expenseflow.app.ui.components.ProgressBar
import com.expenseflow.app.ui.components.ReportPeriod
import com.expenseflow.app.ui.components.SegmentedControl
import com.expenseflow.app.ui.components.periodWeekStart
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.DisplayFont
import com.expenseflow.app.ui.theme.NumberFont
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.largeShadow
import com.expenseflow.app.ui.theme.smallShadow
import com.expenseflow.app.viewmodel.ExpenseViewModel
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.composables.icons.lucide.Check
import com.expenseflow.app.ui.components.EDIT_ICONS
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFButtonVariant
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.components.ICON_BY_KEY
import com.expenseflow.app.ui.components.IconPickerCell
import com.expenseflow.app.ui.components.KEY_BY_ICON
import com.expenseflow.app.ui.theme.Radii
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val TX_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun parseTxDate(s: String): LocalDate? = try { LocalDate.parse(s, TX_DATE) } catch (e: Exception) { null }

private fun inPeriod(d: LocalDate, period: ReportPeriod, sel: LocalDate): Boolean = when (period) {
    ReportPeriod.DAY -> d == sel
    ReportPeriod.WEEK -> { val s = periodWeekStart(sel); d >= s && d <= s.plusDays(6) }
    ReportPeriod.MONTH -> d.year == sel.year && d.month == sel.month
    ReportPeriod.YEAR -> d.year == sel.year
}

private fun previousRef(period: ReportPeriod, sel: LocalDate): LocalDate = when (period) {
    ReportPeriod.DAY -> sel.minusDays(1)
    ReportPeriod.WEEK -> sel.minusWeeks(1)
    ReportPeriod.MONTH -> sel.minusMonths(1)
    ReportPeriod.YEAR -> sel.minusYears(1)
}

private fun vsLabel(period: ReportPeriod): String = when (period) {
    ReportPeriod.DAY -> "vs prev day"
    ReportPeriod.WEEK -> "vs last week"
    ReportPeriod.MONTH -> "vs last month"
    ReportPeriod.YEAR -> "vs last year"
}

private data class ReportCat(
    val id: String,           // original category — the grouping key / stable identity
    val name: String,         // display name (may be overridden via Edit)
    val icon: ImageVector,    // display icon (may be overridden via Edit)
    val tone: CategoryTone,
    val amount: Double,
    val pct: Double,          // % of total
    val progress: Float,      // bar fill, relative to the largest category
    val delta: String,        // "+12%"
    val down: Boolean,
    val pinned: Boolean,
)

@Composable
fun ReportScreen(viewModel: ExpenseViewModel, modifier: Modifier = Modifier) {
    val c = MaterialTheme.efColors
    val transactions by viewModel.transactions.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var seg by remember { mutableStateOf("exp") }
    var chart by remember { mutableStateOf("pie") }
    var period by remember { mutableStateOf(ReportPeriod.MONTH) }
    var selDate by remember { mutableStateOf(LocalDate.now()) }
    val isInc = seg == "inc"

    // Long-press menu + pin/delete stay as per-segment screen state (design parity).
    var menuFor by remember { mutableStateOf<String?>(null) }
    var pinned by remember { mutableStateOf(setOf<String>()) }
    var deleted by remember { mutableStateOf(setOf<String>()) }
    var editing by remember { mutableStateOf<String?>(null) }          // category id being edited
    // Category edits (rename / icon / amount) are persisted to the DB, keyed by category name.
    val categoryMeta by viewModel.categoryMeta.collectAsState()
    fun key(name: String) = "$seg:$name"

    // ---- Aggregate the selected period (and the previous one, for deltas) ----
    val prevRef = previousRef(period, selDate)
    val current = transactions.filter { t ->
        t.isExpense == !isInc && parseTxDate(t.date)?.let { inPeriod(it, period, selDate) } == true
    }
    val previous = transactions.filter { t ->
        t.isExpense == !isInc && parseTxDate(t.date)?.let { inPeriod(it, period, prevRef) } == true
    }
    val rawByCat = current.groupBy { it.category }
        .map { (cat, list) -> cat to list.sumOf { it.amount } }
        .filter { key(it.first) !in deleted }
    val rawMap = rawByCat.toMap()   // pre-override sums — used to detect real amount changes
    val byCat = rawByCat
        .map { (cat, amt) -> cat to (categoryMeta[cat]?.amountOverride ?: amt) }   // apply persisted amount override
        .sortedByDescending { it.second }
    val prevByCat = previous.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val total = byCat.sumOf { it.second }
    val maxAmount = byCat.maxOfOrNull { it.second } ?: 1.0

    val cats = byCat.map { (id, amount) ->
        val meta = categoryMeta[id]
        val prev = prevByCat[id] ?: 0.0
        val deltaPct = when {
            prev > 0 -> ((amount - prev) / prev * 100).roundToInt()
            amount > 0 -> 100
            else -> 0
        }
        ReportCat(
            id = id,
            name = meta?.displayName ?: id,
            icon = meta?.iconKey?.let { ICON_BY_KEY[it] } ?: CategoryVisuals.icon(id),
            tone = CategoryVisuals.tone(id),
            amount = amount,
            pct = if (total > 0) amount / total * 100 else 0.0,
            progress = (amount / maxAmount * 100).toFloat(),
            delta = "${if (deltaPct >= 0) "+" else ""}$deltaPct%",
            down = deltaPct < 0,
            pinned = key(id) in pinned,
        )
    }.sortedByDescending { it.pinned }

    val segments = cats.map { ChartSegment(it.name, it.amount, it.tone) }

    Box(modifier = modifier.fillMaxSize()) {
    // The whole report frosts over while the edit surface is open (Liquid Glass).
    val bgBlur by animateDpAsState(if (editing != null) 20.dp else 0.dp, tween(240), label = "bgBlur")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .then(if (bgBlur > 0.dp) Modifier.blur(bgBlur) else Modifier),
    ) {
        // ---- Header: title + liquid-morph period picker ----
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Report", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                Spacer(Modifier.height(1.dp))
                Text("Where your money went", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
            }
            PeriodPicker(date = selDate, period = period, onChange = { p, d -> period = p; selDate = d })
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            SegmentedControl(
                options = listOf("Expenses" to "exp", "Income" to "inc"),
                selected = seg, onSelect = { seg = it; menuFor = null; editing = null },
            )

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isInc) "Income Report" else "Expenses Report",
                    color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont,
                )
                Row(
                    modifier = Modifier.smallShadow(999.dp).clip(CircleShape).background(c.surfaceCard).padding(4.dp),
                ) {
                    ChartToggle(Lucide.ChartColumnBig, chart == "bar") { chart = "bar" }
                    ChartToggle(Lucide.ChartPie, chart == "pie") { chart = "pie" }
                }
            }
            Spacer(Modifier.height(10.dp))

            // ---- Chart: donut or bars ----
            if (segments.isEmpty()) {
                Text(
                    if (isInc) "No income recorded for this period." else "No expenses recorded for this period.",
                    color = c.textMuted, fontSize = 14.sp, fontFamily = BodyFont,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), textAlign = TextAlign.Center,
                )
            } else if (chart == "pie") {
                Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    DonutChart(segments = segments, size = 308.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isInc) "Total Income" else "Total Expenses", color = c.textSecondary, fontSize = 14.sp, fontFamily = BodyFont)
                            Spacer(Modifier.height(4.dp))
                            Amount(total, 28.sp, c.textPrimary, currencySymbol = currency)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    // Legend — maps each arc color to its category
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        cats.chunked(2).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                rowItems.forEach { cat ->
                                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(c.cat(cat.tone)))
                                        Text(
                                            cat.name, color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            formatPct(cat.pct), color = c.textPrimary, fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold, fontFamily = NumberFont,
                                        )
                                    }
                                }
                                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isInc) "Total Income" else "Total Expenses", color = c.textSecondary, fontSize = 14.sp, fontFamily = BodyFont)
                    Spacer(Modifier.height(4.dp))
                    Amount(total, 28.sp, c.textPrimary, currencySymbol = currency)
                    Spacer(Modifier.height(18.dp))
                    BarChart(
                        segments = segments.take(6).map { it.copy(label = it.label.split(" ").first()) },
                        height = 180.dp,
                        formatValue = { v ->
                            if (v >= 1000) "$currency${String.format(Locale.US, "%.1fk", v / 1000)}"
                            else "$currency${v.toInt()}"
                        },
                    )
                }
            }

            // ---- List header ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(if (isInc) "All Income" else "All Expenses", color = c.textSecondary, fontSize = 15.sp, fontFamily = BodyFont)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Total ", color = c.textSecondary, fontSize = 14.sp, fontFamily = BodyFont)
                    Text(
                        "$currency${String.format(Locale.US, "%,.2f", total)}",
                        color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val updatedAt = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())) }
            Text("Updated at $updatedAt today", color = c.textSecondary, fontSize = 12.sp, fontFamily = BodyFont)
            Spacer(Modifier.height(14.dp))

            // ---- Category cards (long-press for Edit · Pin · Delete) ----
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                cats.forEach { cat ->
                    CategoryReportCard(
                        cat = cat,
                        currency = currency,
                        vsLabel = vsLabel(period),
                        menuOpen = menuFor == cat.id,
                        onOpenMenu = { menuFor = cat.id },
                        onCloseMenu = { menuFor = null },
                        onEdit = { editing = cat.id; menuFor = null },
                        onPin = {
                            pinned = if (key(cat.id) in pinned) pinned - key(cat.id) else pinned + key(cat.id)
                            menuFor = null
                        },
                        onDelete = { deleted = deleted + key(cat.id); menuFor = null },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

        // ---- Liquid-glass edit overlay (raises the tile, frosts the report behind) ----
        val editCat = cats.firstOrNull { it.id == editing }
        LaunchedEffect(editing, editCat == null) {
            if (editing != null && editCat == null) editing = null   // category vanished — close
        }
        if (editCat != null) {
            CategoryEditOverlay(
                cat = editCat,
                currency = currency,
                onDismiss = { editing = null },
                onSave = { newName, newIcon, newAmount ->
                    val id = editCat.id
                    val rawSum = rawMap[id] ?: 0.0
                    // Store null for anything left at its default so the category
                    // stays "live" (real name, default glyph, summed amount).
                    val displayName = newName.takeIf { it.isNotBlank() && it != id }
                    val defaultIconKey = KEY_BY_ICON[CategoryVisuals.icon(id)]
                    val iconKey = KEY_BY_ICON[newIcon]?.takeIf { it != defaultIconKey }
                    val amountOverride = newAmount.takeIf { kotlin.math.abs(it - rawSum) >= 0.005 }
                    viewModel.saveCategoryEdit(id, displayName, iconKey, amountOverride)
                    editing = null
                },
            )
        }
    }
}

private fun formatPct(pct: Double): String {
    val rounded = (pct * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()}%" else "$rounded%"
}

@Composable
private fun ChartToggle(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    Box(
        modifier = Modifier.size(width = 38.dp, height = 34.dp).clip(CircleShape)
            .background(if (active) c.brandSoft else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (active) c.brand else c.textMuted, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun CategoryReportCard(
    cat: ReportCat,
    currency: String,
    vsLabel: String,
    menuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onCloseMenu: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MaterialTheme.efColors
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "catPress")

    Box {
        Box(
            Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .pointerInput(cat.id) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onLongPress = { pressed = false; onOpenMenu() },
                    )
                },
        ) {
            EFCard(
                elevation = EFElevation.Sm,
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(20.dp),
                modifier = if (cat.pinned) Modifier.border(1.5.dp, c.brand, RoundedCornerShape(28.dp)) else Modifier,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    CategoryIcon(tone = cat.tone, icon = cat.icon, size = 40.dp, cornerRadius = 12.dp)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                cat.name, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            if (cat.pinned) Icon(Lucide.Pin, "Pinned", tint = c.brand, modifier = Modifier.size(13.dp))
                        }
                        Text(formatPct(cat.pct) + " of total", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "$currency${String.format(Locale.US, "%,.2f", cat.amount)}",
                            color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont,
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Badge(cat.delta, if (cat.down) BadgeTone.Negative else BadgeTone.Positive)
                            Text(vsLabel, color = c.textMuted, fontSize = 12.sp, fontFamily = BodyFont)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ProgressBar(value = cat.progress, tone = cat.tone, height = 8.dp)
            }
        }

        // ---- Long-press popover: Edit · Pin/Unpin · Delete ----
        if (menuOpen) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopEnd,
                offset = with(density) { IntOffset(-14.dp.roundToPx(), 14.dp.roundToPx()) },
                onDismissRequest = onCloseMenu,
                properties = PopupProperties(focusable = true),
            ) {
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                val menuScale by animateFloatAsState(
                    if (shown) 1f else 0.86f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "menuScale",
                )
                val menuAlpha by animateFloatAsState(if (shown) 1f else 0f, label = "menuAlpha")
                Column(
                    modifier = Modifier
                        .widthIn(min = 168.dp)
                        .width(IntrinsicSize.Max)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(1f, 0f)
                            scaleX = menuScale; scaleY = menuScale; alpha = menuAlpha
                        }
                        .largeShadow(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surfaceCard)
                        .border(1.dp, c.borderSubtle, RoundedCornerShape(16.dp))
                        .padding(6.dp),
                ) {
                    MenuItem(Lucide.Pencil, "Edit", danger = false, onClick = onEdit)
                    MenuItem(
                        if (cat.pinned) Lucide.PinOff else Lucide.Pin,
                        if (cat.pinned) "Unpin" else "Pin",
                        danger = false, onClick = onPin,
                    )
                    MenuItem(Lucide.Trash2, "Delete", danger = true, onClick = onDelete)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, danger: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(icon, null, tint = if (danger) c.catRed else c.textSecondary, modifier = Modifier.size(17.dp))
        Text(
            label, color = if (danger) c.catRed else c.textPrimary,
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
        )
    }
}

/**
 * The category edit surface. Rather than a flat dialog, the report behind is
 * blurred (Liquid Glass) and a frosted translucent panel springs up carrying a
 * live preview of the tile being edited — name, icon and amount update as you type.
 * All changes are report-local overrides (see [ReportScreen]); the DB is untouched.
 */
@Composable
private fun CategoryEditOverlay(
    cat: ReportCat,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: ImageVector, amount: Double) -> Unit,
) {
    val c = MaterialTheme.efColors
    var name by remember(cat.id) { mutableStateOf(cat.name) }
    var icon by remember(cat.id) { mutableStateOf(cat.icon) }
    var amountText by remember(cat.id) { mutableStateOf(amountToInput(cat.amount)) }
    val amount = amountText.toDoubleOrNull() ?: cat.amount

    // Spring the panel up ("raise the tile") and fade the frosted scrim in.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val panelScale by animateFloatAsState(
        if (shown) 1f else 0.9f,
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "panelScale",
    )
    val riseY by animateDpAsState(
        if (shown) 0.dp else 44.dp,
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "panelRise",
    )
    val panelAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(200), label = "panelAlpha")
    val scrimAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(220), label = "scrimAlpha")

    Box(Modifier.fillMaxSize()) {
        // Frosted scrim — tap anywhere outside to dismiss without saving.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha }
                .background(if (c.isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF1E193C).copy(alpha = 0.30f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 44.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = panelScale; scaleY = panelScale
                        alpha = panelAlpha
                        translationY = riseY.toPx()
                    }
                    .fillMaxWidth()
                    .largeShadow(30.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(c.surfaceCard.copy(alpha = if (c.isDark) 0.92f else 0.94f))
                    .border(
                        1.dp,
                        if (c.isDark) c.borderDefault else Color.White.copy(alpha = 0.7f),
                        RoundedCornerShape(30.dp),
                    )
                    // Swallow taps so touching the panel doesn't fall through to the scrim.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .padding(22.dp),
            ) {
                Text("Edit Category", color = c.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                Spacer(Modifier.height(3.dp))
                Text("Changes apply to this report", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
                Spacer(Modifier.height(18.dp))

                // ---- Raised, live preview of the tile being edited ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .largeShadow(22.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceCard)
                        .border(1.dp, c.borderSubtle, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    CategoryIcon(tone = cat.tone, icon = icon, size = 46.dp, cornerRadius = 14.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            name.ifBlank { cat.id }, color = c.textPrimary, fontSize = 17.sp,
                            fontWeight = FontWeight.Bold, fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(formatPct(cat.pct) + " of total", color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
                    }
                    Text(
                        "$currency${String.format(Locale.US, "%,.2f", amount)}",
                        color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont,
                    )
                }
                Spacer(Modifier.height(20.dp))

                EFTextField(value = name, onValueChange = { name = it }, label = "Name", placeholder = cat.id)
                Spacer(Modifier.height(14.dp))
                AmountField(value = amountText, currency = currency, onValueChange = { amountText = sanitizeAmount(it) })
                Spacer(Modifier.height(16.dp))

                Text("Icon", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EDIT_ICONS.chunked(6).forEach { rowIcons ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowIcons.forEach { ic ->
                                IconPickerCell(
                                    icon = ic, tone = cat.tone, selected = ic == icon,
                                    modifier = Modifier.weight(1f), onClick = { icon = ic },
                                )
                            }
                            repeat(6 - rowIcons.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EFButton(text = "Cancel", onClick = onDismiss, variant = EFButtonVariant.Secondary, modifier = Modifier.weight(1f))
                    EFButton(
                        text = "Save", onClick = { onSave(name.trim(), icon, amount) },
                        variant = EFButtonVariant.Primary, leadingIcon = Lucide.Check, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Prominent currency-prefixed amount input (the edit's headline field). */
@Composable
private fun AmountField(value: String, currency: String, onValueChange: (String) -> Unit) {
    val c = MaterialTheme.efColors
    Column {
        Text("Amount", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(Radii.sm))
                .background(c.surfaceInset)
                .border(1.dp, c.borderDefault, RoundedCornerShape(Radii.sm))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(currency, color = c.textSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text("0.00", color = c.textMuted, fontSize = 18.sp, fontFamily = NumberFont)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont),
                    cursorBrush = SolidColor(c.brand),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun amountToInput(a: Double): String = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()

/** Keep only digits and a single decimal point. */
private fun sanitizeAmount(s: String): String {
    val filtered = s.filter { it.isDigit() || it == '.' }
    val dot = filtered.indexOf('.')
    return if (dot == -1) filtered
    else filtered.substring(0, dot + 1) + filtered.substring(dot + 1).replace(".", "")
}
