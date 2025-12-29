package calebxzhou.mykotutils.modrinth

import calebxzhou.mykotutils.ktor.json
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.log.Loggers.provideDelegate
import calebxzhou.mykotutils.std.sha1
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.max

object ModrinthApi {
    //if true, query from the mirror first  if fail then fallback to official, if false always query official
    val useMirror = true
    private val lgr by Loggers
    private val MAX_PARALLEL_DOWNLOADS = max(4, Runtime.getRuntime().availableProcessors() * 4)
    const val BASE_URL = "https://mod.mcimirror.top/modrinth/v2"
    const val OFFICIAL_URL = "https://api.modrinth.com/v2"
    private val httpClient
        get() =
            HttpClient(OkHttp) {
                BrowserUserAgent()
                install(ContentNegotiation){
                    json(Json {
                        ignoreUnknownKeys = true
                    })
                }
            }
    suspend fun mrreq(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, Any>? = null,
        body: Any? = null
    ): HttpResponse {
        suspend fun doRequest(base: String) = httpClient.request {
            url("${base}/${path}")
            json()
            body?.let { setBody(it) }
            params?.forEach { parameter(it.key, it.value) }
            this.method = method
        }

        val mirrorResult = runCatching<HttpResponse> { doRequest(BASE_URL) }
        val mirrorResponse = mirrorResult.getOrNull()
        if (mirrorResponse != null && mirrorResponse.status.isSuccess()) {
            return mirrorResponse
        } else {
            lgr.warn( "Modrinth mirror fail，${mirrorResponse?.status},${mirrorResponse?.bodyAsText()}" )
        }

        mirrorResult.exceptionOrNull()?.let {
            lgr.warn("Modrinth mirror request failed, falling back to official API: ${it.message}")
        } 
        val officialResponse = doRequest(OFFICIAL_URL)
        return officialResponse
    }
    //ID / slugs
    suspend fun List<String>.mapModrinthProjects(): List<ModrinthProject> {
        val normalizedIds = asSequence()
            .distinct()
            .toList()

        val chunkSize = 50
        val projects = mutableListOf<ModrinthProject>()

        normalizedIds.chunked(chunkSize).forEach { chunk ->
            val response = mrreq("projects", params = mapOf("ids" to Json.encodeToString(chunk)))
                .body<List<ModrinthProject>>()
            projects += response

            if (response.size != chunk.size) {
                val missing = chunk.toSet() - response.map { it.id }.toSet() - response.map { it.slug }.toSet()
                if (missing.isNotEmpty()) {
                    lgr.debug("Modrinth: ${missing.size} ids from chunk unmatched: ${missing.joinToString()}")
                }
            }
        }

        lgr.info("Modrinth: fetched ${projects.size} projects for ${normalizedIds.size} requested ids")

        return projects
    }

    suspend fun List<File>.mapModrinthVersions(): Map<String, ModrinthVersionInfo> {
        val hashes = map { it.sha1 }
        val response = mrreq(
            "version_files",
            method = HttpMethod.Post,
            body = ModrinthVersionLookupRequest(hashes = hashes, algorithm = "sha1")
        )
            .body<Map<String, ModrinthVersionInfo>>()

        val missing = hashes.filter { it !in response }
        if (missing.isNotEmpty()) {
            lgr.info("Modrinth: ${response.size} matches, ${missing.size} hashes unmatched")
        } else {
            lgr.info("Modrinth: matched all ${response.size} hashes")
        }

        return response
    }

}