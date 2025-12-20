package calebxzhou.mykotutils.std

import kotlin.test.Test
import kotlin.test.assertTrue

class ProcessUtilsTest {
    @Test
    fun `java exe path present`() {
        assertTrue(javaExePath.isNotBlank())
    }
}
