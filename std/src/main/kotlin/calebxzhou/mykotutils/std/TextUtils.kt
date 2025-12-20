package calebxzhou.mykotutils.std

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

/**
 * calebxzhou @ 2025-12-19 19:25
 */
fun String.camelToSnakeCase(): String =
    replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace("-", "_")
        .lowercase()

val String.urlEncoded
    get() = URLEncoder.encode(this, Charsets.UTF_8)
val String.urlDecoded
    get() = URLDecoder.decode(this, Charsets.UTF_8)
val String.decodeBase64
    get() = String(Base64.getDecoder().decode(this), Charsets.UTF_8)
val String.encodeBase64
    get() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
// Heuristic for wide code points. Covers:
// - CJK Unified Ideographs & Extensions
// - Hangul syllables & Jamo
// - Hiragana, Katakana, Bopomofo
// - Fullwidth and Halfwidth forms (treat fullwidth as wide)
// - Enclosed CJK, Compatibility Ideographs
// - Common emoji blocks (Emoticons, Misc Symbols & Pictographs, Supplemental Symbols & Pictographs, etc.)
fun Int.isWideCodePoint(): Boolean {
    // Fast path ranges
    return when {
        // CJK Unified Ideographs & Ext
        this in 0x4E00..0x9FFF || this in 0x3400..0x4DBF || this in 0x20000..0x2A6DF || this in 0x2A700..0x2B73F || this in 0x2B740..0x2B81F || this in 0x2B820..0x2CEAF -> true
        // Hangul
        this in 0xAC00..0xD7A3 || this in 0x1100..0x11FF || this in 0x3130..0x318F -> true
        // Hiragana / Katakana / Phonetic extensions
        this in 0x3040..0x309F || this in 0x30A0..0x30FF || this in 0x31F0..0x31FF || this in 0x1B000..0x1B0FF -> true
        // Bopomofo
        this in 0x3100..0x312F || this in 0x31A0..0x31BF -> true
        // Fullwidth forms
        this in 0xFF01..0xFF60 || this in 0xFFE0..0xFFE6 -> true
        // Enclosed / compatibility
        this in 0x3200..0x32FF || this in 0x3300..0x33FF || this in 0xF900..0xFAFF || this in 0x2F800..0x2FA1F -> true
        // Common emoji (approximate). Treat them as wide for alignment.
        this in 0x1F300..0x1F64F || this in 0x1F680..0x1F6FF || this in 0x1F900..0x1F9FF || this in 0x1FA70..0x1FAFF || this in 0x2600..0x26FF || this in 0x2700..0x27BF -> true
        else -> false
    }
}
fun String?.isValidHttpUrl(): Boolean {
    if (this == null)
        return false
    val urlRegex = "^(http://|https://).+".toRegex()
    return this.matches(urlRegex)
}
