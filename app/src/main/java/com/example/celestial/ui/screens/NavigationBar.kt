package com.example.celestial.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.celestial.R
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

// Khaki color definition (adjust if you have your own palette)
private val DarkKhaki = Color(0xFF786A58) // Dark khaki brown

// --- DATA ---

sealed class AnimatedNavScreen(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val route: String
) {
    object Home        : AnimatedNavScreen("Home",       Icons.Filled.Home,         Icons.Outlined.Home,         "home")
    object Expiry      : AnimatedNavScreen("Expiry",     Icons.Filled.EventNote,    Icons.Outlined.EventNote,    "expiry")
    object Dashboard   : AnimatedNavScreen("Dashboard",  Icons.Filled.Dashboard,    Icons.Outlined.Dashboard,    "sales_dashboard")
    object Report      : AnimatedNavScreen("Report",     Icons.Filled.Assessment,   Icons.Outlined.Assessment,   "sales_report")
    object ManageSales : AnimatedNavScreen("Sales",      Icons.Filled.PointOfSale,  Icons.Outlined.PointOfSale,  "sales_management")
    object Inventory   : AnimatedNavScreen("Inventory",  Icons.Filled.Inventory2,   Icons.Outlined.Inventory2,   "inventory")
}

// --- ANIMATED ICON FLIP ---

@Composable
fun FlipIcon(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    contentDescription: String,
) {
    val animationRotation by animateFloatAsState(
        targetValue = if (isActive) 180f else 0f,
        animationSpec = spring(
            stiffness = 100f,   // Lower (default is 1000), makes it bouncy/slow
            dampingRatio = 0.35f // Lower makes the bounce more pronounced
        )
    )
    Box(
        modifier = modifier.graphicsLayer {
            rotationY = animationRotation
            cameraDistance = 12 * density
            scaleX = 1f + 0.18f * abs(sin(Math.toRadians(animationRotation.toDouble()))).toFloat()
            scaleY = 1f + 0.18f * abs(sin(Math.toRadians(animationRotation.toDouble()))).toFloat()
        },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (animationRotation > 90f) activeIcon else inactiveIcon,
            contentDescription = contentDescription,
            tint = DarkKhaki // Use dark khaki for icon
        )
    }
}

// --- NAV ITEM ---

@Composable
private fun AnimatedBottomNavItem(
    screen: AnimatedNavScreen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedElevation by animateDpAsState(targetValue = if (isSelected) 15.dp else 0.dp)
    val animatedAlpha by animateFloatAsState(targetValue = if (isSelected) 1f else .5f)
    val animatedIconSize by animateDpAsState(
        targetValue = if (isSelected) 30.dp else 22.dp,
        animationSpec = spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
        )
    )

    val navItemModifier = if (isSelected) {
        Modifier
            .wrapContentHeight()
            .shadow(elevation = animatedElevation, shape = RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = Color.Black,
                shape = RoundedCornerShape(16.dp)
            )
            .background(color = Color.Transparent, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
    } else {
        Modifier
            .height(40.dp)
            .shadow(elevation = animatedElevation, shape = RoundedCornerShape(16.dp))
            .background(color = Color.Transparent, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
    }

    Column(
        modifier = navItemModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FlipIcon(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(animatedIconSize)
                .alpha(animatedAlpha),
            isActive = isSelected,
            activeIcon = screen.activeIcon,
            inactiveIcon = screen.inactiveIcon,
            contentDescription = screen.title
        )
        AnimatedVisibility(visible = isSelected) {
            Text(
                text = screen.title,
                color = DarkKhaki, // Use dark khaki for label
                modifier = Modifier
                    .padding(top = 2.dp, start = 2.dp, end = 2.dp),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// --- MAIN ANIMATED BAR ---

@Composable
fun AnimatedBottomNavigationBar(
    screens: List<AnimatedNavScreen>,
    selectedIndex: Int,
    onScreenSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 2.dp,
                color = Color.Black,
                shape = RoundedCornerShape(24.dp)
            )
            .background(color = Color.Transparent, shape = RoundedCornerShape(24.dp)) // fallback
    ) {
        // Bar background image (below all nav items)
        Image(
            painter = painterResource(id = R.drawable.celestial_paper_text),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )

        // Navigation Row (above background)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEachIndexed { idx, screen ->
                val isSelected = idx == selectedIndex
                val animatedWeight by animateFloatAsState(
                    targetValue = if (isSelected) 1.5f else 1f
                )
                Box(
                    modifier = Modifier.weight(animatedWeight),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedBottomNavItem(
                        screen = screen,
                        isSelected = isSelected,
                        onClick = { onScreenSelected(idx) }
                    )
                }
            }
        }
    }
}
