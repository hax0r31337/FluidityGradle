package me.yuugiri.fluiditygradle.tasks

import me.yuugiri.fluiditygradle.utils.cacheDir
import me.yuugiri.fluiditygradle.utils.generateAndSaveSrgMapping
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

open class TaskPutObfuscationMap: TaskGrouped() {

    @TaskAction
    fun execute() {
        val resourceDir = File(project.buildDir, "resources/main").also {
            if (!it.exists())
                it.mkdirs()
        }
        val cacheDir = cacheDir(project)

        // generate notch-mcp srg mapping
        val srgMapping = File(cacheDir, "1.8.9-mcp.srg").also { generateAndSaveSrgMapping(cacheDir, it) }

        val resources = arrayOf(getGZippedFile(srgMapping))
        resources.forEach { src ->
            val dst = File(resourceDir, src.name)
            if (!dst.exists()) {
                src.copyTo(dst)
            }
        }
    }

    private fun getGZippedFile(originalFile: File): File {
        val file = File(originalFile.parentFile, "${originalFile.name}.gz")
        if (!file.exists()) {
            GZIPOutputStream(FileOutputStream(file)).apply {
                write(originalFile.readBytes())
                close()
            }
        }
        return file
    }

//    private fun getGZippedResource(cacheDir: File, path: String, url: String): File {
//        val file = File(cacheDir, "$path.gz")
//        if (!file.exists()) {
//            val originalFile = resourceCached(cacheDir, path, url)
//            GZIPOutputStream(FileOutputStream(file)).apply {
//                write(originalFile.readBytes())
//                close()
//            }
//        }
//        return file
//    }
}