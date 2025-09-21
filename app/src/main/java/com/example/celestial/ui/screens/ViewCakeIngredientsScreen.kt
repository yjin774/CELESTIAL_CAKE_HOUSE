package com.example.celestial.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import coil.compose.AsyncImage
import android.util.Log
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import com.example.celestial.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.celestial.ui.viewmodels.InventoryViewModel

// ---------- Utility and Data ----------
data class ChartData(
    val label: String,
    val value: Float,
    val color: Color,
    val ratio: Float,
    val ingredientId: String,
    val notEnough: Boolean
)

fun generateBlackVariants(n: Int): List<Color> {
    // Step from light gray (0.8F) to pure black (0F)
    return List(n) { i ->
        val lightness = 0.8f - (i * 0.6f / ((n - 1).coerceAtLeast(1)))
        Color(lightness, lightness, lightness)
    }
}

// ---------- Composable Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewCakeIngredientsScreen(
    navController: NavController, viewModel: InventoryViewModel,
    cakeId: String
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val scrollState = rememberScrollState()
    val cakes by viewModel.cakes.observeAsState(emptyList())
    val ingredients by viewModel.ingredients.observeAsState(emptyList())
    val cake = cakes.find { it.id == cakeId }
    if (cake == null) {
        Log.e("ViewCakeIngredients", "Cake not found for ID: $cakeId")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cake not found", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    // Get current stock for each ingredient
    val ingredientStocks = remember { mutableStateMapOf<String, Double>() }
    for (ingredient in ingredients) {
        val stocks by viewModel.getStocksForIngredient(ingredient.id).observeAsState(emptyList())
        val totalStock = stocks.sumOf {
            val isExpired = try {
                it.expiryDate?.let { d -> java.time.LocalDate.parse(d).isBefore(java.time.LocalDate.now()) } ?: false
            } catch (_: Exception) { false }
            if (!isExpired && it.quantity > 0) {
                if (it.unit.trim().uppercase() == "KG") it.quantity * 1000.0 else it.quantity
            } else 0.0
        }
        ingredientStocks[ingredient.id] = totalStock
    }
    val darkKhaki = Color(0xFF786A58)

    // Prepare color palettes
    val ingredientEntries = cake.ingredients.entries.toList()
    val count = ingredientEntries.size
    val baseColors = listOf(
        Color(0xFFFB7185), Color(0xFF3B82F6), Color(0xFFFACC15), Color(0xFF34D399),
        Color(0xFF8B5CF6), Color(0xFFFF7F50), Color(0xFF7FFF00), Color(0xFF48D1CC),
        Color(0xFFFF1493), Color(0xFF00BFFF), Color(0xFFDC143C)
    )
    val blackVariants = generateBlackVariants(count)

    // Prepare chart data
    val chartData: List<ChartData> = run {
        val data = ingredientEntries.mapIndexed { idx, entry ->
            val ingredientId = entry.key
            val needed = (entry.value as? Number)?.toFloat() ?: 0f
            val ingredient = ingredients.find { it.id == ingredientId }
            val stock = ingredientStocks[ingredientId] ?: 0.0
            val notEnough = needed > stock

            // Get original color
            val originalColor = if (notEnough) blackVariants[idx] else baseColors[idx % baseColors.size]
            // Make it half transparent
            val color = originalColor.copy(alpha = 0.8f)

            ChartData(
                label = ingredient?.name ?: "Unknown",
                value = needed,
                color = color,
                ratio = 0f,
                ingredientId = ingredientId,
                notEnough = notEnough
            )
        }
        val totalWeight = data.sumOf { it.value.toDouble() }
        data.map { it.copy(ratio = if (totalWeight > 0) it.value / totalWeight.toFloat() else 0f) }
    }

    val screenContent: @Composable () -> Unit = {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton { navController.popBackStack() }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = cake.type,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                PieChartAnimated(
                    chartData,
                    diameterFractionOfWidth = 0.7f
                )
            }
            Spacer(modifier = Modifier.height(40.dp))

            // Scrollable ingredient list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chartData) { cd ->
                    val cardModifier = if (cd.notEnough) {
                        Modifier.clickable {
                            navController.navigate(
                                "inventory?autoFocusIngredientId=${cd.ingredientId}&initialTab=1"
                            ) {
                                popUpTo("inventory") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    } else Modifier

                    Card(
                        modifier = cardModifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (cd.notEnough) Color(0xFF333333) else Color.White,
                            contentColor = if (cd.notEnough) Color.White else Color.Black
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Ingredient image
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(
                                        "file:///android_asset/images/celestial_ingredients/${
                                            getIngredientImageFilename(
                                                cd.label
                                            )
                                        }"
                                    )
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "${cd.label} image",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.celestial_moon),
                                error = painterResource(R.drawable.celestial_sun)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // 2. Name & quantities column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                Text(
                                    text = cd.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Needed: ${"%.1f".format(cd.value)} g",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (cd.notEnough) Color.White else Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Needed: ${"%.3f".format(cd.value / 1000)} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (cd.notEnough) Color.White.copy(alpha = 0.8f) else Color.Gray
                                )
                                if (cd.notEnough) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "NOT ENOUGH - TAP TO ADD STOCK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 3. Percentage at the rightmost corner
                            Text(
                                text = "${"%.1f".format(cd.ratio * 100)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cd.color,
                                textAlign = TextAlign.End,
                                fontSize = 40.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ){
            Row(
                modifier = Modifier.fillMaxWidth().weight(0.2f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton { navController.popBackStack() }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = cake.type,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().weight(1.2f))
            {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PieChartAnimated(chartData = chartData, diameterFractionOfWidth = 0.5f)
                }
                Spacer(Modifier.width(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chartData) { cd ->
                        IngredientCard(cd = cd, navController = navController)
                    }
                }
            }
        }
    }  else {
        screenContent()
    }
}

@Composable
fun IngredientCard(cd: ChartData, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clickable(enabled = cd.notEnough) {
                navController.navigate("inventory?autoFocusIngredientId=${cd.ingredientId}&initialTab=1") {
                    launchSingleTop = true
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cd.notEnough) Color(0xFF333333) else Color.White,
            contentColor = if (cd.notEnough) Color.White else Color.Black
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/images/celestial_ingredients/${getIngredientImageFilename(cd.label)}")
                    .crossfade(true)
                    .build(),
                contentDescription = "${cd.label} image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.celestial_moon),
                error = painterResource(R.drawable.celestial_sun)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cd.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Needed: ${"%.1f".format(cd.value)} g", style = MaterialTheme.typography.bodyMedium, color = if (cd.notEnough) Color.White else Color.DarkGray)
                Text("Needed: ${"%.3f".format(cd.value / 1000)} kg", style = MaterialTheme.typography.bodySmall, color = if (cd.notEnough) Color.White.copy(alpha = 0.8f) else Color.Gray)
                if (cd.notEnough) {
                    Text("NOT ENOUGH - TAP TO ADD STOCK", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "${"%.1f".format(cd.ratio * 100)}%",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 40.sp,
                color = cd.color,
                textAlign = TextAlign.End
            )
        }
    }
}


// ---------- PieChartAnimated for chartData ----------
@Composable
fun PieChartAnimated(
    chartData: List<ChartData>,
    diameterFractionOfWidth: Float,
    gapDegrees: Float = 0f
) {
    val scaleAnim = remember { Animatable(0.5f) }
    val sweepAnim = remember { Animatable(0f) }
    val animationDuration = 1200
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, animationSpec = tween(500))
        sweepAnim.animateTo(360f, animationSpec = tween(animationDuration))
    }
    BoxWithConstraints {
        val size = maxWidth * diameterFractionOfWidth
        val radius = size / 2
        Canvas(
            modifier = Modifier.size(size * scaleAnim.value)
        ) {
            val slices = chartData.size
            val actualGap = if (slices <= 1) 0f else gapDegrees
            val sweepAvailable = 360f - (actualGap * slices)
            var startAngle = -90f
            var currentSweep = sweepAnim.value
            for (segment in chartData) {
                val segSweep = segment.ratio * sweepAvailable
                val thisGap = if (slices <= 1) 0f else actualGap
                val segTotal = segSweep + thisGap
                val drawSweep = when {
                    currentSweep <= 0f -> 0f
                    currentSweep < segTotal -> (currentSweep - thisGap / 2).coerceAtLeast(0f).coerceAtMost(segSweep)
                    else -> segSweep
                }
                if (drawSweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle + actualGap / 2,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = radius.toPx() / 2.2f
                        )
                    )
                }
                startAngle += segTotal
                currentSweep -= segTotal
                if (currentSweep <= 0f) break
            }
        }
    }
}
