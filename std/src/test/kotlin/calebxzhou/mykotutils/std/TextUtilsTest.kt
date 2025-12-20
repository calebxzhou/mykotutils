package calebxzhou.mykotutils.std

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextUtilsTest {
    @Test
    fun `camel to snake`() {
        assertEquals("hello_world", "helloWorld".camelToSnakeCase())
        assertEquals("http_request_id", "HTTP-RequestId".camelToSnakeCase())
    }

    @Test
    fun `url encode decode`() {
        val raw = "hello world"
        val encoded = raw.urlEncoded
        assertEquals(raw, encoded.urlDecoded)
    }

    @Test
    fun `base64 roundtrip`() {
        val raw = "kotutils"
        val encoded = raw.encodeBase64
        assertEquals(raw, encoded.decodeBase64)
    }

    @Test
    fun `wide code points`() {
        assertTrue(0x4E00.isWideCodePoint()) // CJK
        assertTrue(0x1F600.isWideCodePoint()) // emoji
        assertFalse(0x41.isWideCodePoint()) // 'A'
    }

    @Test
    fun `http url validation`() {
        assertTrue("https://example.com".isValidHttpUrl())
        assertFalse("ftp://example.com".isValidHttpUrl())
        assertFalse((null as String?).isValidHttpUrl())
    }
}
