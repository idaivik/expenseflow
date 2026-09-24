package com.expenseflow.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.DisplayFont
import com.expenseflow.app.ui.theme.NumberFont
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.smallShadow
import java.util.Locale
import kotlin.math.abs

/** Large formatted money value with visually de-emphasized cents (design: `.85` muted). */
@Composable
fun Amount(
    value: Double,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    weight: FontWeight = FontWeight.Bold,
    symbolMuted: Boolean = false,
    prefix: String = "",
    currencyTrailing: Boolean = false,
) {
    val formatted = String.format(Locale.US, "%,.2f", abs(value))
    val dot = formatted.lastIndexOf('.')
    val whole = if (dot >= 0) formatted.substring(0, dot) else formatted
    val cents = if (dot >= 0) formatted.substring(dot) else ""
    val muted = color.copy(alpha = 0.4f)

    Text(
        text = buildAnnotatedString {
            if (prefix.isNotEmpty()) append(prefix)
            if (!currencyTrailing) withStyle(SpanStyle(color = if (symbolMuted) muted else color)) { append(currencySymbol) }
            append(whole)
            if (cents.isNotEmpty()) withStyle(SpanStyle(color = muted)) { append(cents) }
            if (currencyTrailing) withStyle(SpanStyle(color = if (symbolMuted) muted else color)) { append(currencySymbol) }
        },
        color = color,
        fontFamily = NumberFont,
        fontWeight = weight,
        fontSize = size,
        letterSpacing = (-0.02).em,
        modifier = modifier,
    )
}

enum class BadgeTone { Positive, Negative, Brand, Neutral }

@Composable
fun Badge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val c = MaterialTheme.efColors
    val (bg, fg) = when (tone) {
        BadgeTone.Positive -> c.tintGreen to c.moneyIn
        BadgeTone.Negative -> c.tintRed to c.moneyOut
        BadgeTone.Brand -> c.brandSoft to c.brand
        BadgeTone.Neutral -> c.surfaceSunken to c.textSecondary
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Tag / chip with a leading category-colored dot (or icon) — transaction labels
 * like "Vacation", "Weekly", or the "For the Period" filter chip.
 * Design: sunken chip, 13px medium secondary text, 8px dot in the tone color.
 */
@Composable
fun Tag(text: String, tone: CategoryTone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = MaterialTheme.efColors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(c.surfaceSunken)
            .padding(start = 9.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c.cat(tone), modifier = Modifier.size(13.dp))
        } else {
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.cat(tone)))
        }
        Text(text, color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = BodyFont)
    }
}

/** Circular initials avatar with an optional small brand badge (hero / profile). */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    badge: ImageVector? = null,
) {
    val c = MaterialTheme.efColors
    val initials = name.trim().split(" ").filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "U" }
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .smallShadow(size / 2)
                .clip(CircleShape)
                .background(c.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials, color = c.brand, fontWeight = FontWeight.Bold,
                fontFamily = DisplayFont, fontSize = (size.value * 0.36f).sp,
            )
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(size * 0.42f)
                    .smallShadow(size * 0.21f)
                    .clip(CircleShape)
                    .background(c.surfaceCard),
                contentAlignment = Alignment.Center,
            ) {
                Icon(badge, contentDescription = null, tint = c.brand, modifier = Modifier.size(size * 0.26f))
            }
        }
    }
}

/** Rounded progress track + fill. [value] is 0..100. */
@Composable
fun ProgressBar(
    value: Float,
    tone: CategoryTone,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color? = null,
    fillColor: Color? = null,
) {
    val c = MaterialTheme.efColors
    val fraction by animateFloatAsState((value / 100f).coerceIn(0f, 1f), label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor ?: c.surfaceSunken),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(height)
                .clip(CircleShape)
                .background(fillColor ?: c.cat(tone)),
        )
    }
}

/** Circular progress ring. [value] is 0..100. */
@Composable
fun ProgressRing(
    value: Float,
    tone: CategoryTone,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    stroke: Dp = 6.dp,
    content: @Composable (() -> Unit)? = null,
) {
    val c = MaterialTheme.efColors
    val sweep by animateFloatAsState((value / 100f).coerceIn(0f, 1f) * 360f, label = "ring")
    val track = c.surfaceSunken
    val fill = c.cat(tone)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            drawArc(track, 0f, 360f, false, style = Stroke(sw, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2),
                size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw))
            drawArc(fill, -90f, sweep, false, style = Stroke(sw, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2),
                size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw))
        }
        content?.invoke()
    }
}
