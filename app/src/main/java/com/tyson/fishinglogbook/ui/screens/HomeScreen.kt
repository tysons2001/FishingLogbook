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
fun HomeScreen(onStartTrip: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val activeTrip by repo.observeActiveTrip().collectAsState(initial = null)
    val trips by repo.observeTrips().collectAsState(initial = emptyList())
    val catches by repo.observeAllCatches().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fishing Logbook") }) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    Text(if (activeTrip != null) "Active trip running ✅" else "No active trip")
                    Button(onClick = onStartTrip) { Text("Start trip (next batch)") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trips: ${trips.size}")
                    Text("Catches: ${catches.size}")
                }
            }

            Text("Next: Trips screen + Add Catch + GPS + Photos", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
