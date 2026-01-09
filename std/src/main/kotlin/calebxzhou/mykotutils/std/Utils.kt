package calebxzhou.mykotutils.std

/**
 * calebxzhou @ 2026-01-09 22:25
 */
inline fun <reified T> Ok(obj: T): Result<T> {
    return Result.success(obj)
}

inline fun <reified T> Err(obj: Throwable): Result<T> {
    return Result.failure(obj)
}