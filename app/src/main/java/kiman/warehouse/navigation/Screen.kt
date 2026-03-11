package kiman.warehouse.scanner.navigation

sealed class Screen(val route: String) {
    data object Start : Screen("start")
    data object Scan : Screen("scan")
    data object Summary : Screen("summary")
}