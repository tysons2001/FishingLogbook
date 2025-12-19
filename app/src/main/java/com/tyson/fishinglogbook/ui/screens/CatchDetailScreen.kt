package com.tyson.fishinglogbook.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.Repository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchDetailScreen(
    catchId: Long,
    onBack: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository(AppDatabase.get(ctx).dao()) }
    val item by repo.observeCatch(catchId).collectAsState(initial = null)
    val fmt = remember { SimpleDateFormat("EEE d MMM yyyy, h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { pad ->
        val c = item
        if (c == null) {
            Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
                Text("Catch not found.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back") }
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(c.species, style = MaterialTheme.typography.titleLarge)
            Text(fmt.format(Date(c.timestampMillis)))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Length: ${c.lengthCm?.let { "$it cm" } ?: "—"}")
                    Text("Weight: ${c.weightKg?.let { "$it kg" } ?: "—"}")
                    Text("Lure/Bait: ${c.lure ?: "—"}")
                    Text("Notes: ${c.notes ?: "—"}")
                    Text("Trip: ${c.tripId?.toString() ?: "None"}")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hasGps = c.latitude != null && c.longitude != null
                    Text(if (hasGps) "GPS: %.5f, %.5f (±%sm)".format(c.latitude, c.longitude, c.accuracyM?.toInt() ?: "?") else "GPS: —")

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            enabled = hasGps,
                            onClick = {
                                val uri = Uri.parse("geo:${c.latitude},${c.longitude}?q=${c.latitude},${c.longitude}")
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        ) { Text("Maps") }

                        Button(
                            enabled = hasGps,
                            onClick = {
                                val text = "https://maps.google.com/?q=${c.latitude},${c.longitude}"
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                ctx.startActivity(Intent.createChooser(share, "Share GPS"))
                            }
                        ) { Text("Share") }
                    }
                }
            }

            if (!c.photoUri.isNullOrBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Photo", style = MaterialTheme.typography.titleMedium)
                        AsyncImage(model = Uri.parse(c.photoUri), contentDescription = "Catch photo", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
