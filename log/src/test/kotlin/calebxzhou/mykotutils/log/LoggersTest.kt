package calebxzhou.mykotutils.log

import io.github.oshai.kotlinlogging.KLogger
import kotlin.test.Test
import kotlin.test.assertNotNull

class LoggersTest {
    private class Demo {
        val log by Loggers
    }

    @Test
    fun `delegate returns logger`() {
        val logger = Demo().log
        assertNotNull(logger)
    }
}
