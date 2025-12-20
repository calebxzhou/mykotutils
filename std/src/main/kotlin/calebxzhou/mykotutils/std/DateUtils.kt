package calebxzhou.mykotutils.std

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * calebxzhou @ 2025-12-19 19:38
 */
const val DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
private fun getDateTimeFormatter(pattern: String = DEFAULT_DATE_TIME_PATTERN) = DateTimeFormatter.ofPattern(pattern)
fun getDateTimeNow(pattern: String = DEFAULT_DATE_TIME_PATTERN) = LocalDateTime.now().format(getDateTimeFormatter(pattern))
val humanDateTimeNow
    get() = getDateTimeNow(DEFAULT_DATE_TIME_PATTERN)
val Int.secondsToHumanDateTime: String
    get() = Instant.ofEpochSecond(this.toLong()).atZone(ZoneId.systemDefault()).format(getDateTimeFormatter(DEFAULT_DATE_TIME_PATTERN))
val Long.millisToHumanDateTime: String
    get() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(getDateTimeFormatter(DEFAULT_DATE_TIME_PATTERN))
