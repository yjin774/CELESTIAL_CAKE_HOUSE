package com.example.celestial.ui.screens

import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.tween
import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import java.net.URLEncoder
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LoadingAnimationScreen(
    navController: NavController,
    reportText: String

) {
    var animationFinished by remember { mutableStateOf(false) }
    var percent by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val duration = 5000
        val steps = 100
        val delayMillis = duration / steps
        for (i in 0..steps) {
            percent = i
            delay(delayMillis.toLong())
        }
        animationFinished = true
    }

    // Navigation after done
    LaunchedEffect(animationFinished) {
        if (animationFinished) {
            navController.navigate(
                "sales_dashboard?showReport=true&reportText=${URLEncoder.encode(reportText, "UTF-8")}"
            ) {
                popUpTo("water_drop_animation") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // ==== Main loading animation content lines up here, over the background ====
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LineSpinFadeLoaderIndicator(
                colors = listOf(
                    Color(0xFFFFF9C4), // lightest yellow
                    Color(0xFFFFF59D),
                    Color(0xFFFFF176),
                    Color(0xFFFFEE58), // darker yellow
                    Color(0xFFFFEB3B),
                    Color(0xFFFDD835),
                    Color(0xFFFBC02D),
                    Color(0xFFF9A825)  // darkest yellow
                ),
                rectCount = 8,
                penThickness = 20f,
                radius = 90f,
                elementHeight = 38f,
                minAlpha = 0.3f,
                maxAlpha = 1.0f,
                linearAnimationType = LinearAnimationType.CIRCULAR
            )
            Spacer(Modifier.height(42.dp))
            ShimmeringTextWithPercent(percent = percent)
        }
    }
}

// Unified shimmer for text and percent
@Composable
fun ShimmeringTextWithPercent(
    percent: Int,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFFFFF9C4), // lightest yellow
        Color(0xFFFFF59D),
        Color(0xFFFFF176),
        Color(0xFFFFEE58), // darker yellow
        Color(0xFFFFEB3B),
        Color(0xFFFDD835),
        Color(0xFFFBC02D),
        Color(0xFFF9A825)  // darkest yellow
    ),
    textStyle: TextStyle = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    animationSpec: DurationBasedAnimationSpec<Float> = tween(durationMillis = 1000, delayMillis = 500, easing = LinearEasing)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShimmeringTextTransition")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animationSpec),
        label = "ShimmerProgress"
    )

    val brush = remember(shimmerProgress) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val initialXOffset = -size.width
                val totalSweepDistance = size.width * 2
                val currentPosition = initialXOffset + totalSweepDistance * shimmerProgress
                // Prepend and append transparent to the gradient colors
                val shaderColors = listOf(Color.Transparent) + colors + listOf(Color.Transparent)
                return LinearGradientShader(
                    colors = shaderColors,
                    from = Offset(currentPosition, 0f),
                    to = Offset(currentPosition + size.width, 0f)
                )
            }
        }
    }


    Text(
        text = "LOADING $percent%",
        modifier = modifier.height(38.dp),
        style = textStyle.copy(brush = brush)
    )
}

enum class LinearAnimationType(val circleDelay: Long, val animDuration: Int) {
    CIRCULAR(circleDelay = 80L, animDuration = 700),
    SKIP_AND_REPEAT(circleDelay = 80L, animDuration = 850)
}

@Composable
fun LineSpinFadeLoaderIndicator(
    colors: List<Color> = listOf(Color(0xFFFFF176)), // Provide any non-empty color list!
    rectCount: Int = 8,
    linearAnimationType: LinearAnimationType = LinearAnimationType.CIRCULAR,
    penThickness: Float = 25f,
    radius: Float = 55f,
    elementHeight: Float = 20f,
    minAlpha: Float = 0.2f,
    maxAlpha: Float = 1.0f
) {
    require(colors.isNotEmpty()) { "colors list must NOT be empty" }
    val angleStep = 360f / rectCount
    val outerRadius = radius + elementHeight
    val innerRadius = radius
    val alphas = (1..rectCount).map { index ->
        var alpha: Float by remember { mutableStateOf(minAlpha) }
        LaunchedEffect(key1 = Unit) {
            when (linearAnimationType) {
                LinearAnimationType.CIRCULAR,
                LinearAnimationType.SKIP_AND_REPEAT -> {
                    delay(linearAnimationType.circleDelay * index)
                }
            }
            animate(
                initialValue = minAlpha,
                targetValue = maxAlpha,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = linearAnimationType.animDuration),
                    repeatMode = RepeatMode.Reverse,
                )
            ) { value, _ -> alpha = value }
        }
        alpha
    }
    Canvas(modifier = Modifier.size(((radius + elementHeight + penThickness) * 2).dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        for (index in 0 until rectCount) {
            // Pick color by cycling through the provided colors list
            val color = colors[index % colors.size]
            val angle = index * angleStep
            val startX = center.x + innerRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val startY = center.y + innerRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val endX = center.x + outerRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val endY = center.y + outerRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = penThickness * alphas[index],
                alpha = alphas[index],
                cap = StrokeCap.Round,
            )
        }
    }
}

