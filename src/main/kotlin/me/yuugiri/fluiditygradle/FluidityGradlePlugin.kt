package me.yuugiri.fluiditygradle

import me.yuugiri.fluiditygradle.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class FluidityGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register("genIntelliJRuns", TaskGenIntelliJRuns::class.java)
        project.tasks.register("cleanMcJarCache", TaskRemoveMinecraftJarCache::class.java)
        project.tasks.register("reobfJar", TaskReobfuscateArtifact::class.java)
        project.tasks.register("stripDebug", TaskStripDebug::class.java)
        project.tasks.register("reobfJarWithStrip", TaskStripDebugAndReobfuscate::class.java)
        project.tasks.register("putObfMap", TaskPutObfuscationMap::class.java)
    }
}