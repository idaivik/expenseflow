package com.expenseflow.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.theme.Radii
import com.expenseflow.app.ui.theme.brandShadow
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.smallShadow
import androidx.compose.material3.MaterialTheme

enum class EFButtonVariant { Primary, Secondary, Tonal, Ghost }
enum class EFButtonSize { Sm, Md, Lg }

@Composable
fun EFButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: EFButtonVariant = EFButtonVariant.Primary,
    size: EFButtonSize = EFButtonSize.Lg,
    fullWidth: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentColorOverride: Color? = null,
) {
    val c = MaterialTheme.efColors
    val height = when (size) { EFButtonSize.Sm -> 40.dp; EFButtonSize.Md -> 48.dp; EFButtonSize.Lg -> 54.dp }
    val fontSize = when (size) { EFButtonSize.Sm -> 14.sp; EFButtonSize.Md -> 15.sp; EFButtonSize.Lg -> 16.sp }
    val hPad = if (size == EFButtonSize.Sm) 16.dp else 22.dp

    val container: Color = when (variant) {
        EFButtonVariant.Primary -> c.brand
        EFButtonVariant.Secondary -> c.surfaceCard
        EFButtonVariant.Tonal -> c.brandSoft
        EFButtonVariant.Ghost -> Color.Transparent
    }
    val content: Color = contentColorOverride ?: when (variant) {
        EFButtonVariant.Primary -> c.onBrand
        EFButtonVariant.Secondary -> c.textPrimary
        EFButtonVariant.Tonal -> c.brand
        EFButtonVariant.Ghost -> c.brand
    }
    val border: BorderStroke? = if (variant == EFButtonVariant.Secondary) BorderStroke(1.dp, c.borderDefault) else null

    val interaction = remember { MutableInteractionSource() }
    var base = modifier
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .height(height)
        .pressScale(interaction)
    if (variant == EFButtonVariant.Primary && enabled) base = base.brandShadow(Radii.pill)

    Box(
        modifier = base
            .clip(RoundedCornerShape(Radii.pill))
            .background(container)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(Radii.pill)) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = hPad),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leadingIcon?.let { Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(19.dp)) }
            Text(text, color = content, fontSize = fontSize, fontWeight = FontWeight.Bold)
            trailingIcon?.let { Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(19.dp)) }
        }
    }
}

/**
 * Circular icon button — mirrors components/core/IconButton.jsx.
 * `variant`: Soft = white card circle w/ soft shadow + hairline border (used on
 * the hero bell too), Plain = transparent (ghost), Filled = brand.
 * Set [dot] to show the small red notification dot ringed in the surface color.
 */
enum class EFIconButtonVariant { Soft, Plain, Filled }

@Composable
fun EFIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: EFIconButtonVariant = EFIconButtonVariant.Soft,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    tint: Color? = null,
    dot: Boolean = false,
    contentDescription: String? = null,
    onLight: Boolean = false, // kept for call-site compatibility; Soft is white everywhere per design
) {
    val c = MaterialTheme.efColors
    val interaction = remember { MutableInteractionSource() }
    val container = when (variant) {
        EFIconButtonVariant.Soft -> c.surfaceCard
        EFIconButtonVariant.Filled -> c.brand
        EFIconButtonVariant.Plain -> if (onLight) Color.White.copy(alpha = 0.20f) else Color.Transparent
    }
    val iconTint = tint ?: when {
        variant == EFIconButtonVariant.Filled -> c.onBrand
        variant == EFIconButtonVariant.Plain && onLight -> Color.White
        else -> c.textPrimary
    }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction, pressedScale = 0.92f)
            .then(if (variant == EFIconButtonVariant.Soft) Modifier.smallShadow(size / 2) else Modifier)
            .clip(CircleShape)
            .background(container)
            .then(if (variant == EFIconButtonVariant.Soft) Modifier.border(1.dp, c.borderSubtle, CircleShape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(size * 0.45f))
        if (dot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = size * 0.18f - 2.dp, end = size * 0.18f - 2.dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(container)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(c.catRed),
            )
        }
    }
}
