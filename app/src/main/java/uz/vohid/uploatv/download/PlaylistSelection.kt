package uz.vohid.uploatv.download

/** Tanlangan raqamlarni yt-dlp `--playlist-items` formatiga o'tkazadi: [1,2,3,5] -> "1-3,5". */
fun compressIndices(indices: Collection<Int>): String {
    val sorted = indices.distinct().sorted()
    if (sorted.isEmpty()) return ""
    val out = StringBuilder()
    var start = sorted[0]
    var prev = start
    for (i in 1..sorted.size) {
        val cur = sorted.getOrNull(i)
        if (cur != null && cur == prev + 1) {
            prev = cur
            continue
        }
        if (out.isNotEmpty()) out.append(',')
        out.append(if (start == prev) "$start" else "$start-$prev")
        if (cur != null) {
            start = cur
            prev = cur
        }
    }
    return out.toString()
}
