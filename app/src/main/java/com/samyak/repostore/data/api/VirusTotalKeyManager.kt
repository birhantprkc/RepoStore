package com.samyak.repostore.data.api

import android.util.Log
import com.google.gson.Gson
import com.samyak.repostore.data.model.VtKeysConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Holds a pool of VirusTotal API keys fetched from a remote JSON config and rotates
 * between them so scanning survives a single key exhausting its daily quota.
 *
 * The keys are pulled once (lazily) from [KEYS_URL] and cached in memory for the process
 * lifetime. When a request fails with HTTP 429 (quota) or 401 (bad key), the caller can
 * [rotate] to the next enabled key and retry.
 */
object VirusTotalKeyManager {

    private const val TAG = "VtKeyManager"

    /** Remote pool of keys. Update this URL to point at your own config repo. */
    private const val KEYS_URL =
        "https://raw.githubusercontent.com/sammax21/security/main/api_keys.json"

    private val gson = Gson()
    private val mutex = Mutex()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var keys: List<String> = emptyList()

    @Volatile
    private var index: Int = 0

    @Volatile
    private var loaded: Boolean = false

    /** True once at least one usable key has been loaded from the remote config. */
    val hasKeys: Boolean
        get() = keys.isNotEmpty()

    /** The key that should be attached to the next VirusTotal request, or null if none. */
    fun currentKey(): String? = keys.getOrNull(index)

    /**
     * Fetch the key pool from the remote config if it has not been loaded yet.
     * Safe to call from multiple coroutines; only the first triggers a network request.
     */
    suspend fun ensureLoaded() {
        if (loaded && keys.isNotEmpty()) return
        refresh()
    }

    /** Force a re-fetch of the key pool from the remote config. */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (loaded && keys.isNotEmpty()) return@withLock
            try {
                val request = Request.Builder()
                    .url(KEYS_URL)
                    .header("Accept", "application/json")
                    .header("User-Agent", "GitHubAppStore-Android")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.d(TAG, "Key config fetch failed: HTTP ${response.code}")
                        return@withLock
                    }
                    val body = response.body?.string().orEmpty()
                    val config = gson.fromJson(body, VtKeysConfig::class.java)
                    keys = config?.virustotal
                        ?.filter { it.enabled && it.remaining > 0 && !it.key.isNullOrBlank() }
                        ?.mapNotNull { it.key }
                        ?: emptyList()
                    index = 0
                    loaded = true
                    Log.d(TAG, "Loaded ${keys.size} VirusTotal key(s)")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Key config fetch error: ${e.message}")
            }
        }
    }

    /**
     * Advance to the next available key after the current one failed (quota/auth).
     *
     * @return true if another key is available to retry with, false if the pool is exhausted.
     */
    fun rotate(): Boolean {
        if (keys.isEmpty()) return false
        if (index + 1 >= keys.size) return false
        index++
        Log.d(TAG, "Rotated to VirusTotal key #${index + 1}/${keys.size}")
        return true
    }
}
