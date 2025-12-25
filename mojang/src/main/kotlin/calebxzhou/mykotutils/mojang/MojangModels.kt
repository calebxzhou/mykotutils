package calebxzhou.mykotutils.mojang

import kotlinx.serialization.Serializable

/**
 * calebxzhou @ 2025-12-24 23:12
 */

@Serializable
private data class MojangProfileResponse(
    val id: String,
    val name: String,
    val properties: List<MojangProperty> = emptyList(),
)

@Serializable
private data class MojangProperty(
    val name: String,
    val value: String,
    val signature: String? = null,
)

@Serializable
private data class MojangTexturesPayload(
    val timestamp: Long? = null,
    val profileId: String? = null,
    val profileName: String? = null,
    val textures: Map<String, MojangTexture> = emptyMap(),
)

@Serializable
private data class MojangTexture(
    val url: String,
    val metadata: MojangTextureMetadata? = null,
)

@Serializable
private data class MojangTextureMetadata(
    val model: String? = null,
)