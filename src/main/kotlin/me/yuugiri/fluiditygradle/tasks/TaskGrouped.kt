package me.yuugiri.fluiditygradle.tasks

import org.gradle.api.DefaultTask

abstract class TaskGrouped : DefaultTask() {

    init {
        group = "fluidity"
    }
}