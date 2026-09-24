package com.expenseflow.app.ui.auth

import com.composables.icons.lucide.*

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.AuthState
import com.expenseflow.app.viewmodel.AuthViewModel

/**
 * In-app "set a new password" screen reached from a Firebase password-reset
 * email link (the link carries an `oobCode` out-of-band code). On entry it
 * validates the code with [AuthViewModel.verifyResetCode]; a valid code shows
 * the new-password form, an expired/invalid one shows a recovery state.
 * Submitting commits the change via [AuthViewModel.confirmPasswordReset].
 *
 * @param onDone dismisses the screen and returns the user to the sign-in flow.
 */
@Composable
fun ResetPasswordScreen(
    oobCode: String,
    viewModel: AuthViewModel,
    onDone: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val state by viewModel.authState.collectAsState()

    var verifying by remember { mutableStateOf(true) }
    var linkEmail by remember { mutableStateOf<String?>(null) }
    var linkError by remember { mutableStateOf<String?>(null) }

    // Validate the reset code exactly once when the link opens.
    LaunchedEffect(oobCode) {
        viewModel.verifyResetCode(oobCode) { result ->
            verifying = false
            result
                .onSuccess { linkEmail = it }
                .onFailure { linkError = it.message ?: "This reset link is invalid or has expired." }
        }
    }

    val finish = { viewModel.resetAuthState(); onDone() }

    Column(
        Modifier.fillMaxSize().background(c.bgApp).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BackButton(light = false, onBack = finish)
        Spacer(Modifier.height(18.dp))

        when {
            state is AuthState.PasswordResetSuccess -> SuccessState(onDone = finish)
            verifying -> VerifyingState()
            linkError != null -> InvalidLinkState(message = linkError!!, onDone = finish)
            else -> ResetForm(oobCode = oobCode, email = linkEmail, viewModel = viewModel, state = state)
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* ---------------- Verifying ---------------- */

@Composable
private fun VerifyingState() {
    val c = MaterialTheme.efColors
    Spacer(Modifier.height(60.dp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = c.brand, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(18.dp))
        Text("Checking your reset link…", color = c.textSecondary, fontSize = 15.sp)
    }
}

/* ---------------- Reset form ---------------- */

@Composable
private fun ResetForm(
    oobCode: String,
    email: String?,
    viewModel: AuthViewModel,
    state: AuthState,
) {
    val c = MaterialTheme.efColors
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val strength = (password.length / 3).coerceAtMost(3)
    val strLabel = listOf("Too short", "Weak", "Good", "Strong")[strength]
    val strColor = listOf(c.moneyOut, c.catOrange, c.catBlue, c.moneyIn)[strength]

    val mismatch = confirm.isNotEmpty() && confirm != password
    val canSubmit = password.length >= 8 && password == confirm && state !is AuthState.Loading

    Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(c.brandSoft), contentAlignment = Alignment.Center) {
        Icon(Lucide.ShieldCheck, null, tint = c.brand, modifier = Modifier.size(26.dp))
    }
    Spacer(Modifier.height(22.dp))
    Text("Set a new password", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(6.dp))
    Text(
        if (email != null) "Choose a new password for $email." else "Choose a new password for your account.",
        color = c.textSecondary, fontSize = 15.sp, lineHeight = 22.sp,
    )
    Spacer(Modifier.height(24.dp))

    EFTextField(
        password, { password = it }, label = "New password", placeholder = "At least 8 characters",
        leadingIcon = Lucide.Lock, isPassword = !showPass,
        trailingContent = {
            Icon(
                if (showPass) Lucide.EyeOff else Lucide.Eye, "Toggle password",
                tint = c.textMuted, modifier = Modifier.size(20.dp).clickable { showPass = !showPass },
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
    Spacer(Modifier.height(16.dp))

    EFTextField(
        confirm, { confirm = it }, label = "Confirm password", placeholder = "Re-enter password",
        leadingIcon = Lucide.Lock, isPassword = !showConfirm,
        trailingContent = {
            Icon(
                if (showConfirm) Lucide.EyeOff else Lucide.Eye, "Toggle password",
                tint = c.textMuted, modifier = Modifier.size(20.dp).clickable { showConfirm = !showConfirm },
            )
        },
    )
    if (mismatch) {
        Text("Passwords don't match", color = c.moneyOut, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    }
    ErrorText(state)
    Spacer(Modifier.height(24.dp))

    EFButton(
        text = if (state is AuthState.Loading) "Updating…" else "Update Password",
        onClick = { viewModel.confirmPasswordReset(oobCode, password) },
        fullWidth = true, enabled = canSubmit,
    )
}

/* ---------------- Invalid / expired link ---------------- */

@Composable
private fun InvalidLinkState(message: String, onDone: () -> Unit) {
    val c = MaterialTheme.efColors
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(92.dp).clip(CircleShape).background(c.tintRed), contentAlignment = Alignment.Center) {
            Icon(Lucide.TriangleAlert, null, tint = c.moneyOut, modifier = Modifier.size(40.dp))
        }
    }
    Spacer(Modifier.height(24.dp))
    Text(
        "Link expired", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        message, color = c.textSecondary, fontSize = 15.sp, lineHeight = 22.sp,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    EFButton("Back to Sign In", onDone, fullWidth = true)
}

/* ---------------- Success ---------------- */

@Composable
private fun SuccessState(onDone: () -> Unit) {
    val c = MaterialTheme.efColors
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(92.dp).clip(CircleShape).background(c.tintGreen), contentAlignment = Alignment.Center) {
            Icon(Lucide.Check, null, tint = c.moneyIn, modifier = Modifier.size(44.dp))
        }
    }
    Spacer(Modifier.height(24.dp))
    Text(
        "Password updated", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "You can now sign in with your new password.", color = c.textSecondary, fontSize = 15.sp, lineHeight = 22.sp,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    EFButton("Back to Sign In", onDone, fullWidth = true)
}
