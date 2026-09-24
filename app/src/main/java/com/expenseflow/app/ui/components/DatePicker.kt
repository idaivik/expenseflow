package com.expenseflow.app.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.DisplayFont
import com.expenseflow.app.ui.theme.NumberFont
import com.expenseflow.app.ui.theme.Radii
import com.expenseflow.app.ui.theme.efColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Formats a picked date for the field label, e.g. "10 Jul 2026". */
private val fieldFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

/**
 * A tappable field that shows the currently selected date and opens a themed
 * calendar dialog to change it. Used in the Add Transaction sheet so the user can
 * record when an expense happened or income was received, rather than always
 * defaulting to "now".
 */
@Composable
fun EFDateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "Date",
) {
    val c = MaterialTheme.efColors
    var open by remember { mutableStateOf(false) }

    Column(modifier) {
        if (label != null) {
            Text(label, color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(Radii.sm))
                .background(c.surfaceInset)
                .border(1.dp, c.borderDefault, RoundedCornerShape(Radii.sm))
                .clickable { open = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.Calendar, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Text(
                fieldFormatter.format(date),
                color = c.textPrimary, fontSize = 15.sp, fontFamily = BodyFont,
                modifier = Modifier.weight(1f),
            )
            Icon(Lucide.ChevronDown, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        }
    }

    if (open) {
        DatePickerDialog(
            initial = date,
            onDismiss = { open = false },
            onConfirm = { onDateChange(it); open = false },
        )
    }
}

/** Themed month-grid calendar shown in a dialog. Confirms with "Set". */
@Composable
private fun DatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val c = MaterialTheme.efColors
    val today = LocalDate.now()
    var view by remember { mutableStateOf(YearMonth.from(initial)) }
    var selected by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.surfaceCard)
                .border(1.dp, c.borderSubtle, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            // Month nav header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavButton(Lucide.ChevronLeft, "Previous month") { view = view.minusMonths(1) }
                Text(
                    "${view.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${view.year}",
                    color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont,
                )
                NavButton(Lucide.ChevronRight, "Next month") { view = view.plusMonths(1) }
            }
            Spacer(Modifier.height(16.dp))

            // Weekday header
            Row(Modifier.fillMaxWidth()) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                    Text(
                        it, color = c.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Day grid (Sunday-based, matching the weekday header)
            val first = view.atDay(1)
            val lead = first.dayOfWeek.value % 7
            val count = view.lengthOfMonth()
            val cells = List(lead) { null } + (1..count).map { view.atDay(it) }
            val rows = (cells.size + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                (0 until rows).forEach { r ->
                    Row(Modifier.fillMaxWidth()) {
                        (0..6).forEach { col ->
                            val cell = cells.getOrNull(r * 7 + col)
                            if (cell == null) {
                                Box(Modifier.weight(1f).height(42.dp))
                            } else {
                                val isSel = cell == selected
                                val isToday = cell == today
                                val interaction = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) c.brand else Color.Transparent)
                                        .then(
                                            if (isToday && !isSel) Modifier.border(1.5.dp, c.borderDefault, RoundedCornerShape(12.dp))
                                            else Modifier,
                                        )
                                        .clickable(interactionSource = interaction, indication = null) { selected = cell },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${cell.dayOfMonth}",
                                        color = if (isSel) c.onBrand else c.textPrimary,
                                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = NumberFont,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            // Footer: Today shortcut · Cancel · Set
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Today", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = BodyFont,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selected = today; view = YearMonth.from(today) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Cancel", color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = BodyFont,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                )
                EFButton(text = "Set", onClick = { onConfirm(selected) }, size = EFButtonSize.Sm)
            }
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(c.surfaceInset)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = c.textSecondary, modifier = Modifier.size(18.dp))
    }
}
