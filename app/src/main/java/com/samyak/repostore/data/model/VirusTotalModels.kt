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
