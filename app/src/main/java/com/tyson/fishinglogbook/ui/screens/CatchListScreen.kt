package com.tyson.fishinglogbook.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.CatchEntity
import com.tyson.fishinglogbook.data.Repository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchListScreen(
    onBack: () -> Unit,
    onOpenCatch: (Long) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val catches by repo.observeAllCatches().collectAsState(initial = emptyList())

    val fmt = remember { SimpleDateFormat("EEE d MMM yyyy, h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catches") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { pad ->
        if (catches.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("No catches yet.")
                Button(onClick = onBack) { Text("Back") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(catches.sortedByDescending { it.timestampMillis }, key = { it.id }) { c ->
                    CatchRow(
                        c = c,
                        fmt = fmt,
                        onClick = { onOpenCatch(c.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CatchRow(
    c: CatchEntity,
    fmt: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo thumbnail (if available)
            if (!c.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(c.photoUri),
                    contentDescription = "Catch photo",
                    modifier = Modifier
                        .size(78.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title line
                Text(
                    text = c.species,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Date/time
                Text(
                    text = fmt.format(Date(c.timestampMillis)),
                    style = MaterialTheme.typography.bodySmall
                )

                // Length/Weight
                val sizeBits = buildList {
                    c.lengthCm?.let { add("${it}cm") }
                    c.weightKg?.let { add("${it}kg") }
                }.joinToString("  •  ")

                if (sizeBits.isNotBlank()) {
                    Text(sizeBits, style = MaterialTheme.typography.bodySmall)
                }

                // Lure/Notes preview
                if (!c.lure.isNullOrBlank()) {
                    Text(
                        text = "Lure: ${c.lure}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!c.notes.isNullOrBlank()) {
                    Text(
                        text = c.notes!!,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // GPS preview
                if (c.latitude != null && c.longitude != null) {
                    Text(
                        text = "GPS: %.5f, %.5f".format(c.latitude, c.longitude),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Weather + Moon preview
                val weatherBits = buildList {
                    c.weatherTempC?.let { add("${"%.1f".format(it)}°C") }
                    c.weatherPressureHpa?.let { add("${"%.0f".format(it)} hPa") }
                    if (!c.moonPhaseName.isNullOrBlank()) {
                        val pct = c.moonIlluminationPct ?: 0
                        add("${c.moonPhaseName} (${pct}%)")
                    }
                }.joinToString("  •  ")

                if (weatherBits.isNotBlank()) {
                    Text(
                        text = weatherBits,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
