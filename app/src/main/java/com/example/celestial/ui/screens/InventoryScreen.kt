package com.example.celestial.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.celestial.data.models.IngredientEditRecord
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import android.net.Uri
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.example.celestial.R
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import coil.compose.AsyncImage
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.celestial.data.models.Cake
import com.example.celestial.data.models.ExpiryChange
import com.example.celestial.data.models.Ingredient
import com.example.celestial.data.models.Stock
import com.example.celestial.ui.viewmodels.InventoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun getCakeImageFilename(cakeType: String): String {
    return cakeType
        .lowercase()
        .replace(" ", "_")
        .replace("'", "") // Handle apostrophes like "Devil's Food"
        .replace("-", "_") // Handle dashes
        .plus(".png")
}

fun getIngredientImageFilename(ingredientType: String): String {
    return ingredientType
        .lowercase()
        .replace(" ", "_")
        .replace("'", "") // Handle apostrophes like "Devil's Food"
        .replace("-", "_") // Handle dashes
        .plus(".png")
}

@Composable
fun ShakingIngredientCard(
    ingredientId: String,
    isShaking: Boolean,
    onClick: () -> Unit,
    content: @Composable (Float) -> Unit
) {
    val scale = if (isShaking) {
        // Only create infiniteTransition when shaking to ensure restart on state changes
        val infiniteTransition = rememberInfiniteTransition()
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f, // smooth scale-up by 10%
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onClick() } // Clicking stops shaking
    ) {
        content(scale)
    }
}

@Composable
fun FlexibleImage(
    imageUrl: String?, // e.g. cake.imageUrl
    fallbackAsset: String, // asset path if imageUrl is null or invalid
    placeholder: Int, // resource id
    error: Int, // resource id
    modifier: Modifier = Modifier
) {
    val isHttpUrl = !imageUrl.isNullOrBlank() && (
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://")
            )
    val model = if (isHttpUrl) imageUrl else fallbackAsset
    AsyncImage(
        model = model,
        contentDescription = "Image",
        contentScale = ContentScale.Crop,
        modifier = modifier,
        placeholder = painterResource(placeholder),
        error = painterResource(error)
    )
}

@Composable
fun RemoveModeOffDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (show) {
        Dialog(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .border(2.dp, Color(0xFF8B7355), RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Transparent, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "REMOVE MODE OFF",
                    color = Color(0xFF8B7355), // dark khaki
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        // Auto-dismiss after 1.5 seconds
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            onDismiss()
        }
    }
}


@Composable
fun RemoveModePopup(
    text: String,
    visible: Boolean,
    onHide: () -> Unit
) {
    if (visible) {
        Dialog(onDismissRequest = onHide) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 220.dp, maxWidth = 340.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Transparent)
                    .border(2.dp, Color(0xFF8B7355), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = text,
                    color = Color(0xFF8B7355),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            onHide()
        }
    }
}


@Composable
fun DialogButtonWithImageBackground(
    text: String,
    backgroundRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = backgroundRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            )
            Text(
                text,
                color = textColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center) // Correct for Box
            )
        }
    }
}



@Composable
fun RemoveActionDialog(
    itemName: String,
    onRemove: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .background(Color.Transparent)
        ) {
            Image(
                painter = painterResource(id = R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Remove $itemName?",
                    color = Color(0xFF8B7355),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                DialogButtonWithImageBackground(
                    text = "REMOVE",
                    backgroundRes = R.drawable.celestial_dark_red_paper,
                    onClick = onRemove,
                    modifier = Modifier.padding(bottom = 16.dp) // Space below, not around
                )

                DialogButtonWithImageBackground(
                    text = "CANCEL",
                    backgroundRes = R.drawable.celestial_dark_khaki_paper,
                    onClick = onCancel
                )
            }
        }
    }
}


@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDeny: () -> Unit
) {
    Dialog(onDismissRequest = onDeny) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .background(Color.Transparent)
        ) {
            Image(
                painter = painterResource(id = R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ARE YOU SURE YOU WANT TO REMOVE $itemName?",
                    color = Color(0xFF8B7355),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                DialogButtonWithImageBackground(
                    text = "YES",
                    backgroundRes = R.drawable.celestial_dark_red_paper,
                    onClick = onConfirm,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DialogButtonWithImageBackground(
                    text = "NO",
                    backgroundRes = R.drawable.celestial_dark_khaki_paper,
                    onClick = onDeny
                )
            }
        }
    }
}



@Composable
fun RemoveDialogButton(text: String, backgroundRes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painterResource(backgroundRes),
                null,
                Modifier.matchParentSize().clip(RoundedCornerShape(12.dp))
            )
            Text(text, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

fun getItemDisplayName(item: Any?): String {
    return when (item) {
        is Cake -> item.type
        is Ingredient -> item.name
        else -> ""
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavHostController,
    viewModel: InventoryViewModel,
    autoFocusIngredientId: String? = null, // from navigation args
    initialTab: Int = 0, // pass from route/query param (0 = Cakes, 1 = Ingredients)
    selectedTabIndex: Int = 5,
    isRemoveModePassed: Boolean = false,
    isEditModePassed: Boolean = false
) {
    val navRemoveMode = navController.currentBackStackEntry?.arguments?.getBoolean("removeMode") ?: false
    val navTab = navController.currentBackStackEntry?.arguments?.getInt("initialTab") ?: 0
    var isRemoveMode by rememberSaveable { mutableStateOf(navRemoveMode) }
    var selectedTab by rememberSaveable { mutableStateOf(navTab) }
    var removeModeTab by rememberSaveable { mutableStateOf(navTab) }
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    var showEditModeOffDialog by rememberSaveable { mutableStateOf(false) }
    var lastTab by rememberSaveable { mutableStateOf(selectedTab) }
    var selectedIngredientForEdit by rememberSaveable { mutableStateOf<Pair<Ingredient, Stock>?>(null) }
    var showIngredientEditDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCakeForEdit by rememberSaveable { mutableStateOf<Cake?>(null) }

    var isEditMode by rememberSaveable { mutableStateOf(isEditModePassed) }
    var showEditModeOnDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isEditModePassed) {
        if (isEditMode != isEditModePassed) {
            isEditMode = isEditModePassed
            if (isEditModePassed) {
                showEditModeOnDialog = true
                delay(1500L)
                showEditModeOnDialog = false
            }
        }
    }


    // --- Remove Mode State ---
    LaunchedEffect(isRemoveModePassed) {
        if (isRemoveMode != isRemoveModePassed)
            isRemoveMode = isRemoveModePassed
    }

    var editModeTab by rememberSaveable { mutableStateOf(initialTab) }

    LaunchedEffect(isEditModePassed, initialTab) {
        if (isEditModePassed) {
            editModeTab = initialTab
        }
    }
    var showRemoveModeDialog by rememberSaveable { mutableStateOf(false) }
    var showExitRemoveModeDialog by rememberSaveable { mutableStateOf(false) }
    var selectedRemoveItem by rememberSaveable { mutableStateOf<Any?>(null) }
    var showRemoveConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showReallyRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var removeTypeRequestDialog by rememberSaveable { mutableStateOf(false) }

    val darkKhaki = Color(0xFF786A58)
    val khakiLight = Color(0xFF8B7D6B)
    val khakiDark = Color(0xFF786A58)
    val darkerKhaki = Color(0xFF6B5A42)

    var selectedScreen by remember { mutableStateOf(selectedTabIndex) }
    val ingredientGridState = rememberLazyGridState()
    var shakeIngredientId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastAutoFocusIdHandled by rememberSaveable { mutableStateOf<String?>(null) }
    val ingredientsState by viewModel.ingredients.observeAsState(emptyList())
    val cakesState by viewModel.cakes.observeAsState(emptyList())
    val errorMessageState by viewModel.errorMessage.observeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf("Cakes", "Ingredients")
    var showCakeDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCakeForDialog by rememberSaveable { mutableStateOf<Cake?>(null) }
    var showProduceDialog by rememberSaveable { mutableStateOf(false) }
    var produceQuantity by rememberSaveable { mutableStateOf("") }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var successMessage by rememberSaveable { mutableStateOf("") }
    var dialogIngredient by rememberSaveable { mutableStateOf<Ingredient?>(null) }
    var showIngredientStockDialog by rememberSaveable { mutableStateOf(false) }
    var showEditExpiryDialog by rememberSaveable { mutableStateOf(false) }
    var selectedStock by rememberSaveable { mutableStateOf<Stock?>(null) }
    var newExpiryDate by rememberSaveable { mutableStateOf("") }
    var handledIngredientIds by rememberSaveable { mutableStateOf(setOf<String>()) }


    // Data Observed from ViewModel
    val cakes by viewModel.cakes.observeAsState(emptyList())
    val ingredients by viewModel.ingredients.observeAsState(emptyList())

    // --- Handle Remove Mode from Navigation ---
    LaunchedEffect(Unit) {
        val args = navController.currentBackStackEntry?.arguments
        val navRemoveMode = args?.getBoolean("removeMode") == true
        val navTab = args?.getInt("initialTab") ?: 0
        if (navRemoveMode && !isRemoveMode) {
            isRemoveMode = true
            removeModeTab = navTab
            selectedTab = navTab
            showRemoveModeDialog = true
        }
    }
    // Exit Remove Mode when switching tabs
    LaunchedEffect(isRemoveMode, selectedTab) {
        if (isRemoveMode && selectedTab != removeModeTab) {
            isRemoveMode = false
            showExitRemoveModeDialog = true
        }
    }

    LaunchedEffect(selectedTab) {
        if (isEditMode && lastTab != selectedTab) {
            isEditMode = false
            showEditModeOffDialog = true
            lastTab = selectedTab
            kotlinx.coroutines.delay(1500)
            showEditModeOffDialog = false
        } else {
            lastTab = selectedTab
        }
    }

    // Cleanly exit on navigation away/unmount
    DisposableEffect(Unit) {
        onDispose {
            if (isRemoveMode) {
                isRemoveMode = false
                showExitRemoveModeDialog = true
            }
        }
    }

    // Error handling
    errorMessageState?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(initialTab) { selectedTab = initialTab }
    // Step 1: Scroll to the item as soon as ingredients and nav arg are available.
    LaunchedEffect(selectedTab, ingredientsState, autoFocusIngredientId) {
        if (
            selectedTab == 1 &&
            ingredientsState.isNotEmpty() &&
            autoFocusIngredientId != null &&
            autoFocusIngredientId != lastAutoFocusIdHandled &&
            !handledIngredientIds.contains(autoFocusIngredientId)
        ) {
            val idx = ingredientsState.indexOfFirst { it.id == autoFocusIngredientId }
            if (idx != -1) {
                delay(200)
                ingredientGridState.animateScrollToItem(idx)
                lastAutoFocusIdHandled = autoFocusIngredientId
            }
        }
    }

// Step 2: Trigger shake after grid state scroll completes, waiting for item visible.
    LaunchedEffect(ingredientGridState.firstVisibleItemIndex, autoFocusIngredientId, ingredientsState) {
        if (
            selectedTab == 1 &&
            autoFocusIngredientId != null &&
            !handledIngredientIds.contains(autoFocusIngredientId)
        ) {
            val idx = ingredientsState.indexOfFirst { it.id == autoFocusIngredientId }
            val visibleRange = ingredientGridState.firstVisibleItemIndex until (
                    ingredientGridState.firstVisibleItemIndex + ingredientGridState.layoutInfo.visibleItemsInfo.size
                    )
            if (idx != -1 && idx in visibleRange) {
                shakeIngredientId = autoFocusIngredientId
            }
        }
    }

    val mainContent: @Composable () -> Unit = {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- SECTION TITLE + REMOVE BUTTON ---
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
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = if (isRemoveMode) "REMOVE MODE"
                    else if (isEditMode) "EDIT MODE"
                    else "INVENTORY SECTION",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }



            Spacer(Modifier.height(16.dp))

            // --- TABS ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .height(48.dp)
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                    contentColor = khakiDark,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(6.dp),
                            color = khakiDark
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) khakiDark else khakiLight
                                )
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            // --- CONTENT ---
            when (selectedTab) {
                0 -> { // Cakes
                    if (cakesState.isEmpty()) {
                        Text(
                            "No cakes available",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cakesState) { cake ->
                                // The "hasAvailableToProduce" and "cardAlpha" logic remains,
                                // but REMOVE MODE must never disable clicking!
                                val imageFilename = getCakeImageFilename(cake.type)
                                val fallbackImagePath =
                                    "file:///android_asset/images/celestial_cakes/$imageFilename"
                                val availableState by viewModel.getAvailableCakesLiveData(cake)
                                    .observeAsState(0)
                                val available = availableState
                                val hasAvailableToProduce = available > 0
                                val cardAlpha = if (hasAvailableToProduce) 1f else 0.6f
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.50f)
                                        .padding(4.dp)
                                        .alpha(cardAlpha)
                                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (isRemoveMode) {
                                                selectedRemoveItem = cake
                                                showRemoveConfirmDialog = true
                                            } else if (isEditMode) {
                                                selectedCakeForEdit = cake
                                            } else {
                                                selectedCakeForDialog = cake
                                                showCakeDialog = true
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box {
                                        Image(
                                            painter = painterResource(R.drawable.celestial_paper_text),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(
                                            Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            FlexibleImage(
                                                imageUrl = cake.imageUrl,
                                                fallbackAsset = fallbackImagePath,
                                                placeholder = R.drawable.celestial_moon,
                                                error = R.drawable.celestial_sun,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                cake.type,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = khakiDark
                                            )
                                            Text(
                                                "Whole: ${cake.wholeCakeQuantity}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = khakiLight
                                            )
                                            Text(
                                                "Slices: ${cake.sliceQuantity}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = khakiLight
                                            )
                                            Text(
                                                "Available: $available",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = khakiLight
                                            )
                                            if (!hasAvailableToProduce) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "NOT AVAILABLE TO PRODUCE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Red
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> { // Ingredients
                    if (ingredientsState.isEmpty()) {
                        Text(
                            "No ingredients available",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = ingredientGridState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(ingredientsState) { idx, ingredient ->
                                val imageFilename = getIngredientImageFilename(ingredient.name)
                                val imageUrl =
                                    "file:///android_asset/images/celestial_ingredients/$imageFilename"
                                val stocks by viewModel.getStocksForIngredient(ingredient.id)
                                    .observeAsState(emptyList())
                                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                                val today = LocalDate.now()
                                val filteredQuantity = stocks.filter { stock ->
                                    val expiryDate = try {
                                        stock.expiryDate?.let { LocalDate.parse(it, formatter) }
                                    } catch (_: Exception) {
                                        null
                                    }
                                    val notExpired =
                                        expiryDate == null || !expiryDate.isBefore(today)
                                    notExpired && stock.quantity > 0
                                }.sumOf {
                                    if (it.unit.trim()
                                            .uppercase() == "KG"
                                    ) it.quantity * 1000 else it.quantity
                                }
                                val hasStock = filteredQuantity > 0
                                val cardAlpha = if (hasStock) 1f else 0.6f
                                val isShaking = shakeIngredientId == ingredient.id

                                ShakingIngredientCard(
                                    ingredientId = ingredient.id,
                                    isShaking = isShaking,
                                    onClick = {
                                        if (isRemoveMode) {
                                            selectedRemoveItem = ingredient
                                            showRemoveConfirmDialog = true
                                        } else if (isEditMode) {
                                            dialogIngredient = ingredient
                                            showIngredientStockDialog = true
                                        } else {
                                            shakeIngredientId = null
                                            handledIngredientIds =
                                                handledIngredientIds + ingredient.id
                                            dialogIngredient = ingredient
                                            showIngredientStockDialog = true
                                        }
                                    }
                                ) { shakeScale ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.60f)
                                            .padding(4.dp)
                                            .alpha(cardAlpha)
                                            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Box {
                                            Image(
                                                painter = painterResource(R.drawable.celestial_paper_text),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(
                                                Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                FlexibleImage(
                                                    imageUrl = ingredient.imageUrl,
                                                    fallbackAsset = imageUrl,
                                                    placeholder = R.drawable.celestial_moon,
                                                    error = R.drawable.celestial_sun,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                                Text(
                                                    ingredient.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = khakiDark
                                                )
                                                Text(
                                                    "QUANTITY : ",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = khakiLight
                                                )
                                                Text(
                                                    "%.1f GRAM".format(filteredQuantity),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = khakiLight
                                                )
                                                Text(
                                                    "${
                                                        String.format(
                                                            "%.3f",
                                                            filteredQuantity / 1000
                                                        )
                                                    } KG",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = khakiLight
                                                )
                                                if (!hasStock) {
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        "OUT OF STOCK",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Red
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedCakeForEdit != null) {
                CakeEditDialog(
                    navController = navController,
                    viewModel = viewModel,
                    cake = selectedCakeForEdit!!,
                    allIngredients = ingredients,
                    onDismiss = { selectedCakeForEdit = null }
                )
            }

            if (showIngredientStockDialog && dialogIngredient != null) {
                val stocksState by viewModel.getStocksForIngredient(dialogIngredient!!.id)
                    .observeAsState(emptyList())
                ThemedDialog(
                    onDismissRequest = {
                        showIngredientStockDialog = false
                        dialogIngredient = null
                    },
                    darkerKhaki = Color(0xFF786A58), // dark khaki
                    title = "Manage Stock for ${dialogIngredient!!.name}",
                    content = {
                        if (stocksState.isEmpty()) {
                            Text("No stocks available.", color = Color(0xFF786A58))
                        } else {
                            LazyColumn {
                                items(stocksState) { stock ->
                                    val stockCardModifier = if (isEditMode) {
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedIngredientForEdit =
                                                    dialogIngredient!! to stock
                                                showIngredientEditDialog = true
                                                showIngredientStockDialog = false
                                                dialogIngredient = null
                                            }
                                    } else {
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                                    }
                                    Card(
                                        modifier = stockCardModifier,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = stockCardColors(stock)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                "Stock ID: ${stock.stockId}",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF786A58)
                                            )
                                            Text(
                                                "Quantity: ${stock.quantity} ${stock.unit}",
                                                color = Color(0xFF786A58)
                                            )
                                            Text(
                                                "Expiry Date: ${stock.expiryDate ?: "N/A"}",
                                                color = Color(0xFF786A58)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    buttons = {
                        ThemedDialogButton(
                            text = "ADD STOCK",
                            onClick = {
                                showIngredientStockDialog = false
                                navController.navigate("add_ingredient_stock/${dialogIngredient!!.id}")
                                dialogIngredient = null
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        ThemedDialogButton(
                            text = "CANCEL",
                            onClick = { showIngredientStockDialog = false; dialogIngredient = null }
                        )
                    }
                )
            }

            LaunchedEffect(isEditMode) {
                if (!isEditMode && showIngredientEditDialog) {
                    showIngredientEditDialog = false
                    selectedIngredientForEdit = null
                }
            }


// --- Launch the IngredientEditDialog ONLY when both ingredient/stock are set ---
            if (showIngredientEditDialog && selectedIngredientForEdit != null) {
                val (ingredient, stock) = selectedIngredientForEdit!!
                IngredientEditDialog(
                    viewModel = viewModel,
                    ingredient = ingredient,
                    stock = stock,
                    onDismiss = {
                        showIngredientEditDialog = false
                        selectedIngredientForEdit = null
                    }
                )
            }


            if (selectedIngredientForEdit != null) {
                val (ingredient, stock) = selectedIngredientForEdit!!
                IngredientEditDialog(
                    viewModel = viewModel,
                    ingredient = ingredient,
                    stock = stock,
                    onDismiss = { selectedIngredientForEdit = null }
                )
            }


            // --- Remove/Confirm Removal Dialogs ---
            if (showRemoveConfirmDialog && selectedRemoveItem != null) {
                RemoveActionDialog(
                    itemName = getItemDisplayName(selectedRemoveItem),
                    onRemove = {
                        showRemoveConfirmDialog = false
                        showReallyRemoveDialog = true
                    },
                    onCancel = { showRemoveConfirmDialog = false },
                    onClose = { showRemoveConfirmDialog = false }
                )
            }
            if (showReallyRemoveDialog && selectedRemoveItem != null) {
                ConfirmDeleteDialog(
                    itemName = getItemDisplayName(selectedRemoveItem),
                    onConfirm = {
                        when (val item = selectedRemoveItem) {
                            is Cake -> viewModel.removeCake(item.id)
                            is Ingredient -> viewModel.removeIngredient(item.id)
                        }
                        showRemoveSuccessDialog = true
                        selectedRemoveItem = null
                        showReallyRemoveDialog = false
                        isRemoveMode = false
                    },
                    onDeny = {
                        showReallyRemoveDialog = false
                        selectedRemoveItem = null
                    }
                )
            }

            if (showEditModeOffDialog) {
                Dialog(onDismissRequest = { showEditModeOffDialog = false }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .border(2.dp, Color(0xFF8B7355), RoundedCornerShape(22.dp))
                            .background(Color.Transparent, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.celestial_paper_text),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(22.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "EDIT MODE OFF",
                            color = Color(0xFF8B7355),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }


            // --- POPUPS ---
            RemoveModePopup(
                text = "REMOVE MODE ON",
                visible = showRemoveModeDialog,
                onHide = { showRemoveModeDialog = false }
            )
            RemoveModeOffDialog(
                show = showExitRemoveModeDialog,
                onDismiss = { showExitRemoveModeDialog = false }
            )

            RemoveModePopup(
                "REMOVED ${getItemDisplayName(selectedRemoveItem)} SUCCESSFUL",
                showRemoveSuccessDialog
            ) { showRemoveSuccessDialog = false }

            // --- Remove Type Request Dialog ---
            if (removeTypeRequestDialog) {
                Dialog(onDismissRequest = { removeTypeRequestDialog = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Remove which type?",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B7355)
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                                Button(
                                    onClick = {
                                        isRemoveMode = true
                                        removeModeTab = 0
                                        selectedTab = 0
                                        showRemoveModeDialog = true
                                        removeTypeRequestDialog = false
                                    }
                                ) { Text("CAKE") }
                                Button(
                                    onClick = {
                                        isRemoveMode = true
                                        removeModeTab = 1
                                        selectedTab = 1
                                        showRemoveModeDialog = true
                                        removeTypeRequestDialog = false
                                    }
                                ) { Text("INGREDIENT") }
                            }
                        }
                    }
                }
            }

            // --- Cake Action Dialogs ---
            if (showCakeDialog && selectedCakeForDialog != null) {
                ThemedDialog(
                    onDismissRequest = { showCakeDialog = false },
                    darkerKhaki = khakiDark,
                    title = selectedCakeForDialog!!.type,
                    content = {
                        Column {
                            Text("Select an action for this cake.", color = khakiDark)
                        }
                    },
                    buttons = {
                        Column {
                            ThemedDialogButton(
                                text = "VIEW DETAILS",
                                onClick = {
                                    navController.navigate("view_cake_ingredients/${selectedCakeForDialog!!.id}")
                                    showCakeDialog = false
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            ThemedDialogButton(
                                text = "PRODUCE",
                                enabled = (viewModel.getAvailableCakesLiveData(selectedCakeForDialog!!).value
                                    ?: 0) > 0,
                                onClick = {
                                    produceQuantity = ""
                                    showProduceDialog = true
                                    showCakeDialog = false
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                            ThemedDialogButton(
                                text = "CANCEL",
                                onClick = { showCakeDialog = false }
                            )
                        }
                    }
                )
            }
            if (showProduceDialog && selectedCakeForDialog != null) {
                val available =
                    viewModel.getAvailableCakesLiveData(selectedCakeForDialog!!).value ?: 0
                ThemedDialog(
                    onDismissRequest = { showProduceDialog = false },
                    darkerKhaki = khakiDark,
                    title = "Produce ${selectedCakeForDialog!!.type}",
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Available to produce: $available", color = khakiDark)
                            ParchmentTextField(
                                value = produceQuantity,
                                onValueChange = {
                                    produceQuantity = it.filter { c -> c.isDigit() }
                                },
                                placeholder = "Quantity to produce",
                                isNumberField = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    buttons = {
                        ThemedDialogButton(
                            text = "CONFIRM PRODUCE",
                            enabled = produceQuantity.toIntOrNull()
                                ?.let { it > 0 && it <= available } == true,
                            onClick = {
                                val qty = produceQuantity.toIntOrNull() ?: 0
                                if (qty in 1..available) {
                                    coroutineScope.launch {
                                        viewModel.produceCake(selectedCakeForDialog!!, qty)
                                        successMessage =
                                            "PRODUCED ${selectedCakeForDialog!!.type} FOR $qty SUCCESSFUL"
                                        showSuccessDialog = true
                                        showProduceDialog = false
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        ThemedDialogButton(
                            text = "CANCEL",
                            onClick = { showProduceDialog = false }
                        )
                    }
                )
            }
            if (showSuccessDialog) {
                ThemedDialog(
                    onDismissRequest = { showSuccessDialog = false },
                    darkerKhaki = khakiDark,
                    title = "Success",
                    content = {
                        Text(
                            successMessage,
                            color = khakiDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    buttons = { }
                )
                LaunchedEffect(showSuccessDialog) {
                    delay(1500)
                    showSuccessDialog = false
                }
            }

            if (showEditExpiryDialog && selectedStock != null && dialogIngredient != null) {
                ThemedDialog(
                    onDismissRequest = {
                        showEditExpiryDialog = false
                        selectedStock = null
                        dialogIngredient = null
                        newExpiryDate = ""
                    },
                    darkerKhaki = khakiDark,
                    title = "Edit Expiry Date for Stock: ${selectedStock!!.stockId}",
                    content = {
                        TextField(
                            value = newExpiryDate,
                            onValueChange = { newExpiryDate = it },
                            label = { Text("New Expiry Date (YYYY-MM-DD)", color = khakiDark) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = khakiDark)
                        )
                    },
                    buttons = {
                        ThemedDialogButton(
                            text = "CHANGE",
                            enabled = try {
                                java.time.LocalDate.parse(newExpiryDate); true
                            } catch (_: Exception) {
                                false
                            },
                            onClick = {
                                val validDate = try {
                                    java.time.LocalDate.parse(newExpiryDate); true
                                } catch (_: Exception) {
                                    false
                                }
                                if (validDate && dialogIngredient != null && selectedStock != null) {
                                    coroutineScope.launch {
                                        viewModel.updateStockExpiry(
                                            dialogIngredient!!.id,
                                            selectedStock!!.stockId,
                                            newExpiryDate
                                        )
                                        successMessage =
                                            "CHANGE ${selectedStock!!.stockId}'S EXPIRY DATE SUCCESSFUL"
                                        showSuccessDialog = true
                                        showEditExpiryDialog = false
                                        selectedStock = null
                                        dialogIngredient = null
                                        newExpiryDate = ""
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        ThemedDialogButton(
                            text = "CANCEL",
                            onClick = {
                                showEditExpiryDialog = false
                                selectedStock = null
                                dialogIngredient = null
                                newExpiryDate = ""
                            }
                        )
                    }
                )
            }
        }
    }


    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

                mainContent()

    } else {
        mainContent()
    }
}


@Composable
fun ThemedDialog(
    onDismissRequest: () -> Unit,
    darkerKhaki: Color,
    title: String,
    content: @Composable () -> Unit,
    buttons: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    color = darkerKhaki,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                content()
                Spacer(Modifier.height(12.dp))
                buttons()
            }
        }
    }
}

@Composable
fun ThemedDialogButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    khaki: Color = Color(0xFF8B7D6B)
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent,   // draw bg image inside
                disabledContentColor = khaki
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        ) {
            Box(Modifier.fillMaxSize()) {
                if (enabled) {
                    Image(
                        painter = painterResource(R.drawable.celestial_dark_khaki_paper),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.celestial_paper_text),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text,
                        color = khaki,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}


@Composable
fun stockCardColors(stock: Stock): CardColors {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val isExpired = try {
        stock.expiryDate != null && LocalDate.parse(stock.expiryDate, formatter).isBefore(today)
    } catch (_: Exception) { false }
    val isDepleted = !isExpired && stock.quantity <= 0

    return when {
        isExpired -> CardDefaults.cardColors(containerColor = Color.Red, contentColor = Color.White)
        isDepleted -> CardDefaults.cardColors(containerColor = Color.DarkGray, contentColor = Color.White)
        else -> CardDefaults.cardColors(containerColor = Color.White, contentColor = Color.Black)
    }
}

@Composable
fun RoundedBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.celestial_paper_text),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        content()
    }
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    val focusedBorderColor = Color(0xFF786A42)
    val unfocusedBorderColor = Color(0xFF8B7D6B)
    val textColor = Color(0xFF786A42)
    var isFocused by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(Color.Transparent)
            .onFocusChanged { isFocused = it.isFocused }
            .height(56.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                enabled = enabled,
                textStyle = LocalTextStyle.current.copy(color = textColor),
                cursorBrush = SolidColor(focusedBorderColor)
            )
        }
    }
}


@Composable
fun ImagePicker(
    imageUrl: String?,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { onImageSelected(it) }
        }

    Box(
        modifier = modifier
            .size(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .clickable { launcher.launch("image/*") },
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            val painter = rememberAsyncImagePainter(model = imageUrl)
            Image(
                painter = painter,
                contentDescription = "Selected Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                "Tap to select image",
                color = Color(0xFF786A42),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun IngredientDropdownSelector(
    ingredients: List<Ingredient>,
    selectedIngredient: Ingredient?,
    onSelectIngredient: (Ingredient) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedText = selectedIngredient?.name ?: ""
    val darkKhaki = Color(0xFF786A42)

    Box(modifier = modifier) {
        StyledTextField(
            value = selectedText,
            onValueChange = {},
            label = "Select Ingredient",
            enabled = false,
            modifier = Modifier
                .clickable { expanded = true }
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            ingredients.forEach { ingredient ->
                DropdownMenuItem(
                    text = { Text(ingredient.name, color = darkKhaki) },
                    onClick = {
                        expanded = false
                        onSelectIngredient(ingredient)
                    }
                )
            }
        }
    }
}

@Composable
fun QuantityInputWithUnit(
    quantity: String,
    onQuantityChange: (String) -> Unit,
    isGram: Boolean,
    onUnitToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        StyledTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = "Quantity",
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isGram) "GRAM" else "KG",
                color = Color(0xFF786A42),
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = isGram,
                onCheckedChange = onUnitToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF786A42),
                    uncheckedTrackColor = Color(0xFF786A42).copy(alpha = 0.5f),
                    checkedThumbColor = Color.White,
                    uncheckedThumbColor = Color.White
                )
            )
        }
    }
}

@Composable
fun CakeEditDialog(
    navController: androidx.navigation.NavHostController,
    viewModel: InventoryViewModel,
    cake: Cake,
    allIngredients: List<Ingredient>,
    onDismiss: () -> Unit,
) {
    var cakeName by rememberSaveable { mutableStateOf(cake.type) }
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedImageUrl by rememberSaveable { mutableStateOf(cake.imageUrl) }
    var ingredientsMap by rememberSaveable { mutableStateOf(cake.ingredients.toMutableMap()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    val darkKhakiColor = Color(0xFF786A42)
    val scrollState = rememberScrollState()
    var wholeCakePrice by rememberSaveable { mutableStateOf(cake.wholeCakePrice.toString()) }
    var sliceCakePrice by rememberSaveable { mutableStateOf(cake.sliceCakePrice.toString()) }


    RoundedBackgroundContainer {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Text(
                "Edit Cake",
                fontWeight = FontWeight.Bold,
                color = darkKhakiColor,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            StyledTextField(
                value = cakeName,
                onValueChange = { cakeName = it },
                label = "Cake Name",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            ImagePicker(
                imageUrl = selectedImageUrl,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                onImageSelected = { uri ->
                    selectedImageUri = uri
                    selectedImageUrl = null // Reset URL if new image selected
                }
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "Ingredients",
                fontWeight = FontWeight.Bold,
                color = darkKhakiColor,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(8.dp))

            val ingredientsScrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(ingredientsScrollState)
            ) {
                ingredientsMap.entries.forEach { (ingredientId, qtyPerCake) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ingredientName = allIngredients.find { it.id == ingredientId }?.name ?: "Unknown"
                        Text(
                            text = ingredientName,
                            modifier = Modifier.weight(1f),
                            color = darkKhakiColor,
                        )
                        Spacer(Modifier.width(8.dp))
                        StyledTextField(
                            value = qtyPerCake.toString(),
                            onValueChange = { newQtyStr ->
                                val newQty = newQtyStr.toDoubleOrNull() ?: 0.0
                                ingredientsMap[ingredientId] = newQty
                            },
                            label = "Qty",
                            modifier = Modifier.width(80.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            modifier = Modifier.border(2.dp, Color.Black, RoundedCornerShape(24.dp)),
                            onClick = {
                                // Remove ingredient locally only
                                ingredientsMap = ingredientsMap.toMutableMap().also { it.remove(ingredientId) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC41E3A))
                        ) {
                            Text("Remove", color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            StyledTextField(
                value = wholeCakePrice,
                onValueChange = { wholeCakePrice = it },
                label = "Whole Cake Price (RM)",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            StyledTextField(
                value = sliceCakePrice,
                onValueChange = { sliceCakePrice = it },
                label = "Slice Cake Price (RM)",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            var dropdownExpanded by rememberSaveable { mutableStateOf(false) }
            val darkKhaki = darkKhakiColor

            Box {
                Button(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Add Ingredient",
                            modifier = Modifier.align(Alignment.Center),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    allIngredients.filter { it.id !in ingredientsMap.keys }.forEach { ingredient ->
                        DropdownMenuItem(
                            text = { Text(ingredient.name, color = darkKhaki) },
                            onClick = {
                                ingredientsMap[ingredient.id] = 0.0
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Row for cancel and save buttons with specified styles
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cancel button with rounded corners, black border, dark khaki bg, white text
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "CANCEL",
                            modifier = Modifier.align(Alignment.Center),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Made changes button with red background etc.
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val updatedCake = cake.copy(
                                type = cakeName,
                                ingredients = ingredientsMap.toMap(),
                                wholeCakePrice = wholeCakePrice.toDoubleOrNull() ?: cake.wholeCakePrice,
                                sliceCakePrice = sliceCakePrice.toDoubleOrNull() ?: cake.sliceCakePrice
                            )

                            viewModel.updateCakeInFirestore(updatedCake, selectedImageUri, context)
                            showSuccessDialog = true
                            delay(1500L)
                            showSuccessDialog = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_red_paper),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "CHANGE",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Popup dialog for changes made confirmation
            if (showSuccessDialog) {
                Dialog(onDismissRequest = { showSuccessDialog = false }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_paper_text),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "CHANGES MADE SUCCESSFUL",
                            color = darkKhakiColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    showSuccessDialog = false
                }
            }
        }
    }
}


@Composable
fun IngredientEditDialog(
    viewModel: InventoryViewModel,
    ingredient: Ingredient,
    stock: Stock,
    onDismiss: () -> Unit
) {

    var ingredientName by rememberSaveable { mutableStateOf(ingredient.name) }
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedImageUrl by rememberSaveable { mutableStateOf(ingredient.imageUrl) }
    var expiryDate by rememberSaveable { mutableStateOf(stock.expiryDate ?: "") }
    var reason by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf(stock.quantity.toString()) }
    var isGram by rememberSaveable { mutableStateOf(stock.unit == "GRAM") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    val darkKhakiColor = Color(0xFF786A42)
    val scrollState = rememberScrollState()
    val previousExpiry = stock.expiryDate
    val currentExpiry = expiryDate // state from user input field

    val currentImageUrl = selectedImageUrl // e.g. user-selected new image url

// Build edit record with change tracking:
    val expiryChange = if (previousExpiry != currentExpiry)
        ExpiryChange(before = previousExpiry, after = currentExpiry)
    else null

    val oldImageUrl: String? = ingredient?.imageUrl // get previous from ingredient
    val newImageUrl: String? = selectedImageUrl     // get new from user input or picker

    val imageChange: String = if (oldImageUrl != newImageUrl) "CHANGED" else "NOT CHANGE"


    RoundedBackgroundContainer {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Text(
                "Edit Ingredient (Stock: ${stock.stockId})",
                fontWeight = FontWeight.Bold,
                color = darkKhakiColor,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            StyledTextField(
                value = ingredientName,
                onValueChange = { ingredientName = it },
                label = "Ingredient Name",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            ImagePicker(
                imageUrl = selectedImageUrl,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                onImageSelected = { uri ->
                    selectedImageUri = uri
                    selectedImageUrl = null
                }
            )
            Spacer(Modifier.height(16.dp))
            StyledTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                label = "Expiry Date (YYYY-MM-DD)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            StyledTextField(
                value = reason,
                onValueChange = { reason = it },
                label = "Reason for Edit",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            QuantityInputWithUnit(
                quantity = quantity,
                onQuantityChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                isGram = isGram,
                onUnitToggle = { isGram = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                            .fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "CANCEL",
                            modifier = Modifier.align(Alignment.Center),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        val qtyDouble = quantity.toDoubleOrNull() ?: 0.0
                        val quantityDiff = (qtyDouble - stock.quantity) * if (isGram) 1.0 else 1000.0
                        val now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        if (reason.isNotBlank()) {
                            val editRecord = IngredientEditRecord(
                                ingredientName = ingredient?.name ?: "",
                                timestamp = now,
                                reason = reason,
                                quantityDiff = quantityDiff,
                                stockId = stock.stockId,
                                expiryChange = expiryChange,
                                imageChange = imageChange
                            )
                            coroutineScope.launch {
                                viewModel.saveIngredientEditRecord(editRecord)
                                val updatedStock = stock.copy(
                                    quantity = qtyDouble,
                                    expiryDate = expiryDate,
                                    unit = if (isGram) "GRAM" else "KG"
                                )
                                viewModel.updateStockForIngredient(ingredient.id, updatedStock)
                                showSuccessDialog = true
                                delay(1500L)
                                showSuccessDialog = false
                                onDismiss()
                            }
                        }
                    },
                    enabled = reason.isNotBlank() && quantity.toDoubleOrNull() != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_red_paper),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "CHANGE",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showSuccessDialog) {
                Dialog(onDismissRequest = { showSuccessDialog = false }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_paper_text),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "CHANGES MADE SUCCESSFUL",
                            color = darkKhakiColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    showSuccessDialog = false
                }
            }
        }
    }
}


