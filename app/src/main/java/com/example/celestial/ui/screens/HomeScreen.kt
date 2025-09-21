package com.example.celestial.ui.screens

import android.R.attr.orientation
import android.content.res.Configuration
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.celestial.data.models.Cake
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.celestial.R
import com.example.celestial.data.models.Ingredient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UsernameBox(
    username: String,
    textColor: Color,
    boxModifier: Modifier = Modifier
) {
    val scrollState = remember { Animatable(0f) }
    val rowWidth = rememberSaveable { mutableStateOf(0f) }
    val textWidth = rememberSaveable { mutableStateOf(0f) }
    val needsScroll = textWidth.value > rowWidth.value


    Box(
        modifier = boxModifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .height(56.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "USERNAME",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            // Marquee/Scrolling username
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(0.dp))
                    .padding(top = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                var ready by rememberSaveable { mutableStateOf(false) }
                val textMeasurer = rememberTextMeasurer()
                val measured = textMeasurer.measure(
                    text = AnnotatedString(username),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 30.sp
                    )
                )
                LaunchedEffect(measured.size.width) {
                    textWidth.value = measured.size.width.toFloat()
                    ready = true
                }
                LaunchedEffect(rowWidth.value, textWidth.value) {
                    if (rowWidth.value > 0 && textWidth.value > rowWidth.value) {
                        while (true) {
                            scrollState.snapTo(0f)
                            scrollState.animateTo(
                                targetValue = textWidth.value - rowWidth.value,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                        }
                    } else {
                        scrollState.snapTo(0f)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            rowWidth.value = it.size.width.toFloat()
                        }
                        .clip(RoundedCornerShape(0.dp))
                        .height(24.dp)
                        .horizontalScroll(rememberScrollState()), // For touch interaction, optional
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ready) {
                        Box(
                            Modifier
                                .offset((-scrollState.value).dp, 0.dp)
                                .widthIn(min = rowWidth.value.dp)
                        ) {
                            Text(
                                text = username,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: InventoryViewModel,
    isMoon: Boolean,
    onThemeToggle: () -> Unit,
    selectedTabIndex: Int = 0
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val profile by viewModel.userProfile
    val username = profile.username
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(firebaseUid) {
        viewModel.startListeningUserProfile(context)
        viewModel.loadUsername(context)
    }

    val currentOrientation = LocalConfiguration.current.orientation
    var showEditModeDialog by rememberSaveable { mutableStateOf(false) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var selectedEditItemType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEditCake by rememberSaveable { mutableStateOf<Cake?>(null) }
    var selectedEditIngredient by rememberSaveable { mutableStateOf<Ingredient?>(null) }
    var showRemoveTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showEditItemDialog by rememberSaveable { mutableStateOf(false) }
    var showAddTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showEditTypeDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadUsername(context) }
    val cakes by viewModel.cakes.observeAsState(emptyList())
    val producedCakes = cakes.filter { it.wholeCakeQuantity > 0 || it.sliceQuantity > 0 }
    var cakeIndex by rememberSaveable { mutableStateOf(0) }
    val infiniteTransition = rememberInfiniteTransition()
    val cakeFadeAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    LaunchedEffect(Unit) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            viewModel.startListeningUserProfile(context)
        }
    }


    LaunchedEffect(selectedTabIndex) {
        if (isEditMode && selectedTabIndex !in listOf(0, 1)) {
            isEditMode = false
        }
    }

    LaunchedEffect(cakeFadeAlpha, producedCakes) {
        if (cakeFadeAlpha == 0f && producedCakes.isNotEmpty()) {
            cakeIndex = (cakeIndex + 1) % producedCakes.size
        }
    }
    val currentCake = if (producedCakes.isNotEmpty()) producedCakes[cakeIndex] else null
    val darkKhakiWhiteBg = painterResource(id = R.drawable.celestial_dark_khaki_paper)
    val paperBg = painterResource(id = R.drawable.celestial_paper_text)
    val black = Color.Black
    val darkKhaki = Color(0xFF8B7355)
    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 1. Title and theme switcher
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),  // Section 1: 400dp
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp))
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = darkKhakiWhiteBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "CELESTIAL CAKE HOUSE",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 54.sp,
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            item {
                // 2. Icon and username row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)   // Section 2: 300dp
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp))
                                .clickable {
                                    navController.navigate("user_profile")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = paperBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Icon",
                                tint = darkKhaki,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Image(
                                painter = darkKhakiWhiteBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            UsernameBox(
                                username = userProfile.username,
                                textColor = Color.White,
                                boxModifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            item {
                // 3. Animated cake display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = currentCake != null) {
                            currentCake?.let {
                                navController.navigate("sales_management")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentCake != null) {
                        val cakeImageName = currentCake.type.lowercase().replace(" ", "_") + ".png"
                        val imagePath =
                            "file:///android_asset/images/celestial_cakes/$cakeImageName"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                                .height(110.dp) // only height here, no fillMaxSize
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp))
                                .background(Color.Transparent)
                                .graphicsLayer { alpha = cakeFadeAlpha },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = paperBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imagePath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = currentCake.type,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.celestial_moon),
                                    error = painterResource(R.drawable.celestial_moon)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = currentCake.type,
                                        color = darkKhaki,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = "WHOLE CAKE : ${currentCake.wholeCakeQuantity}",
                                        color = darkKhaki,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "SLICE CKAE : ${currentCake.sliceQuantity}",
                                        color = darkKhaki,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "No produced cakes available",
                            color = darkKhaki,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            item {
                // 4. Settings/Edit Items
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)   // Section 4: 600dp
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp))
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = darkKhakiWhiteBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                "AVAILABLE CAKES FOR SALE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(6.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, black, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = paperBg,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Button(
                                    onClick = { showEditItemDialog = true },
                                    modifier = Modifier,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Item",
                                        tint = darkKhaki,
                                        modifier = Modifier.size(70.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, black, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            )
                            {
                                Image(
                                    painter = paperBg,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                ThemeSwitcher(
                                    isMoon = isMoon,
                                    color = Color.White,
                                    size = 68.dp,
                                    onClick = onThemeToggle
                                )
                            }
                        }
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

            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
            ) {
                // 1. Title and theme switcher (weight 2f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.3f)
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp))
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = darkKhakiWhiteBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "CELESTIAL CAKE HOUSE",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 54.sp,
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Icon and username row (weight 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp))
                                .clickable {
                                    navController.navigate("user_profile")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = paperBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Icon",
                                tint = darkKhaki,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxSize()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Image(
                                painter = darkKhakiWhiteBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            UsernameBox(
                                username = userProfile.username,
                                textColor = Color.White,
                                boxModifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Animated cake display (weight 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = currentCake != null) {
                            currentCake?.let {
                                navController.navigate("sales_management")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentCake != null) {
                        val cakeImageName = currentCake.type.lowercase().replace(" ", "_") + ".png"
                        val imagePath = "file:///android_asset/images/celestial_cakes/$cakeImageName"
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                                .height(90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp))
                                .background(Color.Transparent)
                                .graphicsLayer { alpha = cakeFadeAlpha },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = paperBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Row(modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp), verticalAlignment = Alignment.CenterVertically)
                            {

                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imagePath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = currentCake.type,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.celestial_moon),
                                    error = painterResource(R.drawable.celestial_moon)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = currentCake.type,
                                        color = darkKhaki,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = "WHOLE CAKE : ${currentCake.wholeCakeQuantity}",
                                        color = darkKhaki,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "SLICE CKAE : ${currentCake.sliceQuantity}",
                                        color = darkKhaki,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "No produced cakes available",
                            color = darkKhaki,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Settings/Edit Items (weight 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(2f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, black, RoundedCornerShape(16.dp))
                                .background(Color.Transparent)
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        )
                        {
                            Image(
                                painter = darkKhakiWhiteBg,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                "AVAILABLE CAKES FOR SALE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, black, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            )
                            {
                                Image(
                                    painter = paperBg,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Button(
                                    onClick = { showEditItemDialog = true },
                                    modifier = Modifier,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Item",
                                        tint = darkKhaki,
                                        modifier = Modifier.size(70.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, black, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            )
                            {
                                Image(
                                    painter = paperBg,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                ThemeSwitcher(
                                    isMoon = isMoon,
                                    color = Color.White,
                                    size = 68.dp,
                                    onClick = onThemeToggle
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditItemDialog) {
        Dialog(onDismissRequest = { showEditItemDialog = false }) {
            Box(
                modifier = Modifier
                    .width(320.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Add or Edit Item",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B7355),
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DialogItemTypeButton(
                            text = "Add Item",
                            onClick = { showEditItemDialog = false; showAddTypeDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                        DialogItemTypeButton(
                            text = "Edit Item",
                            onClick = {
                                showEditItemDialog = false
                                selectedEditItemType = null
                                showEditModeDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                        DialogItemTypeButton(
                            text = "Remove Item",
                            onClick = {
                                showEditItemDialog = false
                                showRemoveTypeDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEditModeDialog) {
        Dialog(onDismissRequest = { showEditModeDialog = false }) {
            Box(
                modifier = Modifier.width(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Select Item Type to Edit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF8B7355),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    DialogItemTypeButton(
                        text = "CAKE",
                        onClick = {
                            showEditModeDialog = false
                            navController.navigate("inventory?initialTab=0&editMode=true")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                    DialogItemTypeButton(
                        text = "INGREDIENT",
                        onClick = {
                            showEditModeDialog = false
                            navController.navigate("inventory?initialTab=1&editMode=true")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }
        }
    }


    if (showAddTypeDialog) {
        Dialog(onDismissRequest = { showAddTypeDialog = false }) {
            Box(
                modifier = Modifier
                    .width(320.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Select Item Type to Add",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B7355),
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DialogItemTypeButton(
                            text = "CAKE",
                            onClick = {
                                showAddTypeDialog = false
                                navController.navigate("add_item_screen/CAKE")
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )
                        DialogItemTypeButton(
                            text = "INGREDIENT",
                            onClick = {
                                showAddTypeDialog = false
                                navController.navigate("add_item_screen/INGREDIENT")
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )
                    }
                }
            }
        }
    }

    if (showRemoveTypeDialog) {
        Dialog(onDismissRequest = { showRemoveTypeDialog = false }) {
            Box(
                modifier = Modifier.width(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.celestial_paper_text),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Item Type to Remove",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B7355),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    DialogItemTypeButton(
                        text = "CAKE",
                        onClick = {
                            showRemoveTypeDialog = false
                            navController.navigate("inventory?initialTab=0&removeMode=true")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )

                    DialogItemTypeButton(
                        text = "INGREDIENT",
                        onClick = {
                            showRemoveTypeDialog = false
                            navController.navigate("inventory?initialTab=1&removeMode=true")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DialogItemTypeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp)) // Rounded corners
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)), // Black border
        shape = RoundedCornerShape(12.dp), // Button shape matches border and clip
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.celestial_dark_khaki_paper),
                contentDescription = null,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun ModeDialogs(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .size(width = 250.dp, height = 150.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.celestial_paper_text),
                contentDescription = null,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = message,
                color = Color(0xFF8B7355),
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
