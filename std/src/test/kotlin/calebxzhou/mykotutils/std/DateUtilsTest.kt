package calebxzhou.mykotutils.std

import kotlin.test.Test
import kotlin.test.assertTrue

class DateUtilsTest {
    @Test
    fun `date formats are non empty`() {
        val nowDefault = getDateTimeNow()
        assertTrue(nowDefault.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
        assertTrue(humanDateTimeNow.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
    }
}
