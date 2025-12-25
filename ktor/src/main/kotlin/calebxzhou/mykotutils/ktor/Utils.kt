package calebxzhou.mykotutils.ktor

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * calebxzhou @ 2025-12-20 23:48
 */
fun HttpRequestBuilder.json() = contentType(ContentType.Application.Json)