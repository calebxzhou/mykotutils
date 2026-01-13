package calebxzhou.mykotutils.std

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * calebxzhou @ 2026-01-09 22:25
 */
inline fun <reified T> Ok(obj: T): Result<T> {
    return Result.success(obj)
}
fun Ok(): Result<Unit> {
    return Result.success(Unit)
}

inline fun <reified T> Err(obj: Throwable): Result<T> {
    return Result.failure(obj)
}
val ioScope get() = CoroutineScope(Dispatchers.IO)