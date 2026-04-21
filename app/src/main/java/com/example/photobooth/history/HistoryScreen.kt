package com.example.photobooth.history

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photobooth.data.PhotoRecord
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    historyList: List<PhotoRecord>,
    onClose: () -> Unit,
    onExport: (Uri) -> Unit,
    onDelete: (PhotoRecord) -> Unit
) {
    var selectedRecord by remember { mutableStateOf<PhotoRecord?>(null) }
    
    if (selectedRecord != null) {
        // Fullscreen view
        FullscreenPhotoView(
            record = selectedRecord!!,
            onClose = { selectedRecord = null },
            onExport = onExport,
            onDelete = {
                onDelete(it)
                selectedRecord = null
            }
        )
    } else {
        // Gallery View
        Column(modifier = Modifier.fillMaxSize()) {
             Row(
                 modifier = Modifier.fillMaxWidth().padding(16.dp),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 Text(text = "My Photo Strips", style = MaterialTheme.typography.headlineMedium)
                 TextButton(onClick = onClose) { Text("Back") }
             }

             LazyVerticalGrid(
                 columns = GridCells.Fixed(2),
                 contentPadding = PaddingValues(8.dp),
                 horizontalArrangement = Arrangement.spacedBy(8.dp),
                 verticalArrangement = Arrangement.spacedBy(8.dp)
             ) {
                 items(historyList) { record ->
                     HistoryItem(
                         record = record,
                         onClick = { selectedRecord = record },
                         onDelete = { onDelete(record) }
                     )
                 }
             }
        }
    }
}

@Composable
fun HistoryItem(record: PhotoRecord, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Photo?") },
            text = { Text("This will permanently remove the photo from your device and the cloud.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.6f) 
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (File(Uri.parse(record.localUri).path ?: "").exists()) record.localUri else record.remoteUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo Strip",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Delete Icon
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            // Sync status icon
            if (record.isSynced) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Synced",
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    tint = Color.Cyan
                )
            }
        }
    }
}

@Composable
fun FullscreenPhotoView(
    record: PhotoRecord, 
    onClose: () -> Unit, 
    onExport: (Uri) -> Unit,
    onDelete: (PhotoRecord) -> Unit
) {
     val context = LocalContext.current
     var showDeleteConfirm by remember { mutableStateOf(false) }

     if (showDeleteConfirm) {
         AlertDialog(
             onDismissRequest = { showDeleteConfirm = false },
             title = { Text("Delete Photo?") },
             confirmButton = {
                 TextButton(onClick = {
                     onDelete(record)
                     showDeleteConfirm = false
                 }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
             },
             dismissButton = {
                 TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
             }
         )
     }

     Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
         Row(
             modifier = Modifier.fillMaxWidth().padding(16.dp),
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
         ) {
             IconButton(onClick = onClose) {
                 Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
             }
             Row {
                 IconButton(onClick = { showDeleteConfirm = true }) {
                     Icon(Icons.Default.Delete, "Delete", tint = Color.White)
                 }
                 IconButton(onClick = { 
                      val uriStr = if (File(Uri.parse(record.localUri).path ?: "").exists()) record.localUri else record.remoteUrl
                      if(uriStr != null) {
                          onExport(Uri.parse(uriStr)) 
                      } else {
                          Toast.makeText(context, "Image not available", Toast.LENGTH_SHORT).show()
                      }
                 }) {
                     Icon(Icons.Default.Download, "Export", tint = Color.White)
                 }
             }
         }
         
         AsyncImage(
             model = ImageRequest.Builder(LocalContext.current)
                .data(if (File(Uri.parse(record.localUri).path ?: "").exists()) record.localUri else record.remoteUrl)
                .crossfade(true)
                .build(),
             contentDescription = "Fullscreen Photo Strip",
             modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
             contentScale = ContentScale.Fit
         )
     }
}
