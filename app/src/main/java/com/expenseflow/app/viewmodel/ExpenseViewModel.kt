package com.expenseflow.app.viewmodel

import com.composables.icons.lucide.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expenseflow.app.data.TransactionEntity
import com.expenseflow.app.data.TransactionRepository
import com.expenseflow.app.data.FrankfurterApi
import com.expenseflow.app.data.SettingsRepository
import com.expenseflow.app.model.Transaction
import com.expenseflow.app.data.BudgetEntity
import com.expenseflow.app.data.BillEntity
import com.expenseflow.app.data.PlannedExpenseEntity
import com.expenseflow.app.data.GoalEntity
import com.expenseflow.app.data.CategoryEntity
import com.expenseflow.app.data.CategoryMetaEntity
import com.expenseflow.app.data.DEFAULT_CATEGORIES
import com.expenseflow.app.data.FirebaseManager
import com.google.firebase.auth.FirebaseUser
import com.expenseflow.app.model.BudgetCategory
import com.expenseflow.app.model.DaySpending
import com.expenseflow.app.model.WeeklyStats
import com.expenseflow.app.model.MonthlyPlanStats
import com.expenseflow.app.model.PlannedSpend
import com.expenseflow.app.model.computeMonthlyPlanStats
import com.expenseflow.app.model.SpendingInsight
import com.expenseflow.app.model.InsightSeverity
import com.expenseflow.app.model.BudgetAlert
import com.expenseflow.app.ui.components.CategoryVisuals
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.R
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpenseViewModel(
    private val repository: TransactionRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    /**
     * Called when a user signs in. Pulls that account's private data down from
     * Firestore into the local cache, and fills the profile from their Auth
     * account (name / email) plus the Firestore profile document (region / phone).
     * A brand-new account has nothing in the cloud, so the app simply starts empty.
     */
    fun onSignedIn(user: FirebaseUser) {
        _userName.value = user.displayName ?: user.email?.substringBefore("@") ?: "User"
        _userEmail.value = user.email ?: ""
        _userPhone.value = user.phoneNumber ?: ""
        viewModelScope.launch {
            try {
                repository.syncFromCloud()
                // A brand-new account's cloud data (and thus the local cache after the
                // sync above) has no categories yet — seed the defaults so the picker
                // isn't empty.
                repository.seedDefaultCategoriesIfEmpty()
                FirebaseManager.loadUserProfile()?.let { profile ->
                    if (profile.region.isNotBlank()) _userRegion.value = profile.region
                    if (_userPhone.value.isBlank() && profile.phone.isNotBlank()) {
                        _userPhone.value = profile.phone
                    }
                }
            } catch (e: Exception) {
                // Offline or a transient Firestore error: keep whatever is cached
                // locally rather than wiping it.
            }
        }
    }

    /** Called on sign-out: clear the local cache and profile so the next account starts clean. */
    fun onSignedOut() {
        viewModelScope.launch { repository.clearAllLocal() }
        _userName.value = ""
        _userEmail.value = ""
        _userPhone.value = ""
        _userRegion.value = ""
    }

    // Display currency symbol, derived from the persisted setting — single source of
    // truth. Started eagerly so `.value` is always current (it's read directly when
    // posting budget-alert notifications, even with no active UI subscriber).
    val currencySymbol: StateFlow<String> = settingsRepo.settings
        .map { it.currencySymbol }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "$")

    // What the transactions list totals show ("expense" | "income" | "net").
    val transactionDisplayMode: StateFlow<String> = settingsRepo.settings
        .map { it.transactionDisplayMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "expense")

    // Status of an in-flight currency-exchange conversion (Settings screen).
    private val _exchangeState = MutableStateFlow<CurrencyExchangeState>(CurrencyExchangeState.Idle)
    val exchangeState: StateFlow<CurrencyExchangeState> = _exchangeState.asStateFlow()

    // Profile fields start empty; they're populated from the signed-in account in
    // onSignedIn(). No hardcoded placeholder identity.
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userPhone = MutableStateFlow("")
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _userRegion = MutableStateFlow("")
    val userRegion: StateFlow<String> = _userRegion.asStateFlow()

    /**
     * Switch the app's display currency and convert every stored amount into it
     * using the live Frankfurter exchange rate. The currency setting is only
     * updated after the conversion succeeds, so the symbol and the numbers always
     * stay consistent. On a network failure nothing is changed and an error is
     * surfaced via [exchangeState].
     */
    fun changeCurrency(toCode: String) = viewModelScope.launch {
        val fromCode = settingsRepo.settings.first().currencyCode
        if (fromCode == toCode) return@launch
        _exchangeState.value = CurrencyExchangeState.Loading
        val rate = FrankfurterApi.fetchRate(
            FrankfurterApi.isoCode(fromCode),
            FrankfurterApi.isoCode(toCode),
        )
        if (rate == null) {
            _exchangeState.value = CurrencyExchangeState.Error(
                "Couldn't reach the exchange service. Check your connection and try again."
            )
            return@launch
        }
        repository.convertAllAmounts(rate)
        settingsRepo.setCurrency(toCode)
        _exchangeState.value = CurrencyExchangeState.Idle
    }

    /** Dismiss a lingering exchange error (e.g. when the user closes the dialog). */
    fun clearExchangeError() {
        _exchangeState.value = CurrencyExchangeState.Idle
    }

    /**
     * User-editable profile fields (name + region). The name is written to the
     * Firebase Auth profile and the region to the Firestore user document, so
     * both persist across restarts and follow the account to another device.
     * Phone editing is not yet available.
     */
    fun updateNameAndRegion(name: String, region: String) {
        name.trim().takeIf { it.isNotEmpty() }?.let {
            _userName.value = it
            FirebaseManager.updateDisplayName(it)
        }
        region.trim().takeIf { it.isNotEmpty() }?.let { _userRegion.value = it }
        FirebaseManager.saveUserProfile(_userRegion.value, _userPhone.value)
    }

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .map { entities ->
            entities.map { it.toModel() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = repository.totalIncome
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = repository.totalExpense
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balance: StateFlow<Double> = repository.allTransactions
        .map { entities ->
            val income = entities.filter { !it.isExpense }.sumOf { it.amount }
            val expense = entities.filter { it.isExpense }.sumOf { it.amount }
            income - expense
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val budgetCategories: StateFlow<List<BudgetCategory>> = combine(
        repository.allTransactions,
        repository.allBudgets
    ) { transactions, budgets ->
        val now = Calendar.getInstance()
        val currentMonthTransactions = transactions.filter { entity ->
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = sdf.parse(entity.date) ?: return@filter false
                val calendar = Calendar.getInstance().apply { time = date }
                calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                        calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            } catch (e: Exception) {
                false
            }
        }

        budgets.map { budget ->
            val spent = currentMonthTransactions
                .filter { it.isExpense && it.category == budget.category }
                .sumOf { it.amount }
            
            BudgetCategory(
                name = budget.category,
                spent = spent,
                total = budget.budgetLimit,
                iconResId = getIconResIdByName(budget.category),
                color = getCategoryColor(budget.category)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBudgetLimit: StateFlow<Double> = repository.totalBudgetLimit
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // All expenses this month across every category — not just ones with a budget
    // limit set — so the home budget card reflects true monthly spending.
    val totalSpentThisMonth: StateFlow<Double> = repository.allTransactions
        .map { transactions -> transactions.filter { it.isExpense && isInCurrentMonth(it.date) }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ---- Spending plan: expenses the user has committed to but not yet made ----

    /** Every planned expense, earliest date first. */
    val plannedExpenses: StateFlow<List<PlannedExpenseEntity>> = repository.allPlannedExpenses
        .map { list -> list.sortedWith(compareBy({ parseDate(it.date)?.time ?: Long.MAX_VALUE }, { it.id })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Live "per day to stay on track" numbers for this month — see [calculateMonthlyPlanStats]. */
    val monthlyPlanStats: StateFlow<MonthlyPlanStats> = combine(
        repository.allTransactions,
        totalBudgetLimit,
        repository.allPlannedExpenses,
    ) { transactions, budget, planned ->
        calculateMonthlyPlanStats(transactions, budget, planned)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyPlanStats())

    fun addPlannedExpense(title: String, category: String, amount: Double, date: String) = viewModelScope.launch {
        repository.insertPlannedExpense(
            PlannedExpenseEntity(title = title, category = category, amount = amount, date = date)
        )
    }

    fun updatePlannedExpense(planned: PlannedExpenseEntity) = viewModelScope.launch {
        repository.updatePlannedExpense(planned)
    }

    fun deletePlannedExpense(planned: PlannedExpenseEntity) = viewModelScope.launch {
        // Deleting a plan that was already logged leaves the real expense in place —
        // the money was genuinely spent; only the plan goes away.
        repository.deletePlannedExpense(planned)
    }

    /**
     * Turn a plan into a real expense on its planned date. The plan stays in the
     * calendar as a logged entry (so it reads as "done" rather than vanishing) and
     * stops being reserved out of the budget, since the spend now counts as actual.
     */
    fun logPlannedExpense(planned: PlannedExpenseEntity) = viewModelScope.launch {
        if (planned.loggedTxnId != 0) return@launch
        val now = Calendar.getInstance().time
        val transaction = TransactionEntity(
            title = planned.title.ifBlank { planned.category },
            category = planned.category,
            amount = planned.amount,
            date = planned.date,
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now),
            iconName = planned.category,
            iconColor = getCategoryColor(planned.category).toArgb().toLong(),
            isExpense = true,
        )
        val newId = repository.insert(transaction)
        repository.updatePlannedExpense(planned.copy(loggedTxnId = newId.toInt()))
        checkBudgetThreshold(planned.category, planned.amount, planned.date)
    }

    /** Reverse [logPlannedExpense]: delete the expense it created and re-reserve the plan. */
    fun unlogPlannedExpense(planned: PlannedExpenseEntity) = viewModelScope.launch {
        if (planned.loggedTxnId != 0) {
            repository.allTransactions.first().find { it.id == planned.loggedTxnId }?.let { repository.delete(it) }
        }
        repository.updatePlannedExpense(planned.copy(loggedTxnId = 0))
    }

    // ---- Plan: goals, savings-budgets, bills ----
    val goals: StateFlow<List<GoalEntity>> = repository.allGoals
        .map { list -> list.filter { it.kind == "goal" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsBudgets: StateFlow<List<GoalEntity>> = repository.allGoals
        .map { list -> list.filter { it.kind == "budget" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<BillEntity>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Expense categories that already have at least one logged transaction. Drives
     * the Plan screen's "merge into the existing Bills category or create a new one"
     * prompt when a bill is marked as paid.
     */
    val expenseCategories: StateFlow<Set<String>> = repository.allTransactions
        .map { list -> list.filter { it.isExpense }.map { it.category }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ---- Report: per-category display overrides (rename / icon / shown amount) ----
    val categoryMeta: StateFlow<Map<String, CategoryMetaEntity>> = repository.allCategoryMeta
        .map { list -> list.associateBy { it.category } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ---- User-managed categories: the Add Transaction picker, in their saved order ----

    // Eager (not WhileSubscribed) so CategoryVisuals' registry — read by screens that
    // never collect `categories` directly, like ledger rows — stays current even
    // when nothing is currently observing this flow.
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .onEach { CategoryVisuals.syncRegistry(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expenseCategoryList: StateFlow<List<CategoryEntity>> = categories
        .map { list -> list.filter { it.isExpense } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategoryList: StateFlow<List<CategoryEntity>> = categories
        .map { list -> list.filterNot { it.isExpense } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Category names that can't be deleted: either a budget limit is set for them, or
     * at least one transaction has been logged under them. Checked by the Manage
     * Categories screen before it lets a delete go through.
     */
    val categoriesInUse: StateFlow<Set<String>> = combine(repository.allTransactions, repository.allBudgets) { txs, budgets ->
        txs.map { it.category }.toSet() + budgets.map { it.category }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Add a brand-new category at the end of its type's list. */
    fun addCategory(name: String, isExpense: Boolean, tone: CategoryTone, iconKey: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@launch
        val siblings = repository.allCategories.first().filter { it.isExpense == isExpense }
        if (siblings.any { it.name.equals(trimmed, ignoreCase = true) }) return@launch
        val nextOrder = (siblings.maxOfOrNull { it.sortOrder } ?: -1) + 1
        repository.upsertCategory(CategoryEntity(trimmed, isExpense, tone.name, iconKey, nextOrder))
    }

    /** Caller is responsible for checking [categoriesInUse] first and confirming with the user. */
    fun deleteCategory(name: String, isExpense: Boolean) = viewModelScope.launch {
        repository.deleteCategory(name, isExpense)
    }

    /** Persist a new permanent order for one type's category list. */
    fun reorderCategories(isExpense: Boolean, orderedNames: List<String>) = viewModelScope.launch {
        val byName = repository.allCategories.first().filter { it.isExpense == isExpense }.associateBy { it.name }
        val reordered = orderedNames.mapIndexedNotNull { index, name -> byName[name]?.copy(sortOrder = index) }
        repository.reorderCategories(reordered)
    }

    /**
     * Persist (or clear) a category's Report overrides. Passing all-null removes
     * the row so the category falls back to its real name, default glyph and
     * summed amount.
     */
    fun saveCategoryEdit(
        category: String,
        displayName: String?,
        iconKey: String?,
        amountOverride: Double?,
    ) = viewModelScope.launch {
        if (displayName == null && iconKey == null && amountOverride == null) {
            repository.deleteCategoryMeta(category)
        } else {
            repository.upsertCategoryMeta(
                CategoryMetaEntity(
                    category = category,
                    displayName = displayName,
                    iconKey = iconKey,
                    amountOverride = amountOverride,
                )
            )
        }
    }

    fun saveGoal(goal: GoalEntity) = viewModelScope.launch {
        if (goal.id == 0) repository.insertGoal(goal) else repository.updateGoal(goal)
    }

    fun deleteGoal(goal: GoalEntity) = viewModelScope.launch { repository.deleteGoal(goal) }

    fun setGoalCompleted(goal: GoalEntity, completed: Boolean) = viewModelScope.launch {
        repository.updateGoal(goal.copy(completed = completed, saved = if (completed) goal.target else goal.saved))
    }

    fun saveBill(bill: BillEntity) = viewModelScope.launch {
        if (bill.id == 0) repository.insertBill(bill) else repository.updateBill(bill)
    }

    fun deleteBill(bill: BillEntity) = viewModelScope.launch { repository.deleteBill(bill) }

    /**
     * Mark a bill as paid and log its amount as an expense so it shows up in the
     * Report. [category] is resolved on the Plan screen: either the existing
     * "Bills" category (merge) or a fresh, bill-named category (create another).
     * The new transaction's id is stored on the bill so un-marking can reverse it.
     */
    fun markBillPaid(bill: BillEntity, category: String) = viewModelScope.launch {
        val now = Calendar.getInstance().time
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
        val transaction = TransactionEntity(
            title = bill.name,
            category = category,
            amount = bill.amount,
            date = date,
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now),
            iconName = category,
            iconColor = getCategoryColor(category).toArgb().toLong(),
            isExpense = true,
        )
        val newId = repository.insert(transaction)
        repository.updateBill(bill.copy(paid = true, paidTxnId = newId.toInt()))
        checkBudgetThreshold(category, bill.amount, date)
    }

    /**
     * Un-mark a paid bill and reverse the expense it logged, if any. Deletes the
     * transaction recorded in [BillEntity.paidTxnId] and clears both flags. Bills
     * marked paid before this link existed (paidTxnId == 0) simply flip back.
     */
    fun unmarkBillPaid(bill: BillEntity) = viewModelScope.launch {
        if (bill.paidTxnId != 0) {
            repository.allTransactions.first().find { it.id == bill.paidTxnId }?.let { repository.delete(it) }
        }
        repository.updateBill(bill.copy(paid = false, paidTxnId = 0))
    }

    fun deleteTransaction(id: String) = viewModelScope.launch {
        repository.allTransactions.first().find { it.id.toString() == id }?.let { repository.delete(it) }
    }

    // ---- History search ----
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    val filteredTransactions: StateFlow<List<Transaction>> = combine(transactions, _searchQuery) { list, q ->
        if (q.isBlank()) list
        else list.filter { it.title.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * A pre-written spending summary that is picked from premade templates based
     * purely on the data stored in the database (monthly category spending vs.
     * the budget limits). Covers every case: no spending, over budget, close to
     * budget, and healthy spending.
     */
    val spendingInsight: StateFlow<SpendingInsight> = combine(
        budgetCategories,
        currencySymbol
    ) { categories, currency ->
        buildSpendingInsight(categories, currency)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SpendingInsight("Add a transaction to see your spending summary.", InsightSeverity.NEUTRAL)
    )

    private fun buildSpendingInsight(
        categories: List<BudgetCategory>,
        currency: String
    ): SpendingInsight {
        val totalSpent = categories.sumOf { it.spent }

        // Case 1: nothing spent yet this month.
        if (totalSpent <= 0.0) {
            return SpendingInsight(
                "You haven't spent anything this month yet. Great start on saving! 🎉",
                InsightSeverity.POSITIVE
            )
        }

        // Case 2: a category is over its budget limit.
        val overBudget = categories
            .filter { it.total > 0 && it.spent > it.total }
            .maxByOrNull { it.spent - it.total }
        if (overBudget != null) {
            val over = (overBudget.spent - overBudget.total).toInt()
            return SpendingInsight(
                "You have spent too much money on ${overBudget.name}! " +
                    "You're $currency$over over your ${overBudget.name} budget.",
                InsightSeverity.DANGER
            )
        }

        // Case 3: a category is close to its budget limit (>= 80% used).
        val nearLimit = categories
            .filter { it.total > 0 && it.spent >= it.total * 0.8 }
            .maxByOrNull { it.spent / it.total }
        if (nearLimit != null) {
            val percent = ((nearLimit.spent / nearLimit.total) * 100).toInt()
            return SpendingInsight(
                "Heads up! You've already used $percent% of your ${nearLimit.name} budget.",
                InsightSeverity.WARNING
            )
        }

        // Case 4: healthy spending - highlight the top category.
        val topCategory = categories.filter { it.spent > 0 }.maxByOrNull { it.spent }
        if (topCategory != null) {
            return SpendingInsight(
                "Most of your spending went to ${topCategory.name} " +
                    "($currency${topCategory.spent.toInt()}). You're still within budget 👍",
                InsightSeverity.NEUTRAL
            )
        }

        return SpendingInsight(
            "You're doing great this month. Keep it up!",
            InsightSeverity.POSITIVE
        )
    }

    // ---- Notifications: synthesized from live data, dismissal persisted forever ----

    /** Ids the user has dismissed — persisted, so "cleared" survives app restarts. */
    private val dismissedNotificationIds: StateFlow<Set<String>> = settingsRepo.dismissedNotificationIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val allNotifications: StateFlow<List<NotificationItem>> = combine(
        budgetCategories, spendingInsight, transactions, goals, currencySymbol,
    ) { budgets, insight, txs, goalsList, currency ->
        buildNotifications(budgets, insight, txs, goalsList, currency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Live notifications minus whatever the user has permanently dismissed. */
    val notifications: StateFlow<List<NotificationItem>> = combine(allNotifications, dismissedNotificationIds) { all, dismissed ->
        all.filter { it.id !in dismissed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasNotifications: StateFlow<Boolean> = notifications
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Dismiss a single notification — persisted, so it never reappears once cleared. */
    fun dismissNotification(id: String) = viewModelScope.launch {
        settingsRepo.dismissNotifications(listOf(id))
    }

    /** Clear every currently-visible notification, permanently. */
    fun clearAllNotifications() = viewModelScope.launch {
        settingsRepo.dismissNotifications(notifications.value.map { it.id })
    }

    // Budget alert events (over-budget / near-limit). Emitted when a new expense
    // pushes a category's monthly spend across a threshold. The UI layer collects
    // these and posts a system notification.
    private val _budgetAlerts = MutableSharedFlow<BudgetAlert>(extraBufferCapacity = 4)
    val budgetAlerts: SharedFlow<BudgetAlert> = _budgetAlerts.asSharedFlow()

    private val _currentWeekOffset = MutableStateFlow(0)
    val currentWeekOffset: StateFlow<Int> = _currentWeekOffset.asStateFlow()

    fun moveWeek(offset: Int) {
        _currentWeekOffset.value += offset
    }

    val weeklyStats: StateFlow<WeeklyStats> = combine(
        repository.allTransactions,
        _currentWeekOffset
    ) { transactions, offset ->
        calculateWeeklyStats(transactions, offset)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyStats(0.0, emptyList(), ""))

    private fun calculateWeeklyStats(transactions: List<TransactionEntity>, offset: Int): WeeklyStats {
        val calendar = Calendar.getInstance()
        // Standardize to start of week (e.g. Monday)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, offset)
        
        // Zero out time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val weekDays = mutableListOf<DaySpending>()
        var totalSpent = 0.0
        
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val rangeSdf = SimpleDateFormat("MMM d", Locale.getDefault())
        
        val weekStartLabel = rangeSdf.format(calendar.time)
        
        val today = Calendar.getInstance()
        
        for (i in 0 until 7) {
            val currentDay = calendar.time
            val dateStr = sdf.format(currentDay)
            
            val daySpent = transactions
                .filter { it.isExpense && it.date == dateStr }
                .sumOf { it.amount }
            
            totalSpent += daySpent
            
            val dayNameSdf = SimpleDateFormat("EEE", Locale.getDefault())
            val isToday = today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) &&
                         today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            
            weekDays.add(DaySpending(dayNameSdf.format(currentDay), daySpent, isToday))
            
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val weekEndLabel = rangeSdf.format(calendar.time)
        
        return WeeklyStats(totalSpent, weekDays, "$weekStartLabel - $weekEndLabel")
    }

    /**
     * Gathers this month's spending and plans, then hands the arithmetic to
     * [computeMonthlyPlanStats] — the same function the daily-budget widget uses,
     * so the two can't drift apart.
     */
    private fun calculateMonthlyPlanStats(
        transactions: List<TransactionEntity>,
        monthlyBudget: Double,
        planned: List<PlannedExpenseEntity>,
    ): MonthlyPlanStats {
        val now = Calendar.getInstance()
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time)

        var spentThisMonth = 0.0
        var spentToday = 0.0
        transactions.forEach { txn ->
            if (!txn.isExpense) return@forEach
            if (isInCurrentMonth(txn.date)) spentThisMonth += txn.amount
            if (txn.date == todayStr) spentToday += txn.amount
        }

        val plannedThisMonth = planned.mapNotNull { p ->
            if (!isInCurrentMonth(p.date)) return@mapNotNull null
            dayOfMonth(p.date)?.let { PlannedSpend(it, p.amount, p.loggedTxnId != 0) }
        }

        return computeMonthlyPlanStats(
            monthlyBudget = monthlyBudget,
            spentThisMonth = spentThisMonth,
            spentToday = spentToday,
            plannedThisMonth = plannedThisMonth,
            todayDay = now.get(Calendar.DAY_OF_MONTH),
            daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH),
        )
    }

    /** Parses a "dd/MM/yyyy" string, or null if it isn't one. */
    private fun parseDate(dateStr: String): Date? = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr)
    } catch (e: Exception) {
        null
    }

    /** Day-of-month for a "dd/MM/yyyy" string, or null if it isn't one. */
    private fun dayOfMonth(dateStr: String): Int? =
        parseDate(dateStr)?.let { Calendar.getInstance().apply { time = it }.get(Calendar.DAY_OF_MONTH) }

    private fun getCategoryColor(name: String): Color {
        return when (name) {
            "Food" -> Color(0xFF2B5CFF)
            "Transport" -> Color(0xFFF59E0B)
            "Shopping" -> Color(0xFFEC4899)
            "Entertainment" -> Color(0xFF8B5CF6)
            "Bills" -> Color(0xFF10B981)
            "Health" -> Color(0xFFEF4444)
            "Salary" -> Color(0xFF16A34A)
            "Gift" -> Color(0xFFEF4444)
            "Other" -> Color(0xFF6B7280)
            else -> Color(0xFF2B5CFF)
        }
    }

    fun getFilteredTransactions(timeframe: String, transactions: List<Transaction>): List<Transaction> {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val now = Calendar.getInstance()
        
        return transactions.filter { transaction ->
            try {
                val date = sdf.parse(transaction.date) ?: return@filter false
                val calendar = Calendar.getInstance().apply { time = date }
                
                when (timeframe) {
                    "Week" -> {
                        // Standardize both to start of week to compare
                        val startOfWeek = now.clone() as Calendar
                        startOfWeek.firstDayOfWeek = Calendar.MONDAY
                        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
                        startOfWeek.set(Calendar.MINUTE, 0)
                        startOfWeek.set(Calendar.SECOND, 0)
                        startOfWeek.set(Calendar.MILLISECOND, 0)

                        val endOfWeek = startOfWeek.clone() as Calendar
                        endOfWeek.add(Calendar.DAY_OF_YEAR, 7)

                        calendar.timeInMillis >= startOfWeek.timeInMillis && 
                                calendar.timeInMillis < endOfWeek.timeInMillis
                    }
                    "Month" -> {
                        calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    }
                    "Year" -> {
                        calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    }
                    else -> true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    fun updateTransaction(
        id: String,
        title: String,
        category: String,
        amount: Double,
        date: String,
        isExpense: Boolean
    ) {
        viewModelScope.launch {
            val existing = repository.allTransactions.first().find { it.id.toString() == id }
            if (existing != null) {
                repository.insert(
                    existing.copy(
                        title = title,
                        category = category,
                        amount = amount,
                        date = date,
                        isExpense = isExpense,
                        isEdited = true,
                        iconName = category,
                        iconColor = getCategoryColor(category).toArgb().toLong()
                    )
                )
            }
        }
    }

    fun addTransaction(
        title: String,
        category: String,
        amount: Double,
        date: String,
        time: String,
        iconName: String,
        iconColor: Color,
        isExpense: Boolean
    ) {
        viewModelScope.launch {
            val transaction = TransactionEntity(
                title = title,
                category = category,
                amount = amount,
                date = date,
                time = time,
                iconName = iconName,
                iconColor = iconColor.toArgb().toLong(),
                isExpense = isExpense
            )
            // repository.insert also mirrors the transaction to Firestore for the
            // signed-in user, using the Room-generated id as the cloud document key.
            repository.insert(transaction)

            // Check whether this expense crossed a budget threshold and, if so,
            // emit an alert so the UI can post a notification.
            if (isExpense) {
                checkBudgetThreshold(category, amount, date)
            }
        }
    }

    /**
     * Recomputes the category's monthly spending after an expense was added and
     * emits a [BudgetAlert] if this transaction is the one that crossed the
     * over-budget (100%) or near-limit (90%) threshold. Comparing the spend
     * before and after the transaction avoids re-alerting on every new expense
     * once a threshold has already been passed.
     */
    private suspend fun checkBudgetThreshold(category: String, amount: Double, date: String) {
        if (!isInCurrentMonth(date)) return

        val budget = repository.allBudgets.first().find { it.category == category } ?: return
        val limit = budget.budgetLimit
        if (limit <= 0.0) return

        val spentAfter = repository.allTransactions.first()
            .filter { it.isExpense && it.category == category && isInCurrentMonth(it.date) }
            .sumOf { it.amount }
        val spentBefore = spentAfter - amount

        val warnThreshold = limit * 0.9

        when {
            spentBefore < limit && spentAfter >= limit -> {
                _budgetAlerts.emit(
                    BudgetAlert(category, spentAfter, limit, isOverBudget = true)
                )
            }
            spentBefore < warnThreshold && spentAfter >= warnThreshold -> {
                _budgetAlerts.emit(
                    BudgetAlert(category, spentAfter, limit, isOverBudget = false)
                )
            }
        }
    }

    private fun isInCurrentMonth(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return false
            val cal = Calendar.getInstance().apply { time = date }
            val now = Calendar.getInstance()
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        } catch (e: Exception) {
            false
        }
    }

    fun updateBudget(category: String, limit: Double) {
        viewModelScope.launch {
            repository.insertBudget(BudgetEntity(category, limit))
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    private fun TransactionEntity.toModel(): Transaction {
        return Transaction(
            id = id.toString(),
            title = title,
            category = category,
            amount = amount,
            date = date,
            time = time,
            iconResId = getIconResIdByName(iconName),
            iconBg = Color(iconColor.toInt()).copy(alpha = 0.12f),
            isExpense = isExpense,
            isEdited = isEdited
        )
    }

    private fun getIconResIdByName(name: String): Int {
        return when (name) {
            "Food" -> R.drawable._cup
            "Transport" -> R.drawable._activity_1
            "Shopping" -> R.drawable._tag
            "Entertainment" -> R.drawable._ps5_2
            "Bills" -> R.drawable._numerical_star
            "Health" -> R.drawable._activity_2
            "Salary" -> R.drawable._award_1
            "Gift" -> R.drawable._gift_2
            "Other" -> R.drawable._category
            else -> R.drawable._category
        }
    }
}

/** State of the currency-conversion request kicked off from Settings. */
sealed interface CurrencyExchangeState {
    object Idle : CurrencyExchangeState
    object Loading : CurrencyExchangeState
    data class Error(val message: String) : CurrencyExchangeState
}

/** A single entry in the notification center, and the dot on the Home bell icon. */
data class NotificationItem(
    val id: String,
    val tone: CategoryTone,
    val icon: ImageVector,
    val title: String,
    val body: String,
)

/**
 * A milestone notification for a savings goal. Purposely amount-blind: it never
 * states the amount saved or the target, only how far along the goal is, so the
 * numbers stay private even when notifications are glanced at on a lock screen.
 */
private fun goalNotif(g: GoalEntity): NotificationItem? {
    val pct = when {
        g.completed -> 100
        g.target > 0 -> ((g.saved / g.target) * 100).toInt().coerceIn(0, 100)
        else -> 0
    }
    return when {
        pct >= 100 -> NotificationItem("goal-done-${g.id}", CategoryTone.GREEN, Lucide.Trophy,
            "${g.name} goal reached! 🎉", "You've fully funded ${g.name}. Time to celebrate!")
        pct >= 90 -> NotificationItem("goal-90-${g.id}", CategoryTone.ORANGE, Lucide.Target,
            "Almost there on ${g.name}", "You're so close to reaching ${g.name} — one last push!")
        pct >= 75 -> NotificationItem("goal-75-${g.id}", CategoryTone.BLUE, Lucide.TrendingUp,
            "${g.name} is coming along nicely", "You've saved most of the way toward ${g.name}. Keep it up!")
        pct >= 50 -> NotificationItem("goal-50-${g.id}", CategoryTone.PURPLE, Lucide.Goal,
            "Halfway to ${g.name}", "You're past the halfway mark on ${g.name}. Great momentum!")
        pct > 0 -> NotificationItem("goal-start-${g.id}", CategoryTone.PURPLE, Lucide.PiggyBank,
            "${g.name} is underway", "You've made a start on ${g.name}. Every bit counts!")
        else -> null
    }
}

/** Builds the live notification list from budgets/insight/transactions/goals (see [ExpenseViewModel.notifications]). */
private fun buildNotifications(
    budgets: List<BudgetCategory>,
    insight: SpendingInsight,
    transactions: List<Transaction>,
    goals: List<GoalEntity>,
    currency: String,
): List<NotificationItem> = buildList {
    budgets.filter { it.total > 0 && it.spent > it.total }.forEach {
        add(NotificationItem("over-${it.name}", CategoryTone.RED, Lucide.TriangleAlert, "${it.name} budget exceeded",
            "You've spent $currency${it.spent.toInt()} of your $currency${it.total.toInt()} ${it.name} budget."))
    }
    budgets.filter { it.total > 0 && it.spent in (it.total * 0.8)..it.total }.forEach {
        val pct = ((it.spent / it.total) * 100).toInt()
        add(NotificationItem("near-${it.name}", CategoryTone.ORANGE, Lucide.ChartPie, "${it.name} budget almost gone",
            "You've used $pct% of your $currency${it.total.toInt()} ${it.name} budget."))
    }
    goals.forEach { g -> goalNotif(g)?.let { add(it) } }
    add(NotificationItem("insight", CategoryTone.PURPLE, Lucide.Sparkles, "Your insight is ready", insight.message))
    transactions.firstOrNull { !it.isExpense }?.let {
        add(NotificationItem("income-${it.id}", CategoryTone.GREEN, Lucide.Banknote, "${it.title} received",
            "+$currency${it.amount.toInt()} just landed in your account."))
    }
}

class ExpenseViewModelFactory(
    private val repository: TransactionRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, settingsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
