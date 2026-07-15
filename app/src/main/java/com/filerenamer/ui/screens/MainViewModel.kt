package com.filerenamer.ui.screens

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filerenamer.data.FileItem
import com.filerenamer.data.RenameOperation
import com.filerenamer.data.RenameType
import com.filerenamer.ui.theme.extractWallpaperColor
import com.filerenamer.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val currentDirectoryUri: Uri? = null,
    val currentDirectoryName: String = "",
    val files: List<FileItem> = emptyList(),
    val selectedCount: Int = 0,
    val isAllSelected: Boolean = false,
    val isLoading: Boolean = false,
    val wallpaperColor: androidx.compose.ui.graphics.Color? = null,
    val renameText: String = "",
    val renameCharCount: String = "",
    val renamePosition: String = "",
    val renameType: RenameType = RenameType.ADD_PREFIX,
    val isRenameDialogVisible: Boolean = false,
    val isRenaming: Boolean = false,
    val renameProgress: Int = 0,
    val renameTotal: Int = 0,
    val renameResult: String? = null,
    val renameFailDetails: List<String> = emptyList(),
    val errorMessage: String? = null,
    val isInitialState: Boolean = true,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init { loadWallpaperColor() }

    private fun loadWallpaperColor() {
        viewModelScope.launch {
            val color = extractWallpaperColor(getApplication())
            _uiState.value = _uiState.value.copy(wallpaperColor = color)
        }
    }

    fun onFolderSelected(uri: Uri) {
        val context = getApplication<Application>()
        val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try { context.contentResolver.takePersistableUriPermission(uri, takeFlags) } catch (_: Exception) {}
        _uiState.value = _uiState.value.copy(
            currentDirectoryUri = uri, currentDirectoryName = getDisplayName(uri),
            files = emptyList(), selectedCount = 0, isAllSelected = false,
            isInitialState = false, errorMessage = null,
        )
        loadDirectory(uri)
    }

    private fun getDisplayName(uri: Uri): String {
        val path = uri.path ?: return "未知目录"
        val segments = path.split("/")
        return Uri.decode(segments.lastOrNull() ?: "").removePrefix("primary:").ifEmpty { "根目录" }
    }

    fun loadDirectory(uri: Uri) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val files = FileUtils.listDirectory(getApplication(), uri)
                _uiState.value = _uiState.value.copy(files = files, isLoading = false, selectedCount = 0, isAllSelected = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "无法读取目录: ${e.message}")
            }
        }
    }

    fun enterDirectory(fileItem: FileItem) {
        if (fileItem.isDirectory) {
            _uiState.value = _uiState.value.copy(
                currentDirectoryUri = fileItem.uri, currentDirectoryName = fileItem.name,
                files = emptyList(), selectedCount = 0, isAllSelected = false,
            )
            loadDirectory(fileItem.uri)
        }
    }

    fun goBack() {
        val currentUri = _uiState.value.currentDirectoryUri ?: run {
            _uiState.value = _uiState.value.copy(isInitialState = true); return
        }
        val parentStr = currentUri.toString().substringBeforeLast("/")
        if (parentStr.length < currentUri.toString().length && parentStr.isNotEmpty()) {
            val parentUri = Uri.parse(parentStr)
            _uiState.value = _uiState.value.copy(currentDirectoryUri = parentUri, files = emptyList())
            loadDirectory(parentUri)
            val name = parentUri.path?.split("/")?.lastOrNull()?.let { Uri.decode(it) }?.removePrefix("primary:") ?: "根目录"
            _uiState.value = _uiState.value.copy(currentDirectoryName = name)
        } else {
            _uiState.value = _uiState.value.copy(currentDirectoryUri = null, currentDirectoryName = "", files = emptyList(), isInitialState = true)
        }
    }

    fun toggleFileSelection(fileItem: FileItem) {
        val updatedFiles = _uiState.value.files.map { if (it.uri == fileItem.uri) it.copy(isSelected = !it.isSelected) else it }
        val selectedCount = updatedFiles.count { it.isSelected }
        _uiState.value = _uiState.value.copy(files = updatedFiles, selectedCount = selectedCount, isAllSelected = selectedCount == updatedFiles.size && updatedFiles.isNotEmpty())
    }

    fun toggleSelectAll() {
        val newSelectAll = !_uiState.value.isAllSelected
        val updatedFiles = _uiState.value.files.map { it.copy(isSelected = newSelectAll) }
        _uiState.value = _uiState.value.copy(files = updatedFiles, selectedCount = if (newSelectAll) updatedFiles.size else 0, isAllSelected = newSelectAll)
    }

    fun reorderFiles(fromIndex: Int, toIndex: Int) {
        val currentFiles = _uiState.value.files.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentFiles.size || toIndex < 0 || toIndex >= currentFiles.size || fromIndex == toIndex) return
        val item = currentFiles.removeAt(fromIndex)
        currentFiles.add(toIndex, item)
        _uiState.value = _uiState.value.copy(files = currentFiles)
    }

    fun showRenameDialog() {
        _uiState.value = _uiState.value.copy(isRenameDialogVisible = true, renameResult = null, renameFailDetails = emptyList(), renameText = "", renameCharCount = "", renamePosition = "")
    }

    fun hideRenameDialog() {
        _uiState.value = _uiState.value.copy(isRenameDialogVisible = false, renameText = "", renameCharCount = "", renamePosition = "")
    }

    fun setRenameText(text: String) { _uiState.value = _uiState.value.copy(renameText = text) }
    fun setRenameCharCount(count: String) { _uiState.value = _uiState.value.copy(renameCharCount = count.filter { it.isDigit() }) }
    fun setRenamePosition(position: String) { _uiState.value = _uiState.value.copy(renamePosition = position.filter { it.isDigit() }) }
    fun setRenameType(type: RenameType) { _uiState.value = _uiState.value.copy(renameType = type) }

    fun executeRename() {
        val state = _uiState.value
        val parentUri = state.currentDirectoryUri ?: return

        when (state.renameType) {
            RenameType.ADD_PREFIX, RenameType.ADD_SUFFIX -> {
                if (state.renameText.isBlank()) { _uiState.value = state.copy(errorMessage = "请输入要添加的文本"); return }
            }
            RenameType.REMOVE_FIRST_N, RenameType.REMOVE_LAST_N -> {
                val count = state.renameCharCount.toIntOrNull()
                if (count == null || count <= 0) { _uiState.value = state.copy(errorMessage = "请输入要删除的字符数（大于0）"); return }
            }
            RenameType.REPLACE_FIRST_N, RenameType.REPLACE_LAST_N -> {
                val count = state.renameCharCount.toIntOrNull()
                if (count == null || count <= 0) { _uiState.value = state.copy(errorMessage = "请输入要替换的字符数（大于0）"); return }
                if (state.renameText.isBlank()) { _uiState.value = state.copy(errorMessage = "请输入替换后的文本"); return }
            }
            RenameType.INSERT_AT_N_FROM_START, RenameType.INSERT_AT_N_FROM_END -> {
                val pos = state.renamePosition.toIntOrNull()
                if (pos == null || pos < 0) { _uiState.value = state.copy(errorMessage = "请输入有效的位置（从0开始）"); return }
                if (state.renameText.isBlank()) { _uiState.value = state.copy(errorMessage = "请输入要插入的文本"); return }
            }
            RenameType.DELETE_AT_N_FROM_START, RenameType.DELETE_AT_N_FROM_END -> {
                val pos = state.renamePosition.toIntOrNull()
                if (pos == null || pos < 0) { _uiState.value = state.copy(errorMessage = "请输入有效的位置（从0开始）"); return }
                val count = state.renameCharCount.toIntOrNull()
                if (count == null || count <= 0) { _uiState.value = state.copy(errorMessage = "请输入要删除的字符数（大于0）"); return }
            }
            RenameType.ADD_ID_PREFIX, RenameType.ADD_ID_SUFFIX -> { }
            RenameType.REPLACE_AT_N_FROM_START, RenameType.REPLACE_AT_N_FROM_END -> {
                val pos = state.renamePosition.toIntOrNull()
                if (pos == null || pos < 0) { _uiState.value = state.copy(errorMessage = "请输入有效的位置（从0开始）"); return }
                val count = state.renameCharCount.toIntOrNull()
                if (count == null || count <= 0) { _uiState.value = state.copy(errorMessage = "请输入要替换的字符数（大于0）"); return }
                if (state.renameText.isBlank()) { _uiState.value = state.copy(errorMessage = "请输入替换后的文本"); return }
            }
            RenameType.REPLACE_SPACE -> { }
        }

        val selectedItems = state.files.filter { it.isSelected }
        if (selectedItems.isEmpty()) { _uiState.value = state.copy(errorMessage = "请先选择文件"); return }

        _uiState.value = state.copy(isRenaming = true, isRenameDialogVisible = false, errorMessage = null, renameProgress = 0, renameTotal = selectedItems.size)

        viewModelScope.launch {
            try {
                val charCount = state.renameCharCount.toIntOrNull() ?: 0
                val position = state.renamePosition.toIntOrNull() ?: 0
                val operation = RenameOperation(state.renameType, state.renameText, charCount, position)
                val result = FileUtils.batchRename(getApplication(), parentUri, selectedItems, operation,
                    onProgress = { current, total -> _uiState.value = _uiState.value.copy(renameProgress = current, renameTotal = total) }
                )
                _uiState.value = _uiState.value.copy(
                    renameResult = "重命名完成：成功 ${result.successCount} 个，失败 ${result.failCount} 个",
                    renameFailDetails = result.failReasons, isRenaming = false,
                )
                loadDirectory(parentUri)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRenaming = false, errorMessage = "重命名出错: ${e.message}")
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
    fun clearRenameResult() { _uiState.value = _uiState.value.copy(renameResult = null, renameFailDetails = emptyList()) }
}
