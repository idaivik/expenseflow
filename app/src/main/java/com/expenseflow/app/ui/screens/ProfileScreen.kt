package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.components.Avatar
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.EFButton
import com.expenseflow.app.ui.components.EFButtonVariant
import com.expenseflow.app.ui.components.EFCard
import com.expenseflow.app.ui.components.EFElevation
import com.expenseflow.app.ui.components.EFTextField
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.heroBrush
import com.expenseflow.app.viewmodel.ExpenseViewModel

@Composable
fun ProfileScreen(viewModel: ExpenseViewModel, onBack: () -> Unit, onLogout: () -> Unit) {
    val c = MaterialTheme.efColors
    val name by viewModel.userName.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val phone by viewModel.userPhone.collectAsState()
    val region by viewModel.userRegion.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val spent by viewModel.totalSpentThisMonth.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val savingsBudgets by viewModel.savingsBudgets.collectAsState()
    val activeGoals = goals.count { !it.completed } + savingsBudgets.count { !it.completed }

    var editing by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(c.bgApp).verticalScroll(rememberScrollState())) {
        // Gradient hero
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                .background(heroBrush()).statusBarsPadding().padding(bottom = 26.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Lucide.ArrowLeft, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("My Profile", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable { editing = true }, contentAlignment = Alignment.Center) {
                    Icon(Lucide.Pencil, "Edit profile", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(name = name, size = 84.dp)
                Spacer(Modifier.height(14.dp))
                Text(name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(email, color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCapsule("Balance", "$currency${balance.toInt()}", Modifier.weight(1f))
                    StatCapsule("This month", "$currency${spent.toInt()}", Modifier.weight(1f))
                    StatCapsule("Goals", "$activeGoals active", Modifier.weight(1f))
                }
            }
        }

        Column(Modifier.padding(20.dp)) {
            SectionLabelP("Personal Info")
            EFCard(elevation = EFElevation.Sm, contentPadding = PaddingValues(horizontal = 18.dp)) {
                ProfileRow(Lucide.User, CategoryTone.BLUE, "Full Name", name)
                ProfileRow(Lucide.Mail, CategoryTone.PURPLE, "Email", email)
                ProfileRow(Lucide.Phone, CategoryTone.GREEN, "Phone", phone.ifBlank { "Not set" }, comingSoon = true)
                ProfileRow(Lucide.MapPin, CategoryTone.ORANGE, "Region", region.ifBlank { "Not set" }, last = true)
            }
            Spacer(Modifier.height(24.dp))
            EFButton(
                text = "Log Out",
                onClick = onLogout,
                variant = EFButtonVariant.Secondary,
                fullWidth = true,
                leadingIcon = Lucide.LogOut,
                contentColorOverride = c.moneyOut,
            )
            Spacer(Modifier.height(16.dp))
            Text("Manage app preferences in the Settings tab", color = c.textMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
        }
    }

    if (editing) {
        EditProfileSheet(
            name = name,
            region = region,
            phone = phone,
            onSave = { newName, newRegion ->
                viewModel.updateNameAndRegion(newName, newRegion)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    name: String,
    region: String,
    phone: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.efColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nameDraft by remember { mutableStateOf(name) }
    var regionDraft by remember { mutableStateOf(region) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surfaceCard) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp).navigationBarsPadding().imePadding()) {
            Text("Edit Profile", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            EFTextField(nameDraft, { nameDraft = it }, label = "Full Name", placeholder = "Your name", leadingIcon = Lucide.User)
            Spacer(Modifier.height(16.dp))
            EFTextField(regionDraft, { regionDraft = it }, label = "Region", placeholder = "e.g. United States", leadingIcon = Lucide.MapPin)
            Spacer(Modifier.height(16.dp))
            // Phone — not yet editable
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Phone", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                ComingSoonBadge()
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surfaceInset)
                    .padding(horizontal = 14.dp)
                    .alpha(0.6f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Lucide.Phone, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(phone.ifBlank { "Not set" }, color = c.textMuted, fontSize = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
            EFButton(
                text = "Save Changes",
                onClick = { onSave(nameDraft, regionDraft) },
                fullWidth = true,
                enabled = nameDraft.isNotBlank() && regionDraft.isNotBlank(),
            )
        }
    }
}

@Composable
private fun StatCapsule(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)).padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
    }
}

@Composable
private fun SectionLabelP(text: String) {
    val c = MaterialTheme.efColors
    Text(text.uppercase(), color = c.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
}

@Composable
private fun ProfileRow(icon: ImageVector, tone: CategoryTone, label: String, value: String, last: Boolean = false, comingSoon: Boolean = false) {
    val c = MaterialTheme.efColors
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(tone, icon, size = 40.dp, cornerRadius = 12.dp)
            Spacer(Modifier.width(14.dp))
            Text(label, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (comingSoon) {
                ComingSoonBadge()
            } else {
                Text(value, color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.borderSubtle))
    }
}

@Composable
private fun ComingSoonBadge() {
    val c = MaterialTheme.efColors
    Text(
        "Coming soon",
        color = c.brand,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.brandSoft)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
