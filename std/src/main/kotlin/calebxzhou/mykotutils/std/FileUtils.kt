package calebxzhou.mykotutils.std

import sun.jvm.hotspot.debugger.amd64.AMD64ThreadContext.RDI
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.collections.joinToString
import kotlin.jvm.java

/**
 * calebxzhou @ 2025-12-19 19:24
 */

fun File.digest(algo: String): String {
    val digest = MessageDigest.getInstance(algo)
    inputStream().use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
val File.sha1: String
    get() = digest("SHA-1")
val File.sha256: String
    get() = digest("SHA-256")
val File.md5: String
    get() = digest("MD5")
val File.sha512: String
    get() = digest("SHA-512")
val File.murmur2: Long
    get() {
        val multiplex = 1540483477u
        val normalizedLength = computeNormalizedLength()

        var num2 = 1u xor normalizedLength
        var num3 = 0u
        var num4 = 0

        val buffer = ByteArray(8192)
        inputStream().use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break

                for (i in 0 until read) {
                    val byte = buffer[i]
                    if (byte.isWhitespaceCharacter()) continue

                    val value = (byte.toInt() and 0xFF).toUInt()
                    num3 = num3 or (value shl num4)
                    num4 += 8

                    if (num4 == 32) {
                        val num6 = num3 * multiplex
                        val num7 = (num6 xor (num6 shr 24)) * multiplex
                        num2 = num2 * multiplex xor num7
                        num3 = 0u
                        num4 = 0
                    }
                }
            }
        }

        if (num4 > 0) {
            num2 = (num2 xor num3) * multiplex
        }

        var num6 = (num2 xor (num2 shr 13)) * multiplex
        num6 = num6 xor (num6 shr 15)
        return num6.toLong()
    }

fun File.computeNormalizedLength(): UInt {
    var count = 0u
    val buffer = ByteArray(8192)
    inputStream().use { stream ->
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break

            for (i in 0 until read) {
                if (!buffer[i].isWhitespaceCharacter()) {
                    count += 1u
                }
            }
        }
    }
    return count
}

fun Byte.isWhitespaceCharacter(): Boolean {
    return when (this.toInt() and 0xFF) {
        9, 10, 13, 32 -> true
        else -> false
    }
}
fun Any.jarResource(path: String): InputStream = this::class.java.classLoader.getResourceAsStream(path)?:run{
    throw IllegalArgumentException("Resource not found: $path")}

fun File.exportFromJarResource(path: String): File {
    jarResource(path).use { input ->
        this.outputStream().use { output ->
            input.copyTo(output)
        }
        return this
    }
}
