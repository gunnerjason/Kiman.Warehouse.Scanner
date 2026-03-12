package kiman.warehouse.scanner.ui.scan

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import kiman.warehouse.scanner.util.BeepPlayer
import kiman.warehouse.scanner.util.CsvExporter
import kiman.warehouse.scanner.util.VibratorHelper
import kiman.warehouse.scanner.viewmodel.ScanResult
import kiman.warehouse.scanner.viewmodel.ScannerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    vm: ScannerViewModel,
    onSummary: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val job = vm.job.value
    val status = vm.status.value

    var hasCamPermission by remember { mutableStateOf(false) }
    val camPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPermission = granted }

    LaunchedEffect(Unit) { camPermLauncher.launch(Manifest.permission.CAMERA) }

    var scanWindowOpen by remember { mutableStateOf(false) }
    var scanToken by remember { mutableStateOf(0) }

    LaunchedEffect(scanToken) {
        if (!scanWindowOpen) return@LaunchedEffect
        delay(1200)
        scanWindowOpen = false
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val exportJob = vm.finalizeForExport() ?: return@rememberLauncherForActivityResult
        try {
            CsvExporter.export(context, exportJob, uri)
            vm.clearJob()
            onFinished()
        } catch (e: Exception) {
            vm.status.value = "Save failed: ${e.message}"
        }
    }

    val onCodeDetected: (String) -> Unit = { code ->
        if (!scanWindowOpen) return@onCodeDetected
        scanWindowOpen = false

        when (vm.scan(code)) {
            is ScanResult.Success -> BeepPlayer.play()
            is ScanResult.Duplicate -> VibratorHelper.vibrate(context)
            ScanResult.NoJob -> {}
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Barcode Scanner") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {

            // ✅ Top content takes remaining space
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    if (!hasCamPermission) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Camera permission required")
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { camPermLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Grant Camera Permission")
                            }
                        }
                    } else {
                        CameraPreview(
                            lifecycleOwner = lifecycleOwner,
                            scanEnabled = scanWindowOpen && job != null,
                            onCodeDetected = onCodeDetected,
                            modifier = Modifier.fillMaxSize()
                        )

                        // ROI box
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .align(Alignment.Center)
                                .border(3.dp, Color.Green)
                        )

                        // Tap overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    if (job == null) {
                                        vm.status.value = "Start a job first."
                                        return@clickable
                                    }
                                    scanWindowOpen = true
                                    scanToken++
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (scanWindowOpen) "Scanning..." else "Tap to Scan",
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Current Group: ${job?.currentGroup?.index ?: "-"}")
                Text(status)
            }

            // ✅ Bottom buttons pinned to bottom
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { vm.newGroup() },
                    enabled = job != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("New Group") }

                OutlinedButton(
                    onClick = onSummary,
                    enabled = job != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Summary") }

                Button(
                    onClick = {
                        val current = vm.job.value ?: return@Button
                        createCsvLauncher.launch("${current.name}.csv")
                    },
                    enabled = job != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Finish Job") }
            }
        }
    }
}