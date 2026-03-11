package kiman.warehouse.scanner.ui.scan

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    scanEnabled: Boolean,
    onCodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    // ✅ ALWAYS read latest values inside analyzer
    val scanEnabledState = rememberUpdatedState(scanEnabled)
    val onCodeDetectedState = rememberUpdatedState(onCodeDetected)

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

            analysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                // ✅ use latest state (not captured value)
                if (!scanEnabledState.value) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val rotation = imageProxy.imageInfo.rotationDegrees
                val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

                val frameW = imageProxy.width
                val frameH = imageProxy.height
                val effW = if (rotation == 90 || rotation == 270) frameH else frameW
                val effH = if (rotation == 90 || rotation == 270) frameW else frameH

                // ROI: center 50%
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

                            if (cx in roiLeft..roiRight && cy in roiTop..roiBottom) {
                                // ✅ call latest handler
                                onCodeDetectedState.value(raw)
                                break
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("CameraPreview", "Scan failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Bind failed", e)
            }
        }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try { ProcessCameraProvider.getInstance(context).get().unbindAll() } catch (_: Exception) {}
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}