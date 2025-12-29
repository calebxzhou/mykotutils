package calebxzhou.mykotutils.std

/**
 * calebxzhou @ 2025-12-19 19:28
 */
val javaExePath = ProcessHandle.current()
    .info()
    .command().orElseThrow { IllegalArgumentException("Can't find java process path ") }

fun restart() {
    val runtimeMx = java.lang.management.ManagementFactory.getRuntimeMXBean()
    val jvmArgs = runtimeMx.inputArguments
    val classPath = System.getProperty("java.class.path")
    val sunCommand = System.getProperty("sun.java.command")
        ?: throw IllegalStateException("Can't find main command for restart")

    val mainParts = sunCommand.split(" ").filter { it.isNotBlank() }
    require(mainParts.isNotEmpty()) { "Main command is empty" }

    val mainTarget = mainParts.first()
    val appArgs = mainParts.drop(1)

    val command = mutableListOf<String>()
    command += javaExePath
    command += jvmArgs

    if (mainTarget.endsWith(".jar")) {
        command += listOf("-jar", mainTarget)
    } else {
        command += listOf("-cp", classPath, mainTarget)
    }

    command += appArgs

    ProcessBuilder(command)
        .inheritIO()
        .start()

    kotlin.system.exitProcess(0)
}