package com.filerenamer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.filerenamer.data.RenameType
import com.filerenamer.ui.components.*
import kotlin.math.roundToInt

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
            // 顶部栏
            if (uiState.isInitialState) {
                TopActionBar(
                    title = "文件批量重命名",
                    onBack = null,
                    accentColor = accentColor,
                )
            } else {
                TopActionBar(
                    title = uiState.currentDirectoryName,
                    onBack = { onGoBack() },
                    accentColor = accentColor,
                ) {
                    IconButton(onClick = {
                        uiState.currentDirectoryUri?.let { uri ->
                            // 刷新由ViewModel处理
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isInitialState -> {
                        InitialContent(
                            onSelectFolder = onSelectFolder,
                            accentColor = accentColor,
                        )
                    }
                    uiState.isLoading || uiState.isRenaming -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (uiState.isRenaming) "正在重命名..." else "加载中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        DraggableFileList(
                            files = uiState.files,
                            onToggleFile = onToggleFile,
                            onEnterDirectory = onEnterDirectory,
                            onReorder = onReorderFiles,
                            accentColor = accentColor,
                        )
                    }
                }

                // 错误提示
                uiState.errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = onClearError) {
                                Text("关闭")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(error)
                    }
                }

                // 结果提示（含失败详情）
                uiState.renameResult?.let { result ->
                    RenameResultCard(
                        summary = result,
                        failDetails = uiState.renameFailDetails,
                        onDismiss = onClearResult,
                        accentColor = accentColor,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }

            // 底部操作栏
            if (!uiState.isInitialState && !uiState.isLoading && !uiState.isRenaming) {
                BottomActionBar(
                    selectedCount = uiState.selectedCount,
                    onSelectAll = onToggleSelectAll,
                    onRename = onShowRenameDialog,
                    isAllSelected = uiState.isAllSelected,
                    accentColor = accentColor,
                )
            }
        }

        // 重命名对话框
        RenameDialog(
            visible = uiState.isRenameDialogVisible,
            renameText = uiState.renameText,
            renameCharCount = uiState.renameCharCount,
            renamePosition = uiState.renamePosition,
            renameType = uiState.renameType,
            onTextChange = onRenameTextChange,
            onCharCountChange = onRenameCharCountChange,
            onPositionChange = onRenamePositionChange,
            onTypeChange = onRenameTypeChange,
            onConfirm = onExecuteRename,
            onDismiss = onHideRenameDialog,
            accentColor = accentColor,
        )
    }
}

/**
 * 可拖拽排序的文件列表
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "此目录为空",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    // 拖拽状态
    var draggedItemIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()

    // 每个item的高度（估算）
    val itemHeight = 72.dp

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Text(
                text = "共 ${files.size} 项（长按拖动排序）",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        itemsIndexed(files, key = { _, file -> file.uri.toString() }) { index, file ->
            val isDragging = draggedItemIndex == index

            // 计算当前item应该偏移的位置
            val offsetY by animateDpAsState(
                targetValue = if (isDragging) dragOffset.dp else 0.dp,
                label = "dragOffset"
            )

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = offsetY.toPx()
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 1.03f else 1f
                        shadowElevation = if (isDragging) 8f else 0f
                    }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedItemIndex = index
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y

                                // 计算当前应该交换到的目标位置
                                val currentPosition = draggedItemIndex
                                if (currentPosition >= 0) {
                                    val itemHeightPx = itemHeight.toPx()
                                    val displacement = dragOffset / itemHeightPx
                                    val targetIndex = (currentPosition + displacement.roundToInt())
                                        .coerceIn(0, files.size - 1)

                                    if (targetIndex != currentPosition) {
                                        // 交换位置
                                        onReorder(currentPosition, targetIndex)
                                        draggedItemIndex = targetIndex
                                        // 重置偏移，因为列表已经重新排列了
                                        dragOffset = 0f
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedItemIndex = -1
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggedItemIndex = -1
                                dragOffset = 0f
                            }
                        )
                    }
            ) {
                FileItemCard(
                    fileItem = file,
                    onToggle = { onToggleFile(file) },
                    onDoubleClick = {
                        if (file.isDirectory) {
                            onEnterDirectory(file)
                        }
                    },
                    accentColor = accentColor,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 重命名结果卡片 - 显示概要并可展开查看失败详情
 */
@Composable
private fun RenameResultCard(
    summary: String,
    failDetails: List<String>,
    onDismiss: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (failDetails.isEmpty())
                accentColor.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 概要行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (failDetails.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (failDetails.isEmpty()) accentColor else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (failDetails.isEmpty())
                        accentColor
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text("关闭", fontSize = 13.sp)
                }
            }

            // 如果有失败详情，显示展开/折叠按钮
            if (failDetails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.padding(start = 34.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (expanded) "收起失败详情" else "查看失败详情（${failDetails.size}条）",
                        fontSize = 13.sp
                    )
                }

                // 失败详情列表（可展开）
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .padding(start = 34.dp, top = 4.dp)
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        failDetails.forEach { detail ->
                            Text(
                                text = "• $detail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 初始页面 - 选择文件夹
 */
@Composable
private fun InitialContent(
    onSelectFolder: () -> Unit,
    accentColor: Color,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // 大图标
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.3f),
                                accentColor.copy(alpha = 0.1f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "文件批量重命名",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "选择一个文件夹，批量添加前缀/后缀\n删除前N个/后N个字符，或替换前N个/后N个字符\n支持内部存储、U盘、SD卡",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onSelectFolder,
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.copy(alpha = 0.2f),
                    contentColor = accentColor,
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "选择文件夹",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = accentColor.copy(alpha = 0.05f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 点击「选择文件夹」按钮\n" +
                                "2. 在系统文件选择器中找到目标文件夹\n" +
                                "3. 浏览文件夹，勾选要重命名的文件和文件夹\n" +
                                "4. 长按文件可拖动排序列表\n" +
                                "5. 点击「批量重命名」选择操作类型\n" +
                                "6. 支持：添加前缀、添加后缀、删除前N个、\n" +
                                "   删除后N个、替换前N个、替换后N个、\n" +
                                "   从前往后第N位插入、从前往后第N位删除、\n" +
                                "   从后往前第N位插入、从后往前第N位删除",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}
