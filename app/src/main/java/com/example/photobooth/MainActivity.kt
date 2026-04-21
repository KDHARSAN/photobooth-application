package com.example.photobooth

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photobooth.auth.AuthViewModel
import com.example.photobooth.auth.LoginScreen
import com.example.photobooth.camera.CameraScreen
import com.example.photobooth.data.AppDatabase
import com.example.photobooth.data.SyncRepository
import com.example.photobooth.filters.FilterScreen
import com.example.photobooth.history.HistoryScreen
import com.example.photobooth.utils.ExportManager
import com.example.photobooth.utils.ImageProcessor
import java.io.File
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val syncRepository = SyncRepository(authViewModel.authManager, database.photoDao())

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
                    
                    if (user == null) {
                        LoginScreen(viewModel = authViewModel)
                    } else {
                        MainAppContent(
                            outputDirectory = getOutputDirectory(),
                            syncRepository = syncRepository,
                            database = database,
                            onSignOut = { authViewModel.signOut() }
                        )
                    }
                }
            }
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
}

enum class AppScreen { CAMERA, FILTER, HISTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    outputDirectory: File,
    syncRepository: SyncRepository,
    database: AppDatabase,
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(AppScreen.CAMERA) }
    var capturedPhotoUris by remember { mutableStateOf<List<Uri>?>(null) }
    
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    
    val historyList by database.photoDao().getAllHistory(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Fetch initial history on mount or user change
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            syncRepository.fetchRemoteHistory()
        }
    }

    Scaffold(
        topBar = {
            if (currentScreen == AppScreen.CAMERA) {
                 TopAppBar(
                     title = { Text("Photo Booth") },
                     actions = {
                         TextButton(onClick = { currentScreen = AppScreen.HISTORY }) {
                             Text("History")
                         }
                         TextButton(onClick = onSignOut) {
                             Text("Sign Out")
                         }
                     }
                 )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                AppScreen.CAMERA -> {
                    CameraScreen(
                        outputDirectory = outputDirectory,
                        onPhotoStripCaptured = { uris ->
                            capturedPhotoUris = uris
                            currentScreen = AppScreen.FILTER
                        }
                    )
                }
                AppScreen.FILTER -> {
                    capturedPhotoUris?.let { uris ->
                        FilterScreen(
                            imageUris = uris,
                            outputDirectory = outputDirectory,
                            onFilterApplied = { finalUri, retroFilter ->
                                coroutineScope.launch {
                                    val savedRecord = syncRepository.saveAndSyncPhotoStrip(finalUri, retroFilter.displayName)
                                    if (savedRecord != null) {
                                        Toast.makeText(context, "Saved & Synced!", Toast.LENGTH_SHORT).show()
                                    }
                                    currentScreen = AppScreen.CAMERA
                                }
                            },
                            onCancel = {
                                capturedPhotoUris = null
                                currentScreen = AppScreen.CAMERA
                            }
                        )
                    }
                }
                AppScreen.HISTORY -> {
                    HistoryScreen(
                        historyList = historyList,
                        onClose = { currentScreen = AppScreen.CAMERA },
                        onExport = { uri -> 
                            coroutineScope.launch {
                                val success = ExportManager.saveToGallery(context, uri)
                                if (success) {
                                    Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = { record ->
                            coroutineScope.launch {
                                syncRepository.deletePhotoStrip(record)
                            }
                        }
                    )
                }
            }
        }
    }
}
