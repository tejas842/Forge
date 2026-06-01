package com.example.domain

import android.content.Context
import java.io.File

class FileManager(private val context: Context) {
    fun createProjectDir(projectId: String): File {
        val root = File(context.filesDir, "projects")
        if (!root.exists()) root.mkdirs()
        val projectDir = File(root, projectId)
        if (!projectDir.exists()) projectDir.mkdirs()
        return projectDir
    }

    fun listFiles(dir: File): List<File> {
        return dir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    fun readFile(file: File): String {
        return if (file.exists() && file.isFile) file.readText() else ""
    }

    fun writeFile(dir: File, path: String, content: String) {
        val file = File(dir, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun deleteFile(file: File) {
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun createFolder(dir: File, name: String) {
        File(dir, name).mkdirs()
    }
}
