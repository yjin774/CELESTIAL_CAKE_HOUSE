package com.example.celestial.ui.screens

import android.content.res.Configuration
import java.net.URLDecoder
import com.example.celestial.R
import java.net.URLEncoder
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.TabRowDefaults
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.celestial.data.models.Cake
import com.example.celestial.data.models.Ingredient
import com.example.celestial.data.models.Sale
import com.example.celestial.data.models.Wastage
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.example.celestial.ui.viewmodels.SalesViewModel

data class ChartDatas(
    val label: String,
    val value: Float,
    val color: Color,
    val ratio: Float
)

// Dynamic HSL color generator:
fun generateDynamicColors(n: Int): List<Color> =
    List(n) { i ->
        val hue = (i * 360f / n) % 360f
        Color.hsl(hue, 0.7f, 0.55f)
    }

fun generateBlackVariantss(n: Int): List<Color> =
    List(n) { i ->
        val l = 0.8f - (i * 0.6f / (n - 1).coerceAtLeast(1))
        Color(l, l, l)
    }

// Define khaki colors at the top of your composable
val KhakiLight = Color(0xFF8B7D6B)
val KhakiDark = Color(0xFF786A58)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDashboardScreen(
    navController: NavController,
    salesViewModel: SalesViewModel,
    viewModel: InventoryViewModel,
    wastageRecords: List<Pair<String, Wastage>>,
    showReportDialogDefault: Boolean = false,
    reportTextDefault: String = "",
    selectedTabIndex: Int = 2
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
    val sales by salesViewModel.sales.observeAsState(emptyList())
    val ingredients by viewModel.ingredients.observeAsState(emptyList())
    LaunchedEffect(Unit) {
        salesViewModel.startSalesListener()
    }
    val wastageRecordsObs by viewModel.wastageRecords.observeAsState(emptyList())
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabTitles = listOf("Sales by Cake Type", "Wastage Analytics", "Ingredient Consumption")
    var showReportDialog by rememberSaveable { mutableStateOf(showReportDialogDefault) }
    var reportText by rememberSaveable { mutableStateOf(reportTextDefault) }
    val decodedReportText = URLDecoder.decode(reportText, "UTF-8")

    val (pieData, dataList, report) = when (selectedTab) {
        0 -> salesByCakeTypeData(cakes, sales)
        1 -> wastagePerformanceData(ingredients, wastageRecordsObs)
        2 -> ingredientConsumptionData(cakes, sales, ingredients)
        else -> Triple(emptyList(), emptyList(), "")
    }

    val zeroData = pieData.isEmpty() || pieData.all { it.value == 0f }
    val dynamicColors = if (zeroData) generateBlackVariants(1) else generateDynamicColors(pieData.size)
    val displayPieData = if (zeroData) {
        listOf(ChartDatas("Empty Data", 1f, dynamicColors[0], 1f))
    } else {
        pieData.mapIndexed { idx, d -> d.copy(color = dynamicColors[idx]) }
    }
    val displayDataList = if (dataList.isEmpty()) listOf("No data available") else dataList
    val scrollState = rememberScrollState()

    val dashboardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                    text = "SALES DASHBOARD SECTION",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Tab row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(60.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = KhakiDark,
                    modifier = Modifier.border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(4.dp),
                            color = KhakiDark
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(title, color = if (selectedTab == index) KhakiDark else KhakiLight)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .align(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        PieChartAnimated(
                            chartData = displayPieData,
                            diameterFractionOfWidth = 0.85f,
                            key = selectedTab
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(scrollState)
                            .padding(start = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (zeroData) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .background(dynamicColors[0], shape = CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("No data available", color = dynamicColors[0])
                            }
                        } else {
                            displayPieData.forEach { slice ->
                                val percentage = (slice.ratio * 100).takeIf { it.isFinite() }?.let { String.format("%.1f", it) } ?: "-"
                                val grams = slice.value
                                val kilograms = grams / 1000f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 12.dp)
                                        .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                        .background(slice.color, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${slice.label}: ${"%.2f".format(grams)} g (${ "%.3f".format(kilograms)} kg) ($percentage%)",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Portrait: Pie chart above, list below, vertical scrollable as before
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChartAnimated(
                        chartData = displayPieData,
                        diameterFractionOfWidth = 0.70f,
                        key = selectedTab
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (zeroData) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .background(dynamicColors[0], shape = CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("No data available", color = dynamicColors[0])
                        }
                    } else {
                        displayPieData.forEach { slice ->
                            val percentage = (slice.ratio * 100).takeIf { it.isFinite() }?.let { String.format("%.1f", it) } ?: "-"
                            val grams = slice.value
                            val kilograms = grams / 1000f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 12.dp)
                                    .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                    .background(slice.color, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${slice.label}: ${"%.2f".format(grams)} g (${ "%.3f".format(kilograms)} kg) ($percentage%)",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                    .height(56.dp)  // slightly taller
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // 1) Drawable background
                Image(
                    painter = painterResource(R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                // 2) Transparent Button on top
                Button(
                    onClick = {
                        val reportString = when (selectedTab) {
                            0 -> salesByCakeTypeData(cakes, sales).third
                            1 -> wastagePerformanceData(ingredients, wastageRecords).third
                            2 -> ingredientConsumptionData(cakes, sales, ingredients).third
                            else -> "No report available"
                        }
                        viewModel.reportText = reportString
                        navController.navigate("water_drop_animation") {
                            // Optional: use popUpTo or launchSingleTop to prevent back stack issues
                            launchSingleTop = true
                            // popUpTo maybe your sales_dashboard route if necessary
                        }

                    },
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 16.dp)
                    ,  // optional side padding
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF665946)  // darker khaki
                    ),
                    contentPadding = PaddingValues()
                ) {
                    Text(
                        "GENERATE REPORTS",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }


        @Composable
        fun ReportRichText(report: String) {
            val mainPointPatterns = listOf(
                "Best Seller:", // example, you can extend the list below
                "Lowest Seller:",
                "Top wastage:",
                "Total wastage:",
                "Top used:",
                "Total used:",
                "Essentials:",
                "Key Metrics:",
                "Business Insights & Actions:",
                "Insights & Recommendations:",
                "Business Recommendations:",
                "Top Performers by Sales:",
                "Wastage By Ingredient:",
                "Usage by Ingredient:"
            )

            val lines = report.trim().split("\n")
            lines.forEach { line ->
                val trimmed = line.trim()
                // Check for main point pattern e.g. Best Seller: Classic Carrot Cake
                val mainPattern = mainPointPatterns.firstOrNull { trimmed.startsWith(it) }
                if (mainPattern != null) {
                    val value = trimmed.removePrefix(mainPattern).trim()
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)) {
                        Text(
                            text = mainPattern,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.1,
                            // Make it pop
                            modifier = Modifier
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = value,
                            color = Color.White,
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.04,
                            modifier = Modifier
                        )
                    }
                } else if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                    // Section headline, e.g. === Wastage Performance Report ===
                    Text(
                        text = trimmed.replace("=", "").trim(),
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize * 1.08,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else if (trimmed.startsWith("•")) {
                    Text(
                        text = trimmed,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier.padding(start = 12.dp, bottom = 1.dp)
                    )
                } else if (trimmed.endsWith(":")) {
                    // Section intro, make bold
                    Text(
                        text = trimmed,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * 1.08,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                } else {
                    Text(
                        text = trimmed,
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        if (showReportDialog && viewModel.reportText != null) {
            Dialog(onDismissRequest = {
                showReportDialog = false
                viewModel.reportText = null
            }){
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .wrapContentHeight()
                            .widthIn(min = 320.dp, max = 500.dp)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.celestial_dark_grey_paper),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .wrapContentHeight()
                                .widthIn(min = 320.dp, max = 500.dp)
                                .verticalScroll(rememberScrollState())
                            ,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Dialog title: much bigger, bold, white
                            Text(
                                "Report Details",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = MaterialTheme.typography.headlineLarge.fontSize * 1.12 // even bigger
                                ),
                                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                            )

                            // Bigger font in report
                            ReportRichTextLarge(viewModel.reportText!!)

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showReportDialog = false },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF786A58) // For ripple
                                ),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .height(56.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color.Transparent)
                                        .fillMaxSize()
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.celestial_paper_text),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                    Text(
                                        text = "CLOSE",
                                        color = Color(0xFF786A58),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.12f,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                PieChartAnimated(chartData = displayPieData, diameterFractionOfWidth = 0.6f, key = selectedTab)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.Start
            ) {
                if (zeroData) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                        Box(Modifier
                            .size(20.dp)
                            .background(dynamicColors[0], shape = CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text("No data available", color = dynamicColors[0])
                    }
                } else {
                    displayPieData.forEach { slice ->
                        val percentage = if (slice.ratio.isFinite()) String.format("%.1f", slice.ratio * 100) else "0"
                        val grams = slice.value
                        val kilograms = grams / 1000f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                .background(slice.color, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "${slice.label} - ${"%.2f".format(grams)} g / ${"%.3f".format(kilograms)} kg ($percentage%)",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
            ) {
                // Scrollable Vertical Tab List takes as much height as needed but can scroll if too tall
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    VerticalTabRow(
                        tabTitles = tabTitles,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fixed height button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.celestial_paper_text),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Button(
                        onClick = {
                            val reportString = when (selectedTab) {
                                0 -> salesByCakeTypeData(cakes, sales).third
                                1 -> wastagePerformanceData(ingredients, wastageRecords).third
                                2 -> ingredientConsumptionData(cakes, sales, ingredients).third
                                else -> "No report available"
                            }
                            viewModel.reportText = reportString
                            navController.navigate("water_drop_animation") {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF665946)
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Text(
                            "GENERATE REPORTS",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.CenterVertically),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

        }
        @Composable
        fun ReportRichText(report: String) {
            val mainPointPatterns = listOf(
                "Best Seller:", // example, you can extend the list below
                "Lowest Seller:",
                "Top wastage:",
                "Total wastage:",
                "Top used:",
                "Total used:",
                "Essentials:",
                "Key Metrics:",
                "Business Insights & Actions:",
                "Insights & Recommendations:",
                "Business Recommendations:",
                "Top Performers by Sales:",
                "Wastage By Ingredient:",
                "Usage by Ingredient:"
            )

            val lines = report.trim().split("\n")
            lines.forEach { line ->
                val trimmed = line.trim()
                // Check for main point pattern e.g. Best Seller: Classic Carrot Cake
                val mainPattern = mainPointPatterns.firstOrNull { trimmed.startsWith(it) }
                if (mainPattern != null) {
                    val value = trimmed.removePrefix(mainPattern).trim()
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)) {
                        Text(
                            text = mainPattern,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.1,
                            // Make it pop
                            modifier = Modifier
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = value,
                            color = Color.White,
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.04,
                            modifier = Modifier
                        )
                    }
                } else if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                    // Section headline, e.g. === Wastage Performance Report ===
                    Text(
                        text = trimmed.replace("=", "").trim(),
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize * 1.08,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else if (trimmed.startsWith("•")) {
                    Text(
                        text = trimmed,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier.padding(start = 12.dp, bottom = 1.dp)
                    )
                } else if (trimmed.endsWith(":")) {
                    // Section intro, make bold
                    Text(
                        text = trimmed,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * 1.08,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                } else {
                    Text(
                        text = trimmed,
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        if (showReportDialog && viewModel.reportText != null) {
            Dialog(onDismissRequest = {
                showReportDialog = false
                viewModel.reportText = null
            }){
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .wrapContentHeight()
                            .widthIn(min = 320.dp, max = 500.dp)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.celestial_dark_grey_paper),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .wrapContentHeight()
                                .widthIn(min = 320.dp, max = 500.dp)
                                .verticalScroll(rememberScrollState())
                            ,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Dialog title: much bigger, bold, white
                            Text(
                                "Report Details",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = MaterialTheme.typography.headlineLarge.fontSize * 1.12 // even bigger
                                ),
                                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                            )

                            // Bigger font in report
                            ReportRichTextLarge(viewModel.reportText!!)

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showReportDialog = false },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF786A58) // For ripple
                                ),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .height(56.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color.Transparent)
                                        .fillMaxSize()
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.celestial_paper_text),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                    Text(
                                        text = "CLOSE",
                                        color = Color(0xFF786A58),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.12f,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
    else {
        dashboardContent()
    }
}

@Composable
fun ReportRichTextLarge(report: String) {
    val mainPointPatterns = listOf(
        "Best Seller:",
        "Lowest Seller:",
        "Top wastage:",
        "Total wastage:",
        "Top used:",
        "Total used:",
        "Essentials:",
        "Key Metrics:",
        "Business Insights & Actions:",
        "Insights & Recommendations:",
        "Business Recommendations:",
        "Top Performers by Sales:",
        "Wastage By Ingredient:",
        "Usage by Ingredient:"
    )

    val lines = report.trim().split("\n")
    lines.forEach { line ->
        val trimmed = line.trim()
        val mainPattern = mainPointPatterns.firstOrNull { trimmed.startsWith(it) }
        if (mainPattern != null) {
            val value = trimmed.removePrefix(mainPattern).trim()
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)) {
                Text(
                    text = mainPattern,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.22f,
                    modifier = Modifier
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.16f,
                    modifier = Modifier
                )
            }
        } else if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
            Text(
                text = trimmed.replace("=", "").trim(),
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize * 1.24f,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else if (trimmed.startsWith("•")) {
            Text(
                text = trimmed,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.10f,
                modifier = Modifier.padding(start = 15.dp, bottom = 2.dp)
            )
        } else if (trimmed.endsWith(":")) {
            Text(
                text = trimmed,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.18f,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp, bottom = 3.dp)
            )
        } else {
            Text(
                text = trimmed,
                color = Color.White,
                fontWeight = FontWeight.Normal,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.13f,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}



// -------- DATA AGGREGATION HELPERS -----------------------

fun salesByCakeTypeData(
    cakes: List<Cake>,
    sales: List<Sale>
): Triple<List<ChartDatas>, List<String>, String> {
    val cakeTypeTotals = mutableMapOf<String, Float>()
    val cakeTypeRevenue = mutableMapOf<String, Double>()
    var totalSales = 0f
    var totalRevenue = 0.0
    sales.forEach { sale ->
        sale.items.forEach { soldCake ->
            val qty = (soldCake.wholeCakeQty * 8 + soldCake.sliceQty).toFloat()
            cakeTypeTotals[soldCake.cakeName] = (cakeTypeTotals[soldCake.cakeName] ?: 0f) + qty
            cakeTypeRevenue[soldCake.cakeName] = (cakeTypeRevenue[soldCake.cakeName] ?: 0.0) + soldCake.totalSale
            totalSales += qty
            totalRevenue += soldCake.totalSale
        }
    }
    val colors = listOf(Color(0xFFFB7185), Color(0xFF3B82F6), Color(0xFFFACC15), Color(0xFF34D399))
    val chartData = cakeTypeTotals.entries.mapIndexed { idx, entry ->
        ChartDatas(
            label = entry.key,
            value = entry.value,
            color = colors[idx % colors.size],
            ratio = if (totalSales > 0) entry.value / totalSales else 0f
        )
    }
    val dataList = chartData.map {
        "${it.label}: ${"%.0f".format(it.value)} sold, RM ${"%.2f".format(cakeTypeRevenue[it.label] ?: 0.0)} revenue"
    }
    val topCake = cakeTypeTotals.maxByOrNull { it.value }?.key ?: "-"
    val lowCake = cakeTypeTotals.minByOrNull { it.value }?.key ?: "-"
    val report = buildString {
        appendLine("=== Sales by Cake Type Report ===")
        appendLine()
        appendLine("Top Performers by Sales:")
        chartData.sortedByDescending { it.value }.forEach {
            appendLine("• ${it.label}: ${"%.0f".format(it.value)} sold | RM ${"%.2f".format(cakeTypeRevenue[it.label] ?: 0.0)}")
        }
        appendLine()
        appendLine("Best Seller: $topCake")
        appendLine("Lowest Seller: $lowCake")
        appendLine("Total Sold: ${"%.0f".format(totalSales)} | Total Revenue: RM ${"%.2f".format(totalRevenue)}")
        appendLine()
        appendLine("Insights & Recommendations:")
        appendLine("- Reinforce promotions for $topCake to maximize growth.")
        appendLine("- Analyze $lowCake for recipe, pricing, or marketing improvements.")
        appendLine("- Periodically review slow-sellers for possible redesign.")
    }
    return Triple(chartData, dataList, report)
}

fun wastagePerformanceData(
    ingredients: List<Ingredient>,
    wastages: List<Pair<String, Wastage>>
): Triple<List<ChartDatas>, List<String>, String> {
    if (ingredients.isEmpty() || wastages.isEmpty())
        return Triple(emptyList(), listOf("No data available"), "=== Wastage Performance Report ===\n\nNo data available")

    val wastagePerIngr = mutableMapOf<String, Float>()
    wastages.forEach { (ingredientId, wastage) ->
        wastagePerIngr[ingredientId] =
            (wastagePerIngr[ingredientId] ?: 0f) + (wastage.expiredQuantityGram?.toFloat() ?: 0f)
    }
    val totalWastage = wastagePerIngr.values.sum()
    if (totalWastage == 0f)
        return Triple(emptyList(), listOf("No data available"), "=== Wastage Performance Report ===\n\nNo wastage recorded")

    val colors = generateDynamicColors(wastagePerIngr.size)
    val chartData = wastagePerIngr.entries.mapIndexed { idx, entry ->
        ChartDatas(
            label = ingredients.find { it.id == entry.key }?.name ?: entry.key,
            value = entry.value,
            color = colors[idx % colors.size],
            ratio = entry.value / totalWastage
        )
    }
    val dataList = chartData.map {
        "${it.label}: ${"%.2f".format(it.value)} g (${(it.ratio * 100).toInt()}%)"
    }
    val topIng = chartData.maxByOrNull { it.value }?.label ?: "-"
    val report = buildString {
        appendLine("=== Wastage Performance Report ===")
        appendLine()
        appendLine("Wastage By Ingredient:")
        chartData.sortedByDescending { it.value }.forEach {
            appendLine("• ${it.label}: ${"%.0f".format(it.value)} g (${(it.ratio * 100).toInt()}%)")
        }
        appendLine()
        appendLine("Key Metrics:")
        appendLine("- Top wastage: $topIng")
        appendLine("- Total wastage: ${"%.0f".format(totalWastage)} g (${String.format("%.3f", totalWastage / 1000)} kg)")
        appendLine()
        appendLine("Business Insights & Actions:")
        appendLine("- Review ordering and storage for $topIng.")
        appendLine("- Tighten FIFO practices for high-waste ingredients.")
        appendLine("- Staff training and regular stock audit recommended.")
        appendLine("- Consider discounts or donation options for soon-to-expire items.")
    }
    return Triple(chartData, dataList, report)
}

fun ingredientConsumptionData(
    cakes: List<Cake>,
    sales: List<Sale>,
    ingredients: List<Ingredient>
): Triple<List<ChartDatas>, List<String>, String> {
    if (cakes.isEmpty() || sales.isEmpty() || ingredients.isEmpty())
        return Triple(emptyList(), listOf("No data available"), "=== Ingredient Consumption Report ===\n\nNo data available")

    val ingredientTotals = mutableMapOf<String, Float>()
    val cakeCounts = mutableMapOf<String, Float>()
    sales.forEach { sale ->
        sale.items.forEach { soldCake ->
            val cakeType = soldCake.cakeName
            val totalCakes = soldCake.wholeCakeQty + soldCake.sliceQty / 8f
            cakeCounts[cakeType] = (cakeCounts[cakeType] ?: 0f) + totalCakes
        }
    }
    cakes.forEach { cake ->
        val totalProduced = cakeCounts[cake.type] ?: 0f
        (cake.ingredients as? Map<String, Float>)?.forEach { (ingredientId, amtPerCake) ->
            ingredientTotals[ingredientId] = (ingredientTotals[ingredientId] ?: 0f) + (amtPerCake * totalProduced)
        }
    }
    val totalsWithName = ingredientTotals.mapKeys { (id, _) -> ingredients.find { it.id == id }?.name ?: id }
    val totalUsed = ingredientTotals.values.sum()
    if (totalUsed == 0f)
        return Triple(emptyList(), listOf("No data available"), "=== Ingredient Consumption Report ===\n\nNo consumption recorded")

    val colors = listOf(Color(0xFFFB7185), Color(0xFF3B82F6), Color(0xFFFACC15), Color(0xFF34D399), Color(0xFF8B5CF6))
    val chartData = totalsWithName.entries.mapIndexed { idx, entry ->
        ChartDatas(
            label = entry.key,
            value = entry.value,
            color = colors[idx % colors.size],
            ratio = entry.value / totalUsed
        )
    }
    val dataList = chartData.map {
        "${it.label}: ${"%.2f".format(it.value)} ${getUnit(ingredients, it.label)}"
    }
    val topIng = totalsWithName.maxByOrNull { it.value }?.key ?: "-"
    val report = buildString {
        appendLine("=== Ingredient Consumption Report ===")
        appendLine()
        appendLine("Usage by Ingredient:")
        chartData.sortedByDescending { it.value }.forEach {
            appendLine("• ${it.label}: ${"%.0f".format(it.value)} g (${(it.ratio * 100).toInt()}%)")
        }
        appendLine()
        appendLine("Essentials:")
        appendLine("- Top used: $topIng")
        appendLine("- Total used: ${"%.0f".format(totalUsed)} g (${String.format("%.3f", totalUsed / 1000)} kg)")
        appendLine()
        appendLine("Business Recommendations:")
        appendLine("- Ensure top two ingredients are always well-stocked to avoid lost sales.")
        appendLine("- Negotiate bulk deals for highest-used items.")
        appendLine("- Monitor for sudden spikes (could reflect errors or wastage).")
    }
    return Triple(chartData, dataList, report)
}

fun getUnit(ingredients: List<Ingredient>, name: String): String {
    return ingredients.find { it.name == name }?.unit ?: "g"
}

// ---------- PIE CHART ANIMATION -------------------------

@Composable
fun PieChartAnimated(
    chartData: List<ChartDatas>,
    diameterFractionOfWidth: Float,
    key: Any,
    gapDegrees: Float = 0f
) {
    val scaleAnim = remember(key) { Animatable(0.5f) }
    val sweepAnim = remember(key) { Animatable(0f) }
    val animationDuration = 900

    LaunchedEffect(key) {
        scaleAnim.animateTo(1f, animationSpec = tween(400))
        sweepAnim.animateTo(360f, animationSpec = tween(animationDuration))
    }

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val size = maxWidth * diameterFractionOfWidth
        val radius = with(LocalDensity.current) { (size / 2).toPx() }
        Canvas(modifier = Modifier.size(size * scaleAnim.value)) {
            val slices = chartData.size
            val actualGap = if (slices <= 1) 0f else gapDegrees
            val sweepAvailable = 360f - (actualGap * slices)
            var startAngle = -90f
            var currentSweep = sweepAnim.value
            for (segment in chartData) {
                val segSweep = segment.ratio * sweepAvailable
                val segTotal = segSweep + if (slices <= 1) 0f else actualGap
                val drawSweep = when {
                    currentSweep <= 0f -> 0f
                    currentSweep < segTotal -> (currentSweep - actualGap / 2).coerceAtLeast(0f).coerceAtMost(segSweep)
                    else -> segSweep
                }
                if (drawSweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle + actualGap / 2,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = radius / 2.3f
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

@Composable
fun VerticalTabRow(
    tabTitles: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = KhakiDark,
    unselectedColor: Color = KhakiLight
) {
    Box(modifier = modifier) {
        // Background image filling the Box
        Image(
            painter = painterResource(id = R.drawable.celestial_paper_text),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        // Foreground Column with tabs with border and rounded corners
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                val backgroundColor = if (isSelected) selectedColor.copy(alpha = 0.15f) else Color.Transparent
                val textColor = if (isSelected) selectedColor else unselectedColor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = title,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .width(4.dp)
                                .background(selectedColor)
                                .align(Alignment.CenterStart)
                        )
                    }
                }
                Divider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp)
            }
        }
    }
}


