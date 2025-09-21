package com.example.celestial.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.celestial.R
import com.example.celestial.data.models.IngredientEditRecord
import com.example.celestial.data.models.Stock
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryScreen(
    navController: NavController,
    viewModel: InventoryViewModel,
    selectedTabIndex: Int = 1
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val coroutineScope = rememberCoroutineScope()

    // Remember UI states with rememberSaveable
    var selectedTab by rememberSaveable { mutableStateOf(selectedTabIndex) }
    var showWastageDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmAllDialog by rememberSaveable { mutableStateOf(false) }
    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var resultDialogMessage by rememberSaveable { mutableStateOf("") }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    val selectedExpiredStocks = remember { mutableStateListOf<Pair<String, Stock>>() }

    val ingredients by viewModel.ingredients.observeAsState(emptyList())
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val today = LocalDate.now()
    val threshold = remember { today.plusDays(7) }

    val stocksLists = ingredients.map { ing ->
        ing.id to viewModel.getStocksForIngredient(ing.id).observeAsState(emptyList()).value
    }

    val expiringStocks by remember(stocksLists) {
        derivedStateOf {
            stocksLists.flatMap { (ingredientId, stocks) ->
                stocks.mapNotNull { stock ->
                    val date = runCatching { stock.expiryDate?.let { LocalDate.parse(it, formatter) } }.getOrNull()
                    if (date != null) {
                        val notExpired = !date.isBefore(today)
                        val expiringSoon = date.isBefore(threshold.plusDays(1))
                        if (notExpired && expiringSoon && stock.quantity > 0) {
                            ingredientId to stock
                        } else null
                    } else null
                }
            }
        }
    }

    val expiredStocks by remember(stocksLists) {
        derivedStateOf {
            stocksLists.flatMap { (ingredientId, stocks) ->
                stocks.mapNotNull { stock ->
                    val date = runCatching { stock.expiryDate?.let { LocalDate.parse(it, formatter) } }.getOrNull()
                    if (date != null && date.isBefore(today)) {
                        ingredientId to stock
                    } else null
                }
            }
        }
    }

    val depletedStocks by remember(stocksLists) {
        derivedStateOf {
            stocksLists.flatMap { (ingredientId, stocks) ->
                stocks.mapNotNull { stock ->
                    val date = runCatching { stock.expiryDate?.let { LocalDate.parse(it, formatter) } }.getOrNull()
                    val expired = date?.isBefore(today) ?: false
                    if (stock.quantity == 0.toDouble() && !expired) {
                        ingredientId to stock
                    } else null
                }
            }
        }
    }

    val currentOrientation = LocalConfiguration.current.orientation

    // Main content composable
    @Composable
    fun ExpiryScreenContent() {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header box with background image and text
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
                    text = "EXPIRY TRACKING SECTION",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Tab row for filtering
            val tabTitles = listOf(
                "Expiry Tracking",
                "Wastage Tracking",
                "Ingredient Consumption",
                "Edited Record"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
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
                    contentColor = Color(0xFF786A58),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF786A58)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) Color(0xFF786A58) else Color(
                                        0xFF786A58
                                    ).copy(alpha = 0.7f)
                                )
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(5.dp))

            when (selectedTab) {
                0 -> {
                    if (expiringStocks.isEmpty()) {
                        EmptyMessageBox("No stocks expiring within 7 days")
                    } else {
                        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                            StockGridListLandscape(
                                stockItems = expiringStocks,
                                ingredients = ingredients,
                                fontColor = Color(0xFF786A58),
                                secondaryFontColor = Color(0xFFB49C83),
                                isDepleted = false,
                                isExpired = false
                            )
                        } else {
                            StockGridList(
                                stockItems = expiringStocks,
                                ingredients = ingredients,
                                fontColor = Color(0xFF786A58),
                                secondaryFontColor = Color(0xFFB49C83),
                                isDepleted = false,
                                isExpired = false
                            )
                        }
                    }
                }
                1 -> {
                    if (expiredStocks.isEmpty()) {
                        EmptyMessageBox("No expired stocks recorded")
                    } else {
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()) // Only if necessary here!
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 500.dp) // or whatever max works for your grid
                                ) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(expiredStocks) { (ingredientId, stock) ->
                                            val ingredientName = ingredients.find { it.id == ingredientId }?.name ?: ingredientId
                                            StockCardLandscape(
                                                ingredientName = ingredientName,
                                                stock = stock,
                                                fontColor = Color.White,
                                                secondaryFontColor = Color.White,
                                                isDepleted = false,
                                                isExpired = true
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        selectedExpiredStocks.clear()
                                        selectedExpiredStocks.addAll(expiredStocks)
                                        showWastageDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFC41E3A),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Text("UPDATE WASTAGE", style = MaterialTheme.typography.titleMedium)
                                }
                            }

                        } else {
                            Column {
                                StockGridList(
                                    stockItems = expiredStocks,
                                    ingredients = ingredients,
                                    fontColor = Color.White,
                                    secondaryFontColor = Color.White,
                                    isDepleted = false,
                                    isExpired = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        selectedExpiredStocks.clear()
                                        selectedExpiredStocks.addAll(expiredStocks)
                                        showWastageDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFC41E3A),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Text("UPDATE WASTAGE", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (depletedStocks.isEmpty()) {
                        EmptyMessageBox("No depleted stocks")
                    } else {
                        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                            StockGridListLandscape(
                                stockItems = depletedStocks,
                                ingredients = ingredients,
                                fontColor = Color.White,
                                secondaryFontColor = Color.White,
                                isDepleted = true,
                                isExpired = false
                            )
                        } else {
                            StockGridList(
                                stockItems = depletedStocks,
                                ingredients = ingredients,
                                fontColor = Color.White,
                                secondaryFontColor = Color.White,
                                isDepleted = true,
                                isExpired = false
                            )
                        }
                    }
                }
                3 -> {
                    val editedRecords by viewModel.ingredientEditRecords.observeAsState(emptyList())
                    if (editedRecords.isEmpty()) {
                        EmptyMessageBox("No Edited Record yet")
                    } else {
                        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                content = {
                                    items(editedRecords) { record ->
                                        FlippableEditRecordCardLandscape(record)
                                    }
                                })
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                content = {
                                    items(editedRecords) { record ->
                                        FlippableEditRecordCard(record)
                                    }
                                })
                        }
                    }
                }
            }
        }
    }

    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            ExpiryScreenContent()

    } else {
        ExpiryScreenContent()
    }


    if (showSuccessDialog) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp, 150.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.celestial_paper_text),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Text(
                        "UPDATED WASTAGE SUCCESSFUL",
                        color = Color(0xFF786A58),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        LaunchedEffect(showSuccessDialog) {
            kotlinx.coroutines.delay(1500)
            showSuccessDialog = false
        }
    }

    // The wastage update dialog
    if (showWastageDialog) {
        LaunchedEffect(showWastageDialog) {
            if (showWastageDialog) {
                selectedExpiredStocks.clear()
            }
        }
        Dialog(onDismissRequest = { showWastageDialog = false }) {
            Box(
                modifier = Modifier
                    .wrapContentSize() // Let the dialog size adapt to content instead of fillMaxSize
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background
                Image(
                    painter = painterResource(R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 420.dp) // reasonable dialog size
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Text(
                        "Select Expired Stock Records",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(expiredStocks) { (ingredientId, stock) ->
                            val isSelected = selectedExpiredStocks.contains(ingredientId to stock)
                            val backgroundPainter = if (isSelected)
                                painterResource(R.drawable.celestial_dark_red_paper)
                            else
                                painterResource(R.drawable.celestial_dark_grey_paper)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSelected) {
                                            selectedExpiredStocks.remove(ingredientId to stock)
                                        } else {
                                            selectedExpiredStocks.add(ingredientId to stock)
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))                     // clip rounded corners here
                                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp)), // border with same rounded corner
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Background
                                    Image(
                                        painter = backgroundPainter,
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Text content (all white font)
                                    Column(
                                        Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Ingredient: $ingredientId", fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Stock ID: ${stock.stockId}", color = Color.White)
                                        Text("Qty: ${stock.quantity} ${stock.unit}", color = Color.White)
                                        Text("Expiry: ${stock.expiryDate ?: "N/A"}", color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Update all expired stocks as wastage
                                showConfirmAllDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC41E3A), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("UPDATE ALL")
                        }

                        Button(
                            onClick = {
                                if (selectedExpiredStocks.isEmpty()) {
                                    // Show error dialog
                                    resultDialogMessage = "PLEASE CHOOSE AT LEAST ONE RECORD TO UPDATE"
                                    showResultDialog = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(1500)
                                        showResultDialog = false
                                        // Do NOT dismiss main dialog
                                    }
                                } else {
                                    coroutineScope.launch {
                                        updateWastageRecords(viewModel, selectedExpiredStocks.toList())
                                        resultDialogMessage = "UPDATE EXPIRED RECORD SUCCESSFUL"
                                        showResultDialog = true
                                        kotlinx.coroutines.delay(1500)
                                        showResultDialog = false
                                        showWastageDialog = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC41E3A), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("UPDATE ONCE")
                        }
                    }
                    Button(
                        onClick = { showWastageDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        )
                    ) {
                        Text("CANCEL", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showConfirmAllDialog) {
        Dialog(onDismissRequest = { showConfirmAllDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(min = 220.dp, max = 320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ARE YOU SURE YOU WANT TO UPDATE ALL RECORD AT ONCE?", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    updateWastageRecords(viewModel, expiredStocks)
                                    showConfirmAllDialog = false
                                    resultDialogMessage = "UPDATE EXPIRED RECORD SUCCESSFUL"
                                    showResultDialog = true
                                    kotlinx.coroutines.delay(1500)
                                    showResultDialog = false
                                    showWastageDialog = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) { Text("YES") }
                        Button(
                            onClick = { showConfirmAllDialog = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("NO") }
                    }
                }
            }
        }
    }

    if (showResultDialog) {
        Dialog(onDismissRequest = { /* No-op while showing */ }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(min = 180.dp, max = 280.dp)
                ) {
                    Text(resultDialogMessage, color = Color(0xFF786A58), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper function to convert Stock to Wastage and save to Firestore via ViewModel
private fun updateWastageRecords(
    viewModel: InventoryViewModel,
    itemsToUpdate: List<Pair<String, Stock>>
) {
    // Launch coroutine on ViewModel scope for async db operations
    viewModel.viewModelScope.launch {
        itemsToUpdate.forEach { (ingredientId, stock) ->
            val qtyInGram = when (stock.unit.trim().uppercase()) {
                "KG" -> stock.quantity * 1000
                else -> stock.quantity
            }
            val qtyInKg = when (stock.unit.trim().uppercase()) {
                "GRAM" -> stock.quantity / 1000
                else -> stock.quantity
            }
            val wastageRecord = com.example.celestial.data.models.Wastage(
                stockId = stock.stockId,
                expiredQuantityGram = qtyInGram,
                expiredQuantityKg = qtyInKg,
                expiryDate = stock.expiryDate,
                orderedDate = stock.orderedDate
            )
            // Call ViewModel function to add wastage record to Firestore
            viewModel.addWastageRecord(ingredientId, wastageRecord)
            // Optionally: update or delete stock record to remove from Expiry Tracking UI
            viewModel.deleteStock(ingredientId, stock.stockId)
        }
    }
}

@Composable
fun StockGridList(
    stockItems: List<Pair<String, Stock>>,
    ingredients: List<com.example.celestial.data.models.Ingredient>,
    fontColor: Color,
    secondaryFontColor: Color,
    isDepleted: Boolean,
    isExpired: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stockItems) { (ingredientId, stock) ->
            val ingredientName = ingredients.find { it.id == ingredientId }?.name ?: ingredientId
            StockCard(
                ingredientName = ingredientName,
                stock = stock,
                fontColor = fontColor,
                secondaryFontColor = secondaryFontColor,
                isDepleted = isDepleted,
                isExpired = isExpired
            )
        }
    }
}

@Composable
fun StockCard(
    ingredientName: String,
    stock: Stock,
    fontColor: Color,
    secondaryFontColor: Color,
    isDepleted: Boolean,
    isExpired: Boolean
) {
    val backgroundPainter = when {
        isExpired -> painterResource(R.drawable.celestial_dark_red_paper)
        isDepleted -> painterResource(R.drawable.celestial_dark_grey_paper)
        else -> painterResource(R.drawable.celestial_paper_text)
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))                     // clip rounded corners here
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp)), // border with same rounded corner
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Ingredient: $ingredientName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Stock ID: ${stock.stockId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryFontColor,
                )
                Text(
                    text = "Qty: ${stock.quantity} ${stock.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fontColor,
                )
                Text(
                    text = "Expiry Date: ${stock.expiryDate ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fontColor,
                )
                Text(
                    text = "Ordered Date: ${stock.orderedDate ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryFontColor,
                )
            }
        }
    }
}

@Composable
fun FlippableEditRecordCard(
    record: IngredientEditRecord,
    modifier: Modifier = Modifier
) {
    var isCardFlipped by remember { mutableStateOf(false) }
    val animDuration = 900
    val density = LocalDensity.current.density
    val zAxisDistance = 10f

    val rotateCardY by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = animDuration, easing = EaseInOut)
    )
    val frontTextAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 0f else 1f,
        animationSpec = tween(durationMillis = animDuration / 2, easing = EaseInOut)
    )
    val backTextAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 1f else 0f,
        animationSpec = tween(durationMillis = animDuration / 2, easing = EaseInOut)
    )

    val backgroundPainter = painterResource(id = R.drawable.celestial_paper_text)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .graphicsLayer {
                rotationY = rotateCardY
                cameraDistance = zAxisDistance * density
            }
            .clickable { isCardFlipped = !isCardFlipped },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)  // Make card transparent so background image visible
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        )  {
            // Front side
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = frontTextAlpha }
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp)) // FRONT border
            ) {
                Image(
                    painter = backgroundPainter,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text("Edited Ingredient: ${record.ingredientName}", fontWeight = FontWeight.Bold, color = Color(0xFF786A58))
                    Text("Stock ID: ${record.stockId}", color = Color(0xFF786A58))
                    Text("Quantity Change: ${record.quantityDiff}", color = Color(0xFF786A58))
                    Text("Timestamp: ${record.timestamp}", color = Color.DarkGray)
                    if (record.expiryChange != null) {
                        Text(
                            text = "Expiry Change: ${record.expiryChange.before ?: "-"} → ${record.expiryChange.after ?: "-"}",
                            color = Color(0xFF786A58),
                            fontWeight = FontWeight.Bold
                        )
                    }

// New image change display
                    Text(text = "Image Change: ${record.imageChange}",
                        color = Color(0xFF786A58),
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Tap to show reason", textAlign = TextAlign.Center, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
            // Back side
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = backTextAlpha
                        rotationY = 180f
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp)) // BACK border
            ) {
                Image(
                    painter = backgroundPainter,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 18.dp)
                ) {
                    Text("Edit Reason", color = Color(0xFF786A58), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(record.reason, color = Color.DarkGray, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

    }
}

@Composable
fun EmptyMessageBox(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    color = Color(0xFF665946), // Dark Khaki approx
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun StockGridListLandscape(
    stockItems: List<Pair<String, Stock>>,
    ingredients: List<com.example.celestial.data.models.Ingredient>,
    fontColor: Color,
    secondaryFontColor: Color,
    isDepleted: Boolean,
    isExpired: Boolean
) {
    // Using LazyVerticalGrid, you can adjust columns or spacings for landscape if needed
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // e.g. more columns for landscape
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stockItems) { (ingredientId, stock) ->
            val ingredientName = ingredients.find { it.id == ingredientId }?.name ?: ingredientId
            StockCardLandscape(
                ingredientName = ingredientName,
                stock = stock,
                fontColor = fontColor,
                secondaryFontColor = secondaryFontColor,
                isDepleted = isDepleted,
                isExpired = isExpired
            )
        }
    }
}

@Composable
fun StockCardLandscape(
    ingredientName: String,
    stock: Stock,
    fontColor: Color,
    secondaryFontColor: Color,
    isDepleted: Boolean,
    isExpired: Boolean
) {
    val backgroundPainter = when {
        isExpired -> painterResource(R.drawable.celestial_dark_red_paper)
        isDepleted -> painterResource(R.drawable.celestial_dark_grey_paper)
        else -> painterResource(R.drawable.celestial_paper_text)
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))                     // clip rounded corners here
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp)), // border with same rounded corner
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Ingredient: $ingredientName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Stock ID: ${stock.stockId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryFontColor,
                )
                Text(
                    text = "Qty: ${stock.quantity} ${stock.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fontColor,
                )
                Text(
                    text = "Expiry Date: ${stock.expiryDate ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fontColor,
                )
                Text(
                    text = "Ordered Date: ${stock.orderedDate ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryFontColor,
                )
            }
        }
    }
}

@Composable
fun FlippableEditRecordCardLandscape(
    record: IngredientEditRecord,
    modifier: Modifier = Modifier
) {
    var isCardFlipped by remember { mutableStateOf(false) }
    val animDuration = 900
    val density = LocalDensity.current.density
    val zAxisDistance = 10f

    val rotateCardY by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = animDuration, easing = EaseInOut)
    )
    val frontTextAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 0f else 1f,
        animationSpec = tween(durationMillis = animDuration / 2, easing = EaseInOut)
    )
    val backTextAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 1f else 0f,
        animationSpec = tween(durationMillis = animDuration / 2, easing = EaseInOut)
    )

    val backgroundPainter = painterResource(id = R.drawable.celestial_paper_text)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .graphicsLayer {
                rotationY = rotateCardY
                cameraDistance = zAxisDistance * density
            }
            .clickable { isCardFlipped = !isCardFlipped },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Front side
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = frontTextAlpha }
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = backgroundPainter,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text("Edited Ingredient: ${record.ingredientName}", fontWeight = FontWeight.Bold, color = Color(0xFF786A58))
                    Text("Stock ID: ${record.stockId}", color = Color(0xFF786A58))
                    Text("Quantity Change: ${record.quantityDiff}", color = Color(0xFF786A58))
                    Text("Timestamp: ${record.timestamp}", color = Color.DarkGray)
                    if (record.expiryChange != null) {
                        Text(
                            text = "Expiry Change: ${record.expiryChange.before ?: "-"} → ${record.expiryChange.after ?: "-"}",
                            color = Color(0xFF786A58),
                            fontWeight = FontWeight.Bold
                        )
                    }

// New image change display
                    Text(text = "Image Change: ${record.imageChange}",
                        color = Color(0xFF786A58),
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Tap to show reason", textAlign = TextAlign.Center, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
            // Back side
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = backTextAlpha
                        rotationY = 180f
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = backgroundPainter,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 18.dp)
                ) {
                    Text("Edit Reason", color = Color(0xFF786A58), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(record.reason, color = Color.DarkGray, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}




