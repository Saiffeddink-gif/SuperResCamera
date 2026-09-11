package com.superres.camera.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraScreen() {
    var burstCount by remember { mutableStateOf(3) }
    var zoomLevel by remember { mutableStateOf(2) }
    var selectedFormat by remember { mutableStateOf("JPEG") }
    var isCapturing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Super-Resolution Camera",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        // Preview Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera Preview")
            }
        }

        // Controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Burst Count Slider
            Text("Burst Count: $burstCount")
            Slider(
                value = burstCount.toFloat(),
                onValueChange = { burstCount = it.toInt() },
                valueRange = 1f..10f,
                modifier = Modifier.fillMaxWidth()
            )

            // Zoom Level Slider
            Text("Super-Res Zoom: ${zoomLevel}×")
            Slider(
                value = zoomLevel.toFloat(),
                onValueChange = { zoomLevel = it.toInt() },
                valueRange = 2f..4f,
                modifier = Modifier.fillMaxWidth()
            )

            // Format Selector
            Text("Output Format")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("JPEG", "PNG", "WEBP", "RAW", "HEIC").forEach { format ->
                    Button(
                        onClick = { selectedFormat = format },
                        modifier = Modifier.padding(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFormat == format)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(format)
                    }
                }
            }
        }

        // Capture Button
        Button(
            onClick = { isCapturing = !isCapturing },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isCapturing
        ) {
            Text(
                text = if (isCapturing) "Capturing..." else "CAPTURE BURST",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return remember { androidx.compose.foundation.ScrollState(0) }
}
