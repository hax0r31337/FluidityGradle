package me.yuugiri.fluiditygradle.tasks

import me.yuugiri.fluiditygradle.remap.ClassInheritanceManager
import me.yuugiri.fluiditygradle.utils.*
import minecraftDep
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


fun generateReobfuscateMapping(project: Project): Map<String, Map<String, String>> {
    val cacheDir = cacheDir(project)
    val result = mutableMapOf<String, MutableMap<String, String>>()
    val mapping = File(cacheDir, "1.8.9-remap-v2.dat")
    if (mapping.exists()) {
        var nowClass = ""
        mapping.readLines(Charsets.UTF_8).forEach {
            val args = it.split(" ")
            if (args[0].startsWith("C:")) {
                result[args[1]] = mutableMapOf()
                nowClass = args[1]
            } else {
                result[nowClass]!![args[0]] = args[1]
            }
        }
        return result
    }

    project.logger.info("Generating re-obfuscate mapping cache...")

    val srgMapping = resourceCached(cacheDir, "1.8.9.srg", "https://ayanoyuugiri.github.io/resources/srg/1.8.9/joined.srg")
    val fieldsCsvMapping = resourceCached(cacheDir, "fields-22.csv", "https://ayanoyuugiri.github.io/resources/srg/1.8.9/fields.csv")
    val methodsCsvMapping = resourceCached(cacheDir, "methods-22.csv", "https://ayanoyuugiri.github.io/resources/srg/1.8.9/methods.csv")

    val fields = readCsvMapping(fieldsCsvMapping.reader(Charsets.UTF_8))
    val methods = readCsvMapping(methodsCsvMapping.reader(Charsets.UTF_8))
    val csm = ClassInheritanceManager(minecraftJar(project).also {
        if (!it.exists()) minecraftDep(project)
    })
    srgMapping.readLines(Charsets.UTF_8).forEach {
        val args = it.split(" ")
        fun classMap(klass: String) = result[klass] ?: mutableMapOf<String, String>().also { result[klass] = it }
        when (args[0]) {
            "FD:" -> {
                val fd = args[2]
                val klass = fd.substring(0, fd.lastIndexOf('/'))
                val field = fd.substring(fd.lastIndexOf('/')+1)
                val str = "${fields[field]}"
                classMap(klass)[str] = field
                csm.searchChildTree(klass).forEach { klass ->
                    classMap(klass)[str] = field
                }
            }
            "MD:" -> {
                val md = args[3]
                val klass = md.substring(0, md.lastIndexOf('/'))
                val method = md.substring(md.lastIndexOf('/')+1)
                val str = "${methods[method]}${args[4]}"
                classMap(klass)[str] = method
                csm.searchChildTree(klass).forEach { klass ->
                    classMap(klass)[str] = method
                }
            }
        }
    }

    csm.clear()

    val sb = StringBuilder()
    result.forEach { (klass, info) ->
        sb.append("C: $klass\n")
        info.forEach { (orig, mapped) ->
            sb.append("$orig $mapped\n")
        }
    }
    mapping.writeText(sb.toString(), Charsets.UTF_8)

    return result
}

open class TaskReobfuscateArtifact : TaskGrouped() {

    @get:Internal
    val mapping by lazy { generateReobfuscateMapping(project) }

    @TaskAction
    fun execute() {
        val rootDir = File(project.buildDir, "libs")
        rootDir.listFiles()?.forEach {
            if (it.name.endsWith(".jar")) {
                logger.info("Patching ${it.absolutePath}...")
                patchJar(it)
            }
        }
    }

    private fun patchJar(file: File) {
        val resources = mutableMapOf<String, ByteArray>()
        val classes = mutableListOf<ByteArray>()
        val classMngr = ClassInheritanceManager()

        // read jar data
        val jis = ZipInputStream(FileInputStream(file))
        val buffer = ByteArray(1024)
        while (true) {
            val entry = jis.nextEntry ?: break
            if (entry.isDirectory) continue
            val bos = ByteArrayOutputStream()
            var n: Int
            while (jis.read(buffer).also { n = it } != -1) {
                bos.write(buffer, 0, n)
            }
            bos.close()
            val body = bos.toByteArray()
            if (entry.name.endsWith(".class")) {
                classMngr.addClass(toClassNode(body))
                classes.add(body)
            } else {
                resources[entry.name] = body
            }
        }
        jis.close()

        val jos = ZipOutputStream(FileOutputStream(file))
        resources.forEach { (name, bytes) ->
            jos.putNextEntry(ZipEntry(name))
            jos.write(bytes)
            jos.closeEntry()
        }
        resources.clear()
        classes.forEach {
            val klass = toClassNode(it)
            reobfuscateClass(klass, classMngr)
            additionalRuns(klass)

            jos.putNextEntry(ZipEntry(klass.name+".class"))
            jos.write(toBytes(klass))
            jos.closeEntry()
        }
        mergedMaps.clear()
        classMngr.clear()
        jos.close()
    }

    private val mergedMaps = mutableMapOf<String, MergedMap<String, String>?>()
    private val emptyMap = MergedMap<String, String>(emptyList())

    private fun getMergedMap(name: String, classMngr: ClassInheritanceManager): MergedMap<String, String> {
        if(mergedMaps.containsKey(name)) {
            return mergedMaps[name] ?: emptyMap
        }
        val parents = classMngr.searchParentTree(name).mapNotNull { mapping[it] }
        val mergedMap = if (parents.isNotEmpty()) {
            MergedMap(parents)
        } else null
        mergedMaps[name] = mergedMap
        return mergedMap ?: emptyMap
    }

    private fun reobfuscateClass(klass: ClassNode, classMngr: ClassInheritanceManager) {
        val mergedMap = getMergedMap(klass.name, classMngr)
        klass.methods.forEach {
            mergedMap["${it.name}${it.desc}"]?.also { str ->
                it.name = str
            }
            reobfuscateMethod(it, classMngr)
        }
    }

    private fun reobfuscateMethod(method: MethodNode, classMngr: ClassInheritanceManager) {
        method.instructions.forEach { insn ->
            if (insn is MethodInsnNode) {
                getMergedMap(insn.owner, classMngr)["${insn.name}${insn.desc}"]?.also {
                    insn.name = it
                }
            } else if (insn is FieldInsnNode) {
                getMergedMap(insn.owner, classMngr)[insn.name]?.also {
                    insn.name = it
                }
            }
        }
    }

    protected open fun additionalRuns(klass: ClassNode) {

    }
}