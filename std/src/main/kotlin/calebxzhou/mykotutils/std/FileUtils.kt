package calebxzhou.mykotutils.std

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.collections.joinToString
import kotlin.io.path.inputStream
import kotlin.jvm.java

/**
 * calebxzhou @ 2025-12-19 19:24
 */

fun File.digest(algo: String): String {
    if(!this.exists()) return "0"
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
val Path.murmur2 get() = runCatching { this.inputStream().murmur2 }
    .getOrElse { if (it is java.io.FileNotFoundException) 0 else throw it }
val File.murmur2 get() = runCatching { this.inputStream().murmur2 }
    .getOrElse { if (it is java.io.FileNotFoundException) 0 else throw it }
val InputStream.murmur2: Long
    get() {
        val multiplex = 1540483477u

        val data = use { it.readBytes() }
        if (data.isEmpty()) return 0
        val normalizedLength = data.count { !it.isWhitespaceCharacter }.toUInt()

        var num2 = 1u xor normalizedLength
        var num3 = 0u
        var num4 = 0

        for (byte in data) {
            if (byte.isWhitespaceCharacter) continue

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

        if (num4 > 0) {
            num2 = (num2 xor num3) * multiplex
        }

        var num6 = (num2 xor (num2 shr 13)) * multiplex
        num6 = num6 xor (num6 shr 15)
        return num6.toLong()
    }
val InputStream.normalizedLength: UInt
    get() {
        var count = 0u
        val buffer = ByteArray(8192)
        use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break

                for (i in 0 until read) {
                    if (!buffer[i].isWhitespaceCharacter) {
                        count += 1u
                    }
                }
            }
        }
        return count
    }
val File.normalizedLength: UInt
    get() = inputStream().use { it.normalizedLength }
val Byte.isWhitespaceCharacter: Boolean
    get() = when (this.toInt() and 0xFF) {
        9, 10, 13, 32 -> true
        else -> false

    }

fun Any.jarResource(path: String): InputStream {
    val cl = Thread.currentThread().contextClassLoader
        ?: this::class.java.classLoader
        ?: ClassLoader.getSystemClassLoader()
    return cl?.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")
}

fun File.exportFromJarResource(path: String): File {
    jarResource(path).use { input ->
        this.outputStream().use { output ->
            input.copyTo(output)
        }
        return this
    }
}

fun File.openChineseZip(): ZipFile {
    var lastError: Throwable = IOException("Unable to open zip file with tried charsets")
    val attempted = mutableListOf<String>()

    fun tryOpen(charset: Charset?): ZipFile? {
        return try {
            if (charset == null) ZipFile(this) else ZipFile(this, charset)
        } catch (ex: Exception) {
            lastError = ex
            attempted += charset?.name() ?: "system-default"
            null
        }
    }

    tryOpen(null)?.let { return it }
    buildList {
        add(StandardCharsets.UTF_8)
        add(Charset.defaultCharset())
        runCatching { add(Charset.forName("GB18030")) }.getOrNull()
        runCatching { add(Charset.forName("GBK")) }.getOrNull()
        add(StandardCharsets.ISO_8859_1)
    }.filterNotNull().distinct().forEach { charset ->
        tryOpen(charset)?.let { return it }
    }
    throw lastError
}
/**
 * Recursively delete a directory and all its contents, but when encountering a symbolic link,
 * only delete the link itself, not the target it points to.
 */
fun File.deleteRecursivelyNoSymlink() {
    val path = this.toPath()

    if (!this.exists()) {
        return
    }

    // If this is a symbolic link, just delete the link itself
    if (Files.isSymbolicLink(path)) {
        Files.delete(path)
        return
    }

    // If it's a directory, recursively delete its contents first
    if (this.isDirectory) {
        this.listFiles()?.forEach { child ->
            child.deleteRecursivelyNoSymlink()
        }
    }

    // Finally delete this file/directory
    this.delete()
}