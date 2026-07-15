package com.filerenamer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.filerenamer.data.RenameType
import com.filerenamer.ui.components.*
import sh.calvin.reorderable.*

@Composable
fun MainScreen(
    uiState: MainUiState,
    onSelectFolder: () -> Unit,
    onEnterDirectory: (com.filerenamer.data.FileItem) -> Unit,
    onGoBack: () -> Unit,
    onToggleFile: (com.filerenamer.data.FileItem) -> Unit,
    onToggleSelectAll: () -> Unit,
    onReorderFiles: (Int, Int) -> Unit,
    onShowRenameDialog: () -> Unit,
    onHideRenameDialog: () -> Unit,
    onRenameTextChange: (String) -> Unit,
    onRenameCharCountChange: (String) -> Unit,
    onRenamePositionChange: (String) -> Unit,
    onRenameTypeChange: (RenameType) -> Unit,
    onExecuteRename: () -> Unit,
    onClearError: () -> Unit,
    onClearResult: () -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isInitialState) {
                TopActionBar(title = "文件批量重命名", onBack = null, accentColor = accentColor)
            } else {
                TopActionBar(
                    title = uiState.currentDirectoryName,
                    onBack = { onGoBack() },
                    accentColor = accentColor,
                ) {
                    IconButton(onClick = { uiState.currentDirectoryUri?.let { } }) {
                        Icon(Icons.Default.Refresh, "刷新", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isInitialState -> InitialContent(onSelectFolder = onSelectFolder, accentColor = accentColor)
                    uiState.isLoading || uiState.isRenaming -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    if (uiState.isRenaming) "正在重命名..." else "加载中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> DraggableFileList(
                        files = uiState.files,
                        onToggleFile = onToggleFile,
                        onEnterDirectory = onEnterDirectory,
                        onReorder = onReorderFiles,
                        accentColor = accentColor,
                    )
                }

                uiState.errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        action = { TextButton(onClick = onClearError) { Text("关闭") } },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(error) }
                }

                uiState.renameResult?.let { result ->
                    RenameResultCard(
                        summary = result, failDetails = uiState.renameFailDetails,
                        onDismiss = onClearResult, accentColor = accentColor,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }

            if (!uiState.isInitialState && !uiState.isLoading && !uiState.isRenaming) {
                BottomActionBar(
                    selectedCount = uiState.selectedCount, onSelectAll = onToggleSelectAll,
                    onRename = onShowRenameDialog, isAllSelected = uiState.isAllSelected,
                    accentColor = accentColor,
                )
            }
        }

        RenameDialog(
            visible = uiState.isRenameDialogVisible, renameText = uiState.renameText,
            renameCharCount = uiState.renameCharCount, renamePosition = uiState.renamePosition,
            renameType = uiState.renameType, onTextChange = onRenameTextChange,
            onCharCountChange = onRenameCharCountChange, onPositionChange = onRenamePositionChange,
            onTypeChange = onRenameTypeChange, onConfirm = onExecuteRename,
            onDismiss = onHideRenameDialog, accentColor = accentColor,
        )
    }
}

/**
 * 可拖拽排序的文件列表 - 使用 sh.calvin.reorderable 库
 *
 * 每个 item 左侧有一个拖拽手柄图标，长按手柄即可拖动排序。
 * 点击 item 其他区域触发勾选/取消勾选。
 *
 * 注意：LazyColumn 中标题 item 占 index 0，所以文件 item 的 index 需要减 1。
 */
@Composable
private fun DraggableFileList(
    files: List<com.filerenamer.data.FileItem>,
    onToggleFile: (com.filerenamer.data.FileItem) -> Unit,
    onEnterDirectory: (com.filerenamer.data.FileItem) -> Unit,
    onReorder: (Int, Int) -> Unit,
    accentColor: Color,
) {
    if (files.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FolderOpen, null, Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))
                Text("此目录为空", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
        return
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // from.index 和 to.index 是 LazyColumn 中的 index（包含标题 item 0）
        // 文件列表的 index 需要减 1
        val fromFileIdx = from.index - 1
        val toFileIdx = to.index - 1
        if (fromFileIdx >= 0 && toFileIdx >= 0 && fromFileIdx < files.size && toFileIdx < files.size) {
            onReorder(fromFileIdx, toFileIdx)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Text(
                text = "共 ${files.size} 项（拖拽手柄排序）",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        itemsIndexed(files, key = { _, file -> file.uri.toString() }) { index, file ->
            ReorderableItem(
                reorderableLazyListState,
                key = file.uri.toString(),
            ) { isDragging ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            scaleX = if (isDragging) 1.03f else 1f
                            scaleY = if (isDragging) 1.03f else 1f
                            shadowElevation = if (isDragging) 10f else 0f
                        }
                ) {
                    FileItemCard(
                        fileItem = file,
                        onToggle = { onToggleFile(file) },
                        onDoubleClick = {
                            if (file.isDirectory) onEnterDirectory(file)
                        },
                        onDragHandle = { Modifier.draggableHandle() },
                        accentColor = accentColor,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun RenameResultCard(
    summary: String, failDetails: List<String>,
    onDismiss: () -> Unit, accentColor: Color, modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (failDetails.isEmpty()) accentColor.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    if (failDetails.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning, null,
                    tint = if (failDetails.isEmpty()) accentColor else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = if (failDetails.isEmpty()) accentColor else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("关闭", fontSize = 13.sp) }
            }
            if (failDetails.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(start = 34.dp)) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (expanded) "收起失败详情" else "查看失败详情（${failDetails.size}条）", fontSize = 13.sp)
                }
                AnimatedVisibility(visible = expanded) {
                    Column(Modifier.padding(start = 34.dp, top = 4.dp).heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        failDetails.forEach { detail ->
                            Text("• $detail", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                lineHeight = 20.sp, modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialContent(onSelectFolder: () -> Unit, accentColor: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(100.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.3f), accentColor.copy(alpha = 0.1f)))),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FolderOpen, null, tint = accentColor, modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("文件批量重命名", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("选择一个文件夹，批量添加前缀/后缀\n删除前N个/后N个字符，或替换前N个/后N个字符\n支持内部存储、U盘、SD卡",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, lineHeight = 24.sp)
            Spacer(Modifier.height(40.dp))
            Button(onClick = onSelectFolder, modifier = Modifier.height(56.dp).clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f), contentColor = accentColor),
                shape = RoundedCornerShape(16.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)) {
                Icon(Icons.Default.Folder, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("选择文件夹", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }
            Spacer(Modifier.height(48.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.05f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("使用说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text("1. 点击「选择文件夹」按钮\n2. 在系统文件选择器中找到目标文件夹\n3. 浏览文件夹，勾选要重命名的文件和文件夹\n4. 拖拽手柄排序列表\n5. 点击「批量重命名」选择操作类型\n6. 支持：添加前缀、添加后缀、删除前N个、\n   删除后N个、替换前N个、替换后N个、\n   从前往后第N位插入、从前往后第N位删除、\n   从后往前第N位插入、从后往前第N位删除",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                }
            }
        }
    }
}
