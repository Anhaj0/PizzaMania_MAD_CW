package com.pizzamania.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pizzamania.screens.admin.AdminBranchEditScreen
import com.pizzamania.screens.admin.AdminBranchesScreen
import com.pizzamania.screens.admin.AdminMenuEditScreen
import com.pizzamania.screens.admin.AdminMenusScreen
import com.pizzamania.screens.auth.AuthScreen
import com.pizzamania.screens.builder.PizzaBuilderScreen
import com.pizzamania.screens.cart.CartScreen
import com.pizzamania.screens.checkout.ConfirmDeliveryScreen
import com.pizzamania.screens.home.HomeScreen
import com.pizzamania.screens.menu.BranchMenuScreen
import com.pizzamania.screens.orders.AdminOrdersScreen
import com.pizzamania.screens.orders.AdminOrdersViewModel
import com.pizzamania.screens.orders.OrdersScreen
import com.pizzamania.screens.orders.OrdersViewModel
import com.pizzamania.screens.orders.TrackOrdersScreen
import com.pizzamania.screens.profile.ProfileScreen
import com.pizzamania.screens.splash.SplashScreen

@Composable
fun PizzaManiaNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        // Core
        composable(Routes.Splash) { SplashScreen(navController) }
        composable(Routes.Auth)   { AuthScreen(navController) }
        composable(Routes.Home)   { HomeScreen(navController) }

        // Customer: Menu
        composable(
            route = Routes.BranchMenuPattern,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStack ->
            val branchId = backStack.arguments?.getString("branchId") ?: return@composable
            BranchMenuScreen(navController, branchId)
        }

        // Customer: Cart
        composable(
            route = Routes.CartPattern,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStack ->
            val branchId = backStack.arguments?.getString("branchId") ?: return@composable
            CartScreen(navController, branchId)
        }

        // Customer: Builder
        composable(
            route = Routes.BuilderPattern,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStack ->
            val branchId = backStack.arguments?.getString("branchId") ?: return@composable
            PizzaBuilderScreen(navController, branchId)
        }

        // Checkout
        composable(
            route = Routes.ConfirmDeliveryPattern,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStack ->
            val branchId = backStack.arguments?.getString("branchId") ?: return@composable
            ConfirmDeliveryScreen(navController, branchId)
        }

        // Orders list
        composable(Routes.Orders) {
            val vm: OrdersViewModel = hiltViewModel()
            OrdersScreen(vm = vm)
        }

        // Tracking screen expects 'navBack'
        composable(Routes.TrackOrders) {
            TrackOrdersScreen(navBack = { navController.popBackStack() })
        }

        // Profile
        composable(Routes.Profile) { ProfileScreen(navController) }

        // Admin
        composable(Routes.AdminBranches) { AdminBranchesScreen(navController) }
        composable(Routes.AdminOrders) {
            val vm: AdminOrdersViewModel = hiltViewModel()
            // NOTE: your AdminOrdersScreen only takes vm
            AdminOrdersScreen(vm = vm)
        }

        // Admin: Branch edit/create
        composable(Routes.AdminBranchNew) {
            AdminBranchEditScreen(navController, null)
        }
        composable(
            route = Routes.AdminBranchPattern,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStack ->
            val branchId = backStack.arguments?.getString("branchId")
            AdminBranchEditScreen(navController, branchId)
        }

        // Admin: Menus list for a branch
        composable(
            route = Routes.AdminBranchMenusPattern,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStack ->
            val branchId = backStack.arguments?.getString("branchId") ?: return@composable
            AdminMenusScreen(navController, branchId)
        }

        // Admin: Menu item new
        composable(
            route = Routes.AdminMenuNewPattern,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStack ->
            val branchId = backStack.arguments?.getString("branchId") ?: return@composable
            AdminMenuEditScreen(navController, branchId, null)
        }

        // Admin: Menu item edit
        composable(
            route = Routes.AdminMenuEditPattern,
            arguments = listOf(
                navArgument("branchId") { type = NavType.StringType },
                navArgument("itemId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val branchId = backStack.arguments?.getString("branchId") ?: return@composable
            val itemId   = backStack.arguments?.getString("itemId")
            AdminMenuEditScreen(navController, branchId, itemId)
        }
    }
}
