package com.tyson.fishinglogbook.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.CatchEntity
import com.tyson.fishinglogbook.data.Repository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCatchScreen(onDone: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val scope = rememberCoroutineScope()

    val activeTrip by repo.observeActiveTrip().collectAsState(initial = null)

    var species by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var lure by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var liveLat by remember { mutableStateOf<Double?>(null) }
    var liveLon by remember { mutableStateOf<Double?>(null) }
    var liveAcc by remember { mutableStateOf<Float?>(null) }

    var savedLat by remember { mutableStateOf<Double?>(null) }
    var savedLon by remember { mutableStateOf<Double?>(null) }
    var savedAcc by remember { mutableStateOf<Float?>(null) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val permsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    LaunchedEffect(Unit) {
        permsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            )
        )
    }

    // Gallery picker
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri
    }

    // Camera
    fun newTempPhotoUri(): Uri {
        val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "catch_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) photoUri = pendingCameraUri
        pendingCameraUri = null
    }

    fun refreshLiveGps() {
        try {
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            client.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        liveLat = loc.latitude
                        liveLon = loc.longitude
                        liveAcc = loc.accuracy
                        status = "GPS updated"
                    } else {
                        status = "No GPS fix yet (try again outside)"
                    }
                }
                .addOnFailureListener { e ->
                    status = "GPS failed: ${e.message}"
                }
        } catch (e: Exception) {
            status = "GPS error: ${e.message}"
        }
    }

    Scaffold(topBar = { TopAppBar(title
