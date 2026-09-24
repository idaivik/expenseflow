package com.expenseflow.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.NumberFont
import com.expenseflow.app.ui.theme.efColors
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class ChartSegment(val label: String, val value: Double, val tone: CategoryTone)

// --- Donut geometry, in the design's 220×220 viewBox space ---
private const val D_VIEWBOX = 220f
private const val D_CENTER = 110f   // cx / cy
private const val D_TRACK_R = 86f   // sunken track ring radius
private const val D_TRACK_SW = 28f  // track ring stroke
private const val D_RO = 100f       // colored band outer radius
private const val D_RI = 72f        // colored band inner radius
private const val D_RR = 7f         // corner radius
private const val D_GAP = 5f        // gap between segments (deg)

private fun dFmt(v: Float) = String.format(Locale.US, "%.2f", v)

/** Point on a circle of radius [r] at [deg], measured clockwise from 12 o'clock. */
private fun dP(r: Float, deg: Float): String {
    val a = Math.toRadians(deg.toDouble())
    return "${dFmt((D_CENTER + r * sin(a)).toFloat())} ${dFmt((D_CENTER - r * cos(a)).toFloat())}"
}

/**
 * SVG path for one donut sector spanning [a0]..[a1] (degrees) with uniformly
 * rounded corners of radius [rr] on both ends — a 1:1 port of the Report
 * mockup's `sectorPath`, so both radial edges are drawn identically.
 */
private fun donutSectorPath(a0: Float, a1: Float, ro: Float = D_RO, ri: Float = D_RI, rr: Float = D_RR): String {
    val oa = (rr / ro) * 180f / Math.PI.toFloat()   // corner inset along the outer arc
    val ia = (rr / ri) * 180f / Math.PI.toFloat()   // corner inset along the inner arc
    val oL = if ((a1 - oa) - (a0 + oa) > 180f) 1 else 0
    val iL = if ((a1 - ia) - (a0 + ia) > 180f) 1 else 0
    return "M ${dP(ro, a0 + oa)} A $ro $ro 0 $oL 1 ${dP(ro, a1 - oa)} " +
        "A $rr $rr 0 0 1 ${dP(ro - rr, a1)} L ${dP(ri + rr, a1)} " +
        "A $rr $rr 0 0 1 ${dP(ri, a1 - ia)} A $ri $ri 0 $iL 0 ${dP(ri, a0 + ia)} " +
        "A $rr $rr 0 0 1 ${dP(ri + rr, a0)} L ${dP(ro - rr, a0)} A $rr $rr 0 0 1 ${dP(ro, a0 + oa)} Z"
}

/**
 * Donut chart — mirrors the Report mockup's custom donut: flat colored sectors
 * (outer r 100 / inner r 72 in a 220 viewBox) with softly rounded corners and
 * a 5° gap, floating over a slim sunken track ring. Each sector is a fully
 * closed rounded-rectangle-around-an-arc path (identical corners on both ends),
 * so the two radial edges always render at the same width.
 */
@Composable
fun DonutChart(
    segments: List<ChartSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 308.dp,
    centerContent: @Composable (() -> Unit)? = null,
) {
    val c = MaterialTheme.efColors
    val total = segments.sumOf { it.value }.takeIf { it > 0 } ?: 1.0
    val track = c.surfaceSunken

    // A single non-zero category spans the full circle — draw it as a seamless
    // ring instead of a gapped sector, since the gap/rounded-corner sector path
    // never actually closes and left a visible split at 100%.
    val fullTone: CategoryTone? = segments.filter { it.value > 0 }.singleOrNull()?.tone

    // Build each sector path once (in 220-space) and reuse across redraws/scroll.
    val arcs: List<Pair<CategoryTone, Path>> = remember(segments) {
        if (fullTone != null) return@remember emptyList()
        var cum = 0f
        segments.mapNotNull { s ->
            val span = (s.value / total).toFloat() * 360f
            val a0 = cum + D_GAP / 2
            val a1 = cum + span - D_GAP / 2
            cum += span
            if (a1 > a0) s.tone to PathParser().parsePathString(donutSectorPath(a0, a1)).toPath() else null
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val scale = this.size.width / D_VIEWBOX
            scale(scale, scale, pivot = Offset.Zero) {
                // Background track ring (r 86, stroke 28)
                drawCircle(color = track, radius = D_TRACK_R, center = Offset(D_CENTER, D_CENTER), style = Stroke(width = D_TRACK_SW))
                if (fullTone != null) {
                    drawCircle(
                        color = c.cat(fullTone),
                        radius = (D_RO + D_RI) / 2,
                        center = Offset(D_CENTER, D_CENTER),
                        style = Stroke(width = D_RO - D_RI),
                    )
                } else {
                    arcs.forEach { (tone, path) -> drawPath(path, color = c.cat(tone)) }
                }
            }
        }
        centerContent?.invoke()
    }
}

/**
 * Bar chart — mirrors components/data/BarChart.jsx: value labels on top,
 * full-width bars growing (staggered) inside soft sunken capsule tracks,
 * category labels underneath.
 */
@Composable
fun BarChart(
    segments: List<ChartSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    formatValue: (Double) -> String = { it.toInt().toString() },
) {
    val c = MaterialTheme.efColors
    val max = segments.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            segments.forEachIndexed { i, s ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    val alpha by animateFloatAsState(if (mounted) 1f else 0f, tween(360), label = "barValue")
                    Text(
                        formatValue(s.value), color = c.textSecondary.copy(alpha = alpha),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = NumberFont, maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.surfaceSunken),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        val frac by animateFloatAsState(
                            targetValue = if (mounted) (s.value / max).toFloat().coerceIn(0f, 1f) else 0f,
                            animationSpec = tween(360, delayMillis = i * 45),
                            label = "barFill",
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(frac)
                                .clip(RoundedCornerShape(8.dp))
                                .background(c.cat(s.tone)),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            segments.forEach { s ->
                Text(
                    s.label, color = c.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
