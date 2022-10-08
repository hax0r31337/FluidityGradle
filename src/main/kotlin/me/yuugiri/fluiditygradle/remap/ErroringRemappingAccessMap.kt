package me.yuugiri.fluiditygradle.remap

import net.md_5.specialsource.AccessMap
import java.io.File

class ErroringRemappingAccessMap : AccessMap() {
    val brokenLines = mutableMapOf<String, String>()

    override fun loadAccessTransformer(file: File) {
        val reader = file.bufferedReader(Charsets.UTF_8)
        loadAccessTransformer(reader)
        reader.close()
    }

    override fun addAccessChange(symbolStringIn: String, accessString: String) {
        val symbolString = symbolStringIn.replace(' ', '.')
        super.addAccessChange(symbolString, accessString)
        // convert  package.class  to  package/class
        brokenLines[symbolString.replace('.', '/')] = symbolString
    }

    override fun accessApplied(key: String, oldAccess: Int, newAccess: Int) {
        brokenLines.remove(key.replace(" ", ""))
    }
}