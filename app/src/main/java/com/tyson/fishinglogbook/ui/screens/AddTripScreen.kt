package com.tyson.fishinglogbook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.Repository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(onDone: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val scope = rememberCoroutineScope()
    val activeTrip by repo.observeActiveTrip().collectAsState(initial = null)

    var name by remember { mutableStateOf("") }
    var waterway by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Start Trip") }) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeTrip != null) {
                Text("You already have an active trip. Open it and end it before starting a new one.")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Trip name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = waterway,
                onValueChange = { waterway = it },
                label = { Text("Waterway (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            status?.let { Text(it) }

            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = onDone) { Text("Cancel") }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = activeTrip == null,
                    onClick = {
                        scope.launch {
                            repo.startTrip(
                                name.trim().ifBlank { null },
                                waterway.trim().ifBlank { null },
                                notes.trim().ifBlank { null }
                            )
                            onDone()
                        }
                    }
                ) { Text("Start") }
            }
        }
    }
}
