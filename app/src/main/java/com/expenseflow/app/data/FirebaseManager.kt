package com.expenseflow.app.data

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Handles all Firebase interaction. Firestore is the per-user cloud store: every
 * document lives under `users/{uid}/…`, so each signed-in account only ever sees
 * its own data. Room is kept as the local cache the UI reads from; this object is
 * the bridge that mirrors local writes up to the cloud and pulls a user's data
 * back down on sign-in (see [TransactionRepository.syncFromCloud]).
 */
object FirebaseManager {

    val auth by lazy { Firebase.auth }
    val firestore by lazy { Firebase.firestore }
    val analytics by lazy { Firebase.analytics }

    // Firestore sub-collection names under the user document.
    private const val COL_TX = "transactions"
    private const val COL_BUDGETS = "budgets"
    private const val COL_GOALS = "goals"
    private const val COL_BILLS = "bills"
    private const val COL_PLANNED = "plannedExpenses"
    private const val COL_META = "categoryMeta"
    private const val COL_CATEGORIES = "categories"

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    private val uid: String? get() = auth.currentUser?.uid

    /** The `users/{uid}` document for the signed-in user, or null when signed out. */
    private fun userDoc(): DocumentReference? =
        uid?.let { firestore.collection("users").document(it) }

    /** Suspend until a Firebase [Task] resolves, without pulling in play-services-coroutines. */
    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

    // ---------------------------------------------------------------------
    // Profile (name comes from Firebase Auth; region/phone live on the doc)
    // ---------------------------------------------------------------------

    data class UserProfile(val region: String = "", val phone: String = "")

    suspend fun loadUserProfile(): UserProfile? {
        val doc = userDoc() ?: return null
        val snap = doc.get().awaitResult()
        return UserProfile(
            region = snap.getString("region") ?: "",
            phone = snap.getString("phone") ?: "",
        )
    }

    /** Merge the editable profile fields onto the user document (fire-and-forget). */
    fun saveUserProfile(region: String, phone: String) {
        val doc = userDoc() ?: return
        doc.set(mapOf("region" to region, "phone" to phone), SetOptions.merge())
    }

    /** Persist the display name to the Firebase Auth profile. */
    fun updateDisplayName(name: String) {
        val user = auth.currentUser ?: return
        user.updateProfile(userProfileChangeRequest { displayName = name })
    }

    // ---------------------------------------------------------------------
    // Per-entity upserts / deletes (mirror local writes to the cloud)
    // ---------------------------------------------------------------------

    fun saveTransaction(t: TransactionEntity) {
        userDoc()?.collection(COL_TX)?.document(t.id.toString())?.set(t.toMap())
    }

    fun deleteTransaction(id: Int) {
        userDoc()?.collection(COL_TX)?.document(id.toString())?.delete()
    }

    fun saveBudget(b: BudgetEntity) {
        userDoc()?.collection(COL_BUDGETS)?.document(docId(b.category))?.set(b.toMap())
    }

    fun deleteBudget(category: String) {
        userDoc()?.collection(COL_BUDGETS)?.document(docId(category))?.delete()
    }

    fun saveGoal(g: GoalEntity) {
        userDoc()?.collection(COL_GOALS)?.document(g.id.toString())?.set(g.toMap())
    }

    fun deleteGoal(id: Int) {
        userDoc()?.collection(COL_GOALS)?.document(id.toString())?.delete()
    }

    fun saveBill(b: BillEntity) {
        userDoc()?.collection(COL_BILLS)?.document(b.id.toString())?.set(b.toMap())
    }

    fun deleteBill(id: Int) {
        userDoc()?.collection(COL_BILLS)?.document(id.toString())?.delete()
    }

    fun savePlannedExpense(p: PlannedExpenseEntity) {
        userDoc()?.collection(COL_PLANNED)?.document(p.id.toString())?.set(p.toMap())
    }

    fun deletePlannedExpense(id: Int) {
        userDoc()?.collection(COL_PLANNED)?.document(id.toString())?.delete()
    }

    fun saveCategoryMeta(m: CategoryMetaEntity) {
        userDoc()?.collection(COL_META)?.document(docId(m.category))?.set(m.toMap())
    }

    fun deleteCategoryMeta(category: String) {
        userDoc()?.collection(COL_META)?.document(docId(category))?.delete()
    }

    fun saveCategory(cat: CategoryEntity) {
        userDoc()?.collection(COL_CATEGORIES)?.document(categoryDocId(cat.name, cat.isExpense))?.set(cat.toMap())
    }

    fun deleteCategory(name: String, isExpense: Boolean) {
        userDoc()?.collection(COL_CATEGORIES)?.document(categoryDocId(name, isExpense))?.delete()
    }

    // ---------------------------------------------------------------------
    // Pull the whole dataset for the signed-in user (used on sign-in)
    // ---------------------------------------------------------------------

    data class CloudData(
        val transactions: List<TransactionEntity>,
        val budgets: List<BudgetEntity>,
        val goals: List<GoalEntity>,
        val bills: List<BillEntity>,
        val plannedExpenses: List<PlannedExpenseEntity>,
        val categoryMeta: List<CategoryMetaEntity>,
        val categories: List<CategoryEntity>,
    )

    /** Fetch every collection for the current user, or null if signed out. */
    suspend fun loadAllData(): CloudData? {
        val doc = userDoc() ?: return null
        return CloudData(
            transactions = doc.collection(COL_TX).get().awaitResult().documents.map { it.toTransaction() },
            budgets = doc.collection(COL_BUDGETS).get().awaitResult().documents.map { it.toBudget() },
            goals = doc.collection(COL_GOALS).get().awaitResult().documents.map { it.toGoal() },
            bills = doc.collection(COL_BILLS).get().awaitResult().documents.map { it.toBill() },
            plannedExpenses = doc.collection(COL_PLANNED).get().awaitResult().documents.map { it.toPlannedExpense() },
            categoryMeta = doc.collection(COL_META).get().awaitResult().documents.map { it.toCategoryMeta() },
            categories = doc.collection(COL_CATEGORIES).get().awaitResult().documents.map { it.toCategory() },
        )
    }

    // ---------------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------------

    /** Firestore doc ids can't contain '/' and can't be blank; used for string-keyed entities. */
    private fun docId(key: String): String = key.ifBlank { "_" }.replace('/', '_')

    /** [CategoryEntity]'s key is (name, isExpense), so both parts go into the doc id. */
    private fun categoryDocId(name: String, isExpense: Boolean): String =
        (if (isExpense) "exp_" else "inc_") + docId(name)

    private fun TransactionEntity.toMap() = mapOf(
        "id" to id, "title" to title, "category" to category, "amount" to amount,
        "date" to date, "time" to time, "iconName" to iconName, "iconColor" to iconColor,
        "isExpense" to isExpense, "isEdited" to isEdited,
    )

    private fun DocumentSnapshot.toTransaction() = TransactionEntity(
        id = (getLong("id") ?: 0L).toInt(),
        title = getString("title") ?: "",
        category = getString("category") ?: "",
        amount = getDouble("amount") ?: 0.0,
        date = getString("date") ?: "",
        time = getString("time") ?: "",
        iconName = getString("iconName") ?: "",
        iconColor = getLong("iconColor") ?: 0L,
        isExpense = getBoolean("isExpense") ?: false,
        isEdited = getBoolean("isEdited") ?: false,
    )

    private fun BudgetEntity.toMap() = mapOf(
        "category" to category, "budgetLimit" to budgetLimit,
    )

    private fun DocumentSnapshot.toBudget() = BudgetEntity(
        category = getString("category") ?: "",
        budgetLimit = getDouble("budgetLimit") ?: 0.0,
    )

    private fun GoalEntity.toMap() = mapOf(
        "id" to id, "kind" to kind, "name" to name, "iconKey" to iconKey, "tone" to tone,
        "saved" to saved, "target" to target, "targetDate" to targetDate, "completed" to completed,
    )

    private fun DocumentSnapshot.toGoal() = GoalEntity(
        id = (getLong("id") ?: 0L).toInt(),
        kind = getString("kind") ?: "goal",
        name = getString("name") ?: "",
        iconKey = getString("iconKey") ?: "",
        tone = getString("tone") ?: "",
        saved = getDouble("saved") ?: 0.0,
        target = getDouble("target") ?: 0.0,
        targetDate = getString("targetDate") ?: "",
        completed = getBoolean("completed") ?: false,
    )

    private fun BillEntity.toMap() = mapOf(
        "id" to id, "name" to name, "iconKey" to iconKey, "tone" to tone, "amount" to amount,
        "dueDate" to dueDate, "paid" to paid, "paidTxnId" to paidTxnId,
        "recurrenceType" to recurrenceType, "intervalCount" to intervalCount, "intervalUnit" to intervalUnit,
    )

    private fun DocumentSnapshot.toBill() = BillEntity(
        id = (getLong("id") ?: 0L).toInt(),
        name = getString("name") ?: "",
        iconKey = getString("iconKey") ?: "",
        tone = getString("tone") ?: "",
        amount = getDouble("amount") ?: 0.0,
        dueDate = getString("dueDate") ?: "",
        paid = getBoolean("paid") ?: false,
        paidTxnId = (getLong("paidTxnId") ?: 0L).toInt(),
        recurrenceType = getString("recurrenceType") ?: "monthly",
        intervalCount = (getLong("intervalCount") ?: 1L).toInt(),
        intervalUnit = getString("intervalUnit") ?: "month",
    )

    private fun PlannedExpenseEntity.toMap() = mapOf(
        "id" to id, "title" to title, "category" to category, "amount" to amount,
        "date" to date, "loggedTxnId" to loggedTxnId,
    )

    private fun DocumentSnapshot.toPlannedExpense() = PlannedExpenseEntity(
        id = (getLong("id") ?: 0L).toInt(),
        title = getString("title") ?: "",
        category = getString("category") ?: "",
        amount = getDouble("amount") ?: 0.0,
        date = getString("date") ?: "",
        loggedTxnId = (getLong("loggedTxnId") ?: 0L).toInt(),
    )

    private fun CategoryMetaEntity.toMap() = mapOf(
        "category" to category, "displayName" to displayName,
        "iconKey" to iconKey, "amountOverride" to amountOverride,
    )

    private fun DocumentSnapshot.toCategoryMeta() = CategoryMetaEntity(
        category = getString("category") ?: "",
        displayName = getString("displayName"),
        iconKey = getString("iconKey"),
        amountOverride = getDouble("amountOverride"),
    )

    private fun CategoryEntity.toMap() = mapOf(
        "name" to name, "isExpense" to isExpense, "tone" to tone,
        "iconKey" to iconKey, "sortOrder" to sortOrder,
    )

    private fun DocumentSnapshot.toCategory() = CategoryEntity(
        name = getString("name") ?: "",
        isExpense = getBoolean("isExpense") ?: true,
        tone = getString("tone") ?: "BLUE",
        iconKey = getString("iconKey") ?: "",
        sortOrder = (getLong("sortOrder") ?: 0L).toInt(),
    )
}
