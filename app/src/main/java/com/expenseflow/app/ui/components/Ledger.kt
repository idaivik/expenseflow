package com.expenseflow.app.ui.components

import com.composables.icons.lucide.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.model.InsightSeverity
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.Gray900
import com.expenseflow.app.ui.theme.efColors
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A single ledger row — mirrors components/data/TransactionRow.jsx:
 * a white soft-shadow card holding the tinted category glyph, title + tag chip,
 * and a sign-colored amount (out = red, in = green) with a small sub line.
 */
@Composable
fun TransactionRow(
    title: String,
    category: String,
    time: String,
    amount: Double,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
    date: String = "",
    currencySymbol: String = "$",
    isEdited: Boolean = false,
    pinned: Boolean = false,
) {
    val c = MaterialTheme.efColors
    val tone = CategoryVisuals.tone(category)
    EFCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(14.dp),
        elevation = EFElevation.Sm,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CategoryIcon(tone = tone, icon = CategoryVisuals.icon(category), size = 46.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        fontFamily = BodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (isEdited) {
                        Spacer(Modifier.width(6.dp))
                        Text("(edited)", color = c.textMuted, fontSize = 11.sp, fontFamily = BodyFont)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pinned) {
                        Icon(Lucide.Pin, contentDescription = "Pinned", tint = c.brand, modifier = Modifier.size(13.dp))
                    }
                    Tag(text = category, tone = tone)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Amount(
                    value = amount, size = 16.sp,
                    color = if (isExpense) c.moneyOut else c.moneyIn,
                    currencySymbol = currencySymbol,
                    prefix = if (isExpense) "-" else "+",
                )
                Spacer(Modifier.height(3.dp))
                val dateLabel = formatRowDate(date)
                if (dateLabel != null) {
                    Text(dateLabel, color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont)
                    Spacer(Modifier.height(1.dp))
                    Text(time, color = c.textMuted, fontSize = 11.sp, fontFamily = BodyFont)
                } else {
                    Text(time, color = c.textSecondary, fontSize = 13.sp, fontFamily = BodyFont)
                }
            }
        }
    }
}

// Parsers/formatters for the stored "dd/MM/yyyy" transaction date. Reused across
// rows, so they're created once rather than per-recomposition.
private val storedDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val rowDateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

/** Turns a stored "dd/MM/yyyy" date into a friendly "10 Jul 2026", or null if blank/invalid. */
private fun formatRowDate(date: String): String? {
    if (date.isBlank()) return null
    return try {
        storedDateFormat.parse(date)?.let { rowDateFormat.format(it) }
    } catch (e: Exception) {
        date
    }
}

/** Dark, premium "insight" strip driven by [InsightSeverity]. */
@Composable
fun InsightBanner(
    message: String,
    severity: InsightSeverity,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.efColors
    val accent = when (severity) {
        InsightSeverity.POSITIVE -> c.moneyIn
        InsightSeverity.NEUTRAL -> c.catBlue
        InsightSeverity.WARNING -> c.catOrange
        InsightSeverity.DANGER -> c.moneyOut
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(if (c.isDark) c.surfaceInset else Gray900)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.Sparkles, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Your insight", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(2.dp))
            Text(message, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
        }
    }
}
