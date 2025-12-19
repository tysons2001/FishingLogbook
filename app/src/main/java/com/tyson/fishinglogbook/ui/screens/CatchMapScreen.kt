package com.tyson.fishinglogbook.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.Repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchMapScreen(
    onBack: () -> Unit,
    onOpenCatch: (Long) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val catches by repo.observeAllCatches().collectAsState(initial = emptyList())

    val first = catches.firstOrNull { it.latitude != null && it.longitude != null }
    val startPos = first?.let { LatLng(it.latitude!!, it.longitude!!) } ?: LatLng(-37.8136, 144.9631) // Melbourne

    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(startPos, 8f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch Map") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { pad ->
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = pad
        ) {
            catches.forEach { c ->
                val lat = c.latitude
                val lon = c.longitude
                if (lat != null && lon != null) {
                    Marker(
                        state = MarkerState(position = LatLng(lat, lon)),
                        title = c.species,
                        snippet = "Tap to open",
                        onInfoWindowClick = { onOpenCatch(c.id) }
                    )
                }
            }
        }
    }
}
