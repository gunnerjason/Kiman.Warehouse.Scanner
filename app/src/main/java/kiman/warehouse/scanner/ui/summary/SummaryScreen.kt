package kiman.warehouse.scanner.ui.summary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kiman.warehouse.scanner.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    vm: ScannerViewModel,
    onBack: () -> Unit
) {
    val job = vm.job.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Summary") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        if (job == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active job.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Text(job.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            val totalScans = job.groups.sumOf { it.items.size } + job.currentGroup.items.size
            Text("Groups saved: ${job.groups.size}")
            Text("Current group: ${job.currentGroup.index}")
            Text("Total scans: $totalScans")

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(job.groups + job.currentGroup) { g ->
                    Text("Group ${g.index}", style = MaterialTheme.typography.titleMedium)
                    g.items.forEach { item ->
                        Text("• ${item.code}")
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}