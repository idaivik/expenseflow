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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.expenseflow.app.data.CategoryEntity
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.CategoryVisuals
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFButtonVariant
import com.expenseflow.app.ui.components.EFCard
import com.expenseflow.app.ui.components.EFElevation
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.components.ICON_REGISTRY
import com.expenseflow.app.ui.components.IconPickerCell
import com.expenseflow.app.ui.components.ScreenTopBar
import com.expenseflow.app.ui.components.SegmentedControl
import com.expenseflow.app.ui.components.TonePickerRow
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.ExpenseViewModel

/**
 * Add/reorder/delete screen for the categories offered in the Add Transaction picker.
 * Reordering uses explicit up/down controls rather than a drag gesture — simpler to
 * get right and to verify than a hand-rolled drag-and-drop list, and just as capable
 * of permanently arranging the order.
 */
@Composable
fun ManageCategoriesScreen(
    viewModel: ExpenseViewModel,
    initialExpense: Boolean,
    onBack: () -> Unit,
) {
    val c = MaterialTheme.efColors
    var isExpense by remember { mutableStateOf(initialExpense) }
    val expenseCats by viewModel.expenseCategoryList.collectAsState()
    val incomeCats by viewModel.incomeCategoryList.collectAsState()
    val categoriesInUse by viewModel.categoriesInUse.collectAsState()
    val list = if (isExpense) expenseCats else incomeCats

    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var blockedDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(c.bgApp)) {
        ScreenTopBar("Manage Categories", onBack, trailing = {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(c.brandSoft).clickable { showAdd = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Lucide.Plus, "Add category", tint = c.brand, modifier = Modifier.size(20.dp)) }
        })
        Column(Modifier.padding(horizontal = 20.dp)) {
            SegmentedControl(
                options = listOf("Expense" to true, "Income" to false),
                selected = isExpense, onSelect = { isExpense = it },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Use the arrows to reorder. This order is saved and used everywhere you pick a category.",
                color = c.textSecondary, fontSize = 13.sp, lineHeight = 18.sp,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (list.isEmpty()) {
                item {
                    EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(22.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Lucide.LayoutGrid, null, tint = c.textMuted, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No categories yet", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Tap + to add one.", color = c.textSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
            items(list, key = { "${it.isExpense}:${it.name}" }) { cat ->
                val index = list.indexOf(cat)
                CategoryManageRow(
                    cat = cat,
                    canMoveUp = index > 0,
                    canMoveDown = index < list.lastIndex,
                    onMoveUp = {
                        val reordered = list.toMutableList().apply { add(index - 1, removeAt(index)) }
                        viewModel.reorderCategories(isExpense, reordered.map { it.name })
                    },
                    onMoveDown = {
                        val reordered = list.toMutableList().apply { add(index + 1, removeAt(index)) }
                        viewModel.reorderCategories(isExpense, reordered.map { it.name })
                    },
                    onDelete = {
                        if (cat.name in categoriesInUse) blockedDelete = cat else pendingDelete = cat
                    },
                )
            }
        }
    }

    if (showAdd) {
        AddCategorySheet(
            existing = list.map { it.name },
            onAdd = { name, tone, iconKey ->
                viewModel.addCategory(name, isExpense, tone, iconKey)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    pendingDelete?.let { cat ->
        ConfirmDialog(
            title = "Delete “${cat.name}”?",
            message = "This category will be removed from the picker. This can't be undone.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { viewModel.deleteCategory(cat.name, cat.isExpense); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
    blockedDelete?.let { cat ->
        ConfirmDialog(
            title = "Can't delete “${cat.name}”",
            message = "This category has a budget or transactions on it. Remove those first, then delete the category.",
            confirmText = "Got it",
            destructive = false,
            onConfirm = { blockedDelete = null },
            onDismiss = { blockedDelete = null },
        )
    }
}

@Composable
private fun CategoryManageRow(
    cat: CategoryEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val tone = CategoryTone.from(cat.tone)
    val icon = ICON_REGISTRY.toMap()[cat.iconKey] ?: CategoryVisuals.icon(cat.name)
    EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(tone = tone, icon = icon, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Text(cat.name, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            MoveButton(Lucide.ChevronUp, enabled = canMoveUp, contentDescription = "Move ${cat.name} up", onClick = onMoveUp)
            Spacer(Modifier.width(4.dp))
            MoveButton(Lucide.ChevronDown, enabled = canMoveDown, contentDescription = "Move ${cat.name} down", onClick = onMoveDown)
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(c.tintRed).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) { Icon(Lucide.Trash2, "Delete ${cat.name}", tint = c.catRed, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun MoveButton(icon: ImageVector, enabled: Boolean, contentDescription: String, onClick: () -> Unit) {
    val c = MaterialTheme.efColors
    Box(
        modifier = Modifier
            .size(34.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(c.surfaceSunken)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = c.textSecondary, modifier = Modifier.size(16.dp)) }
}

/** Name + tone + icon picker for a brand-new category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategorySheet(
    existing: List<String>,
    onAdd: (String, CategoryTone, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(CategoryTone.BLUE) }
    var iconKey by remember { mutableStateOf(ICON_REGISTRY.first().first) }
    val icon = ICON_REGISTRY.toMap()[iconKey] ?: ICON_REGISTRY.first().second
    val trimmed = name.trim()
    val duplicate = existing.any { it.equals(trimmed, ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surfaceCard) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text("New Category", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                CategoryIcon(tone = tone, icon = icon, size = 48.dp, cornerRadius = 15.dp)
                Column(Modifier.weight(1f)) {
                    EFTextField(name, { name = it }, placeholder = "Category name")
                    if (duplicate) {
                        Spacer(Modifier.height(4.dp))
                        Text("A category with this name already exists.", color = c.moneyOut, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("Color", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            TonePickerRow(selected = tone, onSelect = { tone = it })
            Spacer(Modifier.height(20.dp))

            Text("Icon", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ICON_REGISTRY.chunked(6).forEach { rowIcons ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowIcons.forEach { (key, ic) ->
                            IconPickerCell(
                                icon = ic, tone = tone, selected = key == iconKey,
                                modifier = Modifier.weight(1f), onClick = { iconKey = key },
                            )
                        }
                        repeat(6 - rowIcons.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            EFButton(
                text = "Add Category",
                onClick = { if (trimmed.isNotEmpty() && !duplicate) onAdd(trimmed, tone, iconKey) },
                fullWidth = true,
                enabled = trimmed.isNotEmpty() && !duplicate,
            )
        }
    }
}

/** Small confirm/info dialog matching the app's rounded-card dialog style. */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.efColors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surfaceCard).padding(24.dp),
        ) {
            Text(title, color = c.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = c.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(20.dp))
            if (destructive) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EFButton(text = "Cancel", onClick = onDismiss, variant = EFButtonVariant.Secondary, modifier = Modifier.weight(1f))
                    EFButton(
                        text = confirmText, onClick = onConfirm, variant = EFButtonVariant.Secondary,
                        contentColorOverride = c.moneyOut, modifier = Modifier.weight(1f),
                    )
                }
            } else {
                EFButton(text = confirmText, onClick = onConfirm, fullWidth = true)
            }
        }
    }
}
