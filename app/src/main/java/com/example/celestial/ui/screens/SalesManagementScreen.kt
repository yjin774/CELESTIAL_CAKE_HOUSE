package com.example.celestial.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.celestial.R
import com.example.celestial.data.models.Cake
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesManagementScreen(
    navController: NavController,
    viewModel: InventoryViewModel,
    selectedTabIndex: Int = 4
) {
    val navItems = listOf(
        AnimatedNavScreen.Home,
        AnimatedNavScreen.Expiry,
        AnimatedNavScreen.Dashboard,
        AnimatedNavScreen.Report,
        AnimatedNavScreen.ManageSales,
        AnimatedNavScreen.Inventory
    )
    var selectedScreen by rememberSaveable { mutableStateOf(selectedTabIndex) }
    val cakes by viewModel.cakes.observeAsState(emptyList())
    val cartItems by viewModel.cartItems.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val availableCakes = cakes.filter { it.wholeCakeQuantity > 0 || it.sliceQuantity > 0 }
    var selectedCake by rememberSaveable { mutableStateOf<Cake?>(null) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmationDialog by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    val hasAvailableCakes = availableCakes.isNotEmpty()
    val cartItemCount = cartItems.sumOf { it.wholeCakeQuantity + it.sliceQuantity }

    // Khaki colors
    val KhakiLight = Color(0xFF786A58)
    val KhakiDark  = Color(0xFF665946)

    var showEmptyCartDialog by rememberSaveable { mutableStateOf(false) }

    if (!hasAvailableCakes) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Back button at top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
            }


                // Centered square box with parchment background and text
                Box(
                    modifier = Modifier
                        .size(200.dp) // square
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    // 1) Background image
                    Image(
                        painter = painterResource(R.drawable.celestial_paper_text),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    // 2) Centered text
                    Text(
                        "PLEASE PRODUCE MORE CAKES FOR SALE",
                        color = KhakiDark,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Background image clipped to rounded corners
                    Image(
                        painter = painterResource(R.drawable.celestial_dark_khaki_paper),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp)), // clip same corners here
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "SALES MANAGEMENT SECTION",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableCakes) { cake ->
                        val cartItem = cartItems.find { it.cake.id == cake.id }
                        val (availableWholeCakes, availableSlices) = viewModel.getAvailableWholeAndSlice(cake, cartItem)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCake = cake
                                    showDialog = true
                                }
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box {
                                Image(
                                    painter = painterResource(R.drawable.celestial_paper_text),
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize().border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = cake.type,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = KhakiDark
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Whole Cakes: $availableWholeCakes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KhakiLight
                                    )

                                    Text(
                                        text = "Slice Cakes: $availableSlices",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KhakiLight
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Whole Cake Price: RM ${"%.2f".format(cake.wholeCakePrice)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KhakiLight
                                    )

                                    Text(
                                        text = "Slice Cake Price: RM ${"%.2f".format(cake.sliceCakePrice)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KhakiLight
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating cart button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (cartItemCount == 0) {
                                showEmptyCartDialog = true
                            } else {
                                navController.navigate("cart_management")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.celestial_paper_text),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "View Cart",
                        tint = KhakiDark
                    )
                }

                if (cartItemCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(24.dp),
                        containerColor = KhakiDark,
                        contentColor = Color.White
                    ) {
                        Text(
                            cartItemCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    // Popup dialog for empty cart, auto-dismiss after 1.5 seconds
    if (showEmptyCartDialog) {
        Dialog(onDismissRequest = { showEmptyCartDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .sizeIn(minWidth = 280.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                Box {
                    Image(
                        painter = painterResource(R.drawable.celestial_paper_text),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        "PLEASE ADD SOME CAKE IN CART",
                        modifier = Modifier
                            .padding(24.dp)
                            .align(Alignment.Center),
                        color = KhakiDark,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            showEmptyCartDialog = false
        }
    }


            // Cake sale dialog
            if (showDialog && selectedCake != null) {
                val cake = selectedCake!!
                val cartItem = cartItems.find { it.cake.id == cake.id }
                val (availableWhole, availableSlice) =
                    viewModel.getAvailableWholeAndSlice(cake, cartItem)
                val canAdd = availableWhole > 0 || availableSlice > 0

                CakeSaleDialog(
                    cake = cake,
                    viewModel = viewModel,
                    onDismiss = { showDialog = false },
                    onAddToCart = { qty, isWhole ->
                        coroutineScope.launch {
                            viewModel.addCakeToCart(cake, qty, isWhole)
                            showDialog = false
                            showConfirmationDialog =
                                cake.type to "ADDED $qty ${if (isWhole) "WHOLE CAKES" else "SLICES OF CAKES"} SUCCESSFUL"
                        }
                    },
                    canAdd = canAdd
                )
            }

            // Confirmation Snackbar-like dialog
            showConfirmationDialog?.let { (cakeName, message) ->
                LaunchedEffect(cakeName) {
                    kotlinx.coroutines.delay(1500)
                    showConfirmationDialog = null
                }
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = null },
                    confirmButton = {},
                    text = { Text(message, style = MaterialTheme.typography.bodyLarge) }
                )
            }
        }


@Composable
fun CakeSaleDialog(
    cake: Cake,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit,
    onAddToCart: (quantity: Int, wholeCake: Boolean) -> Unit,
    canAdd: Boolean
) {
    var quantityInput by rememberSaveable { mutableStateOf("") }
    var isWholeCake by rememberSaveable { mutableStateOf(false) }
    val inputQuantity = quantityInput.toIntOrNull() ?: 0
    val cartItem = viewModel.cartItems.collectAsState().value.find { it.cake.id == cake.id }
    val (availableWhole, availableSlice) = viewModel.getAvailableWholeAndSlice(cake, cartItem)
    val labelText = if (isWholeCake) "WHOLE CAKE" else "SLICE CAKE"

    val darkKhaki = Color(0xFF786A58)
    val darkerKhaki = Color(0xFF665946)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnClickOutside = true)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            Box {
                // Background image for the entire dialog
                Image(
                    painter = painterResource(R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Prices (RM)",
                        style = MaterialTheme.typography.titleMedium,
                        color = darkerKhaki
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Whole Cake: RM ${"%.2f".format(cake.wholeCakePrice)}",
                        color = darkKhaki
                    )
                    Text(
                        "Slice Cake: RM ${"%.2f".format(cake.sliceCakePrice)}",
                        color = darkKhaki
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "AVAILABLE :",
                        color = darkerKhaki,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "WHOLE CAKES: $availableWhole",
                        color = darkKhaki
                    )
                    Text(
                        "SLICE CAKES: $availableSlice",
                        color = darkKhaki
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ParchmentTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it.filter { c -> c.isDigit() } },
                            placeholder = labelText,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(labelText, color = KhakiLight)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isWholeCake,
                            onCheckedChange = { isWholeCake = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF665946),    // darker khaki when ON
                                uncheckedTrackColor = Color(0xFF665946).copy(alpha = 0.5f), // lighter khaki when OFF
                                checkedThumbColor = Color.White,
                                uncheckedThumbColor = Color.White
                            )
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = darkKhaki)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        // Add Cart button with paper background and rounded corners
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Image(
                                painter = painterResource(R.drawable.celestial_paper_text),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                            Button(
                                onClick = { if (inputQuantity > 0) onAddToCart(inputQuantity, isWholeCake) },
                                enabled = inputQuantity > 0 && if (isWholeCake) inputQuantity <= availableWhole else inputQuantity <= availableSlice,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = darkerKhaki
                                ),
                            ) {
                                Text("Add Cart")
                            }
                        }
                    }
                }
            }
        }
    }
}
