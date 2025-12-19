package com.tyson.fishinglogbook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.Repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartTrip: () -> Unit,
    onOpenActiveTrip: () -> Unit,
    onAddCatch: () -> Unit,
    onViewCatches: () -> Unit,
    onOpenMap: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }

    val activeTrip by repo.observeActiveTrip().collectAsState(initial = null)
    val trips by repo.observeTrips().collectAsState(initial = emptyList())
    val catches by repo.observeAllCatches().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Fishing Logbook") })
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* ===== STATUS CARD ===== */
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)

                    Text(
                        if (activeTrip != null)
                            "Active trip running"
                        else
                            "No active trip"
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            enabled = activeTrip == null,
                            onClick = onStartTrip
                        ) {
                            Text("Start trip")
                        }

                        Button(
                            enabled = activeTrip != null,
                            onClick = onOpenActiveTrip
                        ) {
                            Text("Open trip")
                        }
                    }
                }
            }

            /* ===== STATS + ACTIONS ===== */
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Trips: ${trips.size}")
                    Text("Catches: ${catches.size}")

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onAddCatch) {
                            Text("Add Catch")
                        }

                        Button(onClick = onViewCatches) {
                            Text("View Catches")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onOpenMap) {
                            Text("Map")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Map shows all catches with GPS",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
