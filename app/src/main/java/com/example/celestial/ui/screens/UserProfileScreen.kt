package com.example.celestial.ui.screens

import android.util.Patterns
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.celestial.R
import com.example.celestial.ui.viewmodels.InventoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun UserProfileScreen(navController: NavController, viewModel: InventoryViewModel) {
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var editingField by rememberSaveable { mutableStateOf<String?>(null) }
    var editFieldValue by rememberSaveable { mutableStateOf("") }
    var showModeDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var showSuccessDialog by rememberSaveable { mutableStateOf<String?>(null) }
    val profile by viewModel.userProfile
    val coroutineScope = rememberCoroutineScope()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val genderOptions = listOf("MALE", "FEMALE", "GAYGAY")

    fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    // Check username taken function (suspend)
    suspend fun isUsernameTaken(username: String, currentUsername: String): Boolean {
        if (username.equals(currentUsername, ignoreCase = true)) return false
        val firestore = FirebaseFirestore.getInstance()
        val doc = firestore.collection("users_usernames").document(username.lowercase()).get().await()
        return doc.exists()
    }

    // Phone validation - example stub (replace with your actual RegisterScreen implementation)
    fun isValidPhoneNumber(phone: String): Boolean {
        // Simple example: check length and numeric
        return phone.length in 6..15 && phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
    }

    DisposableEffect(Unit) {
        onDispose { isEditMode = false }
    }

    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val black = Color.Black
    val darkKhaki = Color(0xFF8B7355)
    val paperBg = painterResource(id = R.drawable.celestial_paper_text)

    var showProgressDialog by rememberSaveable { mutableStateOf(false) }
    val deletionState by viewModel.deletionState.collectAsState()
    val darkRedBg = painterResource(id = R.drawable.celestial_dark_red_paper)

    var reauthPassword by rememberSaveable { mutableStateOf("") }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var dialogState by rememberSaveable { mutableStateOf("initial") } // initial -> counting -> ready
    var secondsLeft by rememberSaveable { mutableStateOf(5) }
    var showDeletedSuccess by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showDeletedDialog by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    var showReauthDialog by rememberSaveable { mutableStateOf(false) }
    var reauthError by rememberSaveable { mutableStateOf<String?>(null) }
    var isReauthLoading by rememberSaveable { mutableStateOf(false) }

    val deletionProgress by viewModel.deletionProgress.collectAsState()
    var showDeletionProgressDialog by rememberSaveable { mutableStateOf(false) }

    var validationMessage by rememberSaveable { mutableStateOf("") }
    var showValidationDialog by rememberSaveable { mutableStateOf(false) }


    if (showDeletedDialog) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                Text(
                    text = "DELETED SUCCESSFUL",
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }
    }

    errorMsg?.let {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Error") },
            text = { Text(it) },
            confirmButton = {
                Button(onClick = { errorMsg = null }) { Text("OK") }
            }
        )
    }

    showModeDialog?.let {
        ModeDialog(message = it, onDismiss = { showModeDialog = null })
    }
    // Success popup after editing a field
    showSuccessDialog?.let {
        ModeDialog(message = it, onDismiss = { showSuccessDialog = null })
    }
    // Field editing dialog
    editingField?.let { field ->
        EditFieldDialog(
            field = field,
            initialValue = editFieldValue,
            onValueChange = { editFieldValue = it },
            onDismiss = { editingField = null },
            onChange = { newValue ->
                coroutineScope.launch {
                    val success = viewModel.updateUserProfileField(field, newValue)
                    if (success) {
                        showSuccessDialog = "EDITED ${field.uppercase()} SUCCESSFUL"
                    } else {
                        showSuccessDialog = "FAILED TO EDIT ${field.uppercase()}"
                    }
                    editingField = null
                    delay(1500)
                    showSuccessDialog = null
                }
            }
        )


    }
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
    // Load profile and attach listeners (existing logic)
    LaunchedEffect(firebaseUid) {
        viewModel.startAuthBinding(context)
        Log.d("UserProfileVM", "Auth binding started")
        viewModel.startListeningUserProfile(context)
        Log.d("UserProfileVM", "Explicit startListeningUserProfile from screen")
        viewModel.ensureUserProfileFromUsername()
        viewModel.debugFetchUserDocOnce()
    }

    // Countdown to enable password entry
    LaunchedEffect(dialogState) {
        if (dialogState == "counting") {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            dialogState = "ready"
        }
    }

    LaunchedEffect(deletionProgress) {
        if (deletionProgress >= 1f) {
            showDeletedSuccess = true
            delay(1500) // Show success popup for 1.5 seconds
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            showDeletedSuccess = false // Hide popup after navigation
        }
    }

    if (deletionProgress < 1f && deletionProgress > 0f) {
        DeletionProgressDialog(
            percent = (deletionProgress * 100).toInt(),
            onDismissRequest = { /* disable dismiss while running */ }
        )
    }

    if (showDeletedSuccess) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 320.dp, height = 120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
            ) {
                Image(
                    painter = paperBg,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "ACCOUNT DELETED SUCCESSFUL",
                    color = darkKhaki,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 32.dp)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton { navController.popBackStack() }

            // Mode message box, visible beside BackButton
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .fillMaxWidth()
                    .height(45.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                    .background(Color.Transparent)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = if (isEditMode) "EDIT MODE" else "DISPLAY MODE",
                    color = darkKhaki,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.celestial_dark_khaki_paper),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "User Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Profile Overview", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = darkKhaki)
            Spacer(modifier = Modifier.height(12.dp))

            ProfileItemButton(
                label = "Username",
                value = profile?.username ?: "-",
                enabled = isEditMode,
                onClick = {
                    editingField = "username"
                    editFieldValue = profile?.username ?: ""
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            ProfileItemButton(
                label = "Email Address",
                value = profile?.email ?: "-",
                enabled = isEditMode,
                onClick = {
                    editingField = "email"
                    editFieldValue = profile?.email ?: ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = darkKhaki)
            Spacer(modifier = Modifier.height(12.dp))
            ProfileItemButton(
                label = "Full Name",
                value = profile?.fullName ?: "-",
                enabled = isEditMode,
                onClick = {
                    editingField = "full name"
                    editFieldValue = profile?.fullName ?: ""
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            ProfileItemButton(
                label = "Date of Birth",
                value = profile?.dateOfBirth ?: "-",
                enabled = isEditMode,
                onClick = {
                    // Show date picker - disable direct edit
                    showDatePicker = true
                }
            )
            Spacer(modifier = Modifier.height(6.dp))

            ProfileItemButton(
                label = "Gender",
                value = profile?.gender ?: "-",
                enabled = isEditMode,
                onClick = {
                    editingField = "gender"
                    editFieldValue = profile?.gender ?: ""
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            ProfileItemButton(
                label = "Phone Number",
                value = profile?.phone ?: "-",
                enabled = isEditMode,
                onClick = {
                    editingField = "phone number"
                    editFieldValue = profile?.phone ?: ""
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            ProfileItemButton(
                label = "Address",
                value = profile?.address ?: "-",
                enabled = isEditMode,
                onClick = {
                    editingField = "address"
                    editFieldValue = profile?.address ?: ""
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Edit (placeholder)
                Button(
                    onClick = { isEditMode = !isEditMode
                        showModeDialog = if (isEditMode) "EDIT MODE ACTIVATED" else "DISPLAY MODE"
                        coroutineScope.launch {
                            delay(1500)
                            showModeDialog = null
                        } },
                    modifier = Modifier
                        .weight(2f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = darkKhaki
                    ),
                    border = BorderStroke(2.dp, black),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = paperBg,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = darkKhaki)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEditMode) "DISACTIVATE" else "EDIT", color = darkKhaki)
                        }
                    }
                }

                // Log Out (placeholder)
                Button(
                    onClick = {
                        showLogoutDialog = true
                    },
                    modifier = Modifier
                        .weight(2f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = darkKhaki
                    ),
                    border = BorderStroke(2.dp, black),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = paperBg,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Log Out", tint = darkKhaki)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out", color = darkKhaki)
                        }
                    }
                }

                // Delete
                Button(
                    onClick = {
                        showDeleteDialog = true
                        dialogState = "initial"
                        reauthPassword = ""
                    },
                    modifier = Modifier
                        .weight(2f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(2.dp, black),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = darkRedBg,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (editingField != null) {
        when (editingField) {
            "date of birth" -> CustomDatePickerDialog(
                initialDate = profile?.dateOfBirth,
                onDateSelected = { date ->
                    coroutineScope.launch {
                        if (date != null) {
                            val success = viewModel.updateUserProfileField("date of birth", date)
                            validationMessage = if (success) "EDITED DATE OF BIRTH SUCCESSFUL" else "FAILED TO EDIT DATE OF BIRTH"
                        }
                        editingField = null
                        showValidationDialog = true
                    }
                },
                onDismissRequest = { editingField = null }
            )
            "gender" -> GenderSelectionDialog(
                currentSelection = profile?.gender,
                onGenderSelected = { gender ->
                    coroutineScope.launch {
                        val success = viewModel.updateUserProfileField("gender", gender)
                        validationMessage = if (success) "EDITED GENDER SUCCESSFUL" else "FAILED TO EDIT GENDER"
                        editingField = null
                        showValidationDialog = true
                    }
                },
                onDismissRequest = { editingField = null }
            )
            else -> EditFieldDialog(
                field = editingField!!,
                initialValue = editFieldValue,
                onValueChange = { editFieldValue = it },
                onDismiss = { editingField = null },
                onChange = { newValue ->
                    coroutineScope.launch {
                        when (editingField!!.lowercase()) {
                            "username" -> {
                                if (newValue.isBlank()) {
                                    validationMessage = "USERNAME CANNOT BE EMPTY"
                                    showValidationDialog = true
                                } else if (isUsernameTaken(newValue, profile?.username ?: "")) {
                                    validationMessage = "USERNAME IS TAKEN"
                                    showValidationDialog = true
                                } else {
                                    val success = viewModel.updateUserProfileField(editingField!!, newValue)
                                    validationMessage = if (success) "EDITED USERNAME SUCCESSFUL" else "FAILED TO EDIT USERNAME"
                                    showValidationDialog = true
                                }
                            }
                            "email", "email address" -> {
                                if (!isValidEmail(newValue)) {
                                    validationMessage = "PLEASE ENTER PROPER EMAIL FORMAT"
                                    showValidationDialog = true
                                } else {
                                    val success = viewModel.updateUserProfileField(editingField!!, newValue)
                                    validationMessage = if (success) "EDITED EMAIL SUCCESSFUL" else "FAILED TO EDIT EMAIL"
                                    showValidationDialog = true
                                }
                            }
                            "phone number", "phone" -> {
                                if (!isValidPhoneNumber(newValue)) {
                                    validationMessage = "INVALID PHONE NUMBER FORMAT"
                                    showValidationDialog = true
                                } else {
                                    val success = viewModel.updateUserProfileField(editingField!!, newValue)
                                    validationMessage = if (success) "EDITED PHONE NUMBER SUCCESSFUL" else "FAILED TO EDIT PHONE NUMBER"
                                    showValidationDialog = true
                                }
                            }
                            else -> {
                                val success = viewModel.updateUserProfileField(editingField!!, newValue)
                                validationMessage = if (success) "EDITED ${editingField!!.uppercase()} SUCCESSFUL" else "FAILED TO EDIT ${editingField!!.uppercase()}"
                                showValidationDialog = true
                            }
                        }
                        editingField = null
                    }
                }
            )
        }
    }

    if (showValidationDialog) {
        ValidationPopupDialog(message = validationMessage, onDismissRequest = {
            showValidationDialog = false  // dismiss dialog after 1.5s via LaunchedEffect
        })
    }
    // Delete confirmation dialog (existing)
    if (showDeleteDialog) {
        Dialog(onDismissRequest = {
            showDeleteDialog = false
            dialogState = "initial"
        }) {
            DeleteAccountDialogContent(
                paperBgRes = R.drawable.celestial_paper_text,
                onCancel = {
                    showDeleteDialog = false
                    dialogState = "initial"
                    secondsLeft = 5
                },
                dialogState = dialogState,
                secondsLeft = secondsLeft,
                onConfirmOrDelete = {
                    when (dialogState) {
                        "initial" -> {
                            dialogState = "counting"
                            // Launch countdown effect outside this lambda
                        }
                        "ready" -> {
                            // Hide dialog & reset
                            showDeleteDialog = false
                            dialogState = "initial"
                            showDeletionProgressDialog = true
                            // Call your ViewModel async delete function here
                            viewModel.deleteAccountAndDataAsync(
                                username = viewModel.userProfile.value.username,
                                email = viewModel.userProfile.value.email, // provide current user's email
                                password = reauthPassword, // bind this to a state where user can enter password for reauth
                                onSuccess = {
                                    Log.d("DeleteDebug", "Account deletion success")
                                    // navigate back to login screen or show message
                                },
                                onReauthRequired = {
                                    showReauthDialog = true
                                    // show UI to let user enter password again
                                },
                                onError = { errorMsg ->
                                    Log.e("DeleteDebug", "Account deletion error: $errorMsg")
                                    // show error toast/dialog
                                }
                            )

                        }
                    }
                }
                ,
                rightButtonPaperRes = R.drawable.celestial_dark_red_paper,
                leftButtonPaperRes = R.drawable.celestial_dark_khaki_paper,
                password = reauthPassword,
                onPasswordChange = { reauthPassword = it }
            )
        }
    }

    // The countdown logic and async calls should be separated into proper LaunchedEffect blocks, e.g.

    LaunchedEffect(dialogState) {
        if (dialogState == "counting") {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            dialogState = "ready"
        }
    }

    if (showDeletionProgressDialog) {
        DeletionProgressDialog(
            percent = (deletionProgress * 100).toInt(),
            onDismissRequest = { showDeletionProgressDialog = false }
            // pass other parameters if required by dialog
        )
    }

    if (showReauthDialog) {
        ReauthDialog(
            showDialog = showReauthDialog,
            isLoading = isReauthLoading,      // pass current loading state
            errorMessage = reauthError,       // pass current error message
            onDismiss = {
                showReauthDialog = false
                reauthError = null
                reauthPassword = ""
                isReauthLoading = false
            },
            onConfirm = { enteredPassword: String ->
                isReauthLoading = true
                reauthError = null
                viewModel.viewModelScope.launch {
                    val email = viewModel.userProfile.value.email ?: ""

                    val result = try {
                        viewModel.reauthenticateUser(email, enteredPassword)
                    } catch (e: Exception) {
                        false
                    }
                    isReauthLoading = false
                    if (result) {
                        showReauthDialog = false
                        showDeletionProgressDialog = true
                        try {
                            viewModel.deleteAccountAndDataAsync(
                                username = viewModel.userProfile.value.username,
                                email = email,
                                password = enteredPassword,
                                onSuccess = {
                                    showDeletionProgressDialog = false
                                },
                                onReauthRequired = {
                                    showReauthDialog = true
                                },
                                onError = {
                                    // Show error message like a Toast or Snackbar if desired
                                }
                            )
                        } catch (e: Exception) {
                            // Handle deletion error if needed
                        }
                    } else {
                        reauthError = "Reauthentication failed"
                    }
                }
            }
        )
    }




    // Progress dialog with spinner (top) + shimmering percent (bottom)
    if (showProgressDialog && deletionState.running) {
        DeletionProgressDialog(
            percent = deletionState.percent,
            onDismissRequest = { /* disabled while running */ }
        )
    }
    LaunchedEffect(deletionState.running) {
        if (!deletionState.running && showProgressDialog) {
            showProgressDialog = false
            if (deletionState.percent >= 100) showDeletedSuccess = true
        }
    }

    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.padding(20.dp).widthIn(min = 280.dp, max = 400.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Are you sure you want to log out?",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showLogoutDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.5f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    "Cancel",
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                viewModel.handleLogout(context)
                                navController.navigate("login") {
                                    popUpTo("login") { inclusive = true }
                                    launchSingleTop = true
                                }

                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.5f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    "Log Out",
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    // Success popup and navigate
    if (showDeletedSuccess) {
        SuccessPopup(
            message = "ACCOUNT DELETED SUCCESSFUL",
            paperRes = R.drawable.celestial_paper_text
        ) {
            showDeletedSuccess = false
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

// ========= Reusable pieces =========

@Composable
private fun DeleteAccountDialogContent(
    paperBgRes: Int,
    onCancel: () -> Unit,
    dialogState: String,
    secondsLeft: Int,
    onConfirmOrDelete: () -> Unit,
    rightButtonPaperRes: Int,
    leftButtonPaperRes: Int,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            .background(Color.Transparent)
    ) {
        Image(
            painter = painterResource(id = paperBgRes),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(min = 280.dp, max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ARE YOU SURE YOU WANT TO DELETE YOUR ACCOUNT?",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (dialogState == "ready") {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    singleLine = true,
                    label = { Text("Password (to-delete)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, Color.Black),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = leftButtonPaperRes),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            "CANCEL",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            color = Color.White
                        )
                    }
                }

                val rightText = when (dialogState) {
                    "initial" -> "CONFIRM"
                    "counting" -> "$secondsLeft"
                    "ready" -> "DELETE"
                    else -> "CONFIRM"
                }
                val rightEnabled = dialogState != "counting"

                Button(
                    onClick = onConfirmOrDelete,
                    enabled = rightEnabled,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, Color.Black),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = rightButtonPaperRes),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = rightText,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessPopup(
    message: String,
    paperRes: Int,
    onDismiss: () -> Unit
) {
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
                painter = painterResource(id = paperRes),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                message,
                color = Color(0xFF8B7355),
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

// ======= New: Progress dialog using existing animations in dark khaki =======

@Composable
fun DeletionProgressDialog(
    percent: Int,
    onDismissRequest: () -> Unit
) {
    val darkKhaki = Color(0xFF8B7355)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .widthIn(min = 320.dp, max = 420.dp)
                .heightIn(min = 220.dp, max = 360.dp)
        ) {
            // Paper background (request specifies celestisl_paper_text)
            Image(
                painter = painterResource(id = R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: spinner (keeps spinning)
                LineSpinFadeLoaderIndicator(
                    colors = listOf(darkKhaki),
                    rectCount = 8,
                    linearAnimationType = LinearAnimationType.CIRCULAR,
                    penThickness = 20f,
                    radius = 90f,
                    elementHeight = 38f,
                    minAlpha = 0.3f,
                    maxAlpha = 1.0f
                )

                Spacer(Modifier.height(2.dp))

                // Bottom: shimmering percent text
                Text(
                    "DELETING ACCOUNT\nPLEASE WAIT PATIENTLY :)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = darkKhaki,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProfileItemButton(label: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    val black = Color.Black
    val darkKhaki = Color(0xFF8B7355)
    val paperBg = painterResource(id = R.drawable.celestial_paper_text)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(6f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, black, RoundedCornerShape(12.dp))
            // Remove the original vertical 4.dp padding, or only use horizontal if needed
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = paperBg,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Row(
            // This line ensures the Box is only as tall as its tallest child (the Text)
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = darkKhaki,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            Text(
                text = value,
                fontSize = 16.sp,
                color = black,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}


// ModeDialog - dialog for mode reminders
@Composable
fun ModeDialog(message: String, onDismiss: () -> Unit) {
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
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = message,
                color = Color(0xFF8B7355),
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

// EditFieldDialog - reusable dialog for editing a field
@Composable
fun EditFieldDialog(
    field: String,
    initialValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onChange: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(20.dp).widthIn(min = 280.dp, max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Edit $field", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onValueChange(it)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B7355),
                        unfocusedBorderColor = Color(0xFFC3B091),
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    )
                    ,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onChange(text) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            "CHANGE",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = { onDismiss() }, // Example: dismiss, or replace with your own logic
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            "CANCEL",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }


            }
        }
    }
}

@Composable
fun ReauthDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit, // pass entered password back
    isLoading: Boolean,
    errorMessage: String?
) {
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Re-authentication Required", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    var password by rememberSaveable { mutableStateOf("") }
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    if (!errorMessage.isNullOrEmpty()) {
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isLoading
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirm(password) },
                            enabled = password.isNotBlank() && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ValidationPopupDialog(
    message: String,
    onDismissRequest: () -> Unit
) {
    val darkKhaki = Color(0xFF8B7355)

    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            delay(1500)
            onDismissRequest()
        }
    }

    if (message.isNotEmpty()) {
        Dialog(
            onDismissRequest = onDismissRequest,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                // Background Image - filling the Box shape (rounded)
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp))
                )
                Text(
                    text = message,
                    color = darkKhaki,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}


@Composable
fun CustomDatePickerDialog(
    initialDate: String?,
    onDateSelected: (String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedDate by rememberSaveable { mutableStateOf(initialDate ?: "") }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Select Date of Birth", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))

                TextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                    TextButton(onClick = {
                        val valid = try {
                            LocalDate.parse(selectedDate, formatter)
                            true
                        } catch (e: Exception) {
                            false
                        }
                        if (valid) onDateSelected(selectedDate) else onDateSelected(null)
                    }) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun GenderSelectionDialog(
    currentSelection: String?,
    onGenderSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val darkKhaki = Color(0xFF8B7355)
    val options = listOf("MALE", "FEMALE", "GAYGAY")

    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Gender", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))

                options.forEach { gender ->
                    Text(
                        text = gender,
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                onGenderSelected(gender)
                                expanded = false
                            }
                            .padding(12.dp),
                        color = darkKhaki,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Divider()
                }
            }
        }
    }
}



