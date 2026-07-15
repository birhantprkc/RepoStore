package com.samyak.gitcore.util

import java.util.Locale

/**
 * Utility for computing and formatting the total number of downloads for an app.
 *
 * GitHub reports a download count per release asset. The "total app downloads"
 * is the sum of those per-asset counts. This helper is model-agnostic (works on
 * raw integers) so it stays reusable across the GitCore module without depending
 * on the app's data models.
 */
object DownloadCounter {

    /**
     * Sum a collection of per-asset download counts into a single total.
     * Uses [Long] to avoid overflow when a popular repo has millions of downloads.
     */
    fun total(downloadCounts: Iterable<Int>): Long {
        return downloadCounts.sumOf { it.coerceAtLeast(0).toLong() }
    }

    /**
     * Format a download total into a compact, human-readable string.
     * Examples: 0 -> "0", 999 -> "999", 1_500 -> "1.5K", 2_300_000 -> "2.3M".
     */
    fun format(count: Long): String {
        return when {
            count >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    /**
     * Convenience: sum and format in one step.
     */
    fun totalFormatted(downloadCounts: Iterable<Int>): String {
        return format(total(downloadCounts))
    }
}
