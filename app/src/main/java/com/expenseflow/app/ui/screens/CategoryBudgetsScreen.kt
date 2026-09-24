package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.expenseflow.app.ui.components.EFCard
import com.expenseflow.app.ui.components.EFElevation
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.components.ICON_REGISTRY
import com.expenseflow.app.ui.components.IconPickerCell
import com.expenseflow.app.ui.components.ProgressBar
import com.expenseflow.app.ui.components.ScreenTopBar
import com.expenseflow.app.ui.components.SegmentedControl
import com.expenseflow.app.ui.components.TonePickerRow
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.ExpenseViewModel

@Composable
fun CategoryBudgetsScreen(viewModel: ExpenseViewModel, onBack: () -> Unit) {
    val c = MaterialTheme.efColors
    val budgets by viewModel.budgetCategories.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val edits = remember { mutableStateMapOf<String, String>() }
    var showAdd by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(c.bgApp).imePadding()) {
        ScreenTopBar("Monthly Budget", onBack, trailing = {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(c.brandSoft).clickable { showAdd = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Lucide.Plus, "Add category", tint = c.brand, modifier = Modifier.size(20.dp)) }
        })
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Set a monthly spending limit for each category. You'll get an alert as you approach it.", color = c.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(4.dp))
            }
            if (budgets.isEmpty()) {
                item {
                    EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(22.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(c.surfaceSunken), contentAlignment = Alignment.Center) {
                                Icon(Lucide.Wallet, null, tint = c.textMuted, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("No budgets yet", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Tap + to set a monthly limit for a category.", color = c.textSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
            items(budgets) { b ->
                val tone = CategoryVisuals.tone(b.name)
                val pct = if (b.total > 0) (b.spent / b.total * 100).toFloat() else 0f
                EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(tone, CategoryVisuals.icon(b.name), size = 42.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Spent $currency${b.spent.toInt()} of $currency${b.total.toInt()}", color = c.textSecondary, fontSize = 13.sp)
                        }
                        Box(Modifier.width(100.dp)) {
                            EFTextField(
                                value = edits[b.name] ?: b.total.toInt().toString(),
                                onValueChange = { edits[b.name] = it.filter { ch -> ch.isDigit() } },
                                leadingIcon = null,
                                placeholder = "0",
                                keyboardType = KeyboardType.Number,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Lucide.Trash2, "Delete budget", tint = c.moneyOut,
                            modifier = Modifier.size(20.dp).clip(CircleShape).clickable {
                                edits.remove(b.name)
                                viewModel.deleteBudget(b.name)
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ProgressBar(value = pct, tone = tone, height = 7.dp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(20.dp)) {
            EFButton(
                text = "Save Budgets",
                onClick = {
                    edits.forEach { (category, value) ->
                        value.toDoubleOrNull()?.let { viewModel.updateBudget(category, it) }
                    }
                    onBack()
                },
                fullWidth = true,
            )
        }
    }

    if (showAdd) {
        val expenseCats by viewModel.expenseCategoryList.collectAsState()
        AddBudgetCategorySheet(
            allCategories = expenseCats.map { it.name },
            existingBudgets = budgets.map { it.name }.toSet(),
            onAddExisting = { category, limit ->
                viewModel.updateBudget(category, limit)
                showAdd = false
            },
            onCreateNew = { name, tone, iconKey, limit ->
                viewModel.addCategory(name, true, tone, iconKey)
                viewModel.updateBudget(name, limit)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

/**
 * Lets the user either pick one of their existing expense categories that doesn't
 * yet have a budget, or define a brand-new category (name, color, icon) on the
 * spot — so setting up a budget never requires a detour to Manage Categories first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetCategorySheet(
    allCategories: List<String>,
    existingBudgets: Set<String>,
    onAddExisting: (String, Double) -> Unit,
    onCreateNew: (String, CategoryTone, String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Offer the user's expense categories that don't already have a budget.
    val options = remember(allCategories, existingBudgets) { allCategories.filter { it !in existingBudgets } }
    var creatingNew by remember { mutableStateOf(options.isEmpty()) }
    var category by remember { mutableStateOf(options.firstOrNull() ?: "") }
    var limit by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newTone by remember { mutableStateOf(CategoryTone.BLUE) }
    var newIconKey by remember { mutableStateOf(ICON_REGISTRY.first().first) }
    val newIcon = ICON_REGISTRY.toMap()[newIconKey] ?: ICON_REGISTRY.first().second
    val trimmedNewName = newName.trim()
    val duplicateName = allCategories.any { it.equals(trimmedNewName, ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surfaceCard) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                // verticalScroll first so content taller than the sheet (e.g. the icon
                // grid) can be scrolled at all; imePadding chained after it adds its
                // padding *inside* the scrollable area, so the focused field and the
                // "Add Category" button can always be scrolled clear of the keyboard.
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text("Add Budget Category", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))

            if (options.isNotEmpty()) {
                SegmentedControl(
                    options = listOf("Existing category" to false, "New category" to true),
                    selected = creatingNew, onSelect = { creatingNew = it },
                )
                Spacer(Modifier.height(18.dp))
            }

            if (!creatingNew) {
                Text("Category", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                options.chunked(3).forEach { rowItems ->
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
                EFTextField(limit, { limit = it.filter { ch -> ch.isDigit() } }, label = "Monthly limit", placeholder = "0", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(20.dp))
                EFButton(
                    text = "Add Category",
                    onClick = {
                        val value = limit.toDoubleOrNull() ?: 0.0
                        if (category.isNotBlank() && value > 0.0) onAddExisting(category, value)
                    },
                    fullWidth = true,
                    enabled = category.isNotBlank() && (limit.toDoubleOrNull() ?: 0.0) > 0.0,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    CategoryIcon(tone = newTone, icon = newIcon, size = 48.dp, cornerRadius = 15.dp)
                    Column(Modifier.weight(1f)) {
                        EFTextField(newName, { newName = it }, placeholder = "Category name")
                        if (duplicateName) {
                            Spacer(Modifier.height(4.dp))
                            Text("A category with this name already exists.", color = c.moneyOut, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                Text("Color", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                TonePickerRow(selected = newTone, onSelect = { newTone = it })
                Spacer(Modifier.height(20.dp))

                Text("Icon", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ICON_REGISTRY.chunked(6).forEach { rowIcons ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowIcons.forEach { (key, ic) ->
                                IconPickerCell(
                                    icon = ic, tone = newTone, selected = key == newIconKey,
                                    modifier = Modifier.weight(1f), onClick = { newIconKey = key },
                                )
                            }
                            repeat(6 - rowIcons.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                EFTextField(limit, { limit = it.filter { ch -> ch.isDigit() } }, label = "Monthly limit", placeholder = "0", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(20.dp))
                EFButton(
                    text = "Add Category",
                    onClick = {
                        val value = limit.toDoubleOrNull() ?: 0.0
                        if (trimmedNewName.isNotEmpty() && !duplicateName && value > 0.0) onCreateNew(trimmedNewName, newTone, newIconKey, value)
                    },
                    fullWidth = true,
                    enabled = trimmedNewName.isNotEmpty() && !duplicateName && (limit.toDoubleOrNull() ?: 0.0) > 0.0,
                )
            }
        }
    }
}
