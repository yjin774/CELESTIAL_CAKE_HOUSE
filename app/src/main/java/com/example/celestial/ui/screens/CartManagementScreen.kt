package com.example.celestial.ui.screens

import androidx.compose.ui.res.painterResource
import android.R.attr.orientation
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.celestial.R
import com.example.celestial.ui.viewmodels.InventoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.celestial_paper_text),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(onClick = onClick, modifier = Modifier.matchParentSize()) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF665946)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartManagementScreen(
    navController: NavController,
    viewModel: InventoryViewModel
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val darkKhaki = Color(0xFF786A58)
    val khaki = Color(0xFF8B7D6B)

    val cartItems by viewModel.cartItems.collectAsState()
    var customerName by rememberSaveable { mutableStateOf("") }
    var customerAddress by rememberSaveable { mutableStateOf("") }
    val totalPrice = remember(cartItems) { viewModel.calculateCartTotal(cartItems) }

    val coroutineScope = rememberCoroutineScope()
    var showCancelPopup by rememberSaveable { mutableStateOf(false) }
    var showSuccessPopup by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (isLandscape) {
                    // Landscape: Use Column without verticalScroll to avoid measurement issues
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(16.dp), // add padding around content
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                BackButton(onClick = { navController.popBackStack() })
                            }
                            items(cartItems) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, Color.Black, RoundedCornerShape(14.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.celestial_paper_text),
                                            contentDescription = null,
                                            modifier = Modifier.matchParentSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = item.cake.type,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = darkKhaki
                                            )
                                            Text(
                                                text = "WHOLE CAKES: ${item.wholeCakeQuantity}",
                                                color = darkKhaki
                                            )
                                            Text(
                                                text = "SLICE CAKES: ${item.sliceQuantity}",
                                                color = darkKhaki
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(2.dp, Color.Black, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.celestial_paper_text),
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize()
                                            .clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text(
                                        text = "TOTAL PRICE RM %.2f".format(totalPrice),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = darkKhaki,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            item {
                                ParchmentTextField(
                                    value = customerName,
                                    onValueChange = { customerName = it },
                                    placeholder = "Customer Name",
                                    modifier = Modifier.fillMaxWidth(),
                                    isNumberField = false
                                )
                            }
                            item {
                                ParchmentTextField(
                                    value = customerAddress,
                                    onValueChange = { customerAddress = it },
                                    placeholder = "Customer Address",
                                    modifier = Modifier.fillMaxWidth(),
                                    isNumberField = false
                                )
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                                        .height(90.dp)
                                        .fillMaxWidth()
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.celestial_paper_text),
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize()
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.rollbackCartChanges()
                                                showCancelPopup = true
                                                delay(1500)
                                                showCancelPopup = false
                                                navController.navigate("home") {
                                                    popUpTo("home") { inclusive = true }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = darkKhaki),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                            if (customerName.isNotBlank() && customerAddress.isNotBlank()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                                            .height(90.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.celestial_paper_text),
                                            contentDescription = null,
                                            modifier = Modifier.matchParentSize()
                                                .clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    viewModel.confirmSale(customerName, cartItems)
                                                    showSuccessPopup = true
                                                    delay(1500)
                                                    showSuccessPopup = false
                                                    navController.navigate("home") {
                                                        popUpTo("home") { inclusive = true }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = darkKhaki
                                            ),
                                            enabled = cartItems.isNotEmpty(),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text("Confirm Sale")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Main content column with BackButton and LazyColumn + bottom inputs
                    Column(modifier = Modifier.fillMaxSize()) {
                        BackButton(onClick = { navController.popBackStack() })

                        Spacer(modifier = Modifier.height(16.dp))

                        // LazyColumn with constrained weight for cart items scroll.
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .heightIn(min = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            items(cartItems) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, Color.Black, RoundedCornerShape(14.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.celestial_paper_text),
                                            contentDescription = null,
                                            modifier = Modifier.matchParentSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = item.cake.type,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = darkKhaki
                                            )
                                            Text(text = "WHOLE CAKES: ${item.wholeCakeQuantity}", color = darkKhaki)
                                            Text(text = "SLICE CAKES: ${item.sliceQuantity}", color = darkKhaki)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Total price and customer info fields just above navigation (horizontal mode)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(2.dp, Color.Black, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.celestial_paper_text),
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = "TOTAL PRICE RM %.2f".format(totalPrice),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = darkKhaki,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            ParchmentTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                placeholder = "Customer Name",
                                modifier = Modifier.fillMaxWidth(),
                                isNumberField = false
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ParchmentTextField(
                                value = customerAddress,
                                onValueChange = { customerAddress = it },
                                placeholder = "Customer Address",
                                modifier = Modifier.fillMaxWidth(),
                                isNumberField = false
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                    // Portrait mode horizontal navigation bar at bottom
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.celestial_paper_text),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.rollbackCartChanges()
                                        showCancelPopup = true
                                        delay(1500)
                                        showCancelPopup = false
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = darkKhaki)
                            ) {
                                Text("Cancel")
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (customerName.isNotBlank() && customerAddress.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.celestial_paper_text),
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.confirmSale(customerName, cartItems)
                                            showSuccessPopup = true
                                            delay(200)
                                            delay(1300)
                                            showSuccessPopup = false
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = darkKhaki
                                    ),
                                    enabled = cartItems.isNotEmpty(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Confirm Sale")
                                }
                            }
                        }
                    }
                }
            }
            }


        if (showSuccessPopup) {
            Dialog(onDismissRequest = { showSuccessPopup = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_paper_text),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "SOLD SUCCESSFUL",
                            style = MaterialTheme.typography.titleLarge,
                            color = darkKhaki,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    }
                }
            }
        }

        if (showCancelPopup) {
            Dialog(onDismissRequest = { showCancelPopup = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "YOU'VE CANCELED THE SALE",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}