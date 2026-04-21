package com.example.photobooth.filters

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.photobooth.utils.ImageProcessor

@Composable
fun FilterScreen(
    imageUris: List<Uri>,
    outputDirectory: File,
    onFilterApplied: (Uri, RetroFilter) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf(RetroFilter.NORMAL) }
    var selectedFrame by remember { mutableStateOf(FrameStyle.CLASSIC_WHITE) }
    var includeDate by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    // Re-stitch whenever any parameter changes
    LaunchedEffect(selectedFilter, selectedFrame, includeDate, imageUris) {
        isProcessing = true
        withContext(Dispatchers.IO) {
            val newBitmap = ImageProcessor.createPhotoStripBitmap(
                context,
                imageUris,
                selectedFrame,
                includeDate,
                selectedFilter
            )
            withContext(Dispatchers.Main) {
                // Recycle old preview if it exists to save memory
                displayedBitmap?.recycle()
                displayedBitmap = newBitmap
            }
        }
        isProcessing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            displayedBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Photo Strip Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            if (isProcessing) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Feature Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(16.dp)
        ) {
            // Option 1: Filters
            Text("Filters", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RetroFilter.values()) { filter ->
                    FilterOption(
                        name = filter.displayName,
                        isSelected = filter == selectedFilter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            // Option 2: Frames
            Text("Frames", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FrameStyle.values()) { frame ->
                    FrameOption(
                        style = frame,
                        isSelected = frame == selectedFrame,
                        onClick = { selectedFrame = frame }
                    )
                }
            }

            // Option 3: Date Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Include Date Stamp", color = Color.White)
                Switch(checked = includeDate, onCheckedChange = { includeDate = it })
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.White)
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        isProcessing = true
                        val finalUri = withContext(Dispatchers.IO) {
                            ImageProcessor.createPhotoStrip(
                                context,
                                imageUris,
                                outputDirectory,
                                selectedFrame,
                                includeDate,
                                selectedFilter
                            )
                        }
                        isProcessing = false
                        if (finalUri != null) {
                            onFilterApplied(finalUri, selectedFilter)
                        }
                    }
                },
                enabled = !isProcessing && displayedBitmap != null
            ) {
                Text("Save & Sync")
            }
        }
    }
}

@Composable
fun FilterOption(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
        contentColor = Color.White
    ) {
        Text(name, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun FrameOption(style: FrameStyle, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = style.color,
        border = if (isSelected) BorderStroke(2.dp, Color.Cyan) else null,
        modifier = Modifier.size(50.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = style.textColor)
        }
    }
}
