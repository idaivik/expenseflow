package com.expenseflow.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateHelper {
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Trigger refresh on all ExpenseFlow widgets across launcher screens.
     */
    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                ExpenseWidget().updateAll(appContext)
            } catch (_: Exception) {}
            try {
                QuickExpenseWidget().updateAll(appContext)
            } catch (_: Exception) {}
            try {
                DailyBudgetWidget().updateAll(appContext)
            } catch (_: Exception) {}
        }
    }
}
