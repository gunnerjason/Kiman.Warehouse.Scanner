package kiman.warehouse.scanner.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kiman.warehouse.scanner.viewmodel.ScannerViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kiman.warehouse.scanner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartJobScreen(
    vm: ScannerViewModel,
    onStarted: () -> Unit
) {
    var jobName by remember { mutableStateOf("") }


    Scaffold(
        topBar = { TopAppBar(title = { Text("Start New Job") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_bk),
                contentDescription = "Kiman Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = jobName,
                onValueChange = { jobName = it },
                label = { Text("Job Name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    vm.startJob(jobName)
                    onStarted()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Job")
            }
        }
    }
}
