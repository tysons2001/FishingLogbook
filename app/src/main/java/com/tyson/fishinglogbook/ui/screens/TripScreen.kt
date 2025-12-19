package com.tyson.fishinglogbook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.Repository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val scope = rememberCoroutineScope()

    val activeTrip by repo.observeActiveTrip().collectAsState(initial = null)
    val fmt = remember { SimpleDateFormat("EEE d MMM yyyy, h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Trip") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeTrip == null) {
                Text("No active trip.")
                Button(onClick = onBack) { Text("Back") }
                return@Column
            }

            val t = activeTrip!!

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t.name ?: "Unnamed trip", style = MaterialTheme.typography.titleLarge)
                    if (!t.waterway.isNullOrBlank()) Text(t.waterway!!)
                    Text("Started: ${fmt.format(Date(t.startMillis))}")
                    if (!t.notes.isNullOrBlank()) Text(t.notes!!)
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        repo.endTrip(t.id)
                        onBack()
                    }
                }
            ) {
                Text("End Trip")
            }

            Text("Next batch: Add Catch + GPS + Photos", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
