import me.yuugiri.fluiditygradle.minecraft.MinecraftVersion
import me.yuugiri.fluiditygradle.remap.ErroringRemappingAccessMap
import me.yuugiri.fluiditygradle.utils.cacheDir
import me.yuugiri.fluiditygradle.utils.generateAndSaveSrgMapping
import me.yuugiri.fluiditygradle.utils.minecraftJar
import me.yuugiri.fluiditygradle.utils.resourceCached
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.RemapperProcessor
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.Project
import java.io.BufferedReader
import java.io.File


fun minecraftDep(project: Project, vararg accessTransformer: String): String {
    val minecraftJar = minecraftJar(project)
    if (!minecraftJar.exists()) {
        val cacheDir = cacheDir(project)
        val version = MinecraftVersion(resourceCached(cacheDir, "versions/1.8.9-forge1.8.9-11.15.1.2318-1.8.9.json", "https://ayanoyuugiri.github.io/resources/minecraft/1.8.9-forge1.8.9-11.15.1.2318-1.8.9.json"))
        val srgMapping = File(cacheDir, "1.8.9-mcp.srg").also { generateAndSaveSrgMapping(cacheDir, it) }

        val accessMap = ErroringRemappingAccessMap()
        accessTransformer.forEach { accessMap.loadAccessTransformer(File(project.rootDir, it)) }
        val input = Jar.init(version.getJars(cacheDir, "scala-lang"))
        val mapping = JarMapping()
        mapping.loadMappings(BufferedReader(srgMapping.reader(Charsets.UTF_8)), null, null, false)

        // ensure that inheritance provider is used
        val inheritanceProviders = JointProvider()
        inheritanceProviders.add(JarProvider(input))
        mapping.setFallbackInheritanceProvider(inheritanceProviders)

        val remapper = JarRemapper(RemapperProcessor(null, mapping, null), mapping, RemapperProcessor(null, null, accessMap))
        remapper.remapJar(input, minecraftJar)

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