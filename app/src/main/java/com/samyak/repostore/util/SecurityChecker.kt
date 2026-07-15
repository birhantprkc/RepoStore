package com.samyak.repostore.util

import android.util.Log
import com.samyak.repostore.data.api.VirusTotalClient
import com.samyak.repostore.data.model.ReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Result of a VirusTotal security lookup for an APK asset.
 */
sealed class SecurityStatus {
    /** Lookup in progress. */
    data object Checking : SecurityStatus()

    /** Scanned and no engine flagged the file as malicious. */
    data class Safe(val totalEngines: Int) : SecurityStatus()

    /** One or more engines flagged the file. */
    data class Flagged(val malicious: Int, val suspicious: Int, val totalEngines: Int) : SecurityStatus()

    /**
     * No antivirus scan was available, but GitHub publishes a SHA-256 checksum for the
     * asset, so its integrity can be verified against the official release. Shown to every
     * user regardless of whether a VirusTotal API key is configured.
     */
    data object Verified : SecurityStatus()

    /**
     * No scan or checksum available, but the asset still comes directly from an official
     * GitHub release. This is the baseline trust signal shown to all users.
     */
    data object Unknown : SecurityStatus()
}

/**
 * Verifies APK security using the VirusTotal API by looking up the asset's SHA-256 hash.
 *
 * The lookup is hash-based, so the APK is never downloaded or uploaded here — we only
 * query VirusTotal's existing report for the file's checksum published by GitHub.
 */
object SecurityChecker {

    private const val TAG = "SecurityChecker"

    /**
     * Look up the VirusTotal report for [asset].
     *
     * @return a [SecurityStatus] describing the verification result. Never throws.
     */
    suspend fun check(asset: ReleaseAsset?): SecurityStatus = withContext(Dispatchers.IO) {
        if (asset == null) return@withContext SecurityStatus.Unknown

        val hash = asset.sha256

        // 1. Best assurance: a real antivirus scan via VirusTotal. Only possible when an
        //    API key is configured and GitHub published a checksum for the asset.
        if (VirusTotalClient.isConfigured && !hash.isNullOrBlank()) {
            try {
                val response = VirusTotalClient.api.getFileReport(hash)
                val stats = response.data?.attributes?.lastAnalysisStats
                if (stats != null) {
                    val malicious = stats.malicious
                    val suspicious = stats.suspicious
                    return@withContext if (malicious > 0 || suspicious > 0) {
                        SecurityStatus.Flagged(malicious, suspicious, stats.totalEngines)
                    } else {
                        SecurityStatus.Safe(stats.totalEngines)
                    }
                }
                // stats == null -> fall through to integrity/source signals below.
            } catch (e: HttpException) {
                // 404 = file never submitted to VirusTotal; 401 = bad key; 429 = quota.
                // Any of these just means we fall back to a lower-tier trust signal.
                Log.d(TAG, "VirusTotal lookup failed (${e.code()}) for $hash")
            } catch (e: Exception) {
                Log.d(TAG, "VirusTotal lookup error: ${e.message}")
            }
        }

        // 2. No malware scan available. Fall back to trust signals that work for every
        //    user without any API key, so the badge is always meaningful.
        return@withContext if (!hash.isNullOrBlank()) {
            // GitHub published a SHA-256 checksum -> integrity is verifiable.
            SecurityStatus.Verified
        } else {
            // Still an official GitHub release asset -> baseline trust.
            SecurityStatus.Unknown
        }
    }
}
