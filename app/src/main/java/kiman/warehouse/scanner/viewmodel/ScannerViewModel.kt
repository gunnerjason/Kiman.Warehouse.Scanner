package kiman.warehouse.scanner.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kiman.warehouse.scanner.model.Group
import kiman.warehouse.scanner.model.Job
import kiman.warehouse.scanner.model.ScanItem

class ScannerViewModel : ViewModel() {

    val job = mutableStateOf<Job?>(null)
    val status = mutableStateOf("Idle")

    private val scannedCodes = mutableSetOf<String>()

    fun startJob(nameOrBlank: String) {
        val name = nameOrBlank.ifBlank { "Job-${System.currentTimeMillis()}" }
        job.value = Job(
            name = name,
            startedAtMs = System.currentTimeMillis(),
            groups = mutableListOf(),
            currentGroup = Group(1)
        )
        scannedCodes.clear()
        status.value = "Job started: $name"
    }

    fun newGroup() {
        val current = job.value ?: run { status.value = "No active job."; return }
        if (current.currentGroup.items.isNotEmpty()) {
            current.groups.add(current.currentGroup)
        }
        val nextIndex = current.currentGroup.index + 1
        current.currentGroup = Group(nextIndex)
        job.value = current.copy(groups = current.groups, currentGroup = current.currentGroup)
        status.value = "New group #$nextIndex"
    }

    fun finalizeForExport(): Job? {
        val current = job.value ?: return null
        if (current.currentGroup.items.isNotEmpty()) {
            current.groups.add(current.currentGroup)
            current.currentGroup = Group(current.currentGroup.index + 1)
        }
        job.value = current.copy(groups = current.groups, currentGroup = current.currentGroup)
        return current
    }

    fun clearJob() {
        job.value = null
        scannedCodes.clear()
        status.value = "Cleared."
    }

    fun scan(code: String): ScanResult {
        val current = job.value ?: run {
            status.value = "No active job."
            return ScanResult.NoJob
        }

        if (scannedCodes.contains(code)) {
            status.value = "Duplicate: $code"
            return ScanResult.Duplicate(code)
        }

        scannedCodes.add(code)
        current.currentGroup.items.add(ScanItem(code, System.currentTimeMillis()))
        job.value = current.copy(groups = current.groups, currentGroup = current.currentGroup)
        status.value = "Scanned: $code (group ${current.currentGroup.index})"
        return ScanResult.Success(code)
    }
}

sealed class ScanResult {
    data object NoJob : ScanResult()
    data class Success(val code: String) : ScanResult()
    data class Duplicate(val code: String) : ScanResult()
}