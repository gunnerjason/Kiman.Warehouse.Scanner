package kiman.warehouse.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kiman.warehouse.scanner.ui.scan.ScannerScreen
import kiman.warehouse.scanner.ui.start.StartJobScreen
import kiman.warehouse.scanner.ui.summary.SummaryScreen
import kiman.warehouse.scanner.viewmodel.ScannerViewModel


sealed class Screen(val route: String) {
    data object Start : Screen("start")
    data object Scan : Screen("scan")
    data object Summary : Screen("summary")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: ScannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val nav = androidx.navigation.compose.rememberNavController()

            androidx.navigation.compose.NavHost(
                navController = nav,
                startDestination = Screen.Start.route
            ) {
                androidx.navigation.compose.composable(Screen.Start.route) {
                    StartJobScreen(
                        vm = vm,
                        onStarted = { nav.navigate(Screen.Scan.route) }
                    )
                }
                androidx.navigation.compose.composable(Screen.Scan.route) {
                    ScannerScreen(
                        vm = vm,
                        onSummary = { nav.navigate(Screen.Summary.route) },
                        onFinished = {
                            nav.popBackStack(Screen.Start.route, inclusive = false)
                            nav.navigate(Screen.Start.route) // reset UI
                        }
                    )
                }
                androidx.navigation.compose.composable(Screen.Summary.route) {
                    SummaryScreen(
                        vm = vm,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}

