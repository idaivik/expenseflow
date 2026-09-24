package com.expenseflow.app.ui.auth

import com.composables.icons.lucide.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFButtonVariant
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.theme.Radii
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.heroBrush
import com.expenseflow.app.viewmodel.AuthState
import com.expenseflow.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

/* ---------------- Auth flow host ---------------- */

@Composable
fun AuthFlow(authViewModel: AuthViewModel, onEntered: () -> Unit) {
    val state by authViewModel.authState.collectAsState()
    var mode by rememberSaveable { mutableStateOf("welcome") }
    var showSuccess by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthState.Authenticated) {
            if (mode == "signup" || mode == "login") showSuccess = true else onEntered()
        }
    }

    // Verification gate: an unverified account can't reach the app or the success
    // screen — it's held here until it verifies or switches accounts.
    val current = state
    if (current is AuthState.EmailNotVerified) {
        VerifyEmailScreen(
            email = current.user.email ?: "",
            notice = current.notice,
            busy = current.busy,
            // Sign-up auto-sends a link on arrival, so start the resend cooldown
            // immediately for that path; sign-in / cold-start didn't send, so let
            // the user request their first link right away.
            cooldownOnEntry = mode == "signup",
            onResend = authViewModel::resendVerification,
            onCheck = authViewModel::refreshVerificationStatus,
            onUseAnotherAccount = { authViewModel.signOut(); mode = "welcome" },
        )
        return
    }

    if (showSuccess) {
        SuccessScreen(onEnter = onEntered)
        return
    }

    when (mode) {
        "login" -> LoginScreen(
            viewModel = authViewModel,
            onBack = { authViewModel.clearError(); mode = "welcome" },
            onForgot = { authViewModel.clearError(); mode = "forgot" },
            onSignup = { authViewModel.clearError(); mode = "signup" },
        )
        "signup" -> SignupScreen(
            viewModel = authViewModel,
            onBack = { authViewModel.clearError(); mode = "welcome" },
            onLogin = { authViewModel.clearError(); mode = "login" },
        )
        "forgot" -> ForgotPasswordScreen(
            viewModel = authViewModel,
            onBack = { authViewModel.clearError(); mode = "login" },
        )
        else -> WelcomeScreen(
            onGetStarted = { authViewModel.clearError(); mode = "signup" },
            onLogin = { authViewModel.clearError(); mode = "login" },
        )
    }
}

/* ---------------- Shared bits ---------------- */

@Composable
private fun Wordmark(light: Boolean) {
    val c = MaterialTheme.efColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(if (light) Color.White.copy(alpha = 0.18f) else c.brand),
            contentAlignment = Alignment.Center,
        ) { Icon(Lucide.Wallet, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
        Text("ExpenseFlow", color = if (light) Color.White else c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun BackButton(light: Boolean, onBack: () -> Unit) {
    val c = MaterialTheme.efColors
    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape)
            .background(if (light) Color.White.copy(alpha = 0.14f) else c.surfaceCard)
            .border(1.dp, if (light) Color.White.copy(alpha = 0.28f) else c.borderSubtle, CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) { Icon(Lucide.ArrowLeft, "Back", tint = if (light) Color.White else c.textPrimary, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun WhiteCtaButton(text: String, onClick: () -> Unit, trailing: Boolean = false) {
    val c = MaterialTheme.efColors
    Box(
        modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radii.pill))
            .background(Color.White).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, color = c.brand, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (trailing) Icon(Lucide.ArrowRight, null, tint = c.brand, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
internal fun ErrorText(state: AuthState) {
    if (state is AuthState.Error) {
        val c = MaterialTheme.efColors
        Text(state.message, color = c.moneyOut, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/* ---------------- Welcome ---------------- */

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onLogin: () -> Unit) {
    Box(Modifier.fillMaxSize().background(heroBrush())) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            Wordmark(light = true)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                GlassBalancePreview()
            }
            Text(
                "Take control of\nyour money.",
                color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Track spending, set budgets, and hit your savings goals — all in one calm place.",
                color = Color.White.copy(alpha = 0.82f), fontSize = 15.sp, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(22.dp))
            WhiteCtaButton("Get Started", onGetStarted, trailing = true)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radii.pill))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(Radii.pill))
                    .clickable(onClick = onLogin),
                contentAlignment = Alignment.Center,
            ) { Text("I already have an account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun GlassBalancePreview() {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(22.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Total Balance", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 9.dp, vertical = 4.dp)) {
                Text("+12%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("$12,480.00", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Income" to 0.82f, "Expenses" to 0.46f).forEach { (label, frac) ->
                Column(Modifier.weight(1f)) {
                    Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(7.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                        Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
        }
    }
}

/* ---------------- Login ---------------- */

@Composable
fun LoginScreen(viewModel: AuthViewModel, onBack: () -> Unit, onForgot: () -> Unit, onSignup: () -> Unit) {
    val c = MaterialTheme.efColors
    val state by viewModel.authState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(c.bgApp).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BackButton(light = false, onBack = onBack)
        Spacer(Modifier.height(18.dp))
        Text("Welcome back", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("Sign in to pick up where you left off.", color = c.textSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(26.dp))
        EFTextField(email, { email = it }, label = "Email", placeholder = "you@email.com", leadingIcon = Lucide.Mail, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(16.dp))
        EFTextField(
            password, { password = it }, label = "Password", placeholder = "••••••••",
            leadingIcon = Lucide.Lock, isPassword = !show,
            trailingContent = {
                Icon(
                    if (show) Lucide.EyeOff else Lucide.Eye, "Toggle password",
                    tint = c.textMuted, modifier = Modifier.size(20.dp).clickable { show = !show },
                )
            },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Forgot password?", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.End).clickable(onClick = onForgot),
        )
        ErrorText(state)
        Spacer(Modifier.height(22.dp))
        EFButton(
            text = if (state is AuthState.Loading) "Signing in…" else "Sign In",
            onClick = { viewModel.signIn(email, password) },
            fullWidth = true, enabled = state !is AuthState.Loading,
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account? ", color = c.textSecondary, fontSize = 14.sp)
            Text("Sign Up", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onSignup))
        }
        Spacer(Modifier.height(24.dp))
    }
}

/* ---------------- Signup ---------------- */

@Composable
fun SignupScreen(viewModel: AuthViewModel, onBack: () -> Unit, onLogin: () -> Unit) {
    val c = MaterialTheme.efColors
    val state by viewModel.authState.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }

    val strength = (password.length / 3).coerceAtMost(3)
    val strLabel = listOf("Too short", "Weak", "Good", "Strong")[strength]
    val strColor = listOf(c.moneyOut, c.catOrange, c.catBlue, c.moneyIn)[strength]

    Column(
        Modifier.fillMaxSize().background(c.bgApp).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BackButton(light = false, onBack = onBack)
        Spacer(Modifier.height(14.dp))
        Text("Create account", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("Start tracking in under a minute.", color = c.textSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(22.dp))
        EFTextField(name, { name = it }, label = "Full name", placeholder = "Alex Morgan", leadingIcon = Lucide.User)
        Spacer(Modifier.height(14.dp))
        EFTextField(email, { email = it }, label = "Email", placeholder = "you@email.com", leadingIcon = Lucide.Mail, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(14.dp))
        EFTextField(
            password, { password = it }, label = "Password", placeholder = "At least 8 characters",
            leadingIcon = Lucide.Lock, isPassword = !show,
            trailingContent = {
                Icon(
                    if (show) Lucide.EyeOff else Lucide.Eye, "Toggle password",
                    tint = c.textMuted, modifier = Modifier.size(20.dp).clickable { show = !show },
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier.weight(1f).height(5.dp).clip(CircleShape)
                            .background(if (i < strength) strColor else c.surfaceSunken),
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            Text(if (password.isNotEmpty()) strLabel else "", color = strColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        ErrorText(state)
        Spacer(Modifier.height(22.dp))
        EFButton(
            text = if (state is AuthState.Loading) "Creating…" else "Create Account",
            onClick = { viewModel.signUp(email, password, name) },
            fullWidth = true, enabled = state !is AuthState.Loading,
        )
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", color = c.textSecondary, fontSize = 14.sp)
            Text("Sign In", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onLogin))
        }
        Spacer(Modifier.height(24.dp))
    }
}

/* ---------------- Forgot password ---------------- */

@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    val c = MaterialTheme.efColors
    val state by viewModel.authState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    val sent = state is AuthState.PasswordResetSent

    Column(
        Modifier.fillMaxSize().background(c.bgApp).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BackButton(light = false, onBack = onBack)
        Spacer(Modifier.height(18.dp))
        if (!sent) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(c.brandSoft), contentAlignment = Alignment.Center) {
                Icon(Lucide.KeyRound, null, tint = c.brand, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("Reset password", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text("Enter the email tied to your account and we'll send a secure reset link.", color = c.textSecondary, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(24.dp))
            EFTextField(email, { email = it }, label = "Email", placeholder = "you@email.com", leadingIcon = Lucide.Mail, keyboardType = KeyboardType.Email)
            ErrorText(state)
            Spacer(Modifier.height(22.dp))
            EFButton(
                text = if (state is AuthState.Loading) "Sending…" else "Send Reset Link",
                onClick = { viewModel.sendPasswordReset(email) },
                fullWidth = true, enabled = state !is AuthState.Loading,
            )
        } else {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(92.dp).clip(CircleShape).background(c.tintGreen), contentAlignment = Alignment.Center) {
                    Icon(Lucide.MailCheck, null, tint = c.moneyIn, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Check your email", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("We sent a reset link to", color = c.textSecondary, fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(email.ifBlank { "you@email.com" }, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            EFButton("Back to Sign In", onBack, fullWidth = true)
        }
        Spacer(Modifier.height(20.dp))
        Text("Back to Sign In", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clickable(onClick = onBack), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
    }
}

/* ---------------- Verify email (security gate) ---------------- */

private const val RESEND_COOLDOWN_SEC = 30

@Composable
fun VerifyEmailScreen(
    email: String,
    notice: String?,
    busy: Boolean,
    cooldownOnEntry: Boolean,
    onResend: () -> Unit,
    onCheck: () -> Unit,
    onUseAnotherAccount: () -> Unit,
) {
    val c = MaterialTheme.efColors

    // Resend cooldown: Firebase rate-limits verification sends, so gate the button
    // for [RESEND_COOLDOWN_SEC] after each send. [cooldownUntil] is the epoch-millis
    // the cooldown ends (saved across rotation); [remaining] is the live countdown.
    var cooldownUntil by rememberSaveable { mutableStateOf(0L) }
    var remaining by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Only arm on entry if a link was just auto-sent and no cooldown is running.
        if (cooldownOnEntry && cooldownUntil == 0L) {
            cooldownUntil = System.currentTimeMillis() + RESEND_COOLDOWN_SEC * 1000L
        }
    }
    LaunchedEffect(cooldownUntil) {
        while (true) {
            val secs = ((cooldownUntil - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L).toInt()
            remaining = secs
            if (secs <= 0) break
            delay(1000)
        }
    }
    val onCooldown = remaining > 0

    Column(
        Modifier.fillMaxSize().background(c.bgApp).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(52.dp))
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(c.brandSoft),
            contentAlignment = Alignment.Center,
        ) { Icon(Lucide.MailCheck, null, tint = c.brand, modifier = Modifier.size(38.dp)) }
        Spacer(Modifier.height(24.dp))
        Text(
            "Verify your email", color = c.textPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "For your account's security, confirm your email address before you can use ExpenseFlow. We sent a verification link to:",
            color = c.textSecondary, fontSize = 15.sp, lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            email.ifBlank { "your email" }, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Open the link, then come back and tap “I've verified”.",
            color = c.textMuted, fontSize = 13.sp, lineHeight = 19.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        // Transient status line: link sent / not verified yet / error.
        if (notice != null) {
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surfaceSunken).padding(14.dp)) {
                Text(
                    notice, color = c.textSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(26.dp))
        EFButton(
            text = when {
                busy -> "Sending…"
                onCooldown -> "Resend link in ${remaining}s"
                else -> "Send Verification Link"
            },
            onClick = {
                cooldownUntil = System.currentTimeMillis() + RESEND_COOLDOWN_SEC * 1000L
                onResend()
            },
            fullWidth = true, enabled = !busy && !onCooldown, leadingIcon = Lucide.Mail,
        )
        Spacer(Modifier.height(12.dp))
        EFButton(
            text = if (busy) "Checking…" else "I've verified — Continue",
            onClick = onCheck, variant = EFButtonVariant.Secondary, fullWidth = true, enabled = !busy,
            leadingIcon = Lucide.Check,
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "Use a different account", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(enabled = !busy, onClick = onUseAnotherAccount),
        )
        Spacer(Modifier.height(24.dp))
    }
}

/* ---------------- Success ---------------- */

@Composable
fun SuccessScreen(onEnter: () -> Unit) {
    Box(Modifier.fillMaxSize().background(heroBrush())) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(108.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(74.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Lucide.Check, null, tint = MaterialTheme.efColors.brand, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(30.dp))
                Text("You're all set!", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your account is ready. Let's get your first budget going.",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            WhiteCtaButton("Enter ExpenseFlow", onEnter, trailing = true)
            Spacer(Modifier.height(24.dp))
        }
    }
}
