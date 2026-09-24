package com.expenseflow.app.ui.components

import com.composables.icons.lucide.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.theme.efColors

/** Simple back-arrow top bar used by full-screen overlays. */
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val c = MaterialTheme.efColors
    Row(
        modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(c.surfaceCard).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Lucide.ArrowLeft, "Back", tint = c.textPrimary, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(title, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}
