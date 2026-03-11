package kiman.warehouse.scanner.util

import android.content.Context
import android.net.Uri
import kiman.warehouse.scanner.model.Job
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun export(context: Context, job: Job, uri: Uri) {

        context.contentResolver.openOutputStream(uri)?.use { out ->
            BufferedWriter(OutputStreamWriter(out)).use { bw ->

                bw.append("job_name,job_started_at,group_index,scan_index,timestamp_iso,code\n")

                val sdf = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.US
                )

                for (g in job.groups) {
                    var index = 1
                    for (item in g.items) {
                        bw.append(
                            "${job.name},${job.startedAtMs},${g.index},$index," +
                                    "${sdf.format(Date(item.timestampMs))},${escape(item.code)}\n"
                        )
                        index++
                    }
                }

                bw.flush()
            }
        }
    }

    private fun escape(value: String): String {
        val needsQuotes =
            value.contains(",") ||
                    value.contains("\n") ||
                    value.contains("\"")

        return if (!needsQuotes) value
        else "\"" + value.replace("\"", "\"\"") + "\""
    }
}