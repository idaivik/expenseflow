package com.expenseflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-category display overrides edited from the Report screen. Kept separate
 * from [TransactionEntity] so editing a category (rename / re-icon / adjust its
 * shown total) is non-destructive: the underlying transactions are untouched and
 * the override can be cleared by removing the row.
 *
 * A `null` field means "no override — fall back to the derived value" (the real
 * category name, its default glyph, or the summed amount).
 */
@Entity(tableName = "category_meta")
data class CategoryMetaEntity(
    @PrimaryKey val category: String,   // the original category name — the grouping key
    val displayName: String? = null,    // renamed label
    val iconKey: String? = null,        // key into the Report icon registry
    val amountOverride: Double? = null, // a manually-set total that replaces the sum
)
