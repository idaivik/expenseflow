package com.expenseflow.app.data

import android.content.Context
import com.expenseflow.app.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Single source of truth for the app's data. Room is the local cache the UI
 * observes; every mutation is also mirrored up to Firestore for the signed-in
 * user via [FirebaseManager]. On sign-in, [syncFromCloud] replaces the local
 * cache with that user's cloud data so each account only ever sees its own.
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val billDao: BillDao,
    private val plannedExpenseDao: PlannedExpenseDao,
    private val categoryMetaDao: CategoryMetaDao,
    private val categoryDao: CategoryDao,
    private val context: Context? = null,
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()
    val totalExpense: Flow<Double?> = transactionDao.getTotalExpense()

    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val totalBudgetLimit: Flow<Double?> = budgetDao.getTotalBudget()

    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()
    val allBills: Flow<List<BillEntity>> = billDao.getAllBills()
    val allPlannedExpenses: Flow<List<PlannedExpenseEntity>> = plannedExpenseDao.getAllPlannedExpenses()

    val allCategoryMeta: Flow<List<CategoryMetaEntity>> = categoryMetaDao.getAllCategoryMeta()

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    suspend fun insert(transaction: TransactionEntity): Long {
        val newId = transactionDao.insertTransaction(transaction)
        // A fresh insert (id == 0) gets its Room-generated id before mirroring so
        // the cloud document is keyed the same as the local row; an edit keeps its id.
        val saved = if (transaction.id == 0) transaction.copy(id = newId.toInt()) else transaction
        FirebaseManager.saveTransaction(saved)
        notifyWidgets()
        return newId
    }

    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        FirebaseManager.deleteTransaction(transaction.id)
        notifyWidgets()
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        budgetDao.insertBudget(budget)
        FirebaseManager.saveBudget(budget)
        notifyWidgets()
    }

    suspend fun deleteBudget(category: String) {
        budgetDao.deleteBudget(category)
        FirebaseManager.deleteBudget(category)
        notifyWidgets()
    }

    // Goals / savings-budgets
    suspend fun insertGoal(goal: GoalEntity): Long {
        val newId = goalDao.insertGoal(goal)
        val saved = if (goal.id == 0) goal.copy(id = newId.toInt()) else goal
        FirebaseManager.saveGoal(saved)
        return newId
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
        FirebaseManager.saveGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
        FirebaseManager.deleteGoal(goal.id)
    }

    // Bills
    suspend fun insertBill(bill: BillEntity): Long {
        val newId = billDao.insertBill(bill)
        val saved = if (bill.id == 0) bill.copy(id = newId.toInt()) else bill
        FirebaseManager.saveBill(saved)
        return newId
    }

    suspend fun updateBill(bill: BillEntity) {
        billDao.updateBill(bill)
        FirebaseManager.saveBill(bill)
    }

    suspend fun deleteBill(bill: BillEntity) {
        billDao.deleteBill(bill)
        FirebaseManager.deleteBill(bill.id)
    }

    // Planned (committed but not yet made) expenses
    suspend fun insertPlannedExpense(planned: PlannedExpenseEntity): Long {
        val newId = plannedExpenseDao.insertPlannedExpense(planned)
        val saved = if (planned.id == 0) planned.copy(id = newId.toInt()) else planned
        FirebaseManager.savePlannedExpense(saved)
        notifyWidgets()
        return newId
    }

    suspend fun updatePlannedExpense(planned: PlannedExpenseEntity) {
        plannedExpenseDao.updatePlannedExpense(planned)
        FirebaseManager.savePlannedExpense(planned)
        notifyWidgets()
    }

    suspend fun deletePlannedExpense(planned: PlannedExpenseEntity) {
        plannedExpenseDao.deletePlannedExpense(planned)
        FirebaseManager.deletePlannedExpense(planned.id)
        notifyWidgets()
    }

    // Category display overrides (Report edits)
    suspend fun upsertCategoryMeta(meta: CategoryMetaEntity) {
        categoryMetaDao.upsertCategoryMeta(meta)
        FirebaseManager.saveCategoryMeta(meta)
    }

    suspend fun deleteCategoryMeta(category: String) {
        categoryMetaDao.deleteCategoryMeta(category)
        FirebaseManager.deleteCategoryMeta(category)
    }

    // User-managed categories (picker entries: name, tone, icon, order)
    suspend fun upsertCategory(category: CategoryEntity) {
        categoryDao.upsert(category)
        FirebaseManager.saveCategory(category)
    }

    suspend fun deleteCategory(name: String, isExpense: Boolean) {
        categoryDao.delete(name, isExpense)
        FirebaseManager.deleteCategory(name, isExpense)
    }

    suspend fun reorderCategories(categories: List<CategoryEntity>) {
        categoryDao.upsertAll(categories)
        categories.forEach { FirebaseManager.saveCategory(it) }
    }

    /** Seed the original default categories the first time this table is ever empty. */
    suspend fun seedDefaultCategoriesIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.upsertAll(DEFAULT_CATEGORIES)
            DEFAULT_CATEGORIES.forEach { FirebaseManager.saveCategory(it) }
        }
    }

    /**
     * Convert every stored monetary amount by [rate] when the user changes their
     * currency (e.g. USD → INR uses the live Frankfurter rate). Amounts are scaled
     * in place locally, then each converted row is mirrored back up to Firestore so
     * a later [syncFromCloud] doesn't overwrite them with the pre-conversion values.
     * A rate of 1.0 (same currency) is a no-op.
     */
    suspend fun convertAllAmounts(rate: Double) {
        if (rate == 1.0) return

        transactionDao.scaleAmounts(rate)
        budgetDao.scaleLimits(rate)
        goalDao.scaleAmounts(rate)
        billDao.scaleAmounts(rate)
        plannedExpenseDao.scaleAmounts(rate)
        categoryMetaDao.scaleOverrides(rate)

        // Push the converted rows to the cloud so they survive the next sign-in pull.
        allTransactions.first().forEach { FirebaseManager.saveTransaction(it) }
        allBudgets.first().forEach { FirebaseManager.saveBudget(it) }
        allGoals.first().forEach { FirebaseManager.saveGoal(it) }
        allBills.first().forEach { FirebaseManager.saveBill(it) }
        allPlannedExpenses.first().forEach { FirebaseManager.savePlannedExpense(it) }
        allCategoryMeta.first().forEach { FirebaseManager.saveCategoryMeta(it) }
        notifyWidgets()
    }

    // ---- Cloud <-> local sync ----

    /**
     * Replace the local cache with the signed-in user's cloud data. Called on
     * sign-in. Cloud rows are written straight to the DAOs (not the mirroring
     * methods above) so pulling data down doesn't echo it back up. Ids are kept
     * as-is so cross-references like [BillEntity.paidTxnId] stay valid.
     */
    suspend fun syncFromCloud() {
        val data = FirebaseManager.loadAllData() ?: return
        clearAllLocal()
        data.transactions.forEach { transactionDao.insertTransaction(it) }
        data.budgets.forEach { budgetDao.insertBudget(it) }
        data.goals.forEach { goalDao.insertGoal(it) }
        data.bills.forEach { billDao.insertBill(it) }
        data.plannedExpenses.forEach { plannedExpenseDao.insertPlannedExpense(it) }
        data.categoryMeta.forEach { categoryMetaDao.upsertCategoryMeta(it) }
        data.categories.forEach { categoryDao.upsert(it) }
        notifyWidgets()
    }

    /** Wipe every local table (used on sign-out and before a cloud pull). */
    suspend fun clearAllLocal() {
        transactionDao.deleteAll()
        budgetDao.deleteAll()
        goalDao.deleteAll()
        billDao.deleteAll()
        plannedExpenseDao.deleteAll()
        categoryMetaDao.deleteAll()
        categoryDao.deleteAll()
        notifyWidgets()
    }

    private fun notifyWidgets() {
        context?.let { WidgetUpdateHelper.updateAllWidgets(it) }
    }
}
