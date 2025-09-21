package com.example.celestial.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.example.celestial.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun SettingScreen(
    navController: NavHostController,
    viewModel: InventoryViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val darkKhakiWhiteBg = painterResource(id = R.drawable.celestial_dark_khaki_paper)
    val paperBg = painterResource(id = R.drawable.celestial_paper_text)
    val black = Color.Black
    val khaki = Color(0xFF8B7355)
    val darkerKhaki = Color(0xFF6B5A42)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, black, RoundedCornerShape(24.dp))
                .background(Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Image(
                    painter = paperBg,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "SETTINGS",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = darkerKhaki,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // -- Add more settings here if desired! --

            // Logout Button
//            Button(
//                onClick = {
////                    coroutineScope.launch {
////                        viewModel.clearUsername(context)
////                        FirebaseAuth.getInstance().signOut()
////                        viewModel.clearData()
////                        navController.navigate("login") {
////                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
////                            launchSingleTop = true
////                        }
//                    }
//                },
//                modifier = Modifier
//                    .fillMaxWidth(0.7f)
//                    .height(56.dp)
//                    .clip(RoundedCornerShape(14.dp))
//                    .border(2.dp, black, RoundedCornerShape(14.dp)),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color.Transparent
//                ),
//                shape = RoundedCornerShape(14.dp)
//            ) {
//                Box(
//                    Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Image(
//                        painter = darkKhakiWhiteBg,
//                        contentDescription = null,
//                        modifier = Modifier
//                            .matchParentSize()
//                            .clip(RoundedCornerShape(14.dp)),
//                        contentScale = ContentScale.Crop
//                    )
//                    Text(
//                        "LOG OUT",
//                        color = Color.White,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 20.sp
//                    )
//                }
//            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
