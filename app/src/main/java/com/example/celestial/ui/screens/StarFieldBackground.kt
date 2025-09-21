package com.example.celestial.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

data class Star(val x: Float, val y: Float, val alpha: Float)

@Composable
fun StarFieldBackground(
    starCount: Int = 1000,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val stars = remember(widthPx, heightPx, starCount) {
            buildList {
                repeat(starCount) {
                    val x = (Math.random() * widthPx).toFloat()
                    val y = (Math.random() * heightPx).toFloat()
                    val alpha = Math.random().toFloat()
                    add(Star(x, y, alpha))
                }
            }
        }
        val infiniteTransition = rememberInfiniteTransition(label = "StarTwinkle")
        val twinkle by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "twinkle"
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.Black)
        ) {
            for (star in stars) {
                drawCircle(
                    color = Color.White,
                    center = Offset(star.x, star.y),
                    radius = 2f,
                    alpha = star.alpha * twinkle
                )
            }
        }
    }
}

