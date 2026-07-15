package com.samyak.repostore.data.api

import com.samyak.repostore.data.model.VtFileResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * VirusTotal API v3.
 * Files are looked up by their SHA-256 (or SHA-1 / MD5) hash. The API key is sent
 * via the "x-apikey" header, added by [VirusTotalClient].
 */
interface VirusTotalApi {

    @GET("api/v3/files/{id}")
    suspend fun getFileReport(
        @Path("id") hash: String
    ): VtFileResponse
}
