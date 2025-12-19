package com.tyson.fishinglogbook.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
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

    val cameraPositionState = rememberCameraPositionState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) {
        GoogleMap(
            modifier = Modifier,
            cameraPositionState = cameraPositionState
        ) {
            catches.forEach { c ->
                if (c.latitude != null && c.longitude != null) {
                    Marker(
                        state = MarkerState(
                            position = com.google.android.gms.maps.model.LatLng(
                                c.latitude,
                                c.longitude
                            )
                        ),
                        title = c.species,
                        snippet = "Tap to view",
                        onInfoWindowClick = {
                            onOpenCatch(c.id)
                        }
                    )
                }
            }
        }
    }
}
