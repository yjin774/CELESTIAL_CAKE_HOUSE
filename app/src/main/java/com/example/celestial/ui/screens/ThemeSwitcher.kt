package com.example.celestial.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.*
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.Color


private fun DrawScope.drawStar(
    brush: Brush,
    centerOffset: Offset,
    radius: Float,
    alpha: Float = 1f,
) {
    val leverage = radius * 0.1f
    val starPath = Path().apply {
        moveTo(centerOffset.x - radius, centerOffset.y)
        quadraticBezierTo(centerOffset.x - leverage, centerOffset.y - leverage, centerOffset.x, centerOffset.y - radius)
        quadraticBezierTo(centerOffset.x + leverage, centerOffset.y - leverage, centerOffset.x + radius, centerOffset.y)
        quadraticBezierTo(centerOffset.x + leverage, centerOffset.y + leverage, centerOffset.x, centerOffset.y + radius)
        quadraticBezierTo(centerOffset.x - leverage, centerOffset.y + leverage, centerOffset.x - radius, centerOffset.y)
    }
    drawPath(starPath, brush = brush, alpha = alpha)
}

private fun DrawScope.drawRays(
    color: Color,
    radius: Float,
    rayWidth: Float,
    rayLength: Float,
    alpha: Float = 1f,
    rayCount: Int = 8
) {
    for (i in 0 until rayCount) {
        val angle = (2 * Math.PI * i / rayCount).toFloat()
        val startX = center.x + radius * cos(angle)
        val startY = center.y + radius * sin(angle)
        val endX = center.x + (radius + rayLength) * cos(angle)
        val endY = center.y + (radius + rayLength) * sin(angle)
        drawLine(
            color = color,
            alpha = alpha,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            cap = StrokeCap.Round,
            strokeWidth = rayWidth
        )
    }
}

private fun DrawScope.drawMoonToSun(radius: Float, progress: Float, sunGradient: Brush, color: Color) {
    val center = this.center
    val mainCircle = Path().apply { addOval(Rect(center, radius)) }
    val initialOffset = center - Offset(radius * 2.3f, radius * 2.3f)
    val offset = (radius * 1.8f) * progress
    val subtractCircle = Path().apply {
        addOval(Rect(initialOffset + Offset(offset, offset), radius))
    }
    val moonToSunPath = Path().apply { op(mainCircle, subtractCircle, PathOperation.Difference) }
    val brush: Brush = if (progress < 0.99f) sunGradient else SolidColor(color)
    drawPath(moonToSunPath, brush = brush)
}

@Composable
fun ThemeSwitcher(
    isMoon: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = tween(400),
    size: Dp = 48.dp, // controls all scaling
    onClick: (() -> Unit)? = null
) {
    val progress by animateFloatAsState(
        targetValue = if (isMoon) 1f else 0f,
        animationSpec = animationSpec
    )
    val clickableModifier = if (onClick != null) {
        modifier.size(size).aspectRatio(1f).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        modifier.size(size).aspectRatio(1f)
    }
    Canvas(modifier = clickableModifier) {
        val width = this.size.width
        val height = this.size.height
        val baseRadius = width * 0.25f
        val extraRadius = width * 0.2f * progress
        val radius = baseRadius + extraRadius

        // Gradient for the sun and stars
        val sunGradient = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF59D),
                Color(0xFFFFEA00),
                Color(0xFFFFC107)
            ),
            center = this.center,
            radius = radius * 1.5f
        )

        rotate(180f * (1 - progress)) {
            val raysProgress = if (progress < 0.5f) (progress / 0.85f) else 0f
            drawRays(
                color = color,
                alpha = if (progress < 0.5f) 1f else 0f,
                radius = (radius * 1.5f) * (1f - raysProgress),
                rayWidth = radius * 0.3f,
                rayLength = radius * 0.2f
            )
            drawMoonToSun(radius, progress, sunGradient, color)
        }
        val starProgress = if (progress > 0.8f) ((progress - 0.8f) / 0.2f) else 0f
        drawStar(
            brush = sunGradient,
            centerOffset = Offset(width * 0.4f, height * 0.4f),
            radius = (height * 0.05f) * starProgress,
            alpha = starProgress
        )
        drawStar(
            brush = sunGradient,
            centerOffset = Offset(width * 0.2f, height * 0.2f),
            radius = (height * 0.1f) * starProgress,
            alpha = starProgress
        )
    }
}
