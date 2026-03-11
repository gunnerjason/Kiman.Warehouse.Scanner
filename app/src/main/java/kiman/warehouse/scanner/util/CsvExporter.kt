package kiman.warehouse.scanner.util

fun escapeCsv(value: String): String {
    val needsQuotes = value.contains(",") || value.contains("\n") || value.contains("\"")
    return if (!needsQuotes) value
    else "\"" + value.replace("\"", "\"\"") + "\""
}
