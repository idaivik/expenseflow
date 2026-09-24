package com.expenseflow.app.ui.screens

import com.composables.icons.lucide.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.components.CategoryIcon
import com.expenseflow.app.ui.components.ScreenTopBar
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.viewmodel.ExpenseViewModel

@Composable
fun NotificationCenterScreen(viewModel: ExpenseViewModel, onBack: () -> Unit) {
    val c = MaterialTheme.efColors
    val visible by viewModel.notifications.collectAsState()

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(c.bgApp)) {
        ScreenTopBar("Notifications", onBack, trailing = {
            if (visible.isNotEmpty()) {
                Text(
                    "Clear all", color = c.brand, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { viewModel.clearAllNotifications() }.padding(6.dp),
                )
            }
        })
        if (visible.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(c.surfaceSunken), contentAlignment = Alignment.Center) {
                    Icon(Lucide.Sparkles, null, tint = c.textMuted, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("You're all caught up", color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("No notifications right now.", color = c.textSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                items(visible, key = { it.id }) { n ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.brandSoft)
                            .border(1.dp, c.borderSubtle, RoundedCornerShape(18.dp))
                            .clickable { viewModel.dismissNotification(n.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(tone = n.tone, icon = n.icon, size = 44.dp)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(n.title, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(3.dp))
                            Text(n.body, color = c.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
