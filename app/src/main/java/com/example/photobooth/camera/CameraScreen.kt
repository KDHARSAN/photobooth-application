package com.example.photobooth.camera

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    outputDirectory: File,
    onPhotoStripCaptured: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            android.content.pm.PackageManager.PERMISSION_GRANTED ==
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    )
        )
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val cameraManager = remember { CameraManager(context) }
    var lensFacing by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    var isCapturing by remember { mutableStateOf(false) }
    val capturedUris = remember { mutableStateListOf<Uri>() }
    var captureCount by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Animation States
    var showFlash by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(0) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (showFlash) 1f else 0f,
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        finishedListener = { if (it == 1f) showFlash = false }
    )

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { launcher.launch(android.Manifest.permission.CAMERA) }) {
                Text("Grant Camera Permission")
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        key(lensFacing) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraManager.startCamera(lifecycleOwner, previewView, lensFacing)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Flash Overlay
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(flashAlpha)
                    .background(Color.White)
            )
        }

        // Countdown Overlay
        if (countdownValue > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownValue.toString(),
                    color = Color.White,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top UI (Camera Switch)
        if (!isCapturing) {
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                },
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Cached, contentDescription = "Switch Camera", tint = Color.White)
            }
        }

        // Bottom UI (Instructions or Progress)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCapturing) {
                Text(
                    text = "Photo ${captureCount + 1} / 3",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                Button(
                    onClick = {
                        if (!isCapturing) {
                            isCapturing = true
                            capturedUris.clear()
                            captureCount = 0

                            coroutineScope.launch {
                                for (i in 0 until 3) {
                                    // Countdown sequence
                                    for (j in 3 downTo 1) {
                                        countdownValue = j
                                        delay(1000)
                                    }
                                    countdownValue = 0
                                    
                                    // Visual & Audio feedback
                                    showFlash = true
                                    cameraManager.playShutterSound()
                                    
                                    cameraManager.takePhoto(
                                        outputDirectory = outputDirectory,
                                        onImageCaptured = { uri ->
                                            capturedUris.add(uri)
                                            captureCount++
                                            if (captureCount == 3) {
                                                isCapturing = false
                                                onPhotoStripCaptured(capturedUris.toList())
                                            }
                                        },
                                        onError = { exc ->
                                            isCapturing = false
                                            exc.printStackTrace()
                                        }
                                    )
                                    
                                    if (captureCount < 2) {
                                        delay(1000) // Small break between photos
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(text = "Capture 3-Photo Strip", fontSize = 18.sp)
                }
            }
        }
    }
}
