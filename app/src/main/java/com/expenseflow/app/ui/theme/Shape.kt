package com.expenseflow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii — mirrors tokens/effects.css. */
object Radii {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp // default card
    val lg = 22.dp // large card / sheet
    val xl = 28.dp
    val pill = 999.dp
}

val ExpenseFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.xs),
    small = RoundedCornerShape(Radii.sm),
    medium = RoundedCornerShape(Radii.md),
    large = RoundedCornerShape(Radii.lg),
    extraLarge = RoundedCornerShape(Radii.xl),
)
