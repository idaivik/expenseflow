package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFButtonVariant
import com.expenseflow.app.ui.components.EFCard
import com.expenseflow.app.ui.components.EFElevation
import com.expenseflow.app.ui.components.SegmentedControl
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.CurrencyExchangeState
import com.expenseflow.app.viewmodel.ExpenseViewModel
import com.expenseflow.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    expenseViewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.efColors
    val settings by settingsViewModel.settings.collectAsState()
    val exchangeState by expenseViewModel.exchangeState.collectAsState()

    // The currency the user tapped, awaiting confirmation of the conversion.
    var pendingCurrency by remember { mutableStateOf<String?>(null) }

    // Turning SMS auto-detect on needs the RECEIVE_SMS/READ_SMS runtime permissions
    // first; only persist the toggle once the user actually grants them.
    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) settingsViewModel.setSmsAutoDetect(true)
    }

    // Once the conversion lands (the persisted currency now matches the pick), close the dialog.
    LaunchedEffect(settings.currencyCode) {
        if (pendingCurrency != null && settings.currencyCode == pendingCurrency) pendingCurrency = null
    }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Settings", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("App preferences & account controls", color = c.textSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        SectionLabel("Preferences")
        EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(horizontal = 18.dp)) {
            SettingsRow(
                if (settings.isDark) Lucide.Moon else Lucide.Sun, CategoryTone.PURPLE,
                "Dark mode", if (settings.isDark) "On" else "Off",
                trailing = { EFToggle(settings.isDark) { settingsViewModel.setDark(it) } },
            )
            SettingsRow(Lucide.Globe, CategoryTone.BLUE, "Language", null, trailing = { ValueLabel("English") })
            SettingsRow(Lucide.CalendarDays, CategoryTone.ORANGE, "Start of Week", null, last = true, trailing = { ValueLabel("Monday") })
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(CategoryTone.GREEN, Lucide.CircleDollarSign, size = 40.dp, cornerRadius = 12.dp)
            Spacer(Modifier.width(10.dp))
            Text("Currency", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        SegmentedControl(
            options = listOf("USD $" to "usd", "INR ₹" to "inr", "EUR €" to "eur"),
            selected = settings.currencyCode,
            // Don't switch instantly: confirm first, then convert every stored amount
            // at the live exchange rate. The control stays put until that succeeds.
            onSelect = { picked ->
                if (picked != settings.currencyCode) {
                    expenseViewModel.clearExchangeError()
                    pendingCurrency = picked
                }
            },
        )
        Text(
            "Changing currency converts all your amounts at today's exchange rate.",
            color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(CategoryTone.BLUE, Lucide.Receipt, size = 40.dp, cornerRadius = 12.dp)
            Spacer(Modifier.width(10.dp))
            Text("Transactions Total", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        SegmentedControl(
            options = listOf("Expense" to "expense", "Income" to "income", "Net" to "net"),
            selected = settings.transactionDisplayMode,
            onSelect = { settingsViewModel.setTransactionDisplayMode(it) },
        )
        Text(
            "What the totals above your transaction list add up.",
            color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )

        Spacer(Modifier.height(22.dp))
        SectionLabel("Notifications")
        EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(horizontal = 18.dp)) {
            SettingsRow(Lucide.Bell, CategoryTone.ORANGE, "Budget Alerts", "When you near a limit", trailing = { EFToggle(settings.budgetAlerts) { settingsViewModel.setBudgetAlerts(it) } })
            SettingsRow(Lucide.ReceiptText, CategoryTone.BLUE, "Bill Reminders", "3 days before due", trailing = { EFToggle(settings.billReminders) { settingsViewModel.setBillReminders(it) } })
            SettingsRow(Lucide.CalendarDays, CategoryTone.PURPLE, "Weekly Summary", null, trailing = { EFToggle(settings.weeklySummary) { settingsViewModel.setWeeklySummary(it) } })
            SettingsRow(Lucide.Info, CategoryTone.GREEN, "Goal Milestones", null, trailing = { EFToggle(settings.goalMilestones) { settingsViewModel.setGoalMilestones(it) } })
            SettingsRow(
                Lucide.MessageSquare, CategoryTone.BLUE, "Auto-detect from SMS", "Log credits/debits from bank SMS", last = true,
                trailing = {
                    EFToggle(settings.smsAutoDetect) { enabled ->
                        if (enabled) {
                            smsPermissionLauncher.launch(
                                arrayOf(android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.READ_SMS)
                            )
                        } else {
                            settingsViewModel.setSmsAutoDetect(false)
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Support")
        EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(horizontal = 18.dp)) {
            SettingsRow(Lucide.Info, CategoryTone.GREEN, "About", null, last = true, trailing = { ValueLabel("v2.4.0") })
        }
        Spacer(Modifier.height(20.dp))
        Text("ExpenseFlow · Version 2.4.0 (build 218)", color = c.textMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(28.dp))
    }

    pendingCurrency?.let { to ->
        CurrencyConvertDialog(
            fromCode = settings.currencyCode,
            toCode = to,
            state = exchangeState,
            onConfirm = { expenseViewModel.changeCurrency(to) },
            onDismiss = {
                expenseViewModel.clearExchangeError()
                pendingCurrency = null
            },
        )
    }
}

/** Display label + symbol for an in-app currency code, e.g. "usd" → "US Dollar ($)". */
private fun currencyName(code: String): String = when (code) {
    "inr" -> "Indian Rupee (₹)"
    "eur" -> "Euro (€)"
    else -> "US Dollar ($)"
}

/**
 * Confirms converting every stored amount from one currency to another at the live
 * Frankfurter rate. Shows a spinner while the rate is fetched and the amounts are
 * converted, and an inline error (with Retry) if the exchange service is unreachable.
 */
@Composable
private fun CurrencyConvertDialog(
    fromCode: String,
    toCode: String,
    state: CurrencyExchangeState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val loading = state is CurrencyExchangeState.Loading
    val error = state as? CurrencyExchangeState.Error
    Dialog(onDismissRequest = { if (!loading) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.surfaceCard)
                .padding(24.dp),
        ) {
            Text("Convert to ${currencyName(toCode)}?", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "All your amounts will be converted from ${currencyName(fromCode)} to " +
                    "${currencyName(toCode)} at today's exchange rate (via Frankfurter).",
                color = c.textSecondary, fontSize = 14.sp,
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error.message, color = c.moneyOut, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
            if (loading) {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = c.brand, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Converting…", color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                EFButton(
                    text = if (error != null) "Retry" else "Convert",
                    onClick = onConfirm,
                    fullWidth = true,
                )
                Spacer(Modifier.height(10.dp))
                EFButton(text = "Cancel", onClick = onDismiss, variant = EFButtonVariant.Ghost, fullWidth = true)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = MaterialTheme.efColors
    Text(
        text.uppercase(), color = c.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 0.06.em, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

private val Double.em get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)

@Composable
private fun SettingsRow(icon: ImageVector, tone: CategoryTone, label: String, sub: String?, last: Boolean = false, trailing: @Composable () -> Unit) {
    val c = MaterialTheme.efColors
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(tone, icon, size = 40.dp, cornerRadius = 12.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (sub != null) Text(sub, color = c.textSecondary, fontSize = 13.sp)
            }
            trailing()
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.borderSubtle))
    }
}

@Composable
private fun ValueLabel(text: String) {
    val c = MaterialTheme.efColors
    Text(text, color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EFToggle(on: Boolean, onToggle: (Boolean) -> Unit) {
    val c = MaterialTheme.efColors
    val knobOffset by animateDpAsState(if (on) 20.dp else 2.dp, label = "toggle")
    Box(
        modifier = Modifier.size(width = 46.dp, height = 28.dp).clip(CircleShape)
            .background(if (on) c.brand else c.surfaceSunken)
            .clickable { onToggle(!on) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.padding(start = knobOffset).size(24.dp).clip(CircleShape).background(Color.White))
    }
}
