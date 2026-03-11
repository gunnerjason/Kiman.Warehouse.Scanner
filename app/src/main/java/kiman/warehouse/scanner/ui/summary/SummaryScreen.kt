package kiman.warehouse.scanner.ui.summary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kiman.warehouse.scanner.model.Group
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
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No active job.")
            }
            return@Scaffold
        }

        val allGroups: List<Group> = job.groups + job.currentGroup
        val totalScans = allGroups.sumOf { it.items.size }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Text(job.name)
            Spacer(Modifier.height(6.dp))
            Text("Total Groups: ${allGroups.count { it.items.isNotEmpty() }}")
            Text("Total Scans: $totalScans")

            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(allGroups) { g ->
                    if (g.items.isEmpty()) return@items
                    Text("Group ${g.index}")
                    g.items.forEach { item ->
                        Text("• ${item.code}")
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}