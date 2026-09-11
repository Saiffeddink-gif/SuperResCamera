package com.scan3d.camera.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scan3d.camera.domain.camera.RawBurstCaptureManager

@Composable
fun CameraScreen() {
    var burstCount by remember { mutableStateOf(3) }
    var zoomLevel by remember { mutableStateOf(2) }
    var selectedFormat by remember { mutableStateOf("JPEG") }
    var selectedLens by remember { mutableStateOf("PRIMARY_0") }
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "RAW Burst Multi-Lens",
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📷 Camera Preview", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Lens: $selectedLens | Burst: $burstCount | Zoom: ${zoomLevel}×",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lens Selection
            Text("📸 Lens: $selectedLens", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "PRIMARY_0" to "0",
                    "PRIMARY_40" to "40",
                    "ULTRA_WIDE" to "2",
                    "MACRO" to "54"
                ).forEach { (name, id) ->
                    Button(
                        onClick = { selectedLens = name },
                        modifier = Modifier.padding(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedLens == name)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("$name\n($id)", fontSize = 10.sp)
                    }
                }
            }

            // Burst Count Slider
            Text("🔄 Burst Count: $burstCount", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = burstCount.toFloat(),
                onValueChange = { burstCount = it.toInt() },
                valueRange = 1f..10f,
                modifier = Modifier.fillMaxWidth()
            )

            // Zoom Level Slider
            Text("🔍 Super-Res Zoom: ${zoomLevel}×", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = zoomLevel.toFloat(),
                onValueChange = { zoomLevel = it.toInt() },
                valueRange = 2f..4f,
                modifier = Modifier.fillMaxWidth()
            )

            // Format Selector
            Text("💾 Output Format", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("JPEG", "PNG", "WEBP", "RAW", "HEIC").forEach { format ->
                    Button(
                        onClick = { selectedFormat = format },
                        modifier = Modifier.padding(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFormat == format)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(format, fontSize = 11.sp)
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
            enabled = !isCapturing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isCapturing) "⏹️ Capturing..." else "⚡ CAPTURE BURST",
                fontSize = 18.sp
            )
        }
    }
}
