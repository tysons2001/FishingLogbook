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
import androidx.compose.runtime.saveable.rememberSaveable
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

    // Use rememberSaveable so rotation doesn't wipe the form
    var species by rememberSaveable { mutableStateOf("") }
    var length by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var lure by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    var liveLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var liveLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var liveAcc by rememberSaveable { mutableStateOf<Float?>(null) }

    var savedLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedAcc by rememberSaveable { mutableStateOf<Float?>(null) }

    // Save photo as String so it's saveable across rotation
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val photoUri: Uri? = photoUriString?.let { Uri.parse(it) }

    var status by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

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
        if (uri != null) photoUriString = uri.toString()
    }

    // Camera
    fun newTempPhotoUri(): Uri {
        val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "catch_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) photoUriString = pendingCameraUriString
        pendingCameraUriString = null
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Catch") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            status?.let { Text(it) }

            Text(
                if (activeTrip != null) "Will attach to active trip"
                else "No active trip - catch will be saved standalone"
            )

            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Species (required)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = { Text("Length (cm)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = lure,
                onValueChange = { lure = it },
                label = { Text("Lure/Bait") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GPS", style = MaterialTheme.typography.titleMedium)

                    val liveTxt =
                        if (liveLat != null && liveLon != null)
                            "Live: %.5f, %.5f (acc %sm)".format(liveLat, liveLon, (liveAcc?.toInt() ?: 0).toString())
                        else "Live: -"

                    val savedTxt =
                        if (savedLat != null && savedLon != null)
                            "Saved: %.5f, %.5f (acc %sm)".format(savedLat, savedLon, (savedAcc?.toInt() ?: 0).toString())
                        else "Saved: -"

                    Text(liveTxt)
                    Text(savedTxt)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { refreshLiveGps() }) { Text("Get GPS") }

                        Button(
                            enabled = liveLat != null && liveLon != null,
                            onClick = {
                                savedLat = liveLat
                                savedLon = liveLon
                                savedAcc = liveAcc
                                status = "GPS saved"
                            }
                        ) { Text("Save GPS") }

                        Button(
                            enabled = savedLat != null && savedLon != null,
                            onClick = {
                                val uri = Uri.parse("geo:$savedLat,$savedLon?q=$savedLat,$savedLon")
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        ) { Text("Maps") }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Photo", style = MaterialTheme.typography.titleMedium)

                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Catch photo",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("No photo selected")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            val uri = newTempPhotoUri()
                            pendingCameraUriString = uri.toString()
                            takePicture.launch(uri)
                        }) { Text("Take photo") }

                        Button(onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) { Text("Pick gallery") }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            val canSave = species.trim().isNotEmpty() && !isSaving

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = onDone, enabled = !isSaving) {
                    Text("Cancel")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = canSave,
                    onClick = {
                        scope.launch {
                            if (species.trim().isEmpty()) {
                                snackbarHostState.showSnackbar("Species is required")
                                return@launch
                            }

                            isSaving = true
                            try {
                                repo.addCatch(
                                    CatchEntity(
                                        tripId = activeTrip?.id,
                                        timestampMillis = System.currentTimeMillis(),
                                        species = species.trim(),
                                        lengthCm = length.toDoubleOrNull(),
                                        weightKg = weight.toDoubleOrNull(),
                                        lure = lure.trim().ifBlank { null },
                                        notes = notes.trim().ifBlank { null },
                                        latitude = savedLat,
                                        longitude = savedLon,
                                        accuracyM = savedAcc,
                                        photoUri = photoUriString
                                    )
                                )
                                snackbarHostState.showSnackbar("Catch saved")
                                onDone()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Save failed: ${e.message}")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }
        }
    }
}
