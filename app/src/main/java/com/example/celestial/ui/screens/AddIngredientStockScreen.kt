package com.example.celestial.ui.screens

import kotlinx.coroutines.isActive
import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
import android.util.Log
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.celestial.R
import com.example.celestial.data.models.Stock
import com.example.celestial.ui.viewmodels.InventoryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun FlippableSimpleStockCard(
    stock: Stock,
    isExpired: Boolean,
    isDepleted: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    var isCardFlipped by rememberSaveable { mutableStateOf(false) }

    val animDuration = 900
    val cameraDistance = 10f

    val cardColor = when {
        isExpired -> Color(0xFFC41E3A)
        isDepleted -> Color.DarkGray
        else -> Color.White
    }
    val textColor = if (isExpired || isDepleted) Color.White else Color.Black

    val rotateY by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(animDuration, easing = EaseInOut)
    )
    val frontAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 0f else 1f,
        animationSpec = tween(animDuration / 2, easing = EaseInOut)
    )
    val backAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 1f else 0f,
        animationSpec = tween(animDuration / 2, easing = EaseInOut)
    )

    val scaleAnim = remember { Animatable(1f) }
    val translateAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var animationJob by remember { mutableStateOf<Job?>(null) }

    fun startAnimationLoop() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            try {
                while (isActive && !isCardFlipped) {
                    scaleAnim.animateTo(1.1f, tween(800, easing = EaseInOut))
                    scaleAnim.animateTo(1f, tween(800, easing = EaseInOut))
                    translateAnim.animateTo(10f, tween(800, easing = EaseInOut))
                    translateAnim.animateTo(0f, tween(800, easing = EaseInOut))
                }
            } catch (e: CancellationException) {
                // ignored
            }
        }
    }

    fun stopAnimationLoop() {
        animationJob?.cancel()
        animationJob = null
        coroutineScope.launch {
            scaleAnim.snapTo(1f)
            translateAnim.snapTo(0f)
        }
    }

    LaunchedEffect(isCardFlipped) {
        if (!isCardFlipped) {
            startAnimationLoop()
        } else {
            stopAnimationLoop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            animationJob?.cancel()
        }
    }

    val qtyGrams = if (stock.unit.uppercase() == "KG") stock.quantity * 1000 else stock.quantity
    val qtyKg = if (stock.unit.uppercase() == "GRAM") stock.quantity / 1000f else stock.quantity

    Card(
        modifier = modifier
            .padding(4.dp)
            .graphicsLayer(
                rotationY = rotateY,
                cameraDistance = cameraDistance,
                scaleX = scaleAnim.value,
                scaleY = scaleAnim.value,
                translationX = translateAnim.value
            )
            .clickable { isCardFlipped = !isCardFlipped }
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(Modifier.fillMaxWidth().padding(8.dp),contentAlignment = Alignment.Center) {
            if (frontAlpha > 0f) {
                Column(Modifier.graphicsLayer { alpha = frontAlpha }) {
                    Text("STOCK ID: ${stock.stockId}", color = textColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("EXPIRY DATE:", color = textColor, fontWeight = FontWeight.Bold)
                    Text(stock.expiryDate ?: "N/A", color = textColor)
                    Spacer(Modifier.height(4.dp))
                    Text("REMAINING:", color = textColor, fontWeight = FontWeight.Bold)
                    Text("${qtyGrams.toInt()} GRAM", color = textColor)
                    Text("%.3f KG".format(qtyKg), color = textColor)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = scaleAnim.value,
                                scaleY = scaleAnim.value,
                                translationX = translateAnim.value
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "TAP FOR DETAILS",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                }
            }
            if (backAlpha > 0f) {
                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = backAlpha
                            rotationY = 180f
                        }
                        .padding(start = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ORDERED DATE:", color = textColor, fontWeight = FontWeight.Bold)
                    Text(stock.orderedDate ?: "N/A", color = textColor)
                    Spacer(Modifier.height(4.dp))
                    Text("ORDERED AMOUNT:", color = textColor, fontWeight = FontWeight.Bold)
                    Text("${stock.orderedAmountGram} GRAM", color = textColor)
                    Text("%.3f KG".format(stock.orderedAmountKg), color = textColor)
                }
            }
        }
    }
}


@Composable
fun FlippableStockCard(
    stock: Stock,
    isExpired: Boolean,
    isDepleted: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    var isCardFlipped by rememberSaveable { mutableStateOf(false) }

    val animDuration = 900
    val cameraDistance = 10f

    val cardColor = when {
        isExpired -> Color(0xFFC41E3A)
        isDepleted -> Color.DarkGray
        else -> Color.White
    }
    val textColor = if (isExpired || isDepleted) Color.White else Color.Black

    val rotateY by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(animDuration, easing = EaseInOut)
    )
    val frontAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 0f else 1f,
        animationSpec = tween(animDuration / 2, easing = EaseInOut)
    )
    val backAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 1f else 0f,
        animationSpec = tween(animDuration / 2, easing = EaseInOut)
    )

    val scaleAnim = remember { Animatable(1f) }
    val translateAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var animationJob by remember { mutableStateOf<Job?>(null) }

    fun startAnimationLoop() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            try {
                while (isActive && !isCardFlipped) {
                    Log.d("FlippableStockCard", "Animating scaleAnim to 1.1f")
                    scaleAnim.animateTo(1.1f, tween(800, easing = EaseInOut))
                    Log.d("FlippableStockCard", "Animating scaleAnim back to 1f")
                    scaleAnim.animateTo(1f, tween(800, easing = EaseInOut))
                    Log.d("FlippableStockCard", "Animating translateAnim to 10f")
                    translateAnim.animateTo(10f, tween(800, easing = EaseInOut))
                    Log.d("FlippableStockCard", "Animating translateAnim back to 0f")
                    translateAnim.animateTo(0f, tween(800, easing = EaseInOut))
                }
            } catch (e: CancellationException) {
                Log.d("FlippableStockCard", "Animation coroutine cancelled")
            } catch (e: Exception) {
                Log.e("FlippableStockCard", "Animation error", e)
            }
        }
    }

    fun stopAnimationLoop() {
        animationJob?.cancel()
        animationJob = null
        coroutineScope.launch {
            scaleAnim.snapTo(1f)
            translateAnim.snapTo(0f)
        }
    }

    LaunchedEffect(isCardFlipped) {
        Log.d("FlippableStockCard", "LaunchedEffect triggered: isCardFlipped=$isCardFlipped")
        if (!isCardFlipped) {
            startAnimationLoop()
        } else {
            stopAnimationLoop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            animationJob?.cancel()
        }
    }

    val qtyGrams = if (stock.unit.uppercase() == "KG") stock.quantity * 1000 else stock.quantity
    val qtyKg = if (stock.unit.uppercase() == "GRAM") stock.quantity / 1000f else stock.quantity

    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            FlippableStockCardContent(
                stock = stock,
                textColor = textColor,
                frontAlpha = frontAlpha,
                backAlpha = backAlpha,
                scaleAnim = scaleAnim,
                translateAnim = translateAnim,
                isCardFlipped = isCardFlipped,
                rotateY = rotateY,
                cardColor = cardColor,
                cameraDistance = cameraDistance,
                onClick = { isCardFlipped = !isCardFlipped },
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        FlippableStockCardContent(
            stock = stock,
            textColor = textColor,
            frontAlpha = frontAlpha,
            backAlpha = backAlpha,
            scaleAnim = scaleAnim,
            translateAnim = translateAnim,
            isCardFlipped = isCardFlipped,
            rotateY = rotateY,
            cardColor = cardColor,
            cameraDistance = cameraDistance,
            onClick = { isCardFlipped = !isCardFlipped },
            modifier = modifier
        )
    }
}


@Composable
private fun FlippableStockCardContent(
    stock: Stock,
    textColor: Color,
    frontAlpha: Float,
    backAlpha: Float,
    scaleAnim: Animatable<Float, *>,
    translateAnim: Animatable<Float, *>,
    isCardFlipped: Boolean,
    rotateY: Float,
    cardColor: Color,
    cameraDistance: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qtyGrams = if (stock.unit.uppercase() == "KG") stock.quantity * 1000 else stock.quantity
    val qtyKg = if (stock.unit.uppercase() == "GRAM") stock.quantity / 1000f else stock.quantity

    Card(
        modifier = modifier
            .padding(4.dp)
            .graphicsLayer(
                rotationY = rotateY,
                cameraDistance = cameraDistance,
                scaleX = scaleAnim.value,
                scaleY = scaleAnim.value,
                translationX = translateAnim.value
            )
            .clickable { onClick() }
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            if (frontAlpha > 0f) {
                Column(modifier = Modifier.graphicsLayer { alpha = frontAlpha }) {
                    Text("STOCK ID: ${stock.stockId}", color=textColor, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("EXPIRY DATE:", color=textColor, fontWeight=FontWeight.Bold)
                    Text(stock.expiryDate ?: "N/A", color=textColor)
                    Spacer(Modifier.height(4.dp))
                    Text("REMAINING:", color=textColor, fontWeight=FontWeight.Bold)
                    Text("${qtyGrams.toInt()} GRAM", color=textColor)
                    Text(String.format("%.3f KG", qtyKg), color=textColor)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "TAP FOR DETAILS",
                        color=textColor,
                        fontWeight=FontWeight.Bold,
                        modifier = Modifier.graphicsLayer(
                            scaleX = scaleAnim.value,
                            scaleY = scaleAnim.value,
                            translationX = translateAnim.value
                        )
                    )
                }
            }
            if (backAlpha > 0f) {
                Column(modifier = Modifier.graphicsLayer { alpha = backAlpha; rotationY = 180f }) {
                    Text("ORDERED DATE:", color=textColor, fontWeight=FontWeight.Bold)
                    Text(stock.orderedDate ?: "N/A", color=textColor)
                    Spacer(Modifier.height(4.dp))
                    Text("ORDERED AMOUNT:", color=textColor, fontWeight=FontWeight.Bold)
                    Text("${stock.orderedAmountGram} GRAM", color=textColor)
                    Text(String.format("%.3f KG", stock.orderedAmountKg), color=textColor)
                }
            }
        }
    }
}

@Composable
fun PaperTextbox(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = MaterialTheme.typography.titleMedium.fontSize,
    backgroundPainter: Painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
    fontColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = backgroundPainter,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Text(
            text = text,
            color = fontColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 12.dp).align(Alignment.Center)
        )
    }
}


@Composable
fun QuickMessageDialog(
    message: String,
    onDismiss: () -> Unit,
    darkKhaki: Color = Color(0xFF6B5A42)
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .border(2.dp, Color.Black, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White) // fallback for image transparency
        ) {
            Image(
                painter = painterResource(R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Text(
                message,
                color = darkKhaki,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun ParchmentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isNumberField: Boolean = false
) {
    val khakiFocused = Color(0xFF786A58)
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var isFocused by rememberSaveable { mutableStateOf(false) }
    val khakiText = Color(0xFF665946)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                2.dp,
                if (isFocused) khakiFocused else khakiText,
                RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Image(
            painter = painterResource(R.drawable.celestial_paper_text),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        BasicTextField(
            value = value,
            onValueChange = {
                if (isNumberField) {
                    onValueChange(it)
                } else {
                    onValueChange(it)
                }
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = khakiText),
            cursorBrush = SolidColor(khakiText),
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            keyboardOptions = if (isNumberField)
                KeyboardOptions(keyboardType = KeyboardType.Number)
            else
                KeyboardOptions.Default,
            decorationBox = { innerTextField ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = khakiText.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}


@Composable
fun ThemedDialogButtons(
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
                disabledContainerColor = Color.Transparent,
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
fun ThemedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Color(0xFF665946),
            uncheckedTrackColor = Color(0xFF665946).copy(alpha = 0.5f),
            checkedThumbColor = Color.White,
            uncheckedThumbColor = Color.White
        ),
        modifier = Modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIngredientStockScreen(
    navController: NavController,
    ingredientId: String,
    viewModel: InventoryViewModel
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val darkKhakiWhiteBg = painterResource(id = R.drawable.celestial_dark_khaki_paper)
    var showInvalidDateDialog by rememberSaveable { mutableStateOf(false) }
    var showNonNumberDialog by rememberSaveable { mutableStateOf(false) }
    val khakiText = Color(0xFF665946)
    val darkerKhaki = Color(0xFF6B5A42)


    val ingredientsState by viewModel.ingredients.observeAsState(emptyList())
    val ingredient = ingredientsState.find { it.id == ingredientId }

    val stocksState by viewModel.getStocksForIngredient(ingredientId).observeAsState(emptyList())

    val coroutineScope = rememberCoroutineScope()
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val today = LocalDate.now()

    val availableStocks = stocksState.filter { stock ->
        val expiryDate = try { stock.expiryDate?.let { LocalDate.parse(it, formatter) } } catch (_: Exception) { null }
        val notExpired = expiryDate == null || !expiryDate.isBefore(today)
        val notDepleted = stock.quantity > 0
        notExpired && notDepleted
    }
    val totalGrams = availableStocks.sumOf { stock -> if (stock.unit.trim().uppercase() == "KG") stock.quantity * 1000 else stock.quantity }
    val totalKg = totalGrams / 1000.0

    var quantity by rememberSaveable { mutableStateOf("") }
    var isGram by rememberSaveable { mutableStateOf(true) }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var showAddSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var addSuccessMessage by rememberSaveable { mutableStateOf("") }
    var showNoStockDialog by rememberSaveable { mutableStateOf(false) }


    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BackButton(
                    modifier = Modifier.size(48.dp) // Adjust size as needed
                ) {
                    navController.popBackStack()
                }

                PaperTextbox(
                    text = ingredient?.name?.uppercase() ?: "",
                    modifier = Modifier.weight(1f),
                    backgroundPainter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                    fontColor = Color.White
                )
            }

            PaperTextbox(
                text = "TOTAL QUANTITY: ${String.format("%.0f", totalGrams)} GRAM / ${String.format("%.3f", totalKg)} KG",
                modifier = Modifier.fillMaxWidth(),
                backgroundPainter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                fontColor = Color.White
            )

            ParchmentTextField(
                value = quantity,
                onValueChange = { quantity = it },
                placeholder = "Quantity",
                isNumberField = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PaperTextbox(
                    text = if (isGram) "GRAM" else "KG",
                    modifier = Modifier.weight(1f),
                    backgroundPainter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                    fontColor = Color.White
                )
                ThemedSwitch(checked = isGram, onCheckedChange = { isGram = it })
            }

            ParchmentTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                placeholder = "Expiry Date YYYY-MM-DD",
                modifier = Modifier.fillMaxWidth()
            )

            ThemedDialogButtons(
                text = "ADD STOCK",
                enabled = quantity.isNotBlank() && expiryDate.isNotBlank(),
                onClick = {
                    val qty = quantity.toDoubleOrNull()
                    val validDate = try {
                        LocalDate.parse(expiryDate, DateTimeFormatter.ISO_LOCAL_DATE); true
                    } catch (e: Exception) {
                        false
                    }
                    when {
                        quantity.any { !it.isDigit() } -> {
                            showNonNumberDialog = true
                        }

                        qty == null || qty <= 0 -> {
                            showNonNumberDialog = true
                        }

                        !validDate -> {
                            showInvalidDateDialog = true
                        }

                        else -> {
                            coroutineScope.launch {
                                Log.d("AddStock", "Trying add: $qty for $ingredientId")
                                val success = viewModel.addStock(
                                    ingredientId,
                                    qty!!,
                                    if (isGram) "GRAM" else "KG",
                                    expiryDate
                                )
                                Log.d("AddStock", "Result: $success")
                                if (success) {
                                    quantity = ""
                                    expiryDate = ""
                                    addSuccessMessage =
                                        "NEW $qty ${if (isGram) "GRAM" else "KG"} ADDED SUCCESSFUL"
                                    showAddSuccessDialog = true
                                } else {
                                    // Temporary: show an error dialog or Toast for debugging
                                    Log.e(
                                        "AddStock",
                                        "Failed to add stock. Error: ${viewModel.errorMessage.value}"
                                    )
                                    // Optionally: show a dialog or Snackbar
                                }
                            }

                        }
                    }
                }
            )

            PaperTextbox(
                text = "AVAILABLE STOCK",
                modifier = Modifier.fillMaxWidth(),
                backgroundPainter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                fontColor = Color.White
            )

            if (stocksState.isEmpty()) {
                PaperTextbox(
                    text = "NO STOCKS AVAILABLE, ADD NEW STOCK",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    fontColor = Color(0xFF6B5A42)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableStocks) { stock ->
                        val expiryLocalDate = try {
                            stock.expiryDate?.let { LocalDate.parse(it, formatter) }
                        } catch (e: Exception) {
                            null
                        }

                        val isExpired = expiryLocalDate?.isBefore(today) ?: false
                        val isDepleted = stock.quantity <= 0

                        FlippableSimpleStockCard(
                            stock = stock,
                            isExpired = isExpired,
                            isDepleted = isDepleted
                        )
                    }
                }
            }
        }
    }
    else
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Row: Back Button, Spacer, PaperTextbox with uppercased ingredient name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton { navController.popBackStack() }
                Spacer(Modifier.width(12.dp))

                PaperTextbox(
                    text = "ADD STOCK FOR ${ingredient?.name.orEmpty().uppercase()}",
                    modifier = Modifier.weight(1f),
                    backgroundPainter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                    fontColor = Color.White
                )

            }

            Spacer(Modifier.height(16.dp))

            // PaperTextbox for TOTAL QUANTITY
            PaperTextbox(
                text = "TOTAL QUANTITY : \n${
                    String.format(
                        "%.0f",
                        totalGrams
                    )
                } GRAM \n${String.format("%.3f", totalKg)} KG",
                modifier = Modifier.fillMaxWidth(),
                backgroundPainter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                fontColor = Color.White
            )


            Spacer(Modifier.height(16.dp))

            // Quantity input TextField with black border (ParchmentTextField used)
            ParchmentTextField(
                value = quantity,
                onValueChange = { quantity = it },
                placeholder = "Quantity (${if (isGram) "GRAM" else "KG"})"
            )

            Spacer(Modifier.height(2.dp))

            // Row with PaperTextbox for "Unit" and ThemedSwitch on same row
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaperTextbox(
                    text = "UNIT : ${if (isGram) "GRAM" else "KG"}",
                    modifier = Modifier.weight(1f),
                    backgroundPainter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                    fontColor = Color.White
                )

                Spacer(Modifier.width(12.dp))

                ThemedSwitch(isGram) { isGram = it }
            }

            Spacer(Modifier.height(2.dp))

            // Expiry date TextField with black border (ParchmentTextField used)
            ParchmentTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                placeholder = "Expiry Date (YYYY-MM-DD)"
            )

            Spacer(Modifier.height(4.dp))

            // Add Stock Button (ThemedDialogButtons wraps button with black border)
            ThemedDialogButtons(
                text = "ADD STOCK",
                enabled = quantity.isNotBlank() && expiryDate.isNotBlank(),
                onClick = {
                    val qty = quantity.toDoubleOrNull()
                    val validDate = try {
                        LocalDate.parse(expiryDate, DateTimeFormatter.ISO_LOCAL_DATE); true
                    } catch (e: Exception) {
                        false
                    }
                    when {
                        quantity.any { !it.isDigit() } -> {
                            showNonNumberDialog = true
                        }

                        qty == null || qty <= 0 -> {
                            showNonNumberDialog = true
                        }

                        !validDate -> {
                            showInvalidDateDialog = true
                        }

                        else -> {
                            coroutineScope.launch {
                                Log.d("AddStock", "Trying add: $qty for $ingredientId")
                                val success = viewModel.addStock(
                                    ingredientId,
                                    qty!!,
                                    if (isGram) "GRAM" else "KG",
                                    expiryDate
                                )
                                Log.d("AddStock", "Result: $success")
                                if (success) {
                                    quantity = ""
                                    expiryDate = ""
                                    addSuccessMessage =
                                        "NEW $qty ${if (isGram) "GRAM" else "KG"} ADDED SUCCESSFUL"
                                    showAddSuccessDialog = true
                                } else {
                                    // Temporary: show an error dialog or Toast for debugging
                                    Log.e(
                                        "AddStock",
                                        "Failed to add stock. Error: ${viewModel.errorMessage.value}"
                                    )
                                    // Optionally: show a dialog or Snackbar
                                }
                            }

                        }
                    }
                }
            )

            if (showInvalidDateDialog) {
                QuickMessageDialog(
                    message = "INVALID EXPIRY DATE",
                    onDismiss = { showInvalidDateDialog = false }
                )
                LaunchedEffect(Unit) {
                    delay(1500)
                    showInvalidDateDialog = false
                }
            }
            if (showNonNumberDialog) {
                QuickMessageDialog(
                    message = "PLEASE ENTER NUMBER",
                    onDismiss = { showNonNumberDialog = false }
                )
                LaunchedEffect(Unit) {
                    delay(1500)
                    showNonNumberDialog = false
                }
            }


            Spacer(Modifier.height(20.dp))

            PaperTextbox(
                text = "AVAILABLE STOCK",
                modifier = Modifier.fillMaxWidth(),
                backgroundPainter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                fontColor = Color.White
            )


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                if (availableStocks.isEmpty()) {
                    PaperTextbox(
                        text = "NO STOCKS AVAILABLE\nPLEASE ADD NEW STOCK",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp).align(alignment = Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableStocks) { stock ->
                            val expiryLocalDate = try {
                                stock.expiryDate?.let { LocalDate.parse(it, formatter) }
                            } catch (e: Exception) {
                                null
                            }

                            val isExpired = expiryLocalDate?.isBefore(today) ?: false
                            val isDepleted = stock.quantity <= 0

                            FlippableStockCard(
                                stock = stock,
                                isExpired = isExpired,
                                isDepleted = isDepleted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSuccessDialog) {
        ThemedDialog(
            onDismissRequest = { showAddSuccessDialog = false },
            darkerKhaki = darkerKhaki,
            title = "Success",
            content = {
                Text(addSuccessMessage, color = darkerKhaki, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            buttons = {}
        )
        LaunchedEffect(showAddSuccessDialog) {
            delay(1500)
            showAddSuccessDialog = false
        }
    }
}
