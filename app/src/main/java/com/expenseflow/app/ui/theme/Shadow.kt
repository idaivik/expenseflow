package com.expenseflow.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft, diffuse, slightly-violet drop shadow — the design's signature look
 * (tokens/effects.css `--shadow-*`). Ported from the original CommonUI helper.
 */
fun Modifier.softShadow(
    color: Color = Color(0xFF1E193C).copy(alpha = 0.07f),
    borderRadius: Dp = 0.dp,
    blurRadius: Dp = 20.dp,
    offsetY: Dp = 8.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.dp,
) = this.drawBehind {
    if (color.alpha == 0f) return@drawBehind
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        if (blurRadius > 0.dp) {
            frameworkPaint.maskFilter = BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
        frameworkPaint.color = color.toArgb()

        val spreadPx = spread.toPx()
        val left = offsetX.toPx() - spreadPx
        val top = offsetY.toPx() - spreadPx
        val right = size.width + offsetX.toPx() + spreadPx
        val bottom = size.height + offsetY.toPx() + spreadPx

        canvas.drawRoundRect(
            left = left, top = top, right = right, bottom = bottom,
            radiusX = borderRadius.toPx(), radiusY = borderRadius.toPx(), paint = paint,
        )
    }
}

/** `--shadow-card`: 0 8px 24px rgba(30,25,60,.07). */
fun Modifier.cardShadow(cornerRadius: Dp = Radii.md) =
    softShadow(color = Color(0xFF1E193C).copy(alpha = 0.07f), borderRadius = cornerRadius, blurRadius = 22.dp, offsetY = 8.dp)

/** `--shadow-sm`: 0 2px 8px rgba(24,24,40,.06). */
fun Modifier.smallShadow(cornerRadius: Dp = Radii.md) =
    softShadow(color = Color(0xFF181828).copy(alpha = 0.06f), borderRadius = cornerRadius, blurRadius = 10.dp, offsetY = 3.dp)

/** `--shadow-lg`: 0 20px 48px rgba(30,25,60,.14) — popovers / floating menus. */
fun Modifier.largeShadow(cornerRadius: Dp = Radii.md) =
    softShadow(color = Color(0xFF1E193C).copy(alpha = 0.14f), borderRadius = cornerRadius, blurRadius = 30.dp, offsetY = 14.dp)

/** `--shadow-fab`: 0 10px 24px rgba(31,87,230,.42) — the FAB's colored glow. */
fun Modifier.fabShadow(cornerRadius: Dp = Radii.pill) =
    softShadow(color = Blue600.copy(alpha = 0.42f), borderRadius = cornerRadius, blurRadius = 22.dp, offsetY = 10.dp)

/** `--shadow-brand`: 0 8px 20px rgba(31,87,230,.30) — brand button. */
fun Modifier.brandShadow(cornerRadius: Dp = Radii.pill) =
    softShadow(color = Blue600.copy(alpha = 0.30f), borderRadius = cornerRadius, blurRadius = 18.dp, offsetY = 8.dp)
