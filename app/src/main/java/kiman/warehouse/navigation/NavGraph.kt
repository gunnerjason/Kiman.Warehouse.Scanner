package kiman.warehouse.scanner.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kiman.warehouse.scanner.ui.scan.ScannerScreen
import kiman.warehouse.scanner.ui.start.StartJobScreen
import kiman.warehouse.scanner.ui.summary.SummaryScreen
import kiman.warehouse.scanner.viewmodel.ScannerViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    vm: ScannerViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Start.route
    ) {

        composable(Screen.Start.route) {
            StartJobScreen(
                vm = vm,
                onStarted = { navController.navigate(Screen.Scan.route) }
            )
        }

        composable(Screen.Scan.route) {
            ScannerScreen(
                vm = vm,
                onSummary = { navController.navigate(Screen.Summary.route) },
                onFinished = {
                    navController.popBackStack(Screen.Start.route, false)
                }
            )
        }

        composable(Screen.Summary.route) {
            SummaryScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}