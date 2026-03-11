package kiman.warehouse.scanner.ui.scan

import android.Manifest
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import kiman.warehouse.scanner.util.escapeCsv
import kiman.warehouse.scanner.util.playBeepOnce
import kiman.warehouse.scanner.util.vibrateOnce
import kiman.warehouse.scanner.viewmodel.ScanResult
import kiman.warehouse.scanner.viewmodel.ScannerViewModel
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    vm: ScannerViewModel,
    onSummary: () -> Unit,
    onFinished: () -> Unit
) {
    val job = vm.job.value
    val status = vm.status.value

    // tap-to-scan window
    var scanWindowOpen by remember { mutableStateOf(false) }
    var scanToken by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // permissions
    var hasCamPermission by remember { mutableStateOf(false) }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCamPermission = it
    }
    LaunchedEffect(Unit) { camPerm.launch(Manifest.permission.CAMERA) }

    // auto close scan window
    LaunchedEffect(scanToken) {
        if (!scanWindowOpen) return@LaunchedEffect
        kotlinx.coroutines.delay(1200)
        scanWindowOpen = false
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val exportJob = vm.finalizeForExport() ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                BufferedWriter(OutputStreamWriter(out)).use { bw ->
                    bw.append("job_name,job_started_at,group_index,scan_index,timestamp_iso,code\n")
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    for (g in exportJob.groups) {
                        var idx = 1
                        for (item in g.items) {
                            bw.append("${exportJob.name},${exportJob.startedAtMs},${g.index},${idx},${sdf.format(
                                Date(item.timestampMs)
                            )},${escapeCsv(item.code)}\n")
                            idx++
                        }
                    }
                }
            }
            vm.clearJob()
            onFinished()
        } catch (e: Exception) {
            vm.status.value = "Save failed: ${e.message}"
        }
    }

    // handle detections only during scan window
    val onCodeDetected: (String) -> Unit = let@{ code ->
        if (!scanWindowOpen) return@let
        scanWindowOpen = false
        when (vm.scan(code)) {
            is ScanResult.Success -> playBeepOnce()
            is ScanResult.Duplicate -> vibrateOnce(context)
            ScanResult.NoJob -> {}
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Barcode Scanner") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            // camera area with ROI + tap overlay
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
                        Button(onClick = { camPerm.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant")
                        }
                    }
                } else {
                    CameraPreview(
                        lifecycleOwner = lifecycleOwner,
                        scanEnabled = scanWindowOpen,
                        onCodeDetected = onCodeDetected
                    )

                    // ROI box
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.Center)
                            .border(3.dp, Color.Green)
                    )

                    // Tap to scan overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                scanWindowOpen = true
                                scanToken++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (scanWindowOpen) "Scanning..." else "Tap to Scan",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // group label like your mock
            Text(
                text = "Current Group: ${job?.currentGroup?.index ?: "-"}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(status)

            Spacer(Modifier.height(12.dp))

            // 3 buttons exactly
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.newGroup() },
                    enabled = job != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("New Group")
                }

                Button(
                    onClick = onSummary,
                    enabled = job != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Summary")
                }

                Button(
                    onClick = {
                        val current = vm.job.value ?: return@Button
                        createCsvLauncher.launch("${current.name}.csv")
                    },
                    enabled = job != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish Job")
                }
            }
        }
    }
}