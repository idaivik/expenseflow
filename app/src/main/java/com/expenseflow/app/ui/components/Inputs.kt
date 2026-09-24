package com.expenseflow.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseflow.app.ui.theme.BodyFont
import com.expenseflow.app.ui.theme.Radii
import com.expenseflow.app.ui.theme.efColors
import com.expenseflow.app.ui.theme.smallShadow

@Composable
fun EFTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
) {
    val c = MaterialTheme.efColors
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier) {
        if (label != null) {
            Text(label, color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(Radii.sm))
                .background(c.surfaceInset)
                .border(1.dp, c.borderDefault, RoundedCornerShape(Radii.sm))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(10.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, color = c.textMuted, fontSize = 15.sp, fontFamily = BodyFont)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 15.sp, fontFamily = BodyFont),
                    cursorBrush = SolidColor(c.brand),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
                    ),
                    // Retract the keyboard (with the platform's own IME animation) as soon as
                    // the user taps Done/Enter, instead of leaving it stuck open on the field.
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            trailingContent?.let {
                Spacer(Modifier.size(8.dp))
                it()
            }
        }
    }
}

/**
 * Pill segmented control — mirrors components/navigation/SegmentedControl.jsx:
 * sunken track (padding 5), a white soft-shadow thumb that springs between
 * segments, 15px semibold labels (active = ink, inactive = secondary).
 */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.efColors
    val idx = options.indexOfFirst { it.second == selected }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.pill))
            .background(c.surfaceSunken)
            .padding(5.dp),
    ) {
        val segWidth = maxWidth / options.size
        val thumbX by animateDpAsState(
            targetValue = segWidth * idx,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "segThumb",
        )
        Box(
            Modifier
                .offset(x = thumbX)
                .width(segWidth)
                .height(40.dp)
                .smallShadow(Radii.pill)
                .clip(RoundedCornerShape(Radii.pill))
                .background(c.surfaceCard),
        )
        Row {
            options.forEach { (label, value) ->
                val active = value == selected
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(Radii.pill))
                        .clickable(interactionSource = interaction, indication = null) { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (active) c.textPrimary else c.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = BodyFont,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
