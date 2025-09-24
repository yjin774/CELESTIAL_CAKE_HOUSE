package com.example.celestial.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.celestial.ui.components.StarFieldBackground
import com.example.celestial.ui.screens.*
import com.example.celestial.ui.viewmodels.InventoryViewModel
import com.example.celestial.ui.viewmodels.SalesViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun CelestialRoot(
    navController: NavHostController,
    startDestination: String,
    inventoryViewModel: InventoryViewModel,
    salesViewModel: SalesViewModel
) {
    val navItems = listOf(
        AnimatedNavScreen.Home,
        AnimatedNavScreen.Expiry,
        AnimatedNavScreen.Dashboard,
        AnimatedNavScreen.Report,
        AnimatedNavScreen.ManageSales,
        AnimatedNavScreen.Inventory
    )

    var selectedScreen by remember { mutableStateOf(0) }

    val cakes by inventoryViewModel.cakes.observeAsState(emptyList())
    val ingredients by inventoryViewModel.ingredients.observeAsState(emptyList())
    val sales by salesViewModel.sales.observeAsState(emptyList())
    var isMoon by remember { mutableStateOf(false) } // global state
    val wastageRecords by inventoryViewModel.wastageRecords.observeAsState(emptyList())
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar on all screens except "login"
    val showBottomBar = currentRoute != "login" && currentRoute != "water_drop_animation" && currentRoute != "user_profile"
            && currentRoute != "register" && currentRoute != "add_ingredient_stock/{ingredientId}" && currentRoute != "view_cake_ingredients/{cakeId}" && currentRoute != "cart_management"

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = isMoon, animationSpec = tween(900)) { moon ->
            if (!moon) {
                DayBackground()
            } else {
                StarFieldBackground()
            }

            Column(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) {
                    composable(
                        "inventory?autoFocusIngredientId={ingredientId}&initialTab={initialTab}&removeMode={removeMode}&editMode={editMode}",
                        arguments = listOf(
                            navArgument("ingredientId") { type = NavType.StringType; nullable = true },
                            navArgument("initialTab") { type = NavType.IntType; defaultValue = 0 },
                            navArgument("removeMode") { type = NavType.BoolType; defaultValue = false } ,
                            navArgument("editMode") { type = NavType.BoolType; defaultValue = false }
                        )
                    ) { backStackEntry ->

                        val ingredientId = backStackEntry.arguments?.getString("ingredientId")
                        val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
                        val removeMode = backStackEntry.arguments?.getBoolean("removeMode") ?: false
                        val editMode = backStackEntry.arguments?.getBoolean("editMode") ?: false

                        InventoryScreen(
                            navController = navController,
                            viewModel = inventoryViewModel,
                            autoFocusIngredientId = ingredientId,
                            initialTab = initialTab,
                            isRemoveModePassed = removeMode,
                            isEditModePassed = editMode
                        )
                    }


                    composable("expiry") { ExpiryScreen(navController, inventoryViewModel) }

                    composable("sales_dashboard") {
                        SalesDashboardScreen(
                            navController,
                            salesViewModel,
                            inventoryViewModel,
                            wastageRecords
                        )
                    }

                    composable("sales_report") {
                        SalesReportScreen(navController, inventoryViewModel)
                    }

                    composable("sales_management") {
                        SalesManagementScreen(navController, inventoryViewModel)
                    }

                    composable("login") { LoginScreen(navController, inventoryViewModel) }

                    composable("home") {
                        HomeScreen(
                            navController = navController,
                            viewModel = inventoryViewModel,
                            isMoon = isMoon, // Pass global state
                            onThemeToggle = { isMoon = !isMoon } // Pass global setter
                        )
                    }

                    composable("add_item_screen/{itemType}") { backStackEntry ->
                        val itemType = backStackEntry.arguments?.getString("itemType") ?: "CAKE"
                        AddItemScreen(
                            itemType = itemType,
                            onBack = { navController.popBackStack() },
                            viewModel = inventoryViewModel,
                            navController = navController
                        )
                    }


                    composable("user_profile") {
                        UserProfileScreen(navController,inventoryViewModel)
                    }

                    composable(
                        "sales_dashboard?showReport={showReport}&reportText={reportText}",
                        arguments = listOf(
                            navArgument("showReport") {
                                type = NavType.BoolType; defaultValue = false
                            },
                            navArgument("reportText") {
                                type = NavType.StringType; defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val showReportDialog =
                            backStackEntry.arguments?.getBoolean("showReport") ?: false
                        val reportText = backStackEntry.arguments?.getString("reportText") ?: ""
                        SalesDashboardScreen(
                            navController,
                            salesViewModel,
                            inventoryViewModel,
                            wastageRecords,
                            showReportDialog,
                            reportText
                        )
                    }

                    composable(
                        "water_drop_animation?reportText={reportText}",
                        arguments = listOf(navArgument("reportText") {
                            type = NavType.StringType; defaultValue = ""
                        })
                    ) { backStackEntry ->
                        val reportText = backStackEntry.arguments?.getString("reportText") ?: ""
                        LoadingAnimationScreen(navController, reportText)
                    }

                    composable("cart_management") {
                        CartManagementScreen(navController, inventoryViewModel)
                    }

                    composable(
                        "view_cake_ingredients/{cakeId}",
                        arguments = listOf(navArgument("cakeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val cakeId = backStackEntry.arguments?.getString("cakeId") ?: ""
                        ViewCakeIngredientsScreen(navController, inventoryViewModel, cakeId)
                    }

                    composable(
                        "add_ingredient_stock/{ingredientId}",
                        arguments = listOf(navArgument("ingredientId") {
                            type = NavType.StringType
                        })
                    ) { backStackEntry ->
                        val ingredientId = backStackEntry.arguments?.getString("ingredientId") ?: ""
                        AddIngredientStockScreen(navController, ingredientId, inventoryViewModel)
                    }

                    composable("register") { RegisterScreen(navController, inventoryViewModel) }
                }

                if (showBottomBar) {
                    AnimatedBottomNavigationBar(
                        screens = navItems,
                        selectedIndex = selectedScreen,
                        modifier = Modifier.fillMaxWidth(),
                        onScreenSelected = { idx ->
                            selectedScreen = idx
                            when (navItems[idx]) {
                                AnimatedNavScreen.Home -> navController.navigate("home")
                                AnimatedNavScreen.Expiry -> navController.navigate("expiry")
                                AnimatedNavScreen.Dashboard -> navController.navigate("sales_dashboard")
                                AnimatedNavScreen.Report -> navController.navigate("sales_report")
                                AnimatedNavScreen.ManageSales -> navController.navigate("sales_management")
                                AnimatedNavScreen.Inventory -> navController.navigate("inventory")
                            }
                        }
                    )
                }
            }
        }
    }
}