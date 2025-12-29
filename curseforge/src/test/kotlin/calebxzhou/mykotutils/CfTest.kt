package calebxzhou.mykotutils

import calebxzhou.mykotutils.curseforge.CurseForgeApi
import kotlinx.coroutines.runBlocking
import kotlin.test.Test


class CfTest {
    @Test
    fun getModInfo(): Unit = runBlocking{
        CurseForgeApi.getModFileInfo(351264,7291067)?.let {
            println("Mod Info: $it")
        }
    }
}