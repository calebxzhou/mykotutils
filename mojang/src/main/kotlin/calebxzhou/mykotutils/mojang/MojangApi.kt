package calebxzhou.mykotutils.mojang

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.Ok
import calebxzhou.mykotutils.std.decodeBase64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.request.url
import kotlinx.serialization.json.Json

object MojangApi {
    private val lgr by Loggers
    suspend fun getUuidFromName(name: String): Result<String> = runCatching {
        HttpClient().use {
            val resp = it.request { url("https://api.mojang.com/users/profiles/minecraft/${name}") }

            data class IdName(val name: String, val id: String)

            val body = resp.body<IdName>()
            return Ok(body.id)
        }
    }

    suspend fun getProfile(uuidNoDash: String): Result<MojangProfileResponse> = runCatching {
        HttpClient().use {
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