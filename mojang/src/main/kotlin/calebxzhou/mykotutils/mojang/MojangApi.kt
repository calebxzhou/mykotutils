package calebxzhou.mykotutils.mojang

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.Ok
import calebxzhou.mykotutils.std.decodeBase64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object MojangApi {
    private val lgr by Loggers
    private val httpClient get() = HttpClient {
        install(ContentNegotiation){
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    suspend fun getUuidFromName(name: String): Result<String?> = runCatching {
        httpClient.use {
            val resp = it.request { url("https://api.mojang.com/users/profiles/minecraft/${name}") }
            @Serializable
            data class IdName(val name: String, val id: String)
            if(resp.status.value==404) return Ok(null)
            val body = resp.body<IdName>()
            return Ok(body.id)
        }
    }

    suspend fun getProfile(uuidNoDash: String): Result<MojangProfileResponse> = runCatching {
        httpClient.use {
            val resp = it.request { url("https://sessionserver.mojang.com/session/minecraft/profile/$uuidNoDash") }
            val profile = resp.body<MojangProfileResponse>()
            profile
        }
    }
    val MojangProfileResponse.textures
        get() = properties
            .firstOrNull { it.name.equals("textures", ignoreCase = true) }
            ?.value?.decodeBase64?.let { Json.decodeFromString<MojangTexturesPayload>(it) }?.textures?: emptyMap()

}