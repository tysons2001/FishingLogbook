package com.tyson.fishinglogbook.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tyson.fishinglogbook.data.AppDatabase
import com.tyson.fishinglogbook.data.Repository
import java.text.SimpleDateFormat
import java.util.*

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
            Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
                Text("No catches yet.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(catches) { c ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenCatch(c.id) }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(c.species, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(fmt.format(Date(c.timestampMillis)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val gps = if (c.latitude != null && c.longitude != null) "GPS: %.5f, %.5f".format(c.latitude, c.longitude) else "GPS: —"
                            Text(gps, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
