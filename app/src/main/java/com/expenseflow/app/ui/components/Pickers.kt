package com.expenseflow.app.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.*
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.DisplayFont
import com.expenseflow.app.ui.theme.NumberFont
import com.expenseflow.app.ui.theme.cardShadow
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.smallShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.hypot

/* ============================================================
 * Liquid "dynamic island" morph — shared machinery for the
 * Month / Period pickers (MonthPicker.jsx + PeriodPicker.jsx).
 * A compact pill metaball-separates into a popover card via a
 * blur + alpha-threshold "goo" filter and a spring wobble.
 * ============================================================ */

private val EaseOutCubic = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
private val EaseIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)

/** Blur + alpha-threshold chain — the Compose equivalent of the SVG goo filter. */
private fun gooRenderEffect(blurPx: Float): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val blur = android.graphics.RenderEffect.createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.DECAL)
    val matrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 22f, -11f * 255f,
        ),
    )
    return android.graphics.RenderEffect
        .createColorFilterEffect(ColorMatrixColorFilter(matrix), blur)
        .asComposeRenderEffect()
}

/** Card blob wobble: scaleX/scaleY keyframes ported from `mpCardIn` / `mpCardOut`. */
@Composable
private fun rememberWobble(open: Boolean): Pair<Animatable<Float, *>, Animatable<Float, *>> {
    val sx = remember { Animatable(0.62f) }
    val sy = remember { Animatable(0.03f) }
    LaunchedEffect(open) {
        if (open) {
            launch {
                sx.animateTo(
                    1f,
                    keyframes {
                        durationMillis = 720
                        0.62f at 0
                        1.05f at 230 using EaseOutCubic
                        0.98f at 360
                        1.015f at 475
                        0.995f at 576
                        1.004f at 655
                        1f at 720
                    },
                )
            }
            launch {
                sy.animateTo(
                    1f,
                    keyframes {
                        durationMillis = 720
                        0.03f at 0
                        1.09f at 230 using EaseOutCubic
                        0.95f at 360
                        1.035f at 475
                        0.985f at 576
                        1.008f at 655
                        1f at 720
                    },
                )
            }
        } else {
            launch { sx.animateTo(0.62f, tween(300, easing = EaseIn)) }
            launch { sy.animateTo(0.03f, tween(300, easing = EaseIn)) }
        }
    }
    return sx to sy
}

/**
 * Center-outward pop for grid cells (`mpPop` / `ppPop`): scale 0 → 1.18 → 0.95 → 1.
 * The pop only plays while [animate] is true (the open transition). When a cell
 * mounts with [animate] false — e.g. a grid swapped in by a Day/Week/Month/Year
 * tab change or a page nav after the calendar is already open — it appears at
 * rest, so the intro animation isn't replayed on every interaction.
 *
 * Shared with the home screen's spending-plan calendar so every calendar in the
 * app opens with the same motion; bump [runId] to replay it.
 */
@Composable
internal fun Modifier.popIn(runId: Int, delayMs: Int, animate: Boolean = true): Modifier {
    val scale = remember(runId) { Animatable(if (animate) 0f else 1f) }
    val alpha = remember(runId) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(runId) {
        if (!animate) return@LaunchedEffect
        delay(delayMs.toLong())
        launch { alpha.animateTo(1f, tween(140)) }
        scale.animateTo(
            1f,
            keyframes {
                durationMillis = 480
                0f at 0
                1.18f at 298 using EaseOutCubic
                0.95f at 394
                1f at 480
            },
        )
    }
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }
}

/** Stagger delay for [popIn], rippling outward from the centre of a [cols] x [rows] grid. */
internal fun gridPopDelay(index: Int, cols: Int, rows: Int, base: Int = 205, step: Int = 42): Int {
    val row = index / cols
    val col = index % cols
    val dist = hypot(row - (rows - 1) / 2f, col - (cols - 1) / 2f)
    return (base + dist * step).toInt()
}

/**
 * The morph scaffold: renders the goo layer (pill blob + wobbling card blob)
 * and the card content inside a dismissable Popup anchored to the pill.
 */
@Composable
private fun LiquidPopover(
    open: Boolean,
    onDismiss: () -> Unit,
    pillWidth: Dp,
    pillHeight: Dp,
    cardWidth: Dp,
    cardHeight: Dp,
    alignEnd: Boolean, // true = right-edge aligned to the pill, false = centered
    blobColor: Color,
    blobAlpha: Float,
    cardBackground: Color,
    cardBorder: Color,
    onPillTap: () -> Unit,
    pillGhost: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val gap = 12.dp
    val layerHeight = pillHeight + gap + cardHeight
    val density = LocalDensity.current
    val offsetX = with(density) { (if (alignEnd) pillWidth - cardWidth else (pillWidth - cardWidth) / 2).roundToPx() }
    val goo = remember(density) { gooRenderEffect(with(density) { 8.dp.toPx() }) }
    val (sx, sy) = rememberWobble(open)
    val pillBlobAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(open) {
        launch { pillBlobAlpha.animateTo(if (open) 1f else 0f, tween(if (open) 60 else 260, easing = EaseOutCubic)) }
        launch { contentAlpha.animateTo(if (open) 1f else 0f, tween(if (open) 120 else 140, easing = EaseOutCubic)) }
    }

    Popup(
        onDismissRequest = onDismiss,
        offset = IntOffset(offsetX, 0),
        properties = PopupProperties(focusable = true, clippingEnabled = false),
    ) {
        Box(Modifier.size(cardWidth, layerHeight)) {
            // --- Liquid material layer (behind content) ---
            Box(
                Modifier
                    .size(cardWidth, layerHeight)
                    .alpha(blobAlpha)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        if (goo != null) renderEffect = goo
                    },
            ) {
                // pill blob (the "island")
                Box(
                    Modifier
                        .align(if (alignEnd) Alignment.TopEnd else Alignment.TopCenter)
                        .size(pillWidth, pillHeight)
                        .alpha(pillBlobAlpha.value)
                        .clip(CircleShape)
                        .background(blobColor),
                )
                // card blob — springs downward out of the pill
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(y = pillHeight + gap)
                        .size(cardWidth, cardHeight)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            scaleX = sx.value
                            scaleY = sy.value
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(blobColor),
                )
            }

            // --- Pill region: ghost label that shrinks/fades; tap toggles ---
            Box(
                Modifier
                    .align(if (alignEnd) Alignment.TopEnd else Alignment.TopCenter)
                    .size(pillWidth, pillHeight)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPillTap,
                    ),
                contentAlignment = Alignment.Center,
            ) { pillGhost() }

            // --- Card content ---
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(y = pillHeight + gap)
                    .size(cardWidth, cardHeight)
                    .alpha(contentAlpha.value)
                    .cardShadow(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBackground)
                    .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp),
            ) { content() }
        }
    }
}

/* ============================================================
 * MonthPicker — ui_kits/mobile/MonthPicker.jsx
 * Hero theme: frosted pill over the gradient. Light theme:
 * white card pill. Expands to a year-nav + 3×4 month grid.
 * ============================================================ */

enum class PickerTheme { Hero, Light }

@Composable
fun MonthPicker(
    date: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    theme: PickerTheme = PickerTheme.Hero,
) {
    val c = MaterialTheme.efColors
    val hero = theme == PickerTheme.Hero
    var open by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var runId by remember { mutableIntStateOf(0) }
    var viewYear by remember { mutableIntStateOf(date.year) }

    fun doOpen() {
        viewYear = date.year
        runId++
        open = true
        showPopup = true
    }
    fun doClose() { open = false }
    LaunchedEffect(open) {
        if (!open && showPopup) { delay(340); showPopup = false }
    }

    val label = "${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"
    val pillW = 176.dp
    val pillH = 40.dp

    val pillBg = if (hero) Color.White.copy(alpha = 0.22f) else c.surfaceCard
    val pillText = if (hero) Color.White else c.textPrimary
    val chevron = if (hero) Color.White else c.textSecondary
    // Over the frosted (white) hero card the primary text is light in dark mode
    // and unreadable — force it black there so months stay visible.
    val heroCalText = if (hero && c.isDark) Color.Black else c.textPrimary

    Box(modifier) {
        // --- In-place pill ---
        Row(
            modifier = Modifier
                .size(pillW, pillH)
                .alpha(if (showPopup) 0f else 1f)
                .then(if (hero) Modifier else Modifier.smallShadow(999.dp))
                .clip(CircleShape)
                .background(pillBg)
                .then(if (hero) Modifier else Modifier.border(1.dp, c.borderDefault, CircleShape))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { doOpen() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(label, color = pillText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            Icon(Lucide.ChevronDown, null, tint = chevron, modifier = Modifier.size(16.dp))
        }

        if (showPopup) {
            LiquidPopover(
                open = open,
                onDismiss = { doClose() },
                pillWidth = pillW,
                pillHeight = pillH,
                cardWidth = 272.dp,
                cardHeight = 296.dp,
                alignEnd = false,
                blobColor = Color.White,
                blobAlpha = if (hero) 0.7f else 1f,
                cardBackground = if (hero) Color.White.copy(alpha = 0.55f) else c.surfaceCard,
                cardBorder = if (hero) Color.White.copy(alpha = 0.5f) else c.borderDefault,
                onPillTap = { doClose() },
                pillGhost = {},
            ) {
                Column {
                    // Year nav
                    Row(
                        Modifier.fillMaxWidth().popIn(runId, 170),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PickerNavButton(hero = hero, icon = Lucide.ChevronLeft, contentDescription = "Previous year") { viewYear-- }
                        Text(
                            "$viewYear", color = heroCalText, fontSize = 17.sp,
                            fontWeight = FontWeight.Bold, fontFamily = DisplayFont,
                        )
                        PickerNavButton(hero = hero, icon = Lucide.ChevronRight, contentDescription = "Next year") { viewYear++ }
                    }
                    Spacer(Modifier.height(14.dp))
                    // 3×4 month grid — pops center-outward
                    val now = LocalDate.now()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..3).forEach { r ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (0..2).forEach { col ->
                                    val m = r * 3 + col
                                    val sel = date.year == viewYear && date.monthValue == m + 1
                                    val isNow = now.year == viewYear && now.monthValue == m + 1
                                    val cellBg = when {
                                        sel -> c.brand
                                        hero -> Color.White.copy(alpha = 0.4f)
                                        else -> c.surfaceInset
                                    }
                                    val interaction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .popIn(runId, gridPopDelay(m, 3, 4, base = 210, step = 46))
                                            .pressScale(interaction, 0.92f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cellBg)
                                            .then(
                                                when {
                                                    sel -> Modifier
                                                    isNow -> Modifier.border(1.5.dp, c.brand, RoundedCornerShape(12.dp))
                                                    hero -> Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                    else -> Modifier
                                                },
                                            )
                                            .clickable(interactionSource = interaction, indication = null) {
                                                onChange(LocalDate.of(viewYear, m + 1, 1))
                                                doClose()
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            MONTHS_SHORT[m],
                                            color = if (sel) c.onBrand else heroCalText,
                                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val MONTHS_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@Composable
private fun PickerNavButton(
    hero: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: Dp = 32.dp,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size)
            .pressScale(interaction, 0.9f)
            .clip(CircleShape)
            .background(if (hero) Color.White.copy(alpha = 0.35f) else c.surfaceInset)
            .then(if (hero) Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val iconTint = if (hero && c.isDark) Color.Black else c.textSecondary
        Icon(icon, contentDescription, tint = iconTint, modifier = Modifier.size(17.dp))
    }
}

/* ============================================================
 * PeriodPicker — ui_kits/mobile/PeriodPicker.jsx
 * Day / Week / Month / Year selector with calendar body,
 * "Jump to today" and Apply. Right-aligned light popover.
 * ============================================================ */

enum class ReportPeriod(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year") }

fun periodWeekStart(d: LocalDate): LocalDate = d.minusDays(d.dayOfWeek.value % 7L) // Sunday-based

fun periodLabel(period: ReportPeriod, d: LocalDate): String = when (period) {
    ReportPeriod.YEAR -> "${d.year}"
    ReportPeriod.MONTH -> "${d.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${d.year}"
    ReportPeriod.DAY -> "${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}, ${d.year}"
    ReportPeriod.WEEK -> {
        val s = periodWeekStart(d)
        val e = s.plusDays(6)
        val sm = s.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val em = e.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        if (s.month == e.month) "$sm ${s.dayOfMonth} – ${e.dayOfMonth}, ${e.year}"
        else "$sm ${s.dayOfMonth} – $em ${e.dayOfMonth}, ${e.year}"
    }
}

@Composable
fun PeriodPicker(
    date: LocalDate,
    period: ReportPeriod,
    onChange: (ReportPeriod, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.efColors
    var open by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var runId by remember { mutableIntStateOf(0) }
    // The staggered pop only plays for the open transition; interacting with the
    // open calendar (tab switch, page nav, jump-to-today) clears it so grids that
    // remount appear at rest instead of replaying the intro.
    var introPlaying by remember { mutableStateOf(false) }
    var draftP by remember { mutableStateOf(period) }
    var draft by remember { mutableStateOf(date) }
    var view by remember { mutableStateOf(date) }
    val today = LocalDate.now()

    fun doOpen() {
        draftP = period; draft = date; view = date
        runId++; introPlaying = true; open = true; showPopup = true
    }
    fun doClose() { open = false }
    LaunchedEffect(open) {
        if (!open && showPopup) { delay(340); showPopup = false }
    }

    val pillW = 194.dp
    val pillH = 40.dp

    Box(modifier) {
        // --- In-place pill ---
        Row(
            modifier = Modifier
                .size(pillW, pillH)
                .alpha(if (showPopup) 0f else 1f)
                .smallShadow(999.dp)
                .clip(CircleShape)
                .background(c.surfaceCard)
                .border(1.dp, c.borderDefault, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { doOpen() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Lucide.Calendar, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                periodLabel(period, date), color = c.textPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(7.dp))
            Icon(Lucide.ChevronDown, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
        }

        if (showPopup) {
            LiquidPopover(
                open = open,
                onDismiss = { doClose() },
                pillWidth = pillW,
                pillHeight = pillH,
                cardWidth = 306.dp,
                cardHeight = 452.dp,
                alignEnd = true,
                blobColor = c.surfaceCard,
                blobAlpha = 1f,
                cardBackground = c.surfaceCard,
                cardBorder = c.borderSubtle,
                onPillTap = { doClose() },
                pillGhost = {},
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.popIn(runId, 110, introPlaying)) {
                        SegmentedControl(
                            options = ReportPeriod.entries.map { it.label to it },
                            selected = draftP,
                            onSelect = { draftP = it; introPlaying = false },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    // Paging header
                    Row(
                        Modifier.fillMaxWidth().popIn(runId, 160, introPlaying),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PickerNavButton(hero = false, icon = Lucide.ChevronLeft, contentDescription = "Previous", size = 34.dp) {
                            view = when (draftP) {
                                ReportPeriod.DAY, ReportPeriod.WEEK -> view.minusMonths(1)
                                ReportPeriod.MONTH -> view.minusYears(1)
                                ReportPeriod.YEAR -> view.minusYears(12)
                            }
                            introPlaying = false
                        }
                        val header = when (draftP) {
                            ReportPeriod.DAY, ReportPeriod.WEEK ->
                                "${view.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${view.year}"
                            ReportPeriod.MONTH -> "${view.year}"
                            ReportPeriod.YEAR -> { val base = view.year / 12 * 12; "$base – ${base + 11}" }
                        }
                        Text(header, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                        PickerNavButton(hero = false, icon = Lucide.ChevronRight, contentDescription = "Next", size = 34.dp) {
                            view = when (draftP) {
                                ReportPeriod.DAY, ReportPeriod.WEEK -> view.plusMonths(1)
                                ReportPeriod.MONTH -> view.plusYears(1)
                                ReportPeriod.YEAR -> view.plusYears(12)
                            }
                            introPlaying = false
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Body
                    Box(Modifier.weight(1f)) {
                        when (draftP) {
                            ReportPeriod.DAY, ReportPeriod.WEEK -> DayGrid(
                                view = view, draft = draft, week = draftP == ReportPeriod.WEEK,
                                today = today, runId = runId, animate = introPlaying, onPick = { draft = it },
                            )
                            ReportPeriod.MONTH -> TwelveGrid(
                                labels = MONTHS_SHORT,
                                selected = if (draft.year == view.year) draft.monthValue - 1 else -1,
                                now = if (today.year == view.year) today.monthValue - 1 else -1,
                                runId = runId, animate = introPlaying,
                            ) { m -> draft = LocalDate.of(view.year, m + 1, 1) }
                            ReportPeriod.YEAR -> {
                                val base = view.year / 12 * 12
                                TwelveGrid(
                                    labels = (0..11).map { "${base + it}" },
                                    selected = (draft.year - base).takeIf { it in 0..11 } ?: -1,
                                    now = (today.year - base).takeIf { it in 0..11 } ?: -1,
                                    runId = runId, animate = introPlaying,
                                ) { i -> draft = LocalDate.of(base + i, draft.monthValue, 1) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Footer: Jump to today · Apply
                    Row(
                        Modifier.fillMaxWidth().popIn(runId, 230, introPlaying),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Jump to today", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { draft = today; view = today; introPlaying = false }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                        )
                        EFButton(
                            text = "Apply",
                            onClick = { onChange(draftP, draft); doClose() },
                            size = EFButtonSize.Md,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Day-of-month calendar grid, with single-day or full-week selection. */
@Composable
private fun DayGrid(
    view: LocalDate,
    draft: LocalDate,
    week: Boolean,
    today: LocalDate,
    runId: Int,
    animate: Boolean,
    onPick: (LocalDate) -> Unit,
) {
    val c = MaterialTheme.efColors
    val first = view.withDayOfMonth(1)
    val lead = first.dayOfWeek.value % 7 // Sunday-based leading blanks
    val count = view.lengthOfMonth()
    val cells = List(lead) { null } + (1..count).map { first.withDayOfMonth(it) }
    val rows = (cells.size + 6) / 7
    val wkStart = periodWeekStart(draft)
    val wkEnd = wkStart.plusDays(6)

    Column {
        Row {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                Text(
                    it, color = c.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            (0 until rows).forEach { r ->
                Row {
                    (0..6).forEach { col ->
                        val cell = cells.getOrNull(r * 7 + col)
                        if (cell == null) {
                            Box(Modifier.weight(1f).height(40.dp))
                        } else {
                            val isSel = !week && cell == draft
                            val weekSel = week && cell >= wkStart && cell <= wkEnd
                            val isWkS = weekSel && col == 0
                            val isWkE = weekSel && col == 6
                            val isToday = cell == today
                            val shape = when {
                                weekSel -> RoundedCornerShape(
                                    topStart = if (isWkS) 12.dp else 0.dp, bottomStart = if (isWkS) 12.dp else 0.dp,
                                    topEnd = if (isWkE) 12.dp else 0.dp, bottomEnd = if (isWkE) 12.dp else 0.dp,
                                )
                                else -> RoundedCornerShape(12.dp)
                            }
                            val interaction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .popIn(runId, gridPopDelay(r * 7 + col, 7, rows), animate)
                                    .clip(shape)
                                    .background(
                                        when {
                                            isSel -> c.brand
                                            weekSel -> c.brandSoft
                                            else -> Color.Transparent
                                        },
                                    )
                                    .then(if (isToday && !isSel && !weekSel) Modifier.border(1.5.dp, c.borderDefault, shape) else Modifier)
                                    .clickable(interactionSource = interaction, indication = null) { onPick(cell) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${cell.dayOfMonth}",
                                    color = when {
                                        isSel -> c.onBrand
                                        weekSel -> c.brand
                                        else -> c.textPrimary
                                    },
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = NumberFont,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================
 * DayPicker — single-day ledger filter (HomeScreen.jsx DayPicker).
 * A compact pill ("Any date" / "MMM d") that morphs into a month
 * calendar: single-day select, dots on days that hold transactions,
 * and "All dates" / "Today" shortcuts. Right-aligned to the pill.
 * ============================================================ */

private val WEEKDAYS_SHORT = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

@Composable
fun DayPicker(
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    initialMonth: LocalDate = LocalDate.now(),
    marks: Set<LocalDate> = emptySet(),
) {
    val c = MaterialTheme.efColors
    var open by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var runId by remember { mutableIntStateOf(0) }
    var view by remember { mutableStateOf(value ?: initialMonth) }
    val today = LocalDate.now()

    fun doOpen() { view = value ?: initialMonth; runId++; open = true; showPopup = true }
    fun doClose() { open = false }
    LaunchedEffect(open) { if (!open && showPopup) { delay(340); showPopup = false } }

    val pillW = 132.dp
    val pillH = 34.dp
    val pillBg = if (value != null) c.brand else c.brandSoft
    val pillFg = if (value != null) c.onBrand else c.brand
    val label = value?.let { "${MONTHS_SHORT[it.monthValue - 1]} ${it.dayOfMonth}" } ?: "Any date"

    Box(modifier) {
        // --- In-place pill (hidden while the popover morph is on screen) ---
        Row(
            modifier = Modifier
                .size(pillW, pillH)
                .alpha(if (showPopup) 0f else 1f)
                .clip(CircleShape)
                .background(pillBg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { doOpen() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Lucide.CalendarDays, null, tint = pillFg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label, color = pillFg, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            Icon(Lucide.ChevronDown, null, tint = pillFg, modifier = Modifier.size(14.dp))
        }

        if (showPopup) {
            LiquidPopover(
                open = open,
                onDismiss = { doClose() },
                pillWidth = pillW,
                pillHeight = pillH,
                cardWidth = 280.dp,
                cardHeight = 360.dp,
                alignEnd = true,
                blobColor = c.surfaceCard,
                blobAlpha = 1f,
                cardBackground = c.surfaceCard,
                cardBorder = c.borderSubtle,
                onPillTap = { doClose() },
                pillGhost = {},
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // Month nav
                    Row(
                        Modifier.fillMaxWidth().popIn(runId, 150),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PickerNavButton(hero = false, icon = Lucide.ChevronLeft, contentDescription = "Previous month") { view = view.minusMonths(1); runId++ }
                        Text(
                            "${view.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${view.year}",
                            color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont,
                        )
                        PickerNavButton(hero = false, icon = Lucide.ChevronRight, contentDescription = "Next month") { view = view.plusMonths(1); runId++ }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Weekday header
                    Row(Modifier.fillMaxWidth()) {
                        WEEKDAYS_SHORT.forEach {
                            Text(
                                it, color = c.textMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                                textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // Day grid
                    val first = view.withDayOfMonth(1)
                    val lead = first.dayOfWeek.value % 7 // Sunday-based leading blanks
                    val count = view.lengthOfMonth()
                    val cells = List(lead) { null } + (1..count).map { first.withDayOfMonth(it) }
                    val rows = (cells.size + 6) / 7
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        (0 until rows).forEach { r ->
                            Row(Modifier.fillMaxWidth()) {
                                (0..6).forEach { col ->
                                    val cell = cells.getOrNull(r * 7 + col)
                                    if (cell == null) {
                                        Box(Modifier.weight(1f).height(38.dp))
                                    } else {
                                        val isSel = cell == value
                                        val isToday = cell == today
                                        val marked = cell in marks
                                        val interaction = remember { MutableInteractionSource() }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .popIn(runId, gridPopDelay(r * 7 + col, 7, rows))
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) c.brand else Color.Transparent)
                                                .then(if (isToday && !isSel) Modifier.border(1.5.dp, c.borderDefault, RoundedCornerShape(12.dp)) else Modifier)
                                                .clickable(interactionSource = interaction, indication = null) { onChange(cell); doClose() },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "${cell.dayOfMonth}",
                                                color = if (isSel) c.onBrand else c.textPrimary,
                                                fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = NumberFont,
                                            )
                                            if (marked) {
                                                Box(
                                                    Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 5.dp)
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSel) c.onBrand else c.brand),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Footer — clear the date filter / jump to today
                    Row(
                        Modifier.fillMaxWidth().popIn(runId, 230),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = value != null) { onChange(null); doClose() }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                        ) {
                            Icon(Lucide.ListRestart, null, tint = if (value != null) c.brand else c.textMuted, modifier = Modifier.size(15.dp))
                            Text("All dates", color = if (value != null) c.brand else c.textMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont)
                        }
                        Text(
                            "Today", color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onChange(today); doClose() }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 3×4 grid of months or years. */
@Composable
private fun TwelveGrid(
    labels: List<String>,
    selected: Int,
    now: Int,
    runId: Int,
    animate: Boolean,
    onPick: (Int) -> Unit,
) {
    val c = MaterialTheme.efColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (0..3).forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (0..2).forEach { col ->
                    val i = r * 3 + col
                    val active = i == selected
                    val isNow = i == now
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .popIn(runId, gridPopDelay(i, 3, 4), animate)
                            .pressScale(interaction, 0.92f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) c.brand else c.surfaceInset)
                            .then(if (!active && isNow) Modifier.border(1.5.dp, c.brand, RoundedCornerShape(14.dp)) else Modifier)
                            .clickable(interactionSource = interaction, indication = null) { onPick(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            labels[i], color = if (active) c.onBrand else c.textPrimary,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = NumberFont,
                        )
                    }
                }
            }
        }
    }
}
