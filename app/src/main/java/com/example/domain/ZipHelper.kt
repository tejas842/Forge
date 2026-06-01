package com.example.domain

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipHelper {
    fun zipDirectory(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zipFile(sourceDir, sourceDir, zos)
        }
    }

    private fun zipFile(fileToZip: File, rootDir: File, zos: ZipOutputStream) {
        if (fileToZip.isHidden) return
        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles()
            if (children != null) {
                for (childFile in children) {
                    zipFile(childFile, rootDir, zos)
                }
            }
        } else {
            // Keep the path relative to the root folder
            val relativePath = fileToZip.absolutePath.substring(rootDir.absolutePath.length + 1)
            val zipEntry = ZipEntry(relativePath)
            zos.putNextEntry(zipEntry)
            FileInputStream(fileToZip).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }
}
