package kiman.warehouse.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import kiman.warehouse.scanner.navigation.NavGraph
import kiman.warehouse.scanner.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val vm: ScannerViewModel = viewModel()

            NavGraph(
                navController = navController,
                vm = vm
            )
        }
    }
}