package com.example.celestial.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.random.Random

@Composable
fun DayBackground(
    modifier: Modifier = Modifier,
    cloudCount: Int = 30,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val clouds = remember {
        List(cloudCount) {
            CloudParams(
                y = Random.nextFloat() * 0.8f + 0.05f,
                scale = 0.7f + Random.nextFloat() * 1.3f,
                speedPxPerSec = 30f + Random.nextFloat() * 100f,
                alpha = 0.35f + Random.nextFloat() * 0.55f,
                flip = Random.nextBoolean(),
                initialOffset = Random.nextFloat() // initial horizontal [0..1)
            )
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        // Sky
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFFb6eaff), Color(0xFF90caf9))
                ),
                size = size
            )
        }

        // Timestamp for animation
        val timeState = remember { mutableStateOf(0L) }
        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos { now -> timeState.value = now }
            }
        }

        // Clouds in continuous loop
        clouds.forEach { cloud ->
            Canvas(Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val cloudWidth = 120f * cloud.scale // must match your drawing logic!
                val totalTravel = width + cloudWidth

                // Compute progress percentage based on time and speed
                val seconds = timeState.value / 1_000_000_000f
                val delta = (seconds * cloud.speedPxPerSec + cloud.initialOffset * totalTravel) % totalTravel

                // Cloud moves from right (width) to left (-cloudWidth)
                val cX = width - delta
                val cY = cloud.y * height

                translate(left = cX, top = cY) {
                    drawCloud(
                        scale = cloud.scale,
                        alpha = cloud.alpha,
                        flipped = cloud.flip
                    )
                }
            }
        }
        content()
    }
}

private data class CloudParams(
    val y: Float,
    val scale: Float,
    val speedPxPerSec: Float,
    val alpha: Float,
    val flip: Boolean,
    val initialOffset: Float
)


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(
    scale: Float,
    alpha: Float,
    flipped: Boolean
) {
    val baseW = 120f * scale
    val baseH = 50f * scale
    if (flipped) {
        scale(-1f, 1f, pivot = Offset(baseW / 2, baseH / 2)) {
            drawCloudShape(baseW, baseH, scale, alpha)
        }
    } else {
        drawCloudShape(baseW, baseH, scale, alpha)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudShape(
    baseW: Float,
    baseH: Float,
    scale: Float,
    alpha: Float
) {
    drawRoundRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(baseW, baseH),
        cornerRadius = CornerRadius(24f * scale)
    )
    drawOval(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(-25f * scale, 8f * scale),
        size = androidx.compose.ui.geometry.Size(50f * scale, 40f * scale)
    )
    drawOval(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(95f * scale, 11f * scale),
        size = androidx.compose.ui.geometry.Size(42f * scale, 35f * scale)
    )
    drawOval(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(30f * scale, -16f * scale),
        size = androidx.compose.ui.geometry.Size(67f * scale, 40f * scale)
    )
}


