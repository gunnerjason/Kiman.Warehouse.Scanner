package kiman.warehouse.scanner.ui.start

@Composable
fun StartJobScreen(
    vm: ScannerViewModel,
    onStarted: () -> Unit
) {
    var jobName by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Start New Job") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
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