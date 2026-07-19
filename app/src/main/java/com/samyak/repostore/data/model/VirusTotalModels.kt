package com.samyak.repostore.data.model

import com.google.gson.annotations.SerializedName

/**
 * Models for the VirusTotal API v3 file report endpoint.
 * See: https://docs.virustotal.com/reference/file-info
 */
data class VtFileResponse(
    val data: VtFileData?
)

data class VtFileData(
    val id: String?,
    val type: String?,
    val attributes: VtFileAttributes?
)

data class VtFileAttributes(
    @SerializedName("last_analysis_stats") val lastAnalysisStats: VtAnalysisStats?,
    @SerializedName("meaningful_name") val meaningfulName: String?,
    @SerializedName("reputation") val reputation: Int? = null
)

/**
 * Remote pool of VirusTotal API keys, served as JSON from a private config repo.
 * Example: https://raw.githubusercontent.com/sammax21/security/main/api_keys.json
 *
 * Multiple keys let the app keep scanning after a single key hits its daily quota:
 * [VirusTotalKeyManager] rotates to the next enabled key on an HTTP 429/401.
 */
data class VtKeysConfig(
    val virustotal: List<VtApiKey> = emptyList()
)

data class VtApiKey(
    val id: Int = 0,
    val key: String? = null,
    val enabled: Boolean = false,
    @SerializedName("dailyQuota") val dailyQuota: Int = 0,
    val remaining: Int = 0,
    val used: Int = 0
)

data class VtAnalysisStats(
    val harmless: Int = 0,
    val malicious: Int = 0,
    val suspicious: Int = 0,
    val undetected: Int = 0,
    val timeout: Int = 0,
    @SerializedName("confirmed-timeout") val confirmedTimeout: Int = 0,
    val failure: Int = 0,
    @SerializedName("type-unsupported") val typeUnsupported: Int = 0
) {
    /** Total number of antivirus engines that returned a usable verdict. */
    val totalEngines: Int
        get() = harmless + malicious + suspicious + undetected
}
