package com.expenseflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A savings target shown on the Plan screen. [kind] distinguishes a headline
 * "goal" (House by the Sea) from a smaller "budget" savings pot (Save for a Car).
 * Icon + color come from [iconKey]/[tone] (see PlanVisuals).
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val kind: String,            // "goal" | "budget"
    val name: String,
    val iconKey: String,
    val tone: String,
    val saved: Double,
    val target: Double,
    val targetDate: String = "",
    val completed: Boolean = false,
)
