package com.pizzamania.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pizzamania.screens.admin.*
import com.pizzamania.screens.auth.AuthScreen
import com.pizzamania.screens.cart.CartScreen
import com.pizzamania.screens.home.HomeScreen
import com.pizzamania.screens.menu.BranchMenuScreen
import com.pizzamania.screens.profile.ProfileScreen
import com.pizzamania.screens.splash.SplashScreen
import com.pizzamania.screens.orders.TrackOrdersScreen

object Routes {
    const val Splash = "splash"
    const val Home = "home"
    const val BranchMenu = "menu/{branchId}"
    const val Cart = "cart/{branchId}"
    const val Confirm = "confirm/{branchId}"
    const val Auth = "auth"
    const val Profile = "profile"
    const val MyOrders = "orders"

    // Admin
    const val AdminBranches = "admin/branches"
    const val AdminBranchNew = "admin/branch/new"
    const val AdminBranchEdit = "admin/branch/{branchId}"
    const val AdminMenus = "admin/branch/{branchId}/menus"
    const val AdminMenuNew = "admin/branch/{branchId}/menu/new"
    const val AdminMenuEdit = "admin/branch/{branchId}/menu/{itemId}"
    const val AdminOrders = "admin/orders"
}

@Composable
fun PizzaManiaNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable(Routes.Splash) { SplashScreen(navController) }
        composable(Routes.Home) { HomeScreen(navController) }
        composable(Routes.Auth) { AuthScreen(navController) }
        composable(Routes.Profile) { ProfileScreen(navController) }
        composable(Routes.MyOrders) {
            // ensure lambda is () -> Unit (discard Boolean result)
            TrackOrdersScreen(navBack = { navController.popBackStack(); Unit })
        }

        composable(
            Routes.BranchMenu,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            BranchMenuScreen(navController, it.arguments!!.getString("branchId")!!)
        }
        composable(
            Routes.Cart,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            CartScreen(navController, it.arguments!!.getString("branchId")!!)
        }
        composable(
            Routes.Confirm,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            com.pizzamania.screens.checkout.ConfirmDeliveryScreen(
                navController, it.arguments!!.getString("branchId")!!
            )
        }

        // Admin
        composable(Routes.AdminBranches) { AdminBranchesScreen(navController) }
        composable(Routes.AdminBranchNew) { AdminBranchEditScreen(navController, null) }
        composable(
            Routes.AdminBranchEdit,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            AdminBranchEditScreen(navController, it.arguments!!.getString("branchId")!!)
        }
        composable(
            Routes.AdminMenus,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            AdminMenusScreen(navController, it.arguments!!.getString("branchId")!!)
        }
        composable(
            Routes.AdminMenuNew,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            AdminMenuEditScreen(navController, it.arguments!!.getString("branchId")!!, null)
        }
        composable(
            Routes.AdminMenuEdit,
            arguments = listOf(
                navArgument("branchId") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.StringType }
            )
        ) {
            val b = it.arguments!!.getString("branchId")!!
            val m = it.arguments!!.getString("itemId")!!
            AdminMenuEditScreen(navController, b, m)
        }
        composable(Routes.AdminOrders) { AdminOrdersScreen(navController) }
    }
}
