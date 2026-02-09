package calebxzhou.mykotutils.std

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * calebxzhou @ 2026-01-09 22:25
 */
val ioScope get() = CoroutineScope(Dispatchers.IO)