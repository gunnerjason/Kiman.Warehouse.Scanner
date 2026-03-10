package kiman.warehouse.scanner.model

data class Job(
    val name: String,
    val startedAtMs: Long,
    val groups: MutableList<Group> = mutableListOf(),
    var currentGroup: Group
)