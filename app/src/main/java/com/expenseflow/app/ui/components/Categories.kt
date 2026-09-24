package com.expenseflow.app.ui.components

import com.composables.icons.lucide.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.expenseflow.app.data.CategoryEntity
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.efColors

/**
 * Single source of truth mapping a category name to its design "tone" and its
 * Lucide glyph (the icon set used in the Claude Design mockups). Keeps
 * icons/colors consistent across the ledger, charts, add sheet and notifications.
 *
 * [syncRegistry] is called by [com.expenseflow.app.viewmodel.ExpenseViewModel] whenever
 * the user-managed categories table changes, so [tone]/[icon] resolve custom
 * categories (arbitrary name/tone/icon) the same way they resolve the built-in ones —
 * every screen that already calls `CategoryVisuals.tone(name)` / `.icon(name)` picks
 * this up automatically, with no per-screen wiring needed. [legacyTone]/[legacyIcon]
 * remain as the fallback for names that predate the categories table (e.g. "Groceries",
 * "Cafes", "Entertainment" — no longer seeded as defaults, but still valid on old
 * transactions) and as a safety net before the table has first loaded.
 */
object CategoryVisuals {
    private var registry by mutableStateOf<Map<String, CategoryEntity>>(emptyMap())

    fun syncRegistry(categories: List<CategoryEntity>) {
        registry = categories.associateBy { it.name }
    }

    fun tone(category: String): CategoryTone =
        registry[category]?.let { CategoryTone.from(it.tone) } ?: legacyTone(category)

    fun icon(category: String): ImageVector =
        registry[category]?.iconKey?.let { ICON_BY_KEY[it] } ?: legacyIcon(category)

    private fun legacyTone(category: String): CategoryTone = when (category) {
        "Food", "Groceries" -> CategoryTone.BLUE
        "Cafes" -> CategoryTone.PURPLE
        "Transport" -> CategoryTone.GREEN
        "Shopping" -> CategoryTone.ORANGE
        "Stationery" -> CategoryTone.PURPLE
        "Entertainment" -> CategoryTone.PURPLE
        "Bills" -> CategoryTone.RED
        "Health" -> CategoryTone.PINK
        "Salary" -> CategoryTone.GREEN
        "Freelance" -> CategoryTone.BLUE
        "Gift" -> CategoryTone.RED
        "Other" -> CategoryTone.BLUE
        else -> CategoryTone.BLUE
    }

    private fun legacyIcon(category: String): ImageVector = when (category) {
        "Food" -> Lucide.UtensilsCrossed
        "Groceries" -> Lucide.ShoppingCart
        "Cafes" -> Lucide.Coffee
        "Transport" -> Lucide.Car
        "Shopping" -> Lucide.ShoppingBag
        "Stationery" -> Lucide.PencilRuler
        "Entertainment" -> Lucide.Clapperboard
        "Bills" -> Lucide.ReceiptText
        "Health" -> Lucide.HeartPulse
        "Salary" -> Lucide.Banknote
        "Freelance" -> Lucide.Laptop
        "Gift" -> Lucide.Gift
        "Other" -> Lucide.PiggyBank
        else -> Lucide.LayoutGrid
    }
}

// Glyphs offered wherever a category needs an icon picker (Report's category edit,
// Manage Categories' add-category sheet). The String key is what gets persisted
// (CategoryMetaEntity.iconKey / CategoryEntity.iconKey) — keep keys stable.
val ICON_REGISTRY: List<Pair<String, ImageVector>> = listOf(
    "UtensilsCrossed" to Lucide.UtensilsCrossed,
    "Coffee" to Lucide.Coffee,
    "ShoppingCart" to Lucide.ShoppingCart,
    "ShoppingBag" to Lucide.ShoppingBag,
    "Car" to Lucide.Car,
    "Bus" to Lucide.Bus,
    "Fuel" to Lucide.Fuel,
    "Plane" to Lucide.Plane,
    "Clapperboard" to Lucide.Clapperboard,
    "Film" to Lucide.Film,
    "Music" to Lucide.Music,
    "Gamepad2" to Lucide.Gamepad2,
    "ReceiptText" to Lucide.ReceiptText,
    "HeartPulse" to Lucide.HeartPulse,
    "Dumbbell" to Lucide.Dumbbell,
    "Banknote" to Lucide.Banknote,
    "Laptop" to Lucide.Laptop,
    "Gift" to Lucide.Gift,
    "PiggyBank" to Lucide.PiggyBank,
    "House" to Lucide.House,
    "GraduationCap" to Lucide.GraduationCap,
    "BookOpen" to Lucide.BookOpen,
    "PencilRuler" to Lucide.PencilRuler,
    "Smartphone" to Lucide.Smartphone,
    "Wallet" to Lucide.Wallet,
    "CreditCard" to Lucide.CreditCard,
    "TreePalm" to Lucide.TreePalm,
    "Shirt" to Lucide.Shirt,
    "PawPrint" to Lucide.PawPrint,
    "Wrench" to Lucide.Wrench,
)
val EDIT_ICONS: List<ImageVector> = ICON_REGISTRY.map { it.second }
val ICON_BY_KEY: Map<String, ImageVector> = ICON_REGISTRY.toMap()
val KEY_BY_ICON: Map<ImageVector, String> = ICON_REGISTRY.associate { (k, v) -> v to k }

/** One selectable glyph in an icon picker grid. */
@Composable
fun IconPickerCell(
    icon: ImageVector,
    tone: CategoryTone,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.efColors
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.tint(tone) else c.surfaceInset)
            .then(if (selected) Modifier.border(1.5.dp, c.cat(tone), RoundedCornerShape(14.dp)) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (selected) c.cat(tone) else c.textSecondary, modifier = Modifier.size(20.dp))
    }
}

/** A row of the six category-tone swatches, for picking a custom category's color. */
@Composable
fun TonePickerRow(selected: CategoryTone, onSelect: (CategoryTone) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.efColors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CategoryTone.entries.forEach { tone ->
            val isSelected = tone == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(c.cat(tone))
                    .then(
                        if (isSelected) Modifier.border(2.dp, c.textPrimary, CircleShape)
                        else Modifier,
                    )
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(tone) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Icon(Lucide.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
