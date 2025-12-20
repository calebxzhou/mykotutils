package calebxzhou.mykotutils.std

/**
 * calebxzhou @ 2025-12-19 19:28
 */
val javaExePath = ProcessHandle.current()
    .info()
    .command().orElseThrow { IllegalArgumentException("Can't find java process path ") }