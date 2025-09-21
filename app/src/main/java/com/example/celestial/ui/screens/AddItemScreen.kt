package com.example.celestial.ui.screens

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.celestial.R
import com.example.celestial.data.models.Cake
import com.example.celestial.data.models.Ingredient
import com.example.celestial.ui.viewmodels.InventoryViewModel
import kotlinx.coroutines.delay

sealed class AddItemData {
    data class CakeItem(val cake: Cake, val photoUri: Uri?) : AddItemData()
    data class IngredientItem(val ingredient: Ingredient, val photoUri: Uri?) : AddItemData()
}

@Composable
fun AddItemScreen(
    itemType: String, // "CAKE" or "INGREDIENT"
    onBack: () -> Unit,
    viewModel: InventoryViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    val darkKhaki = Color(0xFF8B7355)
    val khaki = Color(0xFF8B7D6B)
    val paperBg = painterResource(id = R.drawable.celestial_paper_text)
    val screenWidth = configuration.screenWidthDp.dp
    val padding = 16.dp
    val boxSize = screenWidth - (padding * 2)

    // Observed ViewModel States
    val addSuccess by viewModel.addSuccess.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()

    // Local Dialog states with rememberSaveable
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }

    // Input Fields state with rememberSaveable for rotation persistence
    var type by rememberSaveable { mutableStateOf("") }
    var wholeCakePrice by rememberSaveable { mutableStateOf("") }
    var sliceCakePrice by rememberSaveable { mutableStateOf("") }
    var ingredientName by rememberSaveable { mutableStateOf("") }
    var ingredientQuantity by rememberSaveable { mutableStateOf("") }
    var ingredientUnit by rememberSaveable { mutableStateOf("") }
    var ingredientExpiry by rememberSaveable { mutableStateOf("") }

    // Image state
    var imageBytes by rememberSaveable { mutableStateOf<ByteArray?>(null) }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = uri
            // Immediately read bytes to keep imageBytes in sync
            val inputStream = context.contentResolver.openInputStream(uri)
            imageBytes = inputStream?.readBytes()
            inputStream?.close()
        }
    }

    // Side effects for showing dialogs on add success/failure
    LaunchedEffect(addSuccess) {
        if (addSuccess == true) {
            showSuccessDialog = true
            viewModel.clearSuccess()
        }
    }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            showErrorDialog = true
        }
    }

    // Form validation for enabling add button
    val localImageBytes = imageBytes
    val allFieldsFilled = if (itemType == "CAKE") {
        type.isNotBlank() && wholeCakePrice.isNotBlank() && sliceCakePrice.isNotBlank()
    } else {
        ingredientName.isNotBlank()
    }
    val isFormComplete = allFieldsFilled && localImageBytes != null && localImageBytes.isNotEmpty()

    // Main Content - switch between portrait and landscape with scroll when landscape
    val content = @Composable {
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.celestial_paper_text),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = darkKhaki)
                }

                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = paperBg,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        "ADD ${itemType.uppercase()}",
                        color = darkKhaki,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Photo picker
            Box(
                modifier = Modifier
                    .size(boxSize)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
                    .clickable {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                if (imageUri == null) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Image",
                        tint = darkKhaki,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "TAP TO CHOOSE IMAGE",
                        fontWeight = FontWeight.Bold,
                        color = darkKhaki,
                        modifier = Modifier.padding(top = 100.dp),
                        fontSize = 18.sp
                    )
                } else {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Preview Image",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input fields for CAKE or INGREDIENT
            if (itemType == "CAKE") {
                CustomStyledTextField("Type", type, { type = it }, darkKhaki, khaki, paperBg)
                CustomStyledTextField("Whole Price", wholeCakePrice, { wholeCakePrice = it }, darkKhaki, khaki, paperBg)
                CustomStyledTextField("Slice Price", sliceCakePrice, { sliceCakePrice = it }, darkKhaki, khaki, paperBg)
            } else {
                CustomStyledTextField("Name", ingredientName, { ingredientName = it }, darkKhaki, khaki, paperBg)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Add button
            Button(
                onClick = {
                    val currentBytes = imageBytes
                    if (itemType == "CAKE" && currentBytes != null && currentBytes.isNotEmpty()) {
                        // Build Cake and add
                        val cake = Cake(
                            type = type,
                            wholeCakeQuantity = 0,
                            sliceQuantity = 0,
                            ingredients = emptyMap(),
                            wholeCakePrice = wholeCakePrice.toDoubleOrNull() ?: 0.0,
                            sliceCakePrice = sliceCakePrice.toDoubleOrNull() ?: 0.0,
                            availableProduce = 0
                        )
                        viewModel.addCakeToFirestore(context, cake, currentBytes)
                    } else if (itemType != "CAKE" && currentBytes != null && currentBytes.isNotEmpty()) {
                        val ingredient = com.example.celestial.data.models.Ingredient(
                            name = ingredientName,
                            quantity = ingredientQuantity.toDoubleOrNull() ?: 0.0,
                            unit = ingredientUnit.takeIf { it.isNotBlank() },
                            expiryDate = ingredientExpiry.takeIf { it.isNotBlank() }
                        )
                        viewModel.addIngredientToFirestore(context, ingredient, currentBytes)
                    }
                },
                enabled = isFormComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text("ADD ITEM")
                }
            }

        }
    }

    // Portrait: original layout
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        content()
    } else {
        // Landscape: same layout but scrollable to avoid shrinking UI
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }
    }

    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
                    .sizeIn(minWidth = 220.dp, maxWidth = 340.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "NEW ${itemType.uppercase()} ADDED SUCCESSFUL",
                    color = darkKhaki,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
        }

        // Dismiss and navigate after 1.5s
        LaunchedEffect(showSuccessDialog) {
            delay(1500)
            showSuccessDialog = false
            // Use the correct index for tab navigation, for example:
            // If itemType is "CAKE" navigate to tab 0 (Cakes)
            // If itemType is "INGREDIENT" navigate to tab 1 (Ingredients)
            val targetTabIndex = when (itemType.uppercase()) {
                "CAKE" -> 0
                "INGREDIENT" -> 1
                else -> 0 // default to Cakes if unknown
            }
            navController.navigate("inventory?initialTab=$targetTabIndex") {
                popUpTo("inventory") { inclusive = true }
            }
        }
    }

    if (showErrorDialog && !errorMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false; viewModel.clearError() },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false; viewModel.clearError() }) {
                    Text("OK")
                }
            },
            title = { Text("Add Error") },
            text = { Text(errorMessage.orEmpty()) }
        )
    }
}

// --- Custom styled text field as before ---

@Composable
fun CustomStyledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusColor: Color,
    unfocusColor: Color,
    paperBg: Painter
) {
    var isFocused by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, if (isFocused) focusColor else unfocusColor, RoundedCornerShape(12.dp))
            .background(Color.Transparent)
    ) {
        Image(
            painter = paperBg,
            contentDescription = null,
            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = focusColor) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .background(Color.Transparent),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = focusColor, fontWeight = FontWeight.Normal),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = focusColor,
                unfocusedTextColor = focusColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

