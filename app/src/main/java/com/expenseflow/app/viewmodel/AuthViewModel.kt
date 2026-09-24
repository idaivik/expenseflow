package com.expenseflow.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseflow.app.data.FirebaseManager
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
    object SignedOut : AuthState()
    object PasswordResetSent : AuthState()
    object PasswordResetSuccess : AuthState()

    /**
     * The account exists but its email address is not verified yet. The app parks
     * the user here — outside the main app — until verification completes, so an
     * unverified account can never reach real data. [notice] is a transient message
     * for the verify screen (e.g. "link sent"); [busy] marks an in-flight
     * resend / status-check so the screen can disable its buttons.
     */
    data class EmailNotVerified(
        val user: FirebaseUser,
        val notice: String? = null,
        val busy: Boolean = false,
    ) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(initialAuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    companion object {
        /**
         * Continue URL applied to every verification email. Firebase's hosted
         * action handler confirms the link server-side, then redirects the browser
         * here. Deliberately URL-only: in-app handling (handleCodeInApp + an Android
         * package name) relied on Firebase Dynamic Links, which shut down in
         * Aug 2025, so we let the web handler verify and rely on
         * refreshVerificationStatus() to pull the user into the app afterward.
         */
        private val verificationSettings: ActionCodeSettings =
            ActionCodeSettings.newBuilder()
                .setUrl("https://expenseflow-e1d08.firebaseapp.com/welcome")
                .build()

        /**
         * Resolve the auth state on a cold start. A persisted session is only
         * allowed straight into the app when its email is verified; an unverified
         * session is routed to the verification gate instead of being trusted.
         */
        private fun initialAuthState(): AuthState {
            val user = FirebaseManager.auth.currentUser
            return when {
                user == null -> AuthState.SignedOut
                user.isEmailVerified -> AuthState.Authenticated(user)
                else -> AuthState.EmailNotVerified(user)
            }
        }
    }

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        
        _authState.value = AuthState.Loading
        FirebaseManager.auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    // Verified accounts go straight in; unverified ones are held at
                    // the gate so a stale-but-signed-in session can't slip through.
                    _authState.value = if (user.isEmailVerified) {
                        AuthState.Authenticated(user)
                    } else {
                        AuthState.EmailNotVerified(user)
                    }
                }
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Sign in failed")
            }
    }

    fun signUp(email: String, pass: String, fullName: String = "") {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        FirebaseManager.auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    // Fire the verification email right after the account is created.
                    // Firebase never sends this on its own — the console template only
                    // defines the message, not when it goes out — so it must be sent here.
                    user.sendEmailVerification(verificationSettings)

                    // A brand-new account is unverified, so it must NOT enter the app.
                    // Persist the display name, then park the user at the verify gate.
                    val gate = AuthState.EmailNotVerified(
                        user,
                        notice = "We sent a verification link to ${user.email}.",
                    )
                    if (fullName.isNotBlank()) {
                        val profile = com.google.firebase.auth.userProfileChangeRequest {
                            displayName = fullName
                        }
                        user.updateProfile(profile)
                            .addOnCompleteListener { _authState.value = gate }
                    } else {
                        _authState.value = gate
                    }
                }
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Sign up failed")
            }
    }

    fun signOut() {
        FirebaseManager.auth.signOut()
        _authState.value = AuthState.SignedOut
    }

    /** Re-send the verification email to the signed-in-but-unverified user. */
    fun resendVerification() {
        val user = FirebaseManager.auth.currentUser ?: run {
            _authState.value = AuthState.SignedOut
            return
        }
        _authState.value = AuthState.EmailNotVerified(user, busy = true)
        user.sendEmailVerification(verificationSettings)
            .addOnSuccessListener {
                _authState.value = AuthState.EmailNotVerified(
                    user,
                    notice = "Verification link sent to ${user.email}. Check your inbox and spam folder.",
                )
            }
            .addOnFailureListener {
                _authState.value = AuthState.EmailNotVerified(
                    user,
                    notice = it.message ?: "Couldn't send the link. Wait a moment and try again.",
                )
            }
    }

    /**
     * Reload the user from Firebase to pick up a verification that happened in the
     * email/browser, then either let them into the app or keep them at the gate.
     * Called by the "I've verified" action on the verify screen.
     */
    fun refreshVerificationStatus() {
        val user = FirebaseManager.auth.currentUser ?: run {
            _authState.value = AuthState.SignedOut
            return
        }
        _authState.value = AuthState.EmailNotVerified(user, busy = true)
        user.reload()
            .addOnSuccessListener {
                val refreshed = FirebaseManager.auth.currentUser ?: user
                _authState.value = if (refreshed.isEmailVerified) {
                    AuthState.Authenticated(refreshed)
                } else {
                    AuthState.EmailNotVerified(
                        refreshed,
                        notice = "Not verified yet. Open the link in your email, then tap “I've verified”.",
                    )
                }
            }
            .addOnFailureListener {
                _authState.value = AuthState.EmailNotVerified(
                    user,
                    notice = it.message ?: "Couldn't check status. Try again in a moment.",
                )
            }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Email cannot be empty")
            return
        }
        _authState.value = AuthState.Loading
        FirebaseManager.auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _authState.value = AuthState.PasswordResetSent
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Failed to send reset email")
            }
    }
    
    /**
     * Validate the out-of-band code carried by a password-reset deep link.
     * On success the callback receives the email the link belongs to (used to
     * greet the user); on failure it receives the reason so the screen can show
     * an "expired / invalid link" state. Kept as a one-shot callback rather than
     * routed through [authState] so the reset screen can verify independently of
     * the global auth flow.
     */
    fun verifyResetCode(oobCode: String, onResult: (Result<String>) -> Unit) {
        FirebaseManager.auth.verifyPasswordResetCode(oobCode)
            .addOnSuccessListener { email -> onResult(Result.success(email)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /**
     * Commit the new password for a reset link's [oobCode]. Drives [authState]
     * to [AuthState.PasswordResetSuccess] on completion so the reset screen can
     * show its confirmation, or [AuthState.Error] with the Firebase message.
     */
    fun confirmPasswordReset(oobCode: String, newPassword: String) {
        if (newPassword.length < 8) {
            _authState.value = AuthState.Error("Password must be at least 8 characters")
            return
        }
        _authState.value = AuthState.Loading
        FirebaseManager.auth.confirmPasswordReset(oobCode, newPassword)
            .addOnSuccessListener {
                _authState.value = AuthState.PasswordResetSuccess
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Could not reset password")
            }
    }

    /** Return to a neutral state — used when a transient reset/error screen is dismissed. */
    fun resetAuthState() {
        if (_authState.value !is AuthState.Authenticated) {
            _authState.value = AuthState.SignedOut
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
