package calebxzhou.mykotutils.hwspec

import kotlinx.serialization.Serializable
import oshi.SystemInfo
import oshi.util.EdidUtil

@Serializable
data class HwSpec(
    val model: String,
    val os: String,
    val cpu: Cpu,
    val gpus: List<Gpu>,
    val mems: List<Memory>,
    val disk: List<Disk>,
    val display: List<Display>,
    val videoMode: List<String>
) {
    @Serializable
    data class Cpu(
        val name: String,
        val cores: Int,
        val threads: Int,
        val frequency: Long, // in Hz
    )

    @Serializable
    data class Gpu(
        val name: String,
        val vram: Long,
    )

    @Serializable
    data class Memory(
        val size: Long,
        val type: String, // e.g., DDR4, LPDDR5
        val speed: Long // in Hz
    )

    @Serializable
    data class Disk(
        val name: String,
        val size: Long,
        val model: String
    )

    @Serializable
    data class Display(
        val name: String,
        val height: Int,
        val width: Int, //cm
    )
    companion object{
        val NOW by lazy { get() }
        fun get(): HwSpec{
            val systemInfo = SystemInfo()
            val hal = systemInfo.hardware
            val model = "${hal.computerSystem.manufacturer} ${hal.computerSystem.model}"
            val os = systemInfo.operatingSystem.run { "$manufacturer $family $versionInfo" }
            val cpu = hal.processor.run {
                HwSpec.Cpu(
                    //amd会在后面加一堆空格 去掉
                    processorIdentifier.name.replace(Regex(" +")," "),
                    physicalProcessorCount,
                    logicalProcessorCount,
                    processorIdentifier.vendorFreq
                )
            }
            val gpu = hal.graphicsCards.map {
                HwSpec.Gpu(it.name, it.vRam)
            }
            val mem = hal.memory.physicalMemory.map {
                HwSpec.Memory(it.capacity, it.memoryType, it.clockSpeed)
            }
            val disk = hal.diskStores.map {
                HwSpec.Disk(it.name, it.size, it.model)
            }


            // Get display resolutions using GLFW as fallback - works in headless mode
            val glfwDisplayModes =  DisplayUtils.getDisplayModes()

            val display = hal.displays.map { it.edid }.mapIndexed { i, edid ->
                val name = EdidUtil.getDescriptors(edid).firstNotNullOfOrNull {
                    when (EdidUtil.getDescriptorType(it)) {
                        0xfc -> EdidUtil.getDescriptorText(it)
                        else -> null
                    }
                } ?: ""
                val hcm = EdidUtil.getHcm(edid)
                val vcm = EdidUtil.getVcm(edid)

                // Try to get mode from GraphicsEnvironment first, then fallback to GLFW


                HwSpec.Display(name, vcm, hcm)
            }
            return HwSpec(
                model,
                os,
                cpu,
                gpu,
                mem,
                disk,
                display,
                glfwDisplayModes
            )
        }
    }
}
