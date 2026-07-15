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

/**
 * 重命名结果
 */
data class RenameResult(
    val successCount: Int,
    val failCount: Int,
    val failReasons: List<String> = emptyList()
)

object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * 列出目录下的文件和文件夹（仅当前层级）
     * 在 IO 线程执行
     *
     * 优化：使用批量查询减少 DocumentFile 调用次数
     */
    suspend fun listDirectory(context: Context, uri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            if (documentFile != null && documentFile.exists() && documentFile.isDirectory) {
                val children = documentFile.listFiles()
                // 预分配容量
                // items.ensureCapacity(children.size) - not available in Kotlin
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
     * 使用父目录的 DocumentFile 来操作子文件
     *
     * @param onProgress 进度回调，参数为 (当前处理序号, 总数)
     */
    suspend fun batchRename(
        context: Context,
        parentUri: Uri,
        items: List<FileItem>,
        operation: RenameOperation,
        onProgress: ((Int, Int) -> Unit)? = null
    ): RenameResult = withContext(Dispatchers.IO) {
        var successCount = 0
        var failCount = 0
        val failReasons = mutableListOf<String>()

        // 获取父目录 DocumentFile
        val parentDir = DocumentFile.fromTreeUri(context, parentUri)
        if (parentDir == null || !parentDir.exists()) {
            return@withContext RenameResult(0, items.size, listOf("无法访问父目录"))
        }

        // 计算 id 号的补零位数
        val idPadLen = when {
            items.size < 10 -> 1
            items.size < 100 -> 2
            items.size < 1000 -> 3
            items.size < 10000 -> 4
            else -> 5
        }

        val total = items.size

        for ((index, item) in items.withIndex()) {
            // 报告进度
            onProgress?.invoke(index + 1, total)

            try {
                // 通过父目录查找文件
                val file = parentDir.findFile(item.name)
                if (file == null || !file.exists()) {
                    val reason = "「${item.name}」找不到该文件"
                    Log.e(TAG, reason)
                    failCount++
                    failReasons.add(reason)
                    continue
                }

                // id 号从 1 开始
                val id = index + 1
                val idStr = id.toString().padStart(idPadLen, '0')

                val newNameResult = computeNewName(item.name, file.isDirectory, operation, idStr)
                if (newNameResult == null) {
                    val reason = "「${item.name}」参数错误，无法计算新文件名"
                    failCount++
                    failReasons.add(reason)
                    continue
                }
                if (!newNameResult.isSuccess) {
                    val reason = "「${item.name}」${newNameResult.errorMsg}"
                    failCount++
                    failReasons.add(reason)
                    continue
                }
                val newName = newNameResult.name
                if (newName == item.name) {
                    val reason = "「${item.name}」新文件名与原名相同，无需修改"
                    failCount++
                    failReasons.add(reason)
                    continue
                }

                val result = file.renameTo(newName)
                if (result) {
                    successCount++
                } else {
                    val reason = "「${item.name}」重命名失败（可能权限不足或文件名冲突）"
                    failCount++
                    failReasons.add(reason)
                }
            } catch (e: Exception) {
                val reason = "「${item.name}」${e.message ?: "未知错误"}"
                Log.e(TAG, "Error renaming ${item.name}", e)
                failCount++
                failReasons.add(reason)
            }
        }

        RenameResult(successCount, failCount, failReasons)
    }

    /**
     * 计算新文件名结果
     */
    private data class NewNameResult(
        val name: String,
        val isSuccess: Boolean = true,
        val errorMsg: String = ""
    )

    /**
     * 计算新文件名
     */
    private fun computeNewName(
        currentName: String,
        isDirectory: Boolean,
        operation: RenameOperation,
        idStr: String = ""
    ): NewNameResult? {
        return when (operation.type) {
            RenameType.ADD_PREFIX -> {
                NewNameResult("${operation.text}$currentName")
            }
            RenameType.ADD_SUFFIX -> {
                val dotIndex = currentName.lastIndexOf('.')
                if (dotIndex > 0 && !isDirectory) {
                    val baseName = currentName.substring(0, dotIndex)
                    val extension = currentName.substring(dotIndex)
                    NewNameResult("$baseName${operation.text}$extension")
                } else {
                    NewNameResult("$currentName${operation.text}")
                }
            }
            RenameType.REMOVE_FIRST_N -> {
                val count = operation.charCount
                if (count <= 0) return null
                if (count >= currentName.length) {
                    return NewNameResult("", false, "文件名总长度(${currentName.length}) ≤ 要删除的字符数($count)，无法删除")
                }

                if (isDirectory) {
                    NewNameResult(currentName.substring(count))
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (count >= baseName.length) {
                            return NewNameResult("", false, "文件名主名长度(${baseName.length}) ≤ 要删除的字符数($count)，无法删除")
                        }
                        NewNameResult("${baseName.substring(count)}$extension")
                    } else {
                        NewNameResult(currentName.substring(count))
                    }
                }
            }
            RenameType.REMOVE_LAST_N -> {
                val count = operation.charCount
                if (count <= 0) return null
                if (count >= currentName.length) {
                    return NewNameResult("", false, "文件名总长度(${currentName.length}) ≤ 要删除的字符数($count)，无法删除")
                }

                if (isDirectory) {
                    NewNameResult(currentName.substring(0, currentName.length - count))
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (count >= baseName.length) {
                            return NewNameResult("", false, "文件名主名长度(${baseName.length}) ≤ 要删除的字符数($count)，无法删除")
                        }
                        NewNameResult("${baseName.substring(0, baseName.length - count)}$extension")
                    } else {
                        NewNameResult(currentName.substring(0, currentName.length - count))
                    }
                }
            }
            RenameType.REPLACE_FIRST_N -> {
                val count = operation.charCount
                val replacement = operation.text
                if (count <= 0) return null
                if (count >= currentName.length) {
                    return NewNameResult("", false, "文件名总长度(${currentName.length}) ≤ 要替换的字符数($count)，无法替换")
                }

                if (isDirectory) {
                    NewNameResult("$replacement${currentName.substring(count)}")
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (count >= baseName.length) {
                            return NewNameResult("", false, "文件名主名长度(${baseName.length}) ≤ 要替换的字符数($count)，无法替换")
                        }
                        NewNameResult("$replacement${baseName.substring(count)}$extension")
                    } else {
                        NewNameResult("$replacement${currentName.substring(count)}")
                    }
                }
            }
            RenameType.REPLACE_LAST_N -> {
                val count = operation.charCount
                val replacement = operation.text
                if (count <= 0) return null
                if (count >= currentName.length) {
                    return NewNameResult("", false, "文件名总长度(${currentName.length}) ≤ 要替换的字符数($count)，无法替换")
                }

                if (isDirectory) {
                    NewNameResult("${currentName.substring(0, currentName.length - count)}$replacement")
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (count >= baseName.length) {
                            return NewNameResult("", false, "文件名主名长度(${baseName.length}) ≤ 要替换的字符数($count)，无法替换")
                        }
                        NewNameResult("${baseName.substring(0, baseName.length - count)}$replacement$extension")
                    } else {
                        NewNameResult("${currentName.substring(0, currentName.length - count)}$replacement")
                    }
                }
            }

            RenameType.INSERT_AT_N_FROM_START -> {
                val pos = operation.position
                val insertText = operation.text
                if (pos < 0) return NewNameResult("", false, "插入位置($pos)不能为负数")
                if (insertText.isEmpty()) return NewNameResult("", false, "插入文本不能为空")

                if (isDirectory) {
                    if (pos > currentName.length) return NewNameResult("", false, "插入位置($pos)超出文件名长度(${currentName.length})")
                    NewNameResult("${currentName.substring(0, pos)}$insertText${currentName.substring(pos)}")
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (pos > baseName.length) return NewNameResult("", false, "插入位置($pos)超出文件名主名长度(${baseName.length})")
                        NewNameResult("${baseName.substring(0, pos)}$insertText${baseName.substring(pos)}$extension")
                    } else {
                        if (pos > currentName.length) return NewNameResult("", false, "插入位置($pos)超出文件名长度(${currentName.length})")
                        NewNameResult("${currentName.substring(0, pos)}$insertText${currentName.substring(pos)}")
                    }
                }
            }
            RenameType.DELETE_AT_N_FROM_START -> {
                val pos = operation.position
                val count = operation.charCount
                if (pos < 0) return NewNameResult("", false, "删除起始位置($pos)不能为负数")
                if (count <= 0) return NewNameResult("", false, "删除字符数($count)必须大于0")

                if (isDirectory) {
                    if (pos + count > currentName.length) return NewNameResult("", false, "从位置${pos}删除${count}个字符超出文件名长度(${currentName.length})")
                    NewNameResult("${currentName.substring(0, pos)}${currentName.substring(pos + count)}")
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (pos + count > baseName.length) return NewNameResult("", false, "从位置${pos}删除${count}个字符超出文件名主名长度(${baseName.length})")
                        NewNameResult("${baseName.substring(0, pos)}${baseName.substring(pos + count)}$extension")
                    } else {
                        if (pos + count > currentName.length) return NewNameResult("", false, "从位置${pos}删除${count}个字符超出文件名长度(${currentName.length})")
                        NewNameResult("${currentName.substring(0, pos)}${currentName.substring(pos + count)}")
                    }
                }
            }
            RenameType.INSERT_AT_N_FROM_END -> {
                val pos = operation.position
                val insertText = operation.text
                if (pos < 0) return NewNameResult("", false, "插入位置($pos)不能为负数")
                if (insertText.isEmpty()) return NewNameResult("", false, "插入文本不能为空")

                if (isDirectory) {
                    if (pos > currentName.length) return NewNameResult("", false, "从后往前第${pos}位超出文件名长度(${currentName.length})")
                    val insertIndex = currentName.length - pos
                    NewNameResult("${currentName.substring(0, insertIndex)}$insertText${currentName.substring(insertIndex)}")
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        if (pos > baseName.length) return NewNameResult("", false, "从后往前第${pos}位超出文件名主名长度(${baseName.length})")
                        val baseInsertIndex = baseName.length - pos
                        NewNameResult("${baseName.substring(0, baseInsertIndex)}$insertText${baseName.substring(baseInsertIndex)}$extension")
                    } else {
                        if (pos > currentName.length) return NewNameResult("", false, "从后往前第${pos}位超出文件名长度(${currentName.length})")
                        val insertIndex = currentName.length - pos
                        NewNameResult("${currentName.substring(0, insertIndex)}$insertText${currentName.substring(insertIndex)}")
                    }
                }
            }
            RenameType.DELETE_AT_N_FROM_END -> {
                val pos = operation.position
                val count = operation.charCount
                if (pos < 0) return NewNameResult("", false, "删除位置($pos)不能为负数")
                if (count <= 0) return NewNameResult("", false, "删除字符数($count)必须大于0")

                if (isDirectory) {
                    val deleteStart = currentName.length - pos - count
                    if (deleteStart < 0 || deleteStart + count > currentName.length) return NewNameResult("", false, "从后往前第${pos}位删除${count}个字符超出文件名长度(${currentName.length})")
                    NewNameResult("${currentName.substring(0, deleteStart)}${currentName.substring(deleteStart + count)}")
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        val deleteStart = baseName.length - pos - count
                        if (deleteStart < 0 || deleteStart + count > baseName.length) return NewNameResult("", false, "从后往前第${pos}位删除${count}个字符超出文件名主名长度(${baseName.length})")
                        NewNameResult("${baseName.substring(0, deleteStart)}${baseName.substring(deleteStart + count)}$extension")
                    } else {
                        val deleteStart = currentName.length - pos - count
                        if (deleteStart < 0 || deleteStart + count > currentName.length) return NewNameResult("", false, "从后往前第${pos}位删除${count}个字符超出文件名长度(${currentName.length})")
                        NewNameResult("${currentName.substring(0, deleteStart)}${currentName.substring(deleteStart + count)}")
                    }
                }
            }

            RenameType.ADD_ID_PREFIX -> {
                if (isDirectory) {
                    NewNameResult("${idStr}_$currentName")
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        NewNameResult("${idStr}_$baseName$extension")
                    } else {
                        NewNameResult("${idStr}_$currentName")
                    }
                }
            }
            RenameType.ADD_ID_SUFFIX -> {
                if (isDirectory) {
                    NewNameResult("${currentName}_$idStr")
                } else {
                    val dotIndex = currentName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val baseName = currentName.substring(0, dotIndex)
                        val extension = currentName.substring(dotIndex)
                        NewNameResult("${baseName}_$idStr$extension")
                    } else {
                        NewNameResult("${currentName}_$idStr")
                    }
                }
            }
        }
    }
}
