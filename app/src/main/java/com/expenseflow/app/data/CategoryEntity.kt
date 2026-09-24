package com.expenseflow.app.data

import androidx.room.Entity

/**
 * A user-facing category shown in the Add Transaction picker (and everywhere else a
 * category needs a name/tone/glyph). Unlike [CategoryMetaEntity] — which only overrides
 * how an *existing* category is displayed on the Report screen — this table is the
 * category itself: its name, color, icon and position in the picker. Seeded once with
 * [DEFAULT_CATEGORIES]; from then on the user can add, reorder or delete rows.
 *
 * Keyed on (name, isExpense) rather than an id so expense and income can each have a
 * category with the same name without colliding.
 */
@Entity(tableName = "categories", primaryKeys = ["name", "isExpense"])
data class CategoryEntity(
    val name: String,
    val isExpense: Boolean,
    val tone: String,     // a CategoryTone name, e.g. "BLUE"
    val iconKey: String,  // key into the shared icon registry (see ui/components/Categories.kt)
    val sortOrder: Int,
)

/** First-run seed data — mirrors the app's original hardcoded category lists. */
val DEFAULT_CATEGORIES: List<CategoryEntity> = listOf(
    CategoryEntity("Food", true, "BLUE", "UtensilsCrossed", 0),
    CategoryEntity("Shopping", true, "ORANGE", "ShoppingBag", 1),
    CategoryEntity("Stationery", true, "PURPLE", "PencilRuler", 2),
    CategoryEntity("Transport", true, "GREEN", "Car", 3),
    CategoryEntity("Bills", true, "RED", "ReceiptText", 4),
    CategoryEntity("Health", true, "PINK", "HeartPulse", 5),

    CategoryEntity("Salary", false, "GREEN", "Banknote", 0),
    CategoryEntity("Freelance", false, "BLUE", "Laptop", 1),
    CategoryEntity("Gift", false, "RED", "Gift", 2),
    CategoryEntity("Other", false, "BLUE", "PiggyBank", 3),
)
