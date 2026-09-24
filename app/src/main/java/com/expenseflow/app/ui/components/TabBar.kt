package com.expenseflow.app.ui.components

import com.composables.icons.lucide.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.Radii
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.fabShadow

data class EFTab(val key: String, val label: String, val icon: ImageVector)

@Composable
fun EFTabBar(
    tabs: List<EFTab>,
    selected: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.efColors
    val half = (tabs.size + 1) / 2
    val left = tabs.take(half)
    val right = tabs.drop(half)

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl))
                .background(c.surfaceCard)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                left.forEach { TabItem(it, it.key == selected, onSelect) }
            }
            Spacer(Modifier.width(70.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                right.forEach { TabItem(it, it.key == selected, onSelect) }
            }
        }

        // Floating center action button
        val fabInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .size(62.dp)
                .pressScale(fabInteraction, 0.92f)
                .fabShadow()
                .clip(CircleShape)
                .background(c.brand)
                .border(5.dp, c.surfaceCard, CircleShape)
                .clickable(interactionSource = fabInteraction, indication = null, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.Plus, contentDescription = "Add transaction", tint = c.onBrand, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun TabItem(tab: EFTab, active: Boolean, onSelect: (String) -> Unit) {
    val c = MaterialTheme.efColors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Radii.sm))
            .clickable(interactionSource = interaction, indication = null) { onSelect(tab.key) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = if (active) c.brand else c.textMuted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(5.dp))
        Text(tab.label, color = if (active) c.textPrimary else c.textMuted, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, fontFamily = BodyFont)
    }
}
