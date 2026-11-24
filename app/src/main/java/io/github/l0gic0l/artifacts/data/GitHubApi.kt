package io.github.l0gic0l.artifacts.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubException(val code: Int, message: String) : IOException(message)

class GitHubApi(private val token: String) {

    private fun request(url: String) = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .build()

    private inline fun <reified T> get(url: String): T =
        client.newCall(request(url)).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw GitHubException(response.code, body.take(200))
            json.decodeFromString<T>(body)
        }

    suspend fun me(): User = withContext(Dispatchers.IO) { get("$API/user") }

    /** Every repo the token can see, most recently pushed first. */
    suspend fun repos(): List<Repo> = withContext(Dispatchers.IO) {
        val all = mutableListOf<Repo>()
        var page = 1
        while (true) {
            val batch: List<Repo> = get(
                "$API/user/repos?per_page=100&page=$page&sort=pushed" +
                    "&affiliation=owner,collaborator,organization_member"
            )
            all += batch
            if (batch.size < 100) break
            page++
        }
        all
    }

    suspend fun artifacts(fullName: String, perPage: Int = 100): ArtifactsResponse =
        withContext(Dispatchers.IO) {
            get("$API/repos/$fullName/actions/artifacts?per_page=$perPage")
        }

    /**
     * Downloads an artifact zip. GitHub answers with a redirect to blob storage;
     * OkHttp drops the Authorization header when the host changes, as required.
     */
    suspend fun download(url: String, dest: File, onProgress: (read: Long, total: Long) -> Unit) =
        withContext(Dispatchers.IO) {
            client.newCall(request(url)).execute().use { response ->
                if (!response.isSuccessful) {
                    throw GitHubException(response.code, response.body?.string().orEmpty().take(200))
                }
                val body = response.body ?: throw IOException("Empty response")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            onProgress(read, total)
                        }
                    }
                }
            }
        }

    companion object {
        private const val API = "https://api.github.com"
        val json = Json { ignoreUnknownKeys = true }
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
