package com.pizzamania.navigation

object Routes {
    // Core
    const val Splash = "splash"
    const val Auth = "auth"
    const val Home = "home"

    // Customer flows
    const val Orders = "orders"
    const val TrackOrders = "orders/track"
    const val Profile = "profile"

    // Dynamic routes + patterns
    const val BranchMenuPattern = "menu/{branchId}"
    fun BranchMenu(branchId: String) = "menu/$branchId"

    const val ConfirmDeliveryPattern = "confirm/{branchId}"
    fun ConfirmDelivery(branchId: String) = "confirm/$branchId"

    // NEW: Cart
    const val CartPattern = "cart/{branchId}"
    fun Cart(branchId: String) = "cart/$branchId"

    // NEW: Builder (requires branchId)
    const val BuilderPattern = "builder/{branchId}"
    fun Builder(branchId: String) = "builder/$branchId"

    // Admin
    const val AdminBranches = "admin/branches"
    const val AdminOrders = "admin/orders"

    // Admin: Branch edit/create
    const val AdminBranchNew = "admin/branch/new"
    const val AdminBranchPattern = "admin/branch/{branchId}"
    fun AdminBranch(id: String) = "admin/branch/$id"

    // Admin: Menus list for a branch
    const val AdminBranchMenusPattern = "admin/branch/{branchId}/menus"
    fun AdminBranchMenus(id: String) = "admin/branch/$id/menus"

    // Admin: Menu item edit/create
    const val AdminMenuNewPattern = "admin/branch/{branchId}/menu/new"
    fun AdminMenuNew(branchId: String) = "admin/branch/$branchId/menu/new"

    const val AdminMenuEditPattern = "admin/branch/{branchId}/menu/{itemId}"
    fun AdminMenuEdit(branchId: String, itemId: String) = "admin/branch/$branchId/menu/$itemId"
}
