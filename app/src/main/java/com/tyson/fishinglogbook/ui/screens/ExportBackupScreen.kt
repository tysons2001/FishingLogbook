package com.tyson.fishinglogbook.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tyson.fishinglogbook.data.AppDatabase
import java.io.File
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBackupScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val dbFile = ctx.getDatabasePath("fishing_logbook.db")

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.putNextEntry(java.util.zip.ZipEntry("fishing_logbook.db"))
                    dbFile.inputStream().copyTo(zip)
                    zip.closeEntry()
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export & Backup") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Export", style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/csv"
            }) {
                Text("Export CSV (next step)")
            }

            Divider()

            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)

            Button(onClick = {
                backupLauncher.launch("fishing_logbook_backup.zip")
            }) {
                Text("Backup database")
            }

            Button(onClick = {
                restoreLauncher.launch(arrayOf("*/*"))
            }) {
                Text("Restore backup")
            }
        }
    }
}
