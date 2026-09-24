package com.expenseflow.app

import com.composables.icons.lucide.*

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expenseflow.app.data.AppDatabase
import com.expenseflow.app.data.SettingsRepository
import com.expenseflow.app.data.TransactionRepository
import com.expenseflow.app.notifications.NotificationHelper
import com.expenseflow.app.ui.auth.AuthFlow
import com.expenseflow.app.ui.auth.ResetPasswordScreen
import com.expenseflow.app.ui.components.EFTab
import com.expenseflow.app.ui.components.EFTabBar
import com.expenseflow.app.ui.screens.AddTransactionSheet
import com.expenseflow.app.ui.screens.BudgetDetailScreen
import com.expenseflow.app.ui.screens.CategoryBudgetsScreen
import com.expenseflow.app.ui.screens.HistoryScreen
import com.expenseflow.app.ui.screens.HomeScreen
import com.expenseflow.app.ui.screens.ManageCategoriesScreen
import com.expenseflow.app.ui.screens.NotificationCenterScreen
import com.expenseflow.app.ui.screens.PlanScreen
import com.expenseflow.app.ui.screens.ProfileScreen
import com.expenseflow.app.ui.screens.ReportScreen
import com.expenseflow.app.ui.screens.SettingsScreen
import com.expenseflow.app.ui.theme.ExpenseFlowTheme
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.AuthState
import com.expenseflow.app.viewmodel.AuthViewModel
import com.expenseflow.app.viewmodel.ExpenseViewModel
import com.expenseflow.app.viewmodel.ExpenseViewModelFactory
import com.expenseflow.app.viewmodel.SettingsViewModel
import com.expenseflow.app.viewmodel.SettingsViewModelFactory
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_ADD = "com.expenseflow.app.OPEN_ADD"
        const val EXTRA_PREFILL_CATEGORY = "com.expenseflow.app.PREFILL_CATEGORY"
        const val EXTRA_PREFILL_AMOUNT = "com.expenseflow.app.PREFILL_AMOUNT"
        const val EXTRA_IS_EXPENSE = "com.expenseflow.app.IS_EXPENSE"
        const val EXTRA_OPEN_TAB = "com.expenseflow.app.OPEN_TAB"
        const val EXTRA_OPEN_OVERLAY = "com.expenseflow.app.OPEN_OVERLAY"
    }

    // Out-of-band code from a password-reset deep link, if the app was opened via one.
    private val pendingResetCode = mutableStateOf<String?>(null)
    private val pendingIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingResetCode.value = extractResetCode(intent)
        pendingIntent.value = intent

        val database = AppDatabase.getDatabase(this)
        val repository = TransactionRepository(
            database.transactionDao(), database.budgetDao(), database.goalDao(), database.billDao(),
            database.plannedExpenseDao(), database.categoryMetaDao(), database.categoryDao(), applicationContext
        )
        val settingsRepository = SettingsRepository(this)
        val expenseFactory = ExpenseViewModelFactory(repository, settingsRepository)
        val settingsFactory = SettingsViewModelFactory(settingsRepository)

        NotificationHelper.createChannel(this)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)
            val settings by settingsViewModel.settings.collectAsState()

            ExpenseFlowTheme(darkTheme = settings.isDark) {
                val expenseViewModel: ExpenseViewModel = viewModel(factory = expenseFactory)
                val authViewModel: AuthViewModel = viewModel()

                Box(Modifier.fillMaxSize().background(MaterialTheme.efColors.bgApp)) {
                    val resetCode = pendingResetCode.value
                    if (resetCode != null) {
                        // A reset link takes over the whole surface until the user finishes or backs out.
                        ResetPasswordScreen(
                            oobCode = resetCode,
                            viewModel = authViewModel,
                            onDone = { pendingResetCode.value = null },
                        )
                    } else {
                        AppRoot(expenseViewModel, authViewModel, settingsViewModel, pendingIntent)
                    }
                }
            }
        }
    }

    // singleTop: when the app is already running, the reset link arrives here instead of a new instance.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractResetCode(intent)?.let { pendingResetCode.value = it }
        pendingIntent.value = intent
    }

    /**
     * Pull the Firebase `oobCode` out of a password-reset deep link. Handles both the
     * custom `expenseflow://reset?...` scheme and the Firebase action URL
     * (`https://<project>.firebaseapp.com/__/auth/action?mode=resetPassword&oobCode=...`).
     * Returns null for any other intent.
     */
    private fun extractResetCode(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val oob = data.getQueryParameter("oobCode") ?: return null
        val mode = data.getQueryParameter("mode")
        return if (mode == null || mode == "resetPassword") oob else null
    }
}

@Composable
private fun AppRoot(
    expenseViewModel: ExpenseViewModel,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    pendingIntent: androidx.compose.runtime.MutableState<Intent?>,
) {
    val authState by authViewModel.authState.collectAsState()
    var entered by remember { mutableStateOf(authViewModel.authState.value is AuthState.Authenticated) }

    // Drive per-account data on auth changes: on sign-in, pull this user's data
    // down from Firestore and fill the profile; on sign-out, wipe the local cache
    // so the next account starts clean.
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> expenseViewModel.onSignedIn(state.user)
            is AuthState.SignedOut -> expenseViewModel.onSignedOut()
            else -> {}
        }
    }

    if (authState is AuthState.Authenticated && entered) {
        MainApp(
            expenseViewModel = expenseViewModel,
            settingsViewModel = settingsViewModel,
            pendingIntent = pendingIntent,
            onLogout = { authViewModel.signOut(); entered = false },
        )
    } else {
        AuthFlow(authViewModel = authViewModel, onEntered = { entered = true })
    }
}

@Composable
private fun MainApp(
    expenseViewModel: ExpenseViewModel,
    settingsViewModel: SettingsViewModel,
    pendingIntent: androidx.compose.runtime.MutableState<Intent?>,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("home") }
    var overlay by remember { mutableStateOf<String?>(null) }
    // Per-budget insight screen; holds the selected category name while open.
    var budgetDetail by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    // Which category-type tab Manage Categories should open on, set by whichever
    // entry point (Add Transaction sheet) launched it.
    var prefillCategory by remember { mutableStateOf<String?>(null) }
    var prefillAmount by remember { mutableStateOf("") }
    var prefillKind by remember { mutableStateOf("exp") }
    var manageCategoriesExpense by remember { mutableStateOf(true) }

    LaunchedEffect(pendingIntent.value) {
        val currentIntent = pendingIntent.value ?: return@LaunchedEffect
        if (currentIntent.getBooleanExtra(MainActivity.EXTRA_OPEN_ADD, false)) {
            prefillCategory = currentIntent.getStringExtra(MainActivity.EXTRA_PREFILL_CATEGORY)
            prefillAmount = currentIntent.getStringExtra(MainActivity.EXTRA_PREFILL_AMOUNT) ?: ""
            prefillKind = if (currentIntent.getBooleanExtra(MainActivity.EXTRA_IS_EXPENSE, true)) "exp" else "inc"
            showAdd = true
        }
        currentIntent.getStringExtra(MainActivity.EXTRA_OPEN_TAB)?.let { targetTab ->
            tab = targetTab
        }
        currentIntent.getStringExtra(MainActivity.EXTRA_OPEN_OVERLAY)?.let { targetOverlay ->
            overlay = targetOverlay
        }
        pendingIntent.value = null
    }

    // Ask for notification permission on Android 13+ so budget alerts can show.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Post a real system notification whenever a budget threshold is crossed.
    LaunchedEffect(Unit) {
        expenseViewModel.budgetAlerts.collect { alert ->
            NotificationHelper.showBudgetAlert(context, alert, expenseViewModel.currencySymbol.value)
        }
    }

    val tabs = remember {
        listOf(
            EFTab("home", "Home", Lucide.House),
            EFTab("report", "Report", Lucide.ChartColumnBig),
            EFTab("plan", "Plan", Lucide.Layers),
            EFTab("settings", "Settings", Lucide.Settings),
        )
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.efColors.bgApp)) {
        Scaffold(
            containerColor = MaterialTheme.efColors.bgApp,
            bottomBar = { EFTabBar(tabs, tab, onSelect = { tab = it }, onAdd = { showAdd = true }) },
        ) { innerPadding ->
            Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                when (tab) {
                    "home" -> HomeScreen(
                        viewModel = expenseViewModel,
                        onAdd = { showAdd = true },
                        onBell = { overlay = "notifications" },
                        onProfile = { overlay = "profile" },
                        onViewAll = { overlay = "history" },
                        onOpenBudget = { overlay = "budgets" },
                        onOpenBudgetDetail = { budgetDetail = it },
                    )
                    "report" -> ReportScreen(expenseViewModel)
                    "plan" -> PlanScreen(
                        viewModel = expenseViewModel,
                        onOpenBudgetDetail = { budgetDetail = it },
                        onManageBudgets = { overlay = "budgets" },
                    )
                    "settings" -> SettingsScreen(settingsViewModel, expenseViewModel)
                }
            }
        }

        when (overlay) {
            "profile" -> ProfileScreen(expenseViewModel, onBack = { overlay = null }, onLogout = { overlay = null; onLogout() })
            "notifications" -> NotificationCenterScreen(expenseViewModel, onBack = { overlay = null })
            "history" -> HistoryScreen(expenseViewModel, onBack = { overlay = null })
            "budgets" -> CategoryBudgetsScreen(expenseViewModel, onBack = { overlay = null })
            "manageCategories" -> ManageCategoriesScreen(
                expenseViewModel, initialExpense = manageCategoriesExpense, onBack = { overlay = null },
            )
        }

        // Per-budget insight screen (opened from Home's over-budget alert or a Plan budget row).
        budgetDetail?.let { category ->
            BudgetDetailScreen(
                expenseViewModel,
                category = category,
                onBack = { budgetDetail = null },
                onAdjust = { budgetDetail = null; overlay = "budgets" },
            )
        }

        if (showAdd) {
            AddTransactionSheet(
                expenseViewModel,
                initialCategory = prefillCategory,
                initialAmount = prefillAmount,
                initialKind = prefillKind,
                onDismiss = {
                    showAdd = false
                    prefillCategory = null
                    prefillAmount = ""
                },
                onManageCategories = { isExpense ->
                    showAdd = false
                    manageCategoriesExpense = isExpense
                    overlay = "manageCategories"
                },
            )
        }
    }
}
