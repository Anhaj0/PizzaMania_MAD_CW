package com.pizzamania.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pizzamania.screens.admin.AdminBranchEditScreen
import com.pizzamania.screens.admin.AdminBranchesScreen
import com.pizzamania.screens.admin.AdminMenuEditScreen
import com.pizzamania.screens.admin.AdminMenusScreen
import com.pizzamania.screens.admin.AdminOrdersScreen
import com.pizzamania.screens.auth.SignInScreen
import com.pizzamania.screens.cart.CartScreen
import com.pizzamania.screens.home.HomeScreen
import com.pizzamania.screens.menu.BranchMenuScreen
import com.pizzamania.screens.splash.SplashScreen

object Routes {
    const val Splash = "splash"
    const val Home = "home"
    const val BranchMenu = "menu/{branchId}"
    const val Cart = "cart/{branchId}"
    const val Confirm = "confirm/{branchId}"
    const val SignIn = "signin"

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
        // Core
        composable(Routes.Splash) { SplashScreen(navController) }
        composable(Routes.Home) { HomeScreen(navController) }
        composable(Routes.SignIn) { SignInScreen(navController) }

        // Customer flow
        composable(
            route = Routes.BranchMenu,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            val branchId = it.arguments!!.getString("branchId")!!
            BranchMenuScreen(navController, branchId)
        }
        composable(
            route = Routes.Cart,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            val branchId = it.arguments!!.getString("branchId")!!
            CartScreen(navController, branchId)
        }
        composable(
            route = Routes.Confirm,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            val branchId = it.arguments!!.getString("branchId")!!
            com.pizzamania.screens.checkout.ConfirmDeliveryScreen(navController, branchId)
        }

        // Admin: branches
        composable(Routes.AdminBranches) { AdminBranchesScreen(navController) }
        composable(Routes.AdminBranchNew) { AdminBranchEditScreen(navController, branchId = null) }
        composable(
            route = Routes.AdminBranchEdit,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            val id = it.arguments!!.getString("branchId")!!
            AdminBranchEditScreen(navController, id)
        }

        // Admin: menus
        composable(
            route = Routes.AdminMenus,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            val id = it.arguments!!.getString("branchId")!!
            AdminMenusScreen(navController, id)
        }
        composable(
            route = Routes.AdminMenuNew,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            val id = it.arguments!!.getString("branchId")!!
            AdminMenuEditScreen(navController, id, itemId = null)
        }
        composable(
            route = Routes.AdminMenuEdit,
            arguments = listOf(
                navArgument("branchId") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.StringType }
            )
        ) {
            val branchId = it.arguments!!.getString("branchId")!!
            val itemId = it.arguments!!.getString("itemId")!!
            AdminMenuEditScreen(navController, branchId, itemId)
        }

        // Admin: orders
        composable(Routes.AdminOrders) { AdminOrdersScreen() }
    }
}
