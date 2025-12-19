package com.tyson.fishinglogbook.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val photoUri = photoUriString?.let { Uri.parse(it) }

    var status by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val permsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            )
        )
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUriString = uri.toString()
    }

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
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                liveLat = loc.latitude
                liveLon = loc.longitude
                liveAcc = loc.accuracy
                status = "GPS updated"
            } else {
                status = "No GPS fix yet"
            }
        }.addOnFailureListener {
            status = "GPS failed"
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Catch") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            status?.let { Text(it) }

            Text(
                if (activeTrip != null)
                    "Will attach to active trip"
                else
                    "No active trip - catch will be saved standalone"
            )

            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Species (required)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GPS", style = MaterialTheme.typography.titleMedium)
                    Text("Live: ${liveLat ?: "-"}, ${liveLon ?: "-"}")
                    Text("Saved: ${savedLat ?: "-"}, ${savedLon ?: "-"}")

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { refreshLiveGps() }) { Text("Get GPS") }
                        Button(
                            enabled = liveLat != null,
                            onClick = {
                                savedLat = liveLat
                                savedLon = liveLon
                                savedAcc = liveAcc
                            }
                        ) { Text("Save GPS") }
                        Button(
                            enabled = savedLat != null,
                            onClick = {
                                val uri = Uri.parse("geo:$savedLat,$savedLon?q=$savedLat,$savedLon")
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        ) { Text("Maps") }
                    }
                }
            }

            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onDone,
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = species.isNotBlank() && !isSaving,
                    onClick = {
                        scope.launch {
                            isSaving = true
                            repo.addCatch(
                                CatchEntity(
                                    tripId = activeTrip?.id,
                                    timestampMillis = System.currentTimeMillis(),
                                    species = species.trim(),
                                    lengthCm = length.toDoubleOrNull(),
                                    weightKg = weight.toDoubleOrNull(),
                                    lure = lure.ifBlank { null },
                                    notes = notes.ifBlank { null },
                                    latitude = savedLat,
                                    longitude = savedLon,
                                    accuracyM = savedAcc,
                                    photoUri = photoUriString
                                )
                            )
                            snackbarHostState.showSnackbar("Catch saved")
                            onDone()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}
