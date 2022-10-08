package me.yuugiri.fluiditygradle.utils

import me.yuugiri.fluiditygradle.remap.Remapper
import java.io.File
import java.io.Reader

fun readCsvMapping(mapping: Reader, export: MutableMap<String, String> = mutableMapOf()): MutableMap<String, String> {
    mapping.readLines().forEach {
        val args = it.split(",")
        if (args[0] == "searge" && args.size >= 2) return@forEach
        export[args[0]] = args[1]
    }
    return export
}

fun generateAndSaveSrgMapping(cacheDir: File, outFile: File) {
    if (outFile.exists()) return
    val srgMapping = resourceCached(cacheDir, "1.8.9.srg", "https://ayanoyuugiri.github.io/resources/srg/1.8.9/joined.srg")
    val fieldsCsvMapping = resourceCached(cacheDir, "fields-22.csv", "https://ayanoyuugiri.github.io/resources/srg/1.8.9/fields.csv")
    val methodsCsvMapping = resourceCached(cacheDir, "methods-22.csv", "https://ayanoyuugiri.github.io/resources/srg/1.8.9/methods.csv")
    outFile.writeText(Remapper()
        .applyWarppedSrg(srgMapping.reader(Charsets.UTF_8), fieldsCsvMapping.reader(Charsets.UTF_8), methodsCsvMapping.reader(Charsets.UTF_8))
        .srgMapping(), Charsets.UTF_8)
}