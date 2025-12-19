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
import com.tyson.fishinglogbook.astronomy.MoonPhase
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.CatchEntity
import com.tyson.fishinglogbook.data.Repository
import com.tyson.fishinglogbook.weather.OpenMeteoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCatchScreen(onDone: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val scope = rememberCoroutineScope()

    val activeTrip by repo.observeActiveTrip().collectAsState(initial = null)

    // Rotation-safe form state
    var species by rememberSaveable { mutableStateOf("") }
    var length by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var lure by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    // GPS
    var liveLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var liveLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var liveAcc by rememberSaveable { mutableStateOf<Float?>(null) }

    var savedLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedAcc by rememberSaveable { mutableStateOf<Float?>(null) }

    // Photo
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val photoUri: Uri? = photoUriString?.let { Uri.parse(it) }

    // Camera “pending” uri must be saveable string (so it survives rotation)
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    // UI
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Permissions
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
        if (uri != null) {
            photoUriString = uri.toString()
            status = "Photo selected"
        }
    }

    // Camera capture
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            photoUriString = pendingCameraUriString
            status = "Photo captured"
        } else {
            status = "Photo cancelled"
        }
        pendingCameraUriString = null
    }

    fun newTempPhotoUri(): Uri {
        val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "catch_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
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

    val canSave = species.trim().isNotEmpty() && !isSaving

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Catch") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
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

            // GPS card
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GPS", style = MaterialTheme.typography.titleMedium)

                    val liveTxt =
                        if (liveLat != null && liveLon != null)
                            "Live: %.5f, %.5f (±%sm)".format(liveLat, liveLon, (liveAcc?.toInt() ?: 0))
                        else "Live: -"

                    val savedTxt =
                        if (savedLat != null && savedLon != null)
                            "Saved: %.5f, %.5f (±%sm)".format(savedLat, savedLon, (savedAcc?.toInt() ?: 0))
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

            // Photo card
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

            Spacer(Modifier.height(6.dp))

            // Bottom actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    onClick = onDone
                ) { Text("Cancel") }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = canSave,
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val now = System.currentTimeMillis()

                                // Moon phase is always available (offline)
                                val moon = MoonPhase.fromMillis(now)

                                // Weather only if GPS is saved
                                val lat = savedLat
                                val lon = savedLon
                                val weather = if (lat != null && lon != null) {
                                    withContext(Dispatchers.IO) {
                                        OpenMeteoClient.fetchCurrent(lat, lon)
                                    }
                                } else null

                                repo.addCatch(
                                    CatchEntity(
                                        tripId = activeTrip?.id,
                                        timestampMillis = now,
                                        species = species.trim(),
                                        lengthCm = length.toDoubleOrNull(),
                                        weightKg = weight.toDoubleOrNull(),
                                        lure = lure.trim().ifBlank { null },
                                        notes = notes.trim().ifBlank { null },
                                        latitude = lat,
                                        longitude = lon,
                                        accuracyM = savedAcc,
                                        photoUri = photoUriString,

                                        weatherTempC = weather?.temperatureC,
                                        weatherPressureHpa = weather?.pressureHpa,
                                        weatherFetchedAtMillis = if (weather != null) now else null,

                                        moonPhaseName = moon.phaseName,
                                        moonIlluminationPct = moon.illuminationPct
                                    )
                                )

                                snackbarHostState.showSnackbar(
                                    if (weather != null) "Catch saved + weather + moon"
                                    else "Catch saved + moon (no GPS saved)"
                                )
                                onDone()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Save failed: ${e.message}")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                ) {
                    Text(if (isSaving) "Saving..." else "Save Catch")
                }
            }
        }
    }
}
