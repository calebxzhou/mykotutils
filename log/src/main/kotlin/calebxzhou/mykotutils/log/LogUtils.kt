package calebxzhou.mykotutils.log

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * calebxzhou @ 2025-12-19 19:42
 */
object Loggers {
    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>
    ): ReadOnlyProperty<Any?, KLogger> {
        val owner = thisRef
        val logger = when (owner) {
            null -> KotlinLogging.logger(prop.name)
            is Class<*> -> KotlinLogging.logger(owner.name)
            else -> KotlinLogging.logger(owner::class.java.name)
        }
        return ReadOnlyProperty { _, _ -> logger }
    }
}