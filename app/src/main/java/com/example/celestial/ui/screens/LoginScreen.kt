package com.example.celestial.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import com.example.celestial.R
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.celestial.ui.components.StarFieldBackground
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.example.celestial.ui.viewmodels.LoginViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@Composable
fun CustomPopupDialog(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundRes: Int = R.drawable.celestial_paper_text,
    textColor: Color = Color(0xFF6B5A42),
    borderColor: Color = Color.Black
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color.White)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Use background image
            Image(
                painter = painterResource(id = backgroundRes),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
            )
            Text(
                message,
                color = textColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .padding(24.dp)
            )
        }
    }
}


// --- Linear black & white gradient (vivid vs. faded) ---
fun animatedLinearBWBrush(phase: Float, vivid: Boolean): Brush {
    val gradientFieldWidthPx = 1200f
    return Brush.linearGradient(
        colors = if (vivid)
            listOf(
                Color.White,
                Color.Black,
                Color.White
            )
        else
            listOf(
                Color.White.copy(alpha = 0.43f),
                Color.Black.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.35f)
            ),
        start = Offset(gradientFieldWidthPx * phase, 0f),
        end = Offset(gradientFieldWidthPx * phase + 600f, 0f),
        tileMode = TileMode.Clamp
    )
}

@Composable
fun AnimatedBWGradientIcon(imageVector: ImageVector, contentDescription: String?, brush: Brush) {
    Icon(
        painter = rememberVectorPainter(image = imageVector),
        contentDescription = contentDescription,
        tint = Color.Unspecified,
        modifier = Modifier.size(24.dp).drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcIn)
            }
        }
    )
}

@Composable
fun AnimatedBWLinearTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable (Brush) -> Unit)? = null
) {
    val anim = remember { Animatable(0f) }
    var isFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2300, easing = LinearEasing)
            )
            anim.snapTo(0f)
        }
    }
    val phase = anim.value
    val vivid = isFocused
    val brush = animatedLinearBWBrush(phase, vivid)

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    style = TextStyle(
                        brush = brush,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            },
            leadingIcon = if (leadingIcon != null) { { leadingIcon(brush) } } else null,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .onFocusChanged { isFocused = it.isFocused },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = Color.White.copy(alpha = 0.93f)
            ),
            singleLine = true
        )
        // Animated gradient border overlay
        Box(
            Modifier
                .matchParentSize()
                .border(
                    3.dp,
                    brush,
                    RoundedCornerShape(16.dp)
                )
        )
    }
}

@Composable
fun AnimatedBWGradientButton(
    text: String,
    onClick: () -> Unit,
    brush: Brush,
    modifier: Modifier = Modifier,
    enabled: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .widthIn(min = 200.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(5.dp, brush, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 30.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        )
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: InventoryViewModel) {
    var username by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showInvalidDialog by rememberSaveable { mutableStateOf(false) }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    // khaki for text, darker khaki for extra style
    val darkerKhaki = Color(0xFF6B5A42)
    val context = LocalContext.current

    // Animation phase for gradient (reuse your utility)
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            anim.animateTo(1f, tween(2300, easing = LinearEasing))
            anim.snapTo(0f)
        }
    }
    val phase = anim.value
    val buttonBrush = animatedLinearBWBrush(phase, vivid = true)

    if (showSuccessDialog) {
        CustomPopupDialog(
            message = "LOGGED IN SUCCESSFUL",
            onDismiss = {},
            backgroundRes = R.drawable.celestial_paper_text,
            textColor = darkerKhaki,
            borderColor = Color.Black
        )
        LaunchedEffect(Unit) {
            delay(1500)
            showSuccessDialog = false
            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
        viewModel.clearData()
    }

    // Show invalid dialog for 1.5 seconds
    if (showInvalidDialog) {
        CustomPopupDialog(
            message = "INVALID USERNAME OR PASSWORD",
            onDismiss = {},
            backgroundRes = R.drawable.celestial_paper_text,
            textColor = darkerKhaki,
            borderColor = Color.Black
        )
        LaunchedEffect(Unit) {
            delay(1500)
            showInvalidDialog = false
        }
    }

    if (showErrorDialog && errorMessage != null) {
        ErrorDialog(errorMessage = errorMessage ?: "") {
            showErrorDialog = false
            errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            StarFieldBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedBWLinearTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedBWLinearTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp), // Same as the login button
                        contentAlignment = Alignment.Center
                    ) {
                        LineSpinFadeLoaderIndicator(
                            colors = listOf(Color.White),
                            rectCount = 8,
                            penThickness = 10f,            // Reduce the thickness for compact look
                            radius = 38f,                  // Adjust radius to fit height (try 16-20)
                            elementHeight = 12f,           // Shorter elements for tighter ring
                            minAlpha = 0.3f,
                            maxAlpha = 1.0f,
                            linearAnimationType = LinearAnimationType.CIRCULAR
                        )
                    }
                } else {
                    AnimatedBWGradientButton(
                        text = "Login",
                        onClick = {
                            // Field shouldn't be empty
                            if (username.trim().isBlank() || password.isBlank()) {
                                errorMessage = "Username and password must not be empty."
                                showErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            // Custom: basic username rule
                            if (username.contains(" ")) {
                                errorMessage = "Username cannot contain spaces."
                                showErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            if (password.length < 6) {
                                errorMessage = "Password must be at least 6 characters."
                                showErrorDialog = true
                                return@AnimatedBWGradientButton
                            }

                            // Start loading and authenticate
                            isLoading = true
                            viewModel.loginWithUsername(
                                username.trim(),
                                password,
                                onSuccess = {
                                    viewModel.handleSuccessfulLogin(context)
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onFailure = {
                                    isLoading = false
                                    errorMessage = "Invalid username or password."
                                    showErrorDialog = true
                                }
                            )
                        },
                        brush = animatedLinearBWBrush(0f, vivid = true),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = true
                    )

                }
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedBWGradientButton(
                    text = "Register",
                    onClick = { navController.navigate("register") },
                    brush = animatedLinearBWBrush(0f, vivid = true),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            }
        }
    }
}


private fun validateInput(email: String, password: String): Boolean {
    return email.contains("@") && email.contains(".") && password.length >= 6
}