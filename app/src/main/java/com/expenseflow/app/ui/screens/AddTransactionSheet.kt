package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.CategoryVisuals
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFDateField
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.components.SegmentedControl
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    initialCategory: String? = null,
    initialKind: String = "exp",
    initialAmount: String = "",
    onManageCategories: (Boolean) -> Unit = {},
) {
    val c = MaterialTheme.efColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember { mutableStateOf(initialKind) }
    val isExpense = kind == "exp"
    val expenseCats by viewModel.expenseCategoryList.collectAsState()
    val incomeCats by viewModel.incomeCategoryList.collectAsState()
    val cats = (if (isExpense) expenseCats else incomeCats).map { it.name }
    var category by remember { mutableStateOf(initialCategory ?: "") }
    var amount by remember { mutableStateOf(initialAmount) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }

    // reset selected category when switching type (or once the list first loads)
    if (category !in cats && category.isBlank()) category = cats.firstOrNull() ?: ""

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surfaceCard) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 28.dp).navigationBarsPadding().imePadding()) {
            Text("Add Transaction", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            SegmentedControl(
                options = listOf("Expense" to "exp", "Income" to "inc"),
                selected = kind, onSelect = { kind = it },
            )
            Spacer(Modifier.height(20.dp))
            EFTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = "Amount", placeholder = "0.00", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Category", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onManageCategories(isExpense) }.padding(4.dp),
                ) {
                    Icon(Lucide.Settings2, contentDescription = null, tint = c.brand, modifier = Modifier.size(14.dp))
                    Text("Manage", color = c.brand, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (cats.isEmpty()) {
                Text("No categories yet. Tap Manage to add one.", color = c.textMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
            }
            cats.chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { cat ->
                        val active = cat == category
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (active) c.brandSoft else c.surfaceInset)
                                .clickable { category = cat }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CategoryIcon(tone = CategoryVisuals.tone(cat), icon = CategoryVisuals.icon(cat), size = 40.dp, cornerRadius = 12.dp)
                            Spacer(Modifier.height(7.dp))
                            Text(cat, color = c.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            EFDateField(date = date, onDateChange = { date = it })
            Spacer(Modifier.height(18.dp))
            EFTextField(note, { note = it }, label = "Note", placeholder = "What was it for?")
            Spacer(Modifier.height(20.dp))
            EFButton(
                text = "Save Transaction",
                onClick = {
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (value > 0.0 && category.isNotBlank()) {
                        // The user picks the day it happened; the time-of-day is
                        // stamped from the moment they save.
                        val pickedDay = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                        val now = Calendar.getInstance().time
                        viewModel.addTransaction(
                            title = note.ifBlank { category },
                            category = category,
                            amount = value,
                            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(pickedDay),
                            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now),
                            iconName = category,
                            iconColor = c.cat(CategoryVisuals.tone(category)),
                            isExpense = isExpense,
                        )
                        onDismiss()
                    }
                },
                fullWidth = true,
            )
        }
    }
}
