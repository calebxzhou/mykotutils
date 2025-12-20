package calebxzhou.mykotutils.std

/**
 * calebxzhou @ 2025-12-19 19:23
 */

//保留小数点后x位
fun Float.toFixed(decPlaces: Int): String {
    return String.format("%.${decPlaces}f", this)
}

fun Double.toFixed(decPlaces: Int): String {
    return this.toFloat().toFixed(decPlaces)
}
val Long.humanFileSize: String
    get() {
        val bytes= this
        if (bytes < 1024) return "${bytes}B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1fKB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1fMB".format(mb)
        val gb = mb / 1024.0
        return "%.1fGB".format(gb)
    }
val Int.humanSize: String
    get() = toLong().humanFileSize
val Double.humanSpeed:String
    get() {
        val bytesPerSecond = this
        if (bytesPerSecond < 1024) return "%.0fB/s".format(bytesPerSecond)
        val kbps = bytesPerSecond / 1024.0
        if (kbps < 1024) return "%.1fKB/s".format(kbps)
        val mbps = kbps / 1024.0
        if (mbps < 1024) return "%.1fMB/s".format(mbps)
        val gbps = mbps / 1024.0
        return "%.1fGB/s".format(gbps)
    }