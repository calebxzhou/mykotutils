package calebxzhou.mykotutils.std

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * calebxzhou @ 2025-12-19 19:38
 */
val dateTimeNow
    get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
val humanDateTimeNow
    get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))