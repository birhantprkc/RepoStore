package com.samyak.repostore.data.api

import com.samyak.repostore.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Dedicated Retrofit client for the VirusTotal API.
 *
 * Kept separate from [RetrofitClient] so the VirusTotal API key is only ever sent to
 * virustotal.com and never leaks to GitHub (principle of least privilege).
 */
object VirusTotalClient {

    private const val BASE_URL = "https://www.virustotal.com/"

    /**
     * True when at least one key is available. Keys are loaded from the remote pool
     * ([VirusTotalKeyManager]); a build-time [BuildConfig.VIRUSTOTAL_API_KEY] still works
     * as a fallback for local/dev builds.
     */
    val isConfigured: Boolean
        get() = VirusTotalKeyManager.hasKeys || BuildConfig.VIRUSTOTAL_API_KEY.isNotBlank()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "GitHubAppStore-Android")

                // Only attach the key to VirusTotal requests.
                val host = request.url.host.lowercase()
                if (host == "virustotal.com" || host.endsWith(".virustotal.com")) {
                    // Prefer a key from the rotating remote pool; fall back to the
                    // build-time key for local/dev builds.
                    val key = VirusTotalKeyManager.currentKey()
                        ?.takeIf { it.isNotBlank() }
                        ?: BuildConfig.VIRUSTOTAL_API_KEY
                    if (key.isNotBlank()) {
                        builder.addHeader("x-apikey", key)
                    }
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val api: VirusTotalApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VirusTotalApi::class.java)
    }
}
