package com.expenseflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.CategoryTone
import com.expenseflow.app.ui.theme.Radii
import com.expenseflow.app.ui.theme.cardShadow
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.smallShadow

enum class EFElevation { None, Sm, Card }

/** The atomic surface: white/dark card, soft shadow, hairline border, rounded corners. */
@Composable
fun EFCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radii.md,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    elevation: EFElevation = EFElevation.Sm,
    border: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.efColors
    val shape = RoundedCornerShape(cornerRadius)
    val shadowMod = when (elevation) {
        EFElevation.None -> Modifier
        EFElevation.Sm -> Modifier.smallShadow(cornerRadius)
        EFElevation.Card -> Modifier.cardShadow(cornerRadius)
    }
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(shadowMod)
            .clip(shape)
            .background(c.surfaceCard)
            .then(if (border) Modifier.border(1.dp, c.borderSubtle, shape) else Modifier)
            .then(
                if (onClick != null)
                    Modifier.pressScale(interaction, 0.985f)
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                else Modifier,
            )
            .padding(contentPadding),
        content = content,
    )
}

/** Rounded tinted tile holding a colored category glyph. */
@Composable
fun CategoryIcon(
    tone: CategoryTone,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    cornerRadius: Dp = 14.dp,
) {
    val c = MaterialTheme.efColors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(c.tint(tone)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.cat(tone), modifier = Modifier.size(size * 0.46f))
    }
}

/** "Your Money" income / expense summary tile. */
@Composable
fun StatCard(
    label: String,
    value: Double,
    tone: CategoryTone,
    icon: ImageVector,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier,
    info: Boolean = true,
) {
    val c = MaterialTheme.efColors
    EFCard(modifier = modifier, cornerRadius = Radii.md, contentPadding = PaddingValues(18.dp), elevation = EFElevation.Sm) {
        CategoryIcon(tone = tone, icon = icon, size = 44.dp, cornerRadius = 13.dp)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = c.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = BodyFont)
            if (info) {
                Box(
                    modifier = Modifier.size(15.dp).border(1.4.dp, c.textMuted, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "i", color = c.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic, lineHeight = 9.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // Kept smaller than the hero "Current Balance" figure (46.sp) so that stays
        // the single biggest, most prominent number on the Home screen.
        Amount(value = value, size = 20.sp, color = c.textPrimary, currencySymbol = currencySymbol)
    }
}
