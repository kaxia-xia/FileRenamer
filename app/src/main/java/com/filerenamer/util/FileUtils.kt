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
        items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    /**
     * 批量重命名 - 在 IO 线程执行
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

        val parentDir = DocumentFile.fromTreeUri(context, parentUri)
        if (parentDir == null || !parentDir.exists()) {
            return@withContext RenameResult(0, items.size, listOf("无法访问父目录"))
        }

        val idPadLen = when {
            items.size < 10 -> 1
            items.size < 100 -> 2
            items.size < 1000 -> 3
            items.size < 10000 -> 4
            else -> 5
        }

        val total = items.size

        for ((index, item) in items.withIndex()) {
            onProgress?.invoke(index + 1, total)
            try {
                val file = parentDir.findFile(item.name)
                if (file == null || !file.exists()) {
                    failCount++; failReasons.add("「${item.name}」找不到该文件"); continue
                }
                val id = index + 1
                val idStr = id.toString().padStart(idPadLen, '0')
                val newNameResult = computeNewName(item.name, file.isDirectory, operation, idStr)
                if (newNameResult == null) { failCount++; failReasons.add("「${item.name}」参数错误"); continue }
                if (!newNameResult.isSuccess) { failCount++; failReasons.add("「${item.name}」${newNameResult.errorMsg}"); continue }
                val newName = newNameResult.name
                if (newName == item.name) { failCount++; failReasons.add("「${item.name}」新文件名与原名相同"); continue }
                if (file.renameTo(newName)) successCount++
                else { failCount++; failReasons.add("「${item.name}」重命名失败") }
            } catch (e: Exception) {
                failCount++; failReasons.add("「${item.name}」${e.message ?: "未知错误"}")
            }
        }
        RenameResult(successCount, failCount, failReasons)
    }

    private data class NewNameResult(val name: String, val isSuccess: Boolean = true, val errorMsg: String = "")

    /** 将文件名拆分为主名和扩展名 */
    private fun splitName(name: String, isDirectory: Boolean): Pair<String, String> {
        if (isDirectory) return Pair(name, "")
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex > 0) Pair(name.substring(0, dotIndex), name.substring(dotIndex)) else Pair(name, "")
    }

    private fun computeNewName(
        currentName: String, isDirectory: Boolean, operation: RenameOperation, idStr: String = ""
    ): NewNameResult? {
        return when (operation.type) {
        RenameType.ADD_PREFIX -> NewNameResult("${operation.text}$currentName")

        RenameType.ADD_SUFFIX -> {
            val (base, ext) = splitName(currentName, isDirectory)
            NewNameResult("$base${operation.text}$ext")
        }

        RenameType.REMOVE_FIRST_N -> {
            val count = operation.charCount
            if (count <= 0) return null
            val (base, ext) = splitName(currentName, isDirectory)
            if (count >= base.length) return NewNameResult("", false, "文件名主名长度(${base.length}) ≤ 要删除的字符数($count)")
            NewNameResult("${base.substring(count)}$ext")
        }

        RenameType.REMOVE_LAST_N -> {
            val count = operation.charCount
            if (count <= 0) return null
            val (base, ext) = splitName(currentName, isDirectory)
            if (count >= base.length) return NewNameResult("", false, "文件名主名长度(${base.length}) ≤ 要删除的字符数($count)")
            NewNameResult("${base.substring(0, base.length - count)}$ext")
        }

        RenameType.REPLACE_FIRST_N -> {
            val count = operation.charCount; val replacement = operation.text
            if (count <= 0) return null
            val (base, ext) = splitName(currentName, isDirectory)
            if (count >= base.length) return NewNameResult("", false, "文件名主名长度(${base.length}) ≤ 要替换的字符数($count)")
            NewNameResult("$replacement${base.substring(count)}$ext")
        }

        RenameType.REPLACE_LAST_N -> {
            val count = operation.charCount; val replacement = operation.text
            if (count <= 0) return null
            val (base, ext) = splitName(currentName, isDirectory)
            if (count >= base.length) return NewNameResult("", false, "文件名主名长度(${base.length}) ≤ 要替换的字符数($count)")
            NewNameResult("${base.substring(0, base.length - count)}$replacement$ext")
        }

        RenameType.INSERT_AT_N_FROM_START -> {
            val pos = operation.position; val insertText = operation.text
            if (pos < 0) return NewNameResult("", false, "插入位置不能为负数")
            if (insertText.isEmpty()) return NewNameResult("", false, "插入文本不能为空")
            val (base, ext) = splitName(currentName, isDirectory)
            if (pos > base.length) return NewNameResult("", false, "插入位置($pos)超出文件名主名长度(${base.length})")
            NewNameResult("${base.substring(0, pos)}$insertText${base.substring(pos)}$ext")
        }

        RenameType.DELETE_AT_N_FROM_START -> {
            val pos = operation.position; val count = operation.charCount
            if (pos < 0) return NewNameResult("", false, "删除起始位置不能为负数")
            if (count <= 0) return NewNameResult("", false, "删除字符数必须大于0")
            val (base, ext) = splitName(currentName, isDirectory)
            if (pos + count > base.length) return NewNameResult("", false, "从位置${pos}删除${count}个字符超出文件名主名长度(${base.length})")
            NewNameResult("${base.substring(0, pos)}${base.substring(pos + count)}$ext")
        }

        RenameType.INSERT_AT_N_FROM_END -> {
            val pos = operation.position; val insertText = operation.text
            if (pos < 0) return NewNameResult("", false, "插入位置不能为负数")
            if (insertText.isEmpty()) return NewNameResult("", false, "插入文本不能为空")
            val (base, ext) = splitName(currentName, isDirectory)
            if (pos > base.length) return NewNameResult("", false, "从后往前第${pos}位超出文件名主名长度(${base.length})")
            val insertIndex = base.length - pos
            NewNameResult("${base.substring(0, insertIndex)}$insertText${base.substring(insertIndex)}$ext")
        }

        RenameType.DELETE_AT_N_FROM_END -> {
            val pos = operation.position; val count = operation.charCount
            if (pos < 0) return NewNameResult("", false, "删除位置不能为负数")
            if (count <= 0) return NewNameResult("", false, "删除字符数必须大于0")
            val (base, ext) = splitName(currentName, isDirectory)
            val deleteStart = base.length - pos - count
            if (deleteStart < 0 || deleteStart + count > base.length) return NewNameResult("", false, "从后往前第${pos}位删除${count}个字符超出文件名主名长度(${base.length})")
            NewNameResult("${base.substring(0, deleteStart)}${base.substring(deleteStart + count)}$ext")
        }

        RenameType.ADD_ID_PREFIX -> {
            val (base, ext) = splitName(currentName, isDirectory)
            NewNameResult("${idStr}_$base$ext")
        }

        RenameType.ADD_ID_SUFFIX -> {
            val (base, ext) = splitName(currentName, isDirectory)
            NewNameResult("${base}_$idStr$ext")
        }

        RenameType.REPLACE_AT_N_FROM_START -> {
            val pos = operation.position; val count = operation.charCount; val replacement = operation.text
            if (pos < 0) return NewNameResult("", false, "替换起始位置不能为负数")
            if (count <= 0) return NewNameResult("", false, "替换字符数必须大于0")
            val (base, ext) = splitName(currentName, isDirectory)
            if (pos + count > base.length) return NewNameResult("", false, "从位置${pos}替换${count}个字符超出文件名主名长度(${base.length})")
            NewNameResult("${base.substring(0, pos)}$replacement${base.substring(pos + count)}$ext")
        }

        RenameType.REPLACE_AT_N_FROM_END -> {
            val pos = operation.position; val count = operation.charCount; val replacement = operation.text
            if (pos < 0) return NewNameResult("", false, "替换位置不能为负数")
            if (count <= 0) return NewNameResult("", false, "替换字符数必须大于0")
            val (base, ext) = splitName(currentName, isDirectory)
            val replaceStart = base.length - pos - count
            if (replaceStart < 0 || replaceStart + count > base.length) return NewNameResult("", false, "从后往前第${pos}位替换${count}个字符超出文件名主名长度(${base.length})")
            NewNameResult("${base.substring(0, replaceStart)}$replacement${base.substring(replaceStart + count)}$ext")
        }

        RenameType.REPLACE_SPACE -> {
            val replacement = operation.text
            val (base, ext) = splitName(currentName, isDirectory)
            NewNameResult("${base.replace(" ", replacement)}$ext")
        }
    }

}
}
