package calebxzhou.mykotutils.ktor

import calebxzhou.mykotutils.log.Loggers
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.ContentEncoding
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.ProxySelector
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.text.contains
import kotlin.text.toLongOrNull

/**
 * calebxzhou @ 2025-12-20 11:49
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Double,
) {
    val percent: Double
        get() = if (totalBytes <= 0) -1.0 else bytesDownloaded.toDouble() / totalBytes.toDouble() * 100.0
}

private data class RemoteFileInfo(val contentLength: Long, val acceptRanges: Boolean)

private val lgr by Loggers
private const val MIN_PARALLEL_DOWNLOAD_SIZE = 2L * 1024 * 1024 // 2MB
private const val TARGET_RANGE_CHUNK_SIZE = 2L * 1024 * 1024 // 2MB per chunk
private val MAX_PARALLEL_RANGE_REQUESTS = max(2, Runtime.getRuntime().availableProcessors())
private val httpDlClient
    get() =
        HttpClient(OkHttp) {
            expectSuccess = false
            engine {
                config {
                    followRedirects(true)
                    connectTimeout(10, TimeUnit.SECONDS)
                    readTimeout(0, TimeUnit.SECONDS)
                    ProxySelector.getDefault()?.let {
                        proxySelector(it)
                    }
                }
            }
            BrowserUserAgent()
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 60_000
            }
        }

suspend fun Path.downloadFileFrom(
    url: String,
    headers: Map<String, String> = emptyMap(),
    onProgress: (DownloadProgress) -> Unit
): Result<Path> {
    lgr.info { "Start download file:  $url" }
    val targetPath = this
    val info = fetchRemoteFileInfo(url, headers)
    val canUseRanges = info?.let { it.acceptRanges && it.contentLength >= MIN_PARALLEL_DOWNLOAD_SIZE } == true
    if (canUseRanges) {
        runCatching {
            downloadWithRanges(url, targetPath, info.contentLength, onProgress, headers)
        }.getOrElse { error ->
            lgr.warn(error) { "Parallel download failed, falling back to single stream" }
            downloadSingleStream(url, targetPath, onProgress, headers)
        }
    } else {
        downloadSingleStream(url, targetPath, onProgress, headers)
    }
    return Result.success(this)
}

private suspend fun fetchRemoteFileInfo(url: String, headers: Map<String, String>): RemoteFileInfo? = try {
    val response = httpDlClient.request {
        method = HttpMethod.Head
        url(url)
        headers.forEach { (key, value) ->
            header(key, value)
        }
        timeout {
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }
    if (!response.status.isSuccess()) {
        null
    } else {
        val length = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        val acceptsRanges = response.headers[HttpHeaders.AcceptRanges]?.contains("bytes", ignoreCase = true) == true
        RemoteFileInfo(length, acceptsRanges)
    }
} catch (e: Exception) {
    lgr.warn(e) { "HEAD request failed for $url" }
    null
}

private suspend fun downloadSingleStream(
    url: String,
    targetPath: Path,
    onProgress: (DownloadProgress) -> Unit,
    headers: Map<String, String>
): Boolean {
    return httpDlClient.prepareGet(url) {
        headers.forEach { (key, value) ->
            header(key, value)
        }
        timeout {
            requestTimeoutMillis = 600_000
            socketTimeoutMillis = 600_000
        }
    }.execute { response ->
        if (!response.status.isSuccess()) {
            return@execute false
        }

        val totalBytes = response.contentLength() ?: response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        targetPath.parent?.let { parent ->
            withContext(Dispatchers.IO) {
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent)
                }
            }
        }

        val channel: ByteReadChannel = response.body()
        val buffer = ByteArray(8192)
        var bytesDownloaded = 0L
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime

        withContext(Dispatchers.IO) {
            Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                .use { outputStream ->
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                        if (bytesRead == -1) break
                        if (bytesRead == 0) continue

                        outputStream.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime >= 500) {
                            val elapsedSeconds = (currentTime - startTime) / 1000.0
                            val speed = if (elapsedSeconds > 0) bytesDownloaded / elapsedSeconds else 0.0
                            onProgress(DownloadProgress(bytesDownloaded, totalBytes, speed))
                            lastUpdateTime = currentTime
                        }
                    }

                    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                    val speed = if (elapsedSeconds > 0) bytesDownloaded / elapsedSeconds else 0.0
                    onProgress(DownloadProgress(bytesDownloaded, totalBytes, speed))
                }
        }

        true
    }
}

private suspend fun downloadWithRanges(
    url: String,
    targetPath: Path,
    totalBytes: Long,
    onProgress: (DownloadProgress) -> Unit,
    headers: Map<String, String>
): Boolean {
    targetPath.parent?.let { parent ->
        withContext(Dispatchers.IO) {
            if (!Files.exists(parent)) {
                Files.createDirectories(parent)
            }
        }
    }

    val chunkCount = max(1, ceil(totalBytes.toDouble() / TARGET_RANGE_CHUNK_SIZE).toInt())
    val ranges = buildRanges(totalBytes, chunkCount)
    val parallelism = min(MAX_PARALLEL_RANGE_REQUESTS, ranges.size)
    val progressUpdater = createProgressAggregator(totalBytes, onProgress)

    return withContext(Dispatchers.IO) {
        FileChannel.open(
            targetPath,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        ).use { fileChannel ->
            coroutineScope {
                val semaphore = Semaphore(parallelism)
                val jobs = ranges.map { (start, end) ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            downloadRangeChunk(url, start, end, totalBytes, fileChannel, progressUpdater, headers)
                        }
                    }
                }
                jobs.awaitAll()
            }
            true
        }
    }.also {
        progressUpdater(0, true)
    }
}

private fun buildRanges(totalBytes: Long, chunkCount: Int): List<Pair<Long, Long>> {
    if (totalBytes <= 0) return listOf(0L to -1L)
    val chunkSize = max(1L, totalBytes / chunkCount)
    val ranges = mutableListOf<Pair<Long, Long>>()
    var start = 0L
    while (start < totalBytes) {
        val end = min(totalBytes - 1, start + chunkSize - 1)
        ranges += start to end
        start = end + 1
    }
    return ranges
}

private suspend fun downloadRangeChunk(
    url: String,
    start: Long,
    end: Long,
    totalBytes: Long,
    fileChannel: FileChannel,
    progressUpdater: (Long, Boolean) -> Unit,
    headers: Map<String, String>
) {
    val response = httpDlClient.get(url) {
        headers.forEach { (key, value) ->
            header(key, value)
        }
        header(HttpHeaders.Range, "bytes=$start-$end")
        header(HttpHeaders.AcceptEncoding, "identity")
        timeout {
            requestTimeoutMillis = 600_000
            socketTimeoutMillis = 600_000
        }
    }

    if (start != 0L || (end != totalBytes - 1)) {
        if (response.status != HttpStatusCode.PartialContent) {
            throw IllegalStateException("Server did not honor range request, status=${response.status.value}")
        }
    }

    val channel: ByteReadChannel = response.body()
    val buffer = ByteArray(8192)
    var position = start
    while (!channel.isClosedForRead) {
        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
        if (bytesRead == -1) break
        if (bytesRead == 0) continue

        writeBuffer(fileChannel, buffer, bytesRead, position)
        position += bytesRead
        progressUpdater(bytesRead.toLong(), false)
    }
}

private fun writeBuffer(channel: FileChannel, buffer: ByteArray, length: Int, position: Long) {
    var written = 0
    while (written < length) {
        val byteBuffer = ByteBuffer.wrap(buffer, written, length - written)
        val bytes = channel.write(byteBuffer, position + written)
        written += bytes
    }
}

private fun createProgressAggregator(
    totalBytes: Long,
    onProgress: (DownloadProgress) -> Unit
): (Long, Boolean) -> Unit {
    val downloaded = AtomicLong(0)
    val startTime = System.currentTimeMillis()
    val lastUpdate = AtomicLong(startTime)

    return prog@{ delta, force ->
        if (delta != 0L) {
            downloaded.addAndGet(delta)
        }
        val now = System.currentTimeMillis()
        if (!force) {
            val last = lastUpdate.get()
            if (now - last < 500) {
                return@prog
            }
        }
        lastUpdate.set(now)
        val elapsedSeconds = ((now - startTime) / 1000.0).coerceAtLeast(0.001)
        val total = downloaded.get()
        val speed = if (elapsedSeconds > 0) total / elapsedSeconds else 0.0
        onProgress(DownloadProgress(total, totalBytes, speed))
    }
}
