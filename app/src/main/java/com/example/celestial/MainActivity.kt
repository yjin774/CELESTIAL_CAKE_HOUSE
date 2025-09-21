package com.example.celestial

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.app.Application
import com.example.celestial.ui.CelestialRoot
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.celestial.data.repositories.InventoryRepository
import com.example.celestial.data.repositories.ExpiryRepository
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.example.celestial.ui.viewmodels.SalesViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            CelestialApp()
        }
    }
}

@Composable
fun CelestialApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // ---- ViewModel setup
    val inventoryRepository = remember { InventoryRepository(context) }
    val expiryRepository = remember { ExpiryRepository(context) }
    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModel.Factory(inventoryRepository, expiryRepository)
    )
    val salesViewModel: SalesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return SalesViewModel(context.applicationContext as Application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )

    // --- Auth logic ---
    val auth = FirebaseAuth.getInstance()
    var isAuthChecked by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf("login") }

    LaunchedEffect(Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            val currUser = firebaseAuth.currentUser
            isLoggedIn = currUser != null
            startDestination = if (currUser != null) "home" else "login"
            isAuthChecked = true
        }
    }

    // ---- Firestore/Inventory initialization after login ----
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            inventoryViewModel.initializeDataAfterLogin()
        }
    }

    if (!isAuthChecked) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    CelestialRoot(
        navController = navController,
        startDestination = startDestination,
        inventoryViewModel = inventoryViewModel,
        salesViewModel = salesViewModel
    )
}
