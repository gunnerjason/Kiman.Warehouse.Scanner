package kiman.warehouse.scanner.model

data class Group(val index: Int, val items: MutableList<ScanItem> = mutableListOf())