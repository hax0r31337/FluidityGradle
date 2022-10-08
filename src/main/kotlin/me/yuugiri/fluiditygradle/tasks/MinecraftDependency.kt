//package me.yuugiri.fluiditygradle.tasks

import me.yuugiri.fluiditygradle.minecraft.MinecraftVersion
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.RemapperProcessor
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.TaskAction
import me.yuugiri.fluiditygradle.remap.ErroringRemappingAccessMap
import me.yuugiri.fluiditygradle.tasks.GroupedTask
import me.yuugiri.fluiditygradle.utils.cacheDir
import me.yuugiri.fluiditygradle.utils.generateAndSaveSrgMapping
import me.yuugiri.fluiditygradle.utils.minecraftJar
import me.yuugiri.fluiditygradle.utils.resourceCached
import java.io.BufferedReader
import java.io.File

fun minecraftDep(project: Project, vararg accessTransformer: String): String {
    val minecraftJar = minecraftJar(project)
    if (!minecraftJar.exists()) {
        val cacheDir = cacheDir(project)
        val version = MinecraftVersion(resourceCached(cacheDir, "versions/1.8.9.json", "https://launchermeta.mojang.com/v1/packages/d546f1707a3f2b7d034eece5ea2e311eda875787/1.8.9.json"))
        val srgMapping = File(cacheDir, "1.8.9-mcp.srg").also { generateAndSaveSrgMapping(cacheDir, it) }

        val accessMap = ErroringRemappingAccessMap() // TODO: consider use original AccessMap when csv support is unused
        accessTransformer.forEach { accessMap.loadAccessTransformer(File(project.rootDir, it)) }
        val mapping = JarMapping()
        mapping.loadMappings(BufferedReader(srgMapping.reader(Charsets.UTF_8)), null, null, false)
        val remapper = JarRemapper(RemapperProcessor(null, mapping, null), mapping, RemapperProcessor(null, null, accessMap))
        remapper.remapJar(Jar.init(version.getJars(cacheDir)), minecraftJar)

        // check for broken accessMap
        if (accessMap.brokenLines.isNotEmpty()) {
            project.logger.error("${accessMap.brokenLines.size} broken Access Transformer lines:")
            accessMap.brokenLines.values.forEach {
                project.logger.error(" --- $it")
            }
            minecraftJar.delete()
            throw RuntimeException("One of your Access Transformers are broke!")
        }
    }
    project.rootProject.allprojects {
        project.repositories.flatDir {
            it.name = "ReplicaMcRepo"
            it.dir(File(project.rootDir, ".gradle/fluidity/repo"))
        }
    }
    return "me.yuugiri:minecraftbin:1.8.9-${project.rootProject.name}"
}

fun launchWrapper(project: Project): ConfigurableFileCollection {
    return project.files(resourceCached(cacheDir(project), "libraries/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar", "https://ayanoyuugiri.github.io/resources/libs/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar"))
}

fun minecraftforge(project: Project): ConfigurableFileCollection {
    return project.files(resourceCached(cacheDir(project), "libraries/net/me.yuugiri.fluiditygradle.tasks.minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9.jar", "https://ayanoyuugiri.github.io/resources/libs/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9.jar"))
}