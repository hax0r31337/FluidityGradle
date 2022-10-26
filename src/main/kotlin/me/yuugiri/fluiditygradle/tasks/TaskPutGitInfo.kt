package me.yuugiri.fluiditygradle.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskPutGitInfo : TaskGrouped() {

    @get:Input
    open var gitFolder: String = project.rootDir.canonicalPath

    @TaskAction
    fun execute() {
        val resourceDir = File(project.buildDir, "resources/main").also {
            if (!it.exists())
                it.mkdirs()
        }
        File(resourceDir, "git.info").writeText(getGitCommitHash(getActualGitFolder()), Charsets.UTF_8)
    }

    private fun getActualGitFolder(gitFolder: File = File(this.gitFolder)): File {
        if (!gitFolder.exists() || !gitFolder.isDirectory) throw IllegalArgumentException("given gitFolder is invalid")

        return if (gitFolder.name == ".git") gitFolder else getActualGitFolder(File(gitFolder, ".git"))
    }

    /**
     * https://gist.github.com/JonasGroeger/7620911
     */
    private fun getGitCommitHash(gitFolder: File): String {
        val head = File(gitFolder, "HEAD").readText(Charsets.UTF_8).split(":") // .git/HEAD
        val isCommit = head.size == 1
        // def isRef = head.length > 1     // ref: refs/heads/master

        if(isCommit) return head[0].trim().take(7)

        val refHead = File(gitFolder, head[1].trim()) // .git/refs/heads/master
        return refHead.readText(Charsets.UTF_8).trim().take(7)
    }
}