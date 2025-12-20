package calebxzhou.mykotutils.std

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FileUtilsTest {
    private val tempFile = kotlin.io.path.createTempFile(prefix = "kotutils-test").toFile()

    @AfterTest
    fun cleanup() {
        tempFile.delete()
    }

    @Test
    fun `digests match known values`() {
        tempFile.writeText("hello")
        assertEquals("5d41402abc4b2a76b9719d911017c592", tempFile.md5)
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", tempFile.sha256)
    }

    @Test
    fun `murmur2 is deterministic and ignores whitespace`() {
        tempFile.writeText("abc\n")
        val h1 = tempFile.murmur2
        tempFile.writeText("a b c\n")
        val h2 = tempFile.murmur2
        assertEquals(h1, h2)
    }

    @Test
    fun `normalized length ignores whitespace`() {
        tempFile.writeText("a b\n c")
        val length = tempFile.computeNormalizedLength()
        assertEquals(3u, length)
    }

    @Test
    fun `jar resource missing throws`() {
        assertFailsWith<IllegalArgumentException> {
            this.jarResource("nope.txt")
        }
    }
}
