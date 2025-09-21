package com.example.celestial.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.celestial.R
import com.example.celestial.ui.components.StarFieldBackground
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.example.celestial.ui.viewmodels.InventoryViewModel.RegisterEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import android.app.DatePickerDialog
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState

@Composable
fun SuccessDialog(
    message: String,
    onDismiss: () -> Unit
) {
    val darkKhaki = Color(0xFF8B7355)
    LaunchedEffect(message) {
        delay(1500)
        onDismiss()
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .widthIn(min = 320.dp, max = 420.dp)
                .heightIn(min = 120.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                message,
                color = darkKhaki,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    val darkKhaki = Color(0xFF8B7355)
    LaunchedEffect(errorMessage) {
        // Auto-dismiss after 1.5 seconds
        delay(1000)
        onDismiss()
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .widthIn(min = 320.dp, max = 420.dp)
                .heightIn(min = 120.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                errorMessage,
                color = darkKhaki,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    genderOptions: List<String>
) {
    val focusRequester = remember { FocusRequester() }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    Box {
        AnimatedBWLinearTextField(
            value = value,
            onValueChange = {}, // prevent direct editing
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    expanded = state.isFocused
                    if (state.isFocused) keyboardController?.hide()
                }
                .focusRequester(focusRequester)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            genderOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: InventoryViewModel
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var hasShownPickerThisFocus by rememberSaveable { mutableStateOf(false) }
    var birthdayDialogOpen by rememberSaveable { mutableStateOf(false) }
    var gender by rememberSaveable { mutableStateOf("") }
    val genderOptions = listOf("MALE", "FEMALE", "GAYGAY")
    var genderExpanded by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showRegisterSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var fieldError by rememberSaveable { mutableStateOf<String?>(null) }
    var showFieldErrorDialog by rememberSaveable { mutableStateOf(false) }
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val calendar = remember { Calendar.getInstance() }

    fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                dateOfBirth = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDayOfMonth)
            },
            year, month, day
        ).show()
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            anim.animateTo(1f, animationSpec = tween(durationMillis = 2300, easing = LinearEasing))
            anim.snapTo(0f)
        }
    }
    val phase = anim.value
    val buttonBrush = animatedLinearBWBrush(phase, vivid = true)

    if (showRegisterSuccessDialog) {
        SuccessDialog(message = "REGISTERED SUCCESSFUL") {
            showRegisterSuccessDialog = false
            viewModel.setLoggedInUsername(username.trim())
            coroutineScope.launch {
                viewModel.saveUsername(context, username.trim())
            }
            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.registerEvents.collect { evt ->
            when (evt) {
                is RegisterEvent.Success -> {
                    showRegisterSuccessDialog = true
                    isLoading = false
                }
                is RegisterEvent.Error -> {
                    fieldError = evt.message
                    showFieldErrorDialog = true
                    isLoading = false
                }
            }
        }
    }

    if (showFieldErrorDialog && fieldError != null) {
        ErrorDialog(errorMessage = fieldError ?: "") {
            showFieldErrorDialog = false
            fieldError = null
        }
    }

    val screenContent: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            StarFieldBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedBWLinearTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedBWLinearTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedBWLinearTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedBWLinearTextField(
                    value = dateOfBirth,
                    onValueChange = {},
                    label = "Date of Birth",
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && !hasShownPickerThisFocus) {
                                showDatePicker()
                                hasShownPickerThisFocus = true
                            }
                            if (!focusState.isFocused) {
                                hasShownPickerThisFocus = false
                            }
                        }
                )
                Spacer(modifier = Modifier.height(4.dp))
                GenderDropdownField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = "Gender",
                    genderOptions = genderOptions
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedBWLinearTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedBWLinearTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Address",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LineSpinFadeLoaderIndicator(
                            colors = listOf(Color.White),
                            rectCount = 8,
                            penThickness = 20f,
                            radius = 90f,
                            elementHeight = 38f,
                            minAlpha = 0.3f,
                            maxAlpha = 1.0f,
                            linearAnimationType = LinearAnimationType.CIRCULAR
                        )
                    }
                } else {
                    AnimatedBWGradientButton(
                        text = "Register",
                        onClick = {
                            if (username.isBlank()) {
                                fieldError = "Username is required."
                                showFieldErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                fieldError = "Please enter a valid email address."
                                showFieldErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            if (password.length < 6) {
                                fieldError = "Password must be at least 6 characters."
                                showFieldErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            if (fullName.isBlank()) {
                                fieldError = "Full name is required."
                                showFieldErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            if (dateOfBirth.isBlank()) {
                                fieldError = "Date of birth is required."
                                showFieldErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            if (gender.isBlank()) {
                                fieldError = "Gender is required."
                                showFieldErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            val phonePattern = Regex("^01\\d{8,9}$")
                            if (!phone.matches(phonePattern)) {
                                fieldError = "Phone number must start with 01 and have 10-11 digits."
                                showFieldErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            if (address.isBlank()) {
                                fieldError = "Address cannot be empty."
                                showFieldErrorDialog = true
                                return@AnimatedBWGradientButton
                            }
                            isLoading = true
                            viewModel.registerWithUsername(
                                username,
                                email,
                                password,
                                fullName,
                                dateOfBirth,
                                gender,
                                phone,
                                address,
                                onSuccess = {
                                    isLoading = false
                                    showRegisterSuccessDialog = true
                                },
                                onFailure = { msg ->
                                    isLoading = false
                                    fieldError = msg
                                    showFieldErrorDialog = true
                                },
                            )
                        },
                        brush = buttonBrush
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { navController.navigate("login") }) {
                    Text("Already have an account? Login", color = Color.White)
                }
                errorMessage?.let {
                    Text(
                        it,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 40.dp)
                    )
                }
            }
        }
    }

    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        val scrollState = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
            StarFieldBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                screenContent()
            }
        }
    } else {
        screenContent()
    }
}
