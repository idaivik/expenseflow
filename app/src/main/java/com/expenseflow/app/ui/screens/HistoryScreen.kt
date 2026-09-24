package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.components.ScreenTopBar
import com.expenseflow.app.ui.components.TransactionRow
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.ExpenseViewModel

@Composable
fun HistoryScreen(viewModel: ExpenseViewModel, onBack: () -> Unit) {
    val c = MaterialTheme.efColors
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.filteredTransactions.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(c.bgApp)) {
        ScreenTopBar("History", onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            EFTextField(query, { viewModel.setSearchQuery(it) }, placeholder = "Search transactions", leadingIcon = Lucide.Search)
            Spacer(Modifier.height(12.dp))
        }
        if (results.isEmpty()) {
            Text(
                if (query.isBlank()) "No transactions yet." else "No matches for \"$query\".",
                color = c.textMuted, fontSize = 14.sp,
                modifier = Modifier.fillMaxSize().padding(top = 60.dp), textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
                items(results) { t ->
                    TransactionRow(
                        title = t.title, category = t.category, time = t.time, date = t.date,
                        amount = t.amount, isExpense = t.isExpense, currencySymbol = currency, isEdited = t.isEdited,
                    )
                }
            }
        }
    }
}
