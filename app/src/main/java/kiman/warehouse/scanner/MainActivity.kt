package kiman.warehouse.scanner

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kiman.warehouse.scanner.model.Group
import kiman.warehouse.scanner.model.Job
import kiman.warehouse.scanner.model.ScanItem
import kiman.warehouse.scanner.viewmodel.ScanResult
import kiman.warehouse.scanner.viewmodel.ScannerViewModel
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: ScannerViewModel = viewModel()

    val job = viewModel.job.value
    val status = viewModel.status.value

    var jobName by remember { mutableStateOf("") }
    var hasCameraPermission by remember { mutableStateOf(false) }

    // tap-to-scan window
    var scanWindowOpen by remember { mutableStateOf(false) }
    var scanWindowToken by remember { mutableStateOf(0) } // increments per tap to restart timer

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val exportJob = viewModel.finalizeForExport()
        if (uri == null || exportJob == null) return@rememberLauncherForActivityResult

        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                BufferedWriter(OutputStreamWriter(out)).use { bw ->
                    bw.append("job_name,job_started_at,group_index,scan_index,timestamp_iso,code\n")
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    for (g in exportJob.groups) {
                        var scanIndex = 1
                        for (item in g.items) {
                            val tsIso = sdf.format(Date(item.timestampMs))
                            val codeEsc = escapeCsv(item.code)
                            bw.append("${exportJob.name},${exportJob.startedAtMs},${g.index},${scanIndex},${tsIso},${codeEsc}\n")
                            scanIndex++
                        }
                    }
                    bw.flush()
                }
            }
            viewModel.clearJob()
        } catch (e: Exception) {
            // keep job in RAM if save fails
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // auto-close scan window after 1200ms
    LaunchedEffect(scanWindowToken) {
        if (!scanWindowOpen) return@LaunchedEffect
        kotlinx.coroutines.delay(1200)
        scanWindowOpen = false
    }

    val onCodeDetected: (String) -> Unit = let@{ code ->
        if (!scanWindowOpen) return@let

        // close window immediately on first result
        scanWindowOpen = false

        when (val result = viewModel.scan(code)) {
            is ScanResult.Success -> {
                playBeepOnce()
            }
            is ScanResult.Duplicate -> {
                vibrateOnce(context)
                // optional: also beep on duplicate? you said vibrate + don't add, so no beep
            }
            ScanResult.NoJob -> {
                // do nothing
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Warehouse Scanner") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {

            // Camera + ROI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (!hasCameraPermission) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Camera permission required")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Camera Permission")
                        }
                    }
                } else {
                    CameraPreview(
                        lifecycleOwner = lifecycleOwner,
                        scanEnabled = scanWindowOpen,
                        onCodeDetected = onCodeDetected
                    )

                    // ROI overlay (visual)
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.Center)
                            .border(3.dp, Color.Red)
                    )

                    // Tap overlay (only blocks when showing prompt)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                // open scan window on tap
                                scanWindowOpen = true
                                scanWindowToken += 1
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (scanWindowOpen) "Scanning..." else "Tap to Scan",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = jobName,
                onValueChange = { jobName = it },
                label = { Text("Job name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.startJob(jobName) },
                    modifier = Modifier.weight(1f)
                ) { Text("Start Job") }

                OutlinedButton(
                    onClick = { viewModel.clearJob() },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.newGroup() },
                    modifier = Modifier.weight(1f),
                    enabled = (job != null)
                ) { Text("New Group") }

                Button(
                    onClick = {
                        val current = viewModel.job.value ?: return@Button
                        // finalize and export
                        createCsvLauncher.launch("${current.name}.csv")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = (job != null)
                ) { Text("Finish & Export") }
            }

            Spacer(Modifier.height(8.dp))
            Text(status)

            // Preview current group scans
            job?.let { j ->
                Spacer(Modifier.height(8.dp))
                Text("Job: ${j.name} | Saved groups: ${j.groups.size} | Current group: ${j.currentGroup.index}")

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    itemsIndexed(j.currentGroup.items.takeLast(12)) { idx, item ->
                        Text("${j.currentGroup.index}.${idx + 1}: ${item.code}")
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    scanEnabled: Boolean,
    onCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor) { imageProxy ->
                if (!scanEnabled) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mediaImage = imageProxy.image ?: run {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val rotation = imageProxy.imageInfo.rotationDegrees
                val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

                // ML Kit bounding boxes are in the coordinate space of the rotated (upright) image.
                val frameW = imageProxy.width
                val frameH = imageProxy.height
                val effW = if (rotation == 90 || rotation == 270) frameH else frameW
                val effH = if (rotation == 90 || rotation == 270) frameW else frameH

                // ROI: center 50% area (matches your 220dp box visually)
                val roiLeft = (effW * 0.25f).toInt()
                val roiTop = (effH * 0.25f).toInt()
                val roiRight = (effW * 0.75f).toInt()
                val roiBottom = (effH * 0.75f).toInt()

                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        for (b in barcodes) {
                            val raw = b.rawValue ?: continue
                            val box = b.boundingBox ?: continue

                            val cx = box.centerX()
                            val cy = box.centerY()

                            // accept only if center point is inside ROI
                            if (cx in roiLeft..roiRight && cy in roiTop..roiBottom) {
                                onCodeDetected(raw)
                                break
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try { ProcessCameraProvider.getInstance(context).get().unbindAll() } catch (_: Exception) {}
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

private fun escapeCsv(value: String): String {
    val needsQuotes = value.contains(",") || value.contains("\n") || value.contains("\"")
    return if (!needsQuotes) value
    else "\"" + value.replace("\"", "\"\"") + "\""
}


private var toneGen: ToneGenerator? = null

fun playBeepOnce() {
    if (toneGen == null) toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
}



fun vibrateOnce(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(Vibrator::class.java)
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    } ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(80)
    }
}
