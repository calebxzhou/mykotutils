package calebxzhou.mykotutils.curseforge

import calebxzhou.mykotutils.ktor.DownloadProgress
import calebxzhou.mykotutils.ktor.downloadFileFrom
import calebxzhou.mykotutils.ktor.json
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.murmur2
import calebxzhou.mykotutils.std.openChineseZip
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.nio.file.AccessDeniedException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.math.max
import kotlin.math.min

object CurseForgeApi {
    //if true, query from the mirror first  if fail then fallback to official, if false always query official
    val useMirror = true
    private val lgr by Loggers
    private val MAX_PARALLEL_DOWNLOADS = max(4, Runtime.getRuntime().availableProcessors() * 4)
    private val MIRROR_URL = "https://mod.mcimirror.top/curseforge/v1"
    const val OFFICIAL_URL = "https://api.curseforge.com/v1"
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
    @VisibleForTesting
    private suspend fun makeRequest(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        body: Any? = null,
        ignoreMirror: Boolean = MIRROR_URL == null
    ): HttpResponse {
        suspend fun doRequest(base: String) = httpClient.request {
            url("${base}/${path}")
            json()
            header(
                "x-api-key", byteArrayOf(
                    36, 50, 97, 36, 49, 48, 36, 55, 87, 87, 86, 49, 87, 69, 76, 99, 119, 88, 56, 88,
                    112, 55, 100, 54, 56, 77, 72, 115, 46, 53, 103, 114, 84, 121, 90, 86, 97, 54,
                    83, 121, 110, 121, 101, 83, 121, 77, 104, 49, 114, 115, 69, 56, 57, 110, 73,
                    97, 48, 57, 122, 79
                ).let { String(it) })
            body?.let { setBody(it) }
            this.method = method
        }
        if (ignoreMirror || !useMirror) {
            return doRequest(OFFICIAL_URL)
        }

        val mirrorResult = runCatching { doRequest(MIRROR_URL) }
        mirrorResult.getOrNull()?.let { response ->
            if (response.status.isSuccess()) return response

            val body = response.bodyAsText()
            lgr.warn { "CurseForge mirror request failed, falling back to official API: $body" }
        }

        mirrorResult.exceptionOrNull()?.let { ex ->
            lgr.warn(ex) { "CurseForge mirror request with exception, falling back to official API: ${ex.message}" }
        }

        return doRequest(OFFICIAL_URL)
    }

    /**
     * matches a list of murmur2 fingerprints against CurseForge's database
     * @param hashes List of Long fingerprints in murmur2 format
     * @return CurseForgeFingerprintData containing exact matches, partial matches, and unmatched fingerprints
     */
    suspend fun matchFingerprintData(hashes: List<Long>): CurseForgeFingerprintData {
        @Serializable
        data class CurseForgeFingerprintRequest(val fingerprints: List<Long>)

        val response = makeRequest(
            //432 for minecraft
            "fingerprints/432",
            HttpMethod.Post,
            CurseForgeFingerprintRequest(fingerprints = hashes)
        ).body<CurseForgeFingerprintResponse>()
        val data = response.data ?: CurseForgeFingerprintData()
        lgr.debug {
            "CurseForge: ${data.exactMatches.size} exact matches, ${data.partialMatches.size} partial matches, ${data.unmatchedFingerprints.size} unmatched"
        }

        return data
    }

    private suspend fun requestModFiles(fileIds: List<Int>, official: Boolean = false): List<CurseForgeFile> {
        @Serializable
        data class CFFileIdsRequest(val fileIds: List<Int>)

        return makeRequest(
            "mods/files",
            HttpMethod.Post,
            CFFileIdsRequest(fileIds),
            ignoreMirror = official,
        ).body<CurseForgeFileListResponse>().data
    }

    private suspend fun requestMods(modIds: List<Int>, official: Boolean = false): List<CurseForgeModInfo> {
        @Serializable
        data class CFModsRequest(val modIds: List<Int>, val filterPcOnly: Boolean = true)

        @Serializable
        data class CFModsResponse(val data: List<CurseForgeModInfo>? = null)
        return makeRequest(
            "mods",
            HttpMethod.Post,
            CFModsRequest(modIds),
            official
        ).body<CFModsResponse>().data!!.filter { it.isMod }

    }

    //从mod project id列表获取cf mod信息
    suspend fun getModsInfo(modIds: List<Int>): List<CurseForgeModInfo> {

        if (modIds.isEmpty()) return emptyList()

        val mods = requestMods(modIds).toMutableList()
        val foundIds = mods.mapTo(mutableSetOf()) { it.id }
        val missingIds = modIds.filterNot { foundIds.contains(it) }
        if (missingIds.isNotEmpty()) {
            lgr.warn { "not found ids：${missingIds}, retry official api" }
            mods += requestMods(missingIds, true)
        }

        return mods
    }

    suspend fun getModFileInfo(modId: Int, fileId: Int): CurseForgeFile? {
        return makeRequest("mods/${modId}/files/${fileId}").body<CurseForgeFileResponse>().data
    }

    suspend fun getModFilesInfo(fileIds: List<Int>): List<CurseForgeFile> {
        if (fileIds.isEmpty()) return emptyList()

        val files = requestModFiles(fileIds).toMutableList()
        val foundIds = files.mapTo(mutableSetOf()) { it.id }
        val missingIds = fileIds.filterNot { foundIds.contains(it) }
        if (missingIds.isNotEmpty()) {
            lgr.warn { "not found ids：${missingIds}, retry official api" }
            files += requestModFiles(missingIds, true)
        }
        return files
    }

    fun parseModpack(zipFile: File): Result<CurseForgePackManifest> {
        zipFile.openChineseZip().use { zip ->
            val manifestEntry = zip.getEntry("manifest.json")
                ?: throw FileNotFoundException("Invalid modpack zip: manifest.json not found")
            val manifestJson = zip.getInputStream(manifestEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val manifest = runCatching {
                Json.decodeFromString<CurseForgePackManifest>(manifestJson)
            }.getOrElse {
                throw SerializationException("manifest.json parse failed: ${it.message}", it)
            }
            return Result.success(manifest)
        }
    }
    suspend fun downloadMod(
        mod:CFDownloadMod,
        onProgress: (DownloadProgress) -> Unit
    ): Result<Path> {
        val fileinfo = getModFileInfo(
            mod.projectId,
            mod.fileId
        ) ?: throw AccessDeniedException("cant get file fingerprint for mod ${mod.projectId}/${mod.fileId}")
        val hash = fileinfo.fileFingerprint
        if(mod.path.exists()&&mod.path.murmur2==hash){
            lgr.debug { "mod file already exists and fingerprint matches: ${mod.path}" }
            return Result.success(mod.path)
        }
        val officialUrl = fileinfo.realDownloadUrl
        val mirrorUrl = if (useMirror) officialUrl
            .replace("edge.forgecdn.net", "mod.mcimirror.top")
            .replace("mediafilez.forgecdn.net", "mod.mcimirror.top")
            .replace("media.forgecdn.net", "mod.mcimirror.top") else officialUrl

        suspend fun attempt(url: String, label: String): Result<Path> = runCatching {
            val dlPath = mod.path.downloadFileFrom(url, onProgress = onProgress).getOrElse { throw it }
            if (dlPath.murmur2 != hash) {
                throw IllegalAccessException("downloaded mod file ${mod} fingerprint mismatch: expected $hash, got ${dlPath.murmur2}")
            }
            dlPath
        }.onFailure { err ->
            if (label == "mirror") {
                lgr.warn(err) { "mirror download failed for ${mod.projectId}/${mod.fileId}, will retry official" }
            }
        }

        val mirrorResult = if (useMirror) attempt(mirrorUrl, "mirror") else null
        val finalResult = when {
            mirrorResult == null -> attempt(officialUrl, "official")
            mirrorResult.isSuccess -> mirrorResult
            else -> attempt(officialUrl, "official")
        }

        return finalResult
    }
    suspend fun downloadMods(
        mods: Collection<CFDownloadMod>,
        onProgress: (CFDownloadMod, DownloadProgress) -> Unit
    ): Result<List<CFDownloadMod>> {
        if (mods.isEmpty()) return Result.success(emptyList())

        val parallelism = min(MAX_PARALLEL_DOWNLOADS, mods.size)
        val semaphore = Semaphore(parallelism)
        val downloaded = mutableListOf<CFDownloadMod>()
        val failures = mutableListOf<Pair<CFDownloadMod, Throwable>>()

        coroutineScope {
            mods.map { mod ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        mod to runCatching { downloadMod(mod) { onProgress(mod, it) }.getOrThrow() }
                    }
                }
            }.awaitAll()
                .forEach { (mod, result) ->
                    result
                        .onSuccess { downloaded.add(mod) }
                        .onFailure { error -> failures += mod to error }
                }
        }

        return if (failures.isEmpty()) {
            Result.success(downloaded)
        } else {
            Result.failure(CFDownloadModException(failures.toList()))
        }
    }

}