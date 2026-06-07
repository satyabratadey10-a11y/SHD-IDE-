package com.example.data

import com.example.data.model.FileNode
import java.io.File

class FileRepository {

    suspend fun loadFileTree(rootPath: String): List<FileNode> {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return emptyList()

        return getNodesRecursively(rootDir, depth = 0, maxDepth = 10)
    }

    private fun getNodesRecursively(dir: File, depth: Int, maxDepth: Int): List<FileNode> {
        if (depth >= maxDepth) return emptyList()
        val files = dir.listFiles() ?: return emptyList()

        return files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { file ->
                FileNode(
                    name = file.name,
                    absolutePath = file.absolutePath,
                    isDirectory = file.isDirectory,
                    depth = depth,
                    children = if (file.isDirectory) getNodesRecursively(file, depth + 1, maxDepth) else emptyList(),
                    isExpanded = false
                )
            }
    }

    suspend fun readFileContent(absolutePath: String): String {
        val file = File(absolutePath)
        return if (file.exists() && file.isFile) {
            file.readText()
        } else ""
    }

    suspend fun writeFileContent(absolutePath: String, content: String) {
        val file = File(absolutePath)
        file.writeText(content)
    }

    suspend fun createFile(parentPath: String, name: String): Result<Unit> {
        val parent = File(parentPath)
        if (!parent.exists()) parent.mkdirs()
        val file = File(parent, name)
        return try {
            if (file.createNewFile()) Result.success(Unit)
            else Result.failure(Exception("File already exists"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(parentPath: String, name: String): Result<Unit> {
        val parent = File(parentPath)
        val folder = File(parent, name)
        return try {
            if (folder.mkdirs()) Result.success(Unit)
            else Result.failure(Exception("Could not create folder"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameNode(oldPath: String, newName: String): Result<Unit> {
        val oldFile = File(oldPath)
        val parent = oldFile.parentFile
        val newFile = File(parent, newName)
        return try {
            if (oldFile.renameTo(newFile)) Result.success(Unit)
            else Result.failure(Exception("Rename failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNode(absolutePath: String): Result<Unit> {
        val file = File(absolutePath)
        return try {
            if (file.deleteRecursively()) Result.success(Unit)
            else Result.failure(Exception("Delete failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
