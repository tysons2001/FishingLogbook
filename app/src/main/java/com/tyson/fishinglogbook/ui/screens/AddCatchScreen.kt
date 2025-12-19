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

    var isSaving by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val perms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    LaunchedEffect(Unit) {
        perms.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            )
        )
    }

    fun refreshGps() {
        LocationServices.getFusedLocationProviderClient(ctx)
            .lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    liveLat = loc.latitude
                    liveLon = loc.longitude
                    liveAcc = loc.accuracy
                }
            }
    }

    fun newTempPhotoUri(): Uri {
        val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "catch_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) photoUriString = pendingCameraUri
        pendingCameraUri = null
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUriString = uri.toString()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Catch") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize()
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(species, { species = it }, label = { Text("Species*") }, modifier = Modifier.fillMaxWidth())

            Row {
                OutlinedTextField(length, { length = it }, label = { Text("Length (cm)") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f))
            }

            OutlinedTextField(lure, { lure = it }, label = { Text("Lure/Bait") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Button(onClick = { refreshGps() }) { Text("Get GPS") }
            Button(
                enabled = liveLat != null,
                onClick = {
                    savedLat = liveLat
                    savedLon = liveLon
                    savedAcc = liveAcc
                }
            ) { Text("Save GPS") }

            Button(onClick = {
                val uri = newTempPhotoUri()
                pendingCameraUri = uri.toString()
                takePicture.launch(uri)
            }) { Text("Take Photo") }

            Button(onClick = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Pick Gallery") }

            Button(
                enabled = species.isNotBlank() && !isSaving,
                onClick = {
                    scope.launch {
                        isSaving = true

                        val moon = MoonPhase.fromMillis(System.currentTimeMillis())

                        val weather = if (savedLat != null && savedLon != null) {
                            withContext(Dispatchers.IO) {
                                OpenMeteoClient.fetchCurrent(savedLat!!, savedLon!!)
                            }
                        } else null

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
                                photoUri = photoUriString,
                                weatherTempC = weather?.temperatureC,
                                weatherPressureHpa = weather?.pressureHpa,
                                weatherFetchedAtMillis = System.currentTimeMillis(),
                                moonPhaseName = moon.phaseName,
                                moonIlluminationPct = moon.illuminationPct
                            )
                        )

                        snackbar.showSnackbar("Catch saved with weather + moon")
                        onDone()
                        isSaving = false
                    }
                }
            ) { Text("Save Catch") }
        }
    }
}
