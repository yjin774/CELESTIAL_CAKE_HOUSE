package com.example.celestial.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.celestial.R
import com.example.celestial.data.models.Sale
import com.example.celestial.data.models.SoldCake
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.example.celestial.ui.viewmodels.SalesViewModel

@Composable
fun FlippableSaleCard(
    sale: Sale,
    modifier: Modifier = Modifier
) {
    var isFlipped by remember { mutableStateOf(false) }
    val animDuration = 900
    val zAxisDistance = 10f

    // Rotation animation for flipping
    val rotateY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(animDuration, easing = LinearEasing)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotateY
                cameraDistance = zAxisDistance
            }
            .clickable { isFlipped = !isFlipped }
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        val darkKhaki = Color(0xFF786A58)

        // Front side
        if (rotateY <= 90f) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "SALE ID : ${sale.id}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = darkKhaki)
                    )
                    Text(
                        "CUSTOMER NAME : ${sale.custName}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = darkKhaki)
                    )
                    Text(
                        "DATE : ${sale.date.take(16)}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = darkKhaki)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "TAP FOR SALE DETAILS",
                        color = darkKhaki.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        } else {
            // Back side - sale items details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = 180f
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val saleItems = sale.items ?: emptyList()
                    for (soldCake in saleItems) {
                        Text(
                            text = soldCake.cakeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = darkKhaki
                            )
                        )
                        Text(
                            "Whole Cake Quantity: ${soldCake.wholeCakeQty}",
                            color = darkKhaki
                        )
                        Text(
                            "Slice Cake Quantity: ${soldCake.sliceQty}",
                            color = darkKhaki
                        )
                        Text(
                            "Total Sale: RM ${"%.2f".format(soldCake.totalSale)}",
                            color = darkKhaki
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun ShakingText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f, // 10% scale up
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(navController: NavController, viewModel: InventoryViewModel,selectedTabIndex: Int = 3)
{
    val navItems = listOf(
        AnimatedNavScreen.Home,
        AnimatedNavScreen.Expiry,
        AnimatedNavScreen.Dashboard,
        AnimatedNavScreen.Report,
        AnimatedNavScreen.ManageSales,
        AnimatedNavScreen.Inventory
    )
    val darkKhaki = Color(0xFF786A58)
    val darkerKhaki = Color(0xFF665946)
    var selectedScreen by remember { mutableStateOf(selectedTabIndex) }
    val viewModel: SalesViewModel = viewModel()
    val sales by viewModel.sales.observeAsState(emptyList())
    val errorMessage by viewModel.errorMessage.observeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSale by remember { mutableStateOf<Sale?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))                     // clip rounded corners here
                        .border(
                            2.dp,
                            Color.Black,
                            RoundedCornerShape(12.dp)
                        ), // border with same rounded corner
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
                        text = "SALES HISTORY SECTION",
                        color = Color.White, // dark khaki color
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                errorMessage?.let { message ->
                    LaunchedEffect(message) {
                        snackbarHostState.showSnackbar(message)
                        viewModel.clearError()
                    }
                }
                if (sales.isEmpty()) {
                    Text(
                        text = "No sales data available",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sales) { sale ->
                            FlippableSaleCard(
                                sale = sale,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            )

                        }
                    }

                    // Popup dialog for sale detail
                    if (selectedSale != null) {
                        Dialog(onDismissRequest = { selectedSale = null }) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                                color = Color.White
                            ) {
                                Box {
                                    Image(
                                        painter = painterResource(R.drawable.celestial_paper_text),
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize()
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(
                                        modifier = Modifier
                                            .padding(24.dp)
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "SALE DETAILS",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = darkKhaki
                                            )
                                        )

                                        Spacer(Modifier.height(4.dp))


                                        // Only access items if selectedSale is still non-null!
                                        val saleItems = selectedSale?.items ?: emptyList<SoldCake>()
                                        LazyColumn(
                                            modifier = Modifier
                                                .heightIn(max = 300.dp)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(saleItems) { soldCake: SoldCake ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(Modifier.padding(12.dp)) {
                                                        Text(
                                                            text = soldCake.cakeName,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Text("Whole Cake Quantity: ${soldCake.wholeCakeQty}")
                                                        Text("Slice Cake Quantity: ${soldCake.sliceQty}")
                                                        Text(
                                                            "Total Sale: RM ${
                                                                "%.2f".format(
                                                                    soldCake.totalSale
                                                                )
                                                            }"
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(20.dp))

                                        Button(
                                            onClick = { selectedSale = null },
                                            modifier = Modifier.align(Alignment.End)
                                        ) { Text("CLOSE") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
}
