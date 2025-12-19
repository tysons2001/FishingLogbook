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
fun CatchDetailScreen(
    catchId: Long,
    onBack: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val item by repo.observeCatch(catchId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch Details") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { pad ->
        val c = item ?: return@Scaffold

        Column(
            Modifier.fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(c.species, style = MaterialTheme.typography.titleLarge)

            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("Temperature: ${c.weatherTempC?.let { "${"%.1f".format(it)} °C" } ?: "—"}")
                    Text("Pressure: ${c.weatherPressureHpa?.let { "${"%.0f".format(it)} hPa" } ?: "—"}")
                    Text("Moon: ${c.moonPhaseName ?: "—"} (${c.moonIlluminationPct ?: 0}%)")
                }
            }
        }
    }
                         }
