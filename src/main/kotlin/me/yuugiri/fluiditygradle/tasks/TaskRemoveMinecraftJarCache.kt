package me.yuugiri.fluiditygradle.tasks

import me.yuugiri.fluiditygradle.utils.minecraftJar
import org.gradle.api.tasks.TaskAction


/**
 * used to refresh access transformer
 */
open class TaskRemoveMinecraftJarCache : GroupedTask() {

    @TaskAction
    fun execute() {
        val minecraftJar = minecraftJar(project)
        if (minecraftJar.exists()) minecraftJar.delete()
    }
}