package com.filerenamer.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.filerenamer.data.FileItem
import com.filerenamer.data.RenameOperation
import com.filerenamer.data.RenameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * 列出目录下的文件和文件夹（仅当前层级）
     * 在 IO 线程执行
     */
    suspend fun listDirectory(context: Context, uri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            if (documentFile != null && documentFile.exists() && documentFile.isDirectory) {
                val children = documentFile.listFiles()
                for (child in children) {
                    val name = child.name ?: "未知"
                    items.add(
                        FileItem(
                            uri = child.uri,
                            name = name,
                            isDirectory = child.isDirectory,
                            size = if (!child.isDirectory) child.length() else 0,
                            lastModified = child.lastModified()
                        )
                    )
                }
            } else {
                Log.w(TAG, "DocumentFile is null, doesn't exist, or is not a directory: $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing directory: $uri", e)
        }
        // 按名称排序：目录在前，文件在后
        items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    /**
     * 批量重命名 - 在 IO 线程执行
     * 使用父目录的 DocumentFile 来操作子文件，避免 fromSingleUri 的问题
     */
    suspend fun batchRename(
        context: Context,
        parentUri: Uri,
        items: List<FileItem>,
        operation: RenameOperation
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var successCount = 0
        var failCount = 0

        // 获取父目录 DocumentFile
        val parentDir = DocumentFile.fromTreeUri(context, parentUri)
        if (parentDir == null || !parentDir.exists()) {
            return@withContext Pair(0, items.size)
        }

        for (item in items) {
            try {
                // 通过父目录查找文件（更可靠的方式）
                val file = parentDir.findFile(item.name)
                if (file == null || !file.exists()) {
                    Log.e(TAG, "File not found in parent: ${item.name}")
                    failCount++
                    continue
                }

                val newName = computeNewName(item.name, file.isDirectory, operation)
                if (newName == null || newName == item.name) {
                    failCount++
                    continue
                }

                val result = file.renameTo(newName)
                if (result) successCount++ else failCount++
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming ${item.name}", e)
                failCount++
            }
        }

        Pair(successCount, failCount)
    }

    /**
     * 计算新文件名
     */
    private fun computeNewName(
        currentName: String,
        isDirectory: Boolean,
        operation: RenameOperation
    ): String? {
        return when (operation.type) {
            RenameType.ADD_PREFIX -> {
                "${operation.text}$currentName"
            }
            RenameType.ADD_SUFFIX -> {
                val dotIndex = currentName.lastIndexOf('.')
                if (dotIndex > 0 && !isDirectory) {
                    val baseName = currentName.substring(0, dotIndex)
                    val extension = currentName.substring(dotIndex)
                    "$baseName${operation.text}$extension"
                } else {
                    "$currentName${operation.text}"
                }
            }
            RenameType.REMOVE_FIRST_N -> {
                val count = operation.charCount
                if (count <= 0) return null
                if (count >= currentName.length) return null  // 不能全删完

                if (isDirectory) {
                    currentName.substring(count)
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (count >= baseName.length) return null
                        "${baseName.substring(count)}$extension"
                    } else {
                        currentName.substring(count)
                    }
                }
            }
            RenameType.REMOVE_LAST_N -> {
                val count = operation.charCount
                if (count <= 0) return null
                if (count >= currentName.length) return null

                if (isDirectory) {
                    currentName.substring(0, currentName.length - count)
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (count >= baseName.length) return null
                        "${baseName.substring(0, baseName.length - count)}$extension"
                    } else {
                        currentName.substring(0, currentName.length - count)
                    }
                }
            }
            RenameType.REPLACE_FIRST_N -> {
                val count = operation.charCount
                val replacement = operation.text
                if (count <= 0) return null
                if (count >= currentName.length) return null

                if (isDirectory) {
                    "$replacement${currentName.substring(count)}"
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (count >= baseName.length) return null
                        "$replacement${baseName.substring(count)}$extension"
                    } else {
                        "$replacement${currentName.substring(count)}"
                    }
                }
            }
            RenameType.REPLACE_LAST_N -> {
                val count = operation.charCount
                val replacement = operation.text
                if (count <= 0) return null
                if (count >= currentName.length) return null

                if (isDirectory) {
                    "${currentName.substring(0, currentName.length - count)}$replacement"
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (count >= baseName.length) return null
                        "${baseName.substring(0, baseName.length - count)}$replacement$extension"
                    } else {
                        "${currentName.substring(0, currentName.length - count)}$replacement"
                    }
                }
            }
        }
    }
}
