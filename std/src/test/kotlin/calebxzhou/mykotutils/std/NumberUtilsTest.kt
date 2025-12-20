package calebxzhou.mykotutils.std

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberUtilsTest {
    @Test
    fun `human file sizes`() {
        assertEquals("999B", 999L.humanFileSize)
        assertEquals("1.0KB", 1024L.humanFileSize)
        assertEquals("1.0MB", (1024L * 1024).humanFileSize)
    }

    @Test
    fun `human speeds`() {
        assertEquals("999B/s", 999.0.humanSpeed)
        assertEquals("1.0KB/s", 1024.0.humanSpeed)
    }

    @Test
    fun `fixed decimals`() {
        assertEquals("3.14", 3.14159f.toFixed(2))
        assertEquals("2.72", 2.71828.toFixed(2))
    }
}
