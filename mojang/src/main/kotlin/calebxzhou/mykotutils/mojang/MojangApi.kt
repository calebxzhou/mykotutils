package calebxzhou.mykotutils.mojang

import calebxzhou.mykotutils.std.decodeBase64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.request.url

object MojangApi {
    suspend fun getUuidFromName(name: String): String? {
        try {
            val resp = HttpClient().request{url( "https://api.mojang.com/users/profiles/minecraft/${name}")}
            data class IdName(val name: String,val id: String)
            val body = resp.body<IdName>()
            return body.id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /*suspend fun getCloth(uuid: String): RAccount.Cloth? {
        return try {
            val resp = httpRequest { url("https://sessionserver.mojang.com/session/minecraft/profile/$uuid") }
            val profile = resp.body<MojangProfileResponse>()
            val texturesValue = profile.properties.firstOrNull { it.name.equals("textures", ignoreCase = true) }?.value
                ?: return null

            val decodedTextures = try {
                texturesValue.decodeBase64
            } catch (ex: IllegalArgumentException) {
                throw IllegalStateException("Failed to decode textures payload", ex)
            }

            val payload = serdesJson.decodeFromString<MojangTexturesPayload>(decodedTextures)
            val textures = payload.textures
            val skin = textures["SKIN"] ?: return null

            val cloth = RAccount.Cloth(
                isSlim = skin.metadata?.model.equals("slim", ignoreCase = true),
                skin = skin.url,
            )
            textures["CAPE"]?.let { cloth.cape = it.url }
            cloth
        } catch (e: Exception) {
            lgr.warn( "Failed to fetch Mojang cloth for uuid=$uuid" )
            e.printStackTrace()
            null
        }
    }*/

}